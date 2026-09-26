package com.regentmediagroup.embertv.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Talks to the Ember TV API v2 (the web app's /v2 routes). Sign-in is by
 * activation code: the TV shows a short code, the viewer approves it at
 * app.emberstreaming.com/activate, and the TV receives a normal session
 * (access + refresh token) that it keeps in private app storage and refreshes.
 */
sealed class EmberApiException(message: String) : Exception(message) {
    class SignedOut : EmberApiException("Please sign in again.")
    class Network : EmberApiException(
        "Can't reach Ember TV. Check your internet connection and try again."
    )
    class Server(val status: Int, val code: String?, serverMessage: String?) : EmberApiException(
        serverMessage ?: "Something went wrong. Please try again in a moment."
    )
}

/** Result of one activation poll. */
sealed class ActivationPoll {
    /** Not approved yet: poll again after [interval] seconds. */
    data class Pending(val interval: Int) : ActivationPoll()
    /** Approved: the session is stored and [EmberApiClient.signedIn] is now true. */
    object Approved : ActivationPoll()
    /** The code expired, was already used or is unknown: get a new one. */
    object Restart : ActivationPoll()
}

object EmberApiClient {

    private const val SESSION_PREFS = "EmberSession"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_EXPIRES = "expires_at_millis"
    private const val DEVICE_PREFS = "EmberDevice"
    private const val KEY_DEVICE_ID = "device_id"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Fire-and-forget work (progress reports, sign-out) that outlives a screen. */
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** One refresh at a time: Supabase rotates refresh tokens, so two
     *  concurrent refreshes with the same token would sign the TV out. */
    private val refreshLock = Mutex()

    private lateinit var prefs: SharedPreferences
    private var session: AuthSession? = null

    private val _signedIn = MutableStateFlow(false)

    /** Drives the root screen: activation vs library. */
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    /** Stable per install; identifies this TV for playback and resume. */
    lateinit var deviceId: String
        private set

    /** Call once from MainActivity.onCreate before anything else. */
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val app = context.applicationContext

        // State from the old Base44 app; none of it is valid any more.
        app.getSharedPreferences("EmberPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        app.getSharedPreferences("EmberPlaybackState", Context.MODE_PRIVATE).edit().clear().apply()

        val devicePrefs = app.getSharedPreferences(DEVICE_PREFS, Context.MODE_PRIVATE)
        deviceId = devicePrefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            devicePrefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }

        prefs = app.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        val access = prefs.getString(KEY_ACCESS, null)
        val refresh = prefs.getString(KEY_REFRESH, null)
        if (access != null && refresh != null) {
            session = AuthSession(access, refresh, prefs.getLong(KEY_EXPIRES, 0L))
        }
        _signedIn.value = session != null
    }

    // --- Activation ---

    suspend fun startActivation(): DeviceCodeResponse {
        val body = mapOf("client" to EmberConfig.CLIENT, "device_id" to deviceId)
        val result = send(apiRequest("v2/device/code", "POST", body))
        if (result.status != 200) throw serverError(result)
        val code = decode<DeviceCodeResponse>(result)
        if (code.deviceCode.isNullOrEmpty() || code.userCode.isNullOrEmpty()) {
            throw EmberApiException.Server(result.status, null, null)
        }
        return code
    }

    suspend fun pollActivation(deviceCode: String): ActivationPoll {
        val result = send(apiRequest("v2/device/token", "POST", mapOf("device_code" to deviceCode)))
        return when (result.status) {
            200 -> {
                val token = decode<DeviceTokenResponse>(result)
                val access = token.accessToken
                val refresh = token.refreshToken
                if (access.isNullOrEmpty() || refresh.isNullOrEmpty()) {
                    throw EmberApiException.Server(200, null, null)
                }
                store(AuthSession(access, refresh, expiresAt(token.expiresIn)))
                ActivationPoll.Approved
            }
            202 -> {
                val pending = runCatching { decode<DeviceTokenResponse>(result) }.getOrNull()
                ActivationPoll.Pending(pending?.interval ?: 5)
            }
            400, 409, 410 -> ActivationPoll.Restart
            else -> throw serverError(result)
        }
    }

    // --- Sign out ---

    fun signOut() {
        val token = session?.accessToken
        if (token != null) {
            // Best effort: revoke this TV's refresh token on the server.
            backgroundScope.launch {
                runCatching {
                    val request = Request.Builder()
                        .url(EmberConfig.SUPABASE_URL + "auth/v1/logout?scope=local")
                        .header("apikey", EmberConfig.SUPABASE_PUBLISHABLE_KEY)
                        .header("Authorization", "Bearer $token")
                        .post("{}".toRequestBody(jsonType))
                        .build()
                    http.newCall(request).execute().close()
                }
            }
        }
        clearSession()
    }

    // --- Library, films, playback, progress ---

    suspend fun fetchLibrary(): List<Rental> {
        val result = authorized("v2/library")
        if (result.status != 200) throw serverError(result)
        val items = decode<LibraryResponse>(result).items.orEmpty()
        return items.mapNotNull { item ->
            val entitlement = item.entitlement ?: return@mapNotNull null
            val film = item.film ?: return@mapNotNull null
            // Gson can leave "non-null" fields null when the server omits them.
            if (film.id == null || film.slug == null || film.title == null) return@mapNotNull null
            Rental(entitlement, film, item.screening, item.resume)
        }
    }

    /** Public film details (descriptions, genres). No token needed. */
    suspend fun fetchFilm(slug: String): FilmDetail? {
        val result = send(apiRequest("v2/films/$slug"))
        if (result.status != 200) throw serverError(result)
        return decode<FilmDetailResponse>(result).film
    }

    /** A fresh signed HLS URL for this device. Request one on every play. */
    suspend fun startPlayback(filmId: String): String {
        val body = mapOf("client" to EmberConfig.CLIENT, "device_id" to deviceId)
        val result = authorized("v2/playback/$filmId", "POST", body)
        if (result.status != 200) throw serverError(result)
        return decode<PlaybackResponse>(result).playback?.url
            ?: throw EmberApiException.Server(result.status, "playback_not_available", null)
    }

    /**
     * Resume position, shared across the viewer's devices. Runs in the
     * background and ignores failures: progress is a convenience and must
     * never interrupt playback.
     */
    fun reportProgress(filmId: String, seconds: Long) {
        if (seconds < 0 || session == null) return
        val body = mapOf(
            "film_id" to filmId,
            "device_id" to deviceId,
            "client" to EmberConfig.CLIENT,
            "position_seconds" to seconds
        )
        backgroundScope.launch {
            runCatching { authorized("v2/progress", "POST", body) }
        }
    }

    // --- Session ---

    private fun expiresAt(expiresInSeconds: Int?): Long =
        System.currentTimeMillis() + (expiresInSeconds ?: 3600) * 1000L

    @Synchronized
    private fun store(newSession: AuthSession) {
        session = newSession
        prefs.edit()
            .putString(KEY_ACCESS, newSession.accessToken)
            .putString(KEY_REFRESH, newSession.refreshToken)
            .putLong(KEY_EXPIRES, newSession.expiresAtMillis)
            .apply()
        _signedIn.value = true
    }

    @Synchronized
    private fun clearSession() {
        session = null
        prefs.edit().clear().apply()
        _signedIn.value = false
    }

    private suspend fun validAccessToken(): String {
        val current = session ?: throw EmberApiException.SignedOut()
        if (current.expiresAtMillis - System.currentTimeMillis() > 60_000L) return current.accessToken
        return refreshSession(current.refreshToken).accessToken
    }

    /** Refreshes unless another caller already did while we waited for the lock. */
    private suspend fun refreshSession(staleRefreshToken: String): AuthSession = refreshLock.withLock {
        val current = session ?: throw EmberApiException.SignedOut()
        if (current.refreshToken != staleRefreshToken) return@withLock current
        val fresh = try {
            performRefresh(current.refreshToken)
        } catch (e: EmberApiException.SignedOut) {
            clearSession()
            throw e
        }
        store(fresh)
        fresh
    }

    private suspend fun performRefresh(refreshToken: String): AuthSession {
        val request = Request.Builder()
            .url(EmberConfig.SUPABASE_URL + "auth/v1/token?grant_type=refresh_token")
            .header("apikey", EmberConfig.SUPABASE_PUBLISHABLE_KEY)
            .post(gson.toJson(mapOf("refresh_token" to refreshToken)).toRequestBody(jsonType))
            .build()
        // Offline: send() throws Network and the session is kept for later.
        val result = send(request)
        return when (result.status) {
            200 -> {
                val r = decode<RefreshResponse>(result)
                val access = r.accessToken
                val refresh = r.refreshToken
                if (access.isNullOrEmpty() || refresh.isNullOrEmpty()) {
                    throw EmberApiException.Server(200, null, null)
                }
                AuthSession(access, refresh, expiresAt(r.expiresIn))
            }
            // Revoked or expired: sign in again.
            400, 401, 403 -> throw EmberApiException.SignedOut()
            else -> throw EmberApiException.Server(result.status, null, null)
        }
    }

    // --- Requests ---

    private class HttpResult(val status: Int, val body: String)

    /** Sends with the access token; on a 401, refreshes once and retries. */
    private suspend fun authorized(
        path: String,
        method: String = "GET",
        body: Map<String, Any>? = null
    ): HttpResult {
        val token = validAccessToken()
        val refreshUsed = session?.refreshToken ?: throw EmberApiException.SignedOut()
        val first = send(apiRequest(path, method, body, token))
        if (first.status != 401) return first

        val fresh = refreshSession(refreshUsed)
        val second = send(apiRequest(path, method, body, fresh.accessToken))
        if (second.status == 401) {
            clearSession()
            throw EmberApiException.SignedOut()
        }
        return second
    }

    private fun apiRequest(
        path: String,
        method: String = "GET",
        body: Map<String, Any>? = null,
        token: String? = null
    ): Request {
        val builder = Request.Builder()
            .url(EmberConfig.API_BASE_URL + path)
            .header("Accept", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        if (method == "POST") {
            builder.post(gson.toJson(body ?: emptyMap<String, Any>()).toRequestBody(jsonType))
        } else {
            builder.get()
        }
        return builder.build()
    }

    private suspend fun send(request: Request): HttpResult = withContext(Dispatchers.IO) {
        try {
            http.newCall(request).execute().use { response ->
                HttpResult(response.code, response.body?.string().orEmpty())
            }
        } catch (e: IOException) {
            throw EmberApiException.Network()
        }
    }

    // --- Decoding ---

    private fun serverError(result: HttpResult): EmberApiException {
        val envelope = try {
            gson.fromJson(result.body, ApiErrorEnvelope::class.java)
        } catch (e: JsonParseException) {
            null
        }
        return EmberApiException.Server(result.status, envelope?.error?.code, envelope?.error?.message)
    }

    private inline fun <reified T> decode(result: HttpResult): T {
        val value: T? = try {
            gson.fromJson(result.body, T::class.java)
        } catch (e: JsonParseException) {
            null
        }
        return value ?: throw EmberApiException.Server(result.status, null, null)
    }
}

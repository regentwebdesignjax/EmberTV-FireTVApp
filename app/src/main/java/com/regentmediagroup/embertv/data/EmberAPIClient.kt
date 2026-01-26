package com.regentmediagroup.embertv.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface EmberApiService {
    @POST("authLogin")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("apiMyRentals")
    suspend fun getMyRentals(@Header("Authorization") token: String): RentalsResponse

    // UPDATED: Pointing to apiFilms
    // We return JsonObject so we can inspect the raw data without crashing
    @GET("api/apps/691721b89e14bc8b401725d6/functions/apiFilms")
    suspend fun getFilmDetailsRaw(@Query("id") id: String): JsonObject
}

object EmberApiClient {
    private const val BASE_URL = "https://embervod.base44.app/api/apps/691721b89e14bc8b401725d6/functions/"
    private const val PREFS_NAME = "EmberPrefs"
    private const val TOKEN_KEY = "EmberAuthToken"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()

    private val api: EmberApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EmberApiService::class.java)
    }

    suspend fun login(context: Context, email: String, pass: String): Boolean {
        return try {
            val response = api.login(LoginRequest(email, pass))
            if (response.token.isNotEmpty()) {
                saveToken(context, response.token)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchMyRentals(context: Context): List<Rental> {
        val token = getToken(context) ?: return emptyList()
        return try {
            val response = api.getMyRentals("Bearer $token")
            response.data
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // UPDATED FUNCTION: Manually parses the apiFilms response
    suspend fun fetchFilmDetails(filmId: String): RentalFilmSummary? {
        return try {
            Log.d("EmberDebug", "Fetching details for ID: $filmId from apiFilms...")
            val rawJson = api.getFilmDetailsRaw(filmId)

            Log.d("EmberDebug", "RAW RESPONSE: $rawJson")

            // Base44 usually returns a list for plural endpoints.
            // We check if "data" exists and is an array, OR if the root itself is the object.

            val targetJson = if (rawJson.has("data") && rawJson.get("data").isJsonArray) {
                // Case A: { "data": [ { ... } ] }
                val array = rawJson.getAsJsonArray("data")
                if (array.size() > 0) array.get(0).asJsonObject else null
            } else if (rawJson.has("id")) {
                // Case B: It returned the single object directly
                rawJson
            } else {
                // Case C: Maybe just a raw array?
                null
            }

            if (targetJson != null) {
                // Manually parse to ensure we get the fields we need
                val gson = Gson()
                val summary = gson.fromJson(targetJson, RentalFilmSummary::class.java)

                // Double check short_description specifically
                if (targetJson.has("short_description") && !targetJson.get("short_description").isJsonNull) {
                    val manualShort = targetJson.get("short_description").asString
                    Log.d("EmberDebug", "Manual Parse Short Desc: $manualShort")
                    // Return a copy with the confirmed description if Gson missed it
                    return summary.copy(shortDescription = manualShort)
                }

                return summary
            }
            null
        } catch (e: Exception) {
            Log.e("EmberDebug", "Error parsing film details", e)
            null
        }
    }

    private fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_KEY, null)
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(TOKEN_KEY).apply()
    }
}
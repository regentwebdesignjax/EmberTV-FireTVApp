package com.regentmediagroup.embertv.data

import com.google.gson.annotations.SerializedName
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Shapes from the Ember TV API v2 (docs/API-v2.md in the web app repo).
// Gson fills missing fields with null even when a type says otherwise, so
// wire types are nullable and checked where they are used.

// --- Errors ---

data class ApiErrorEnvelope(val error: ApiErrorBody?)

data class ApiErrorBody(val code: String?, val message: String?)

// --- Activation (sign in with a code) ---

data class DeviceCodeResponse(
    @SerializedName("device_code") val deviceCode: String?,
    @SerializedName("user_code") val userCode: String?,
    @SerializedName("verification_uri") val verificationUri: String?,
    @SerializedName("verification_uri_complete") val verificationUriComplete: String?,
    @SerializedName("expires_in") val expiresIn: Int?,
    @SerializedName("interval") val interval: Int?
)

data class DeviceTokenResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("interval") val interval: Int?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Int?
)

/** Supabase's refresh_token grant response (only the fields we keep). */
data class RefreshResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Int?
)

/** The stored sign-in. Refreshed about a minute before [expiresAtMillis]. */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long
)

// --- Library ---

data class EntitlementSummary(
    @SerializedName("id") val id: String?,
    @SerializedName("film_id") val filmId: String?,
    /** "rental" | "public_performance" */
    @SerializedName("type") val type: String?,
    /** "active" | "expired" | ... */
    @SerializedName("status") val status: String?,
    @SerializedName("starts_at") val startsAt: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("seconds_remaining") val secondsRemaining: Int?
)

data class LibraryFilm(
    @SerializedName("id") val id: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("title") val title: String,
    /** Portrait poster. */
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    /** Wide background art. */
    @SerializedName("banner_image_url") val bannerImageUrl: String?,
    @SerializedName("duration_minutes") val durationMinutes: Int?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("release_year") val releaseYear: Int?
) {
    val posterUrl: String? get() = thumbnailUrl ?: bannerImageUrl
    val backdropUrl: String? get() = bannerImageUrl ?: thumbnailUrl
}

data class ScreeningInfo(
    @SerializedName("venue_name") val venueName: String?,
    /** YYYY-MM-DD, the venue's local date. */
    @SerializedName("screening_date") val screeningDate: String?
)

data class ResumePoint(
    @SerializedName("position_seconds") val positionSeconds: Int?,
    @SerializedName("at") val at: String?,
    @SerializedName("client") val client: String?
)

/** One row of GET /v2/library as it arrives. */
data class LibraryItem(
    @SerializedName("entitlement") val entitlement: EntitlementSummary?,
    @SerializedName("film") val film: LibraryFilm?,
    @SerializedName("screening") val screening: ScreeningInfo?,
    @SerializedName("resume") val resume: ResumePoint?
)

data class LibraryResponse(
    @SerializedName("items") val items: List<LibraryItem>?
)

/** A rental (or screening licence) and its film. */
data class Rental(
    val entitlement: EntitlementSummary,
    val film: LibraryFilm,
    val screening: ScreeningInfo?,
    val resume: ResumePoint?
) {
    val isScreening: Boolean get() = entitlement.type == "public_performance"

    /** Not yet started: a screening licence whose window opens later. */
    fun isUpcoming(now: Long = System.currentTimeMillis()): Boolean {
        val start = EmberDates.parseMillis(entitlement.startsAt) ?: return false
        return start > now
    }

    /** Playable right now, judged by the clock (playback re-checks on the server). */
    fun isWatchable(now: Long = System.currentTimeMillis()): Boolean {
        if (entitlement.status != "active" || isUpcoming(now)) return false
        val end = EmberDates.parseMillis(entitlement.expiresAt) ?: return true
        return end > now
    }

    /** Short line under the poster: time left, upcoming, or ended. */
    fun statusLabel(now: Long = System.currentTimeMillis()): String {
        if (isUpcoming(now)) {
            val date = screening?.screeningDate
            return if (isScreening && date != null) "Screening $date" else "Starts soon"
        }
        if (!isWatchable(now)) return "Expired"
        val end = EmberDates.parseMillis(entitlement.expiresAt) ?: return "Ready to watch"
        val minutes = ((end - now) / 60_000L).coerceAtLeast(1)
        return when {
            minutes >= 24 * 60 -> "${minutes / (24 * 60)}d left"
            minutes >= 60 -> "${minutes / 60}h left"
            else -> "${minutes}m left"
        }
    }
}

// --- Film detail (description for the detail screen) ---

data class FilmDetail(
    @SerializedName("id") val id: String?,
    @SerializedName("short_description") val shortDescription: String?,
    @SerializedName("long_description") val longDescription: String?,
    @SerializedName("genres") val genres: List<String>?
)

data class FilmDetailResponse(
    @SerializedName("film") val film: FilmDetail?
)

// --- Playback ---

data class PlaybackInfo(
    /** "hls" for TV clients. */
    @SerializedName("type") val type: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("expires_at") val expiresAt: String?
)

data class PlaybackResponse(
    @SerializedName("playback") val playback: PlaybackInfo?
)

/**
 * ISO 8601 timestamps as the API sends them: "2026-09-25T04:00:00Z", with
 * milliseconds ("...00.123Z") or Postgres microseconds ("...00.123456+00:00").
 * java.time needs API 26 and Fire OS 6 is API 25, so this uses SimpleDateFormat.
 */
object EmberDates {
    private val fraction = Regex("""\.(\d+)""")

    fun parseMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        var millis = 0L
        val match = fraction.find(value)
        if (match != null) {
            millis = match.groupValues[1].padEnd(3, '0').take(3).toLong()
        }
        val base = fraction.replace(value, "")
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(base)?.time?.plus(millis)
        } catch (e: ParseException) {
            null
        }
    }
}

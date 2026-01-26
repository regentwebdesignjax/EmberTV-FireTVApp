package com.regentmediagroup.embertv.data

import com.google.gson.annotations.SerializedName

// --- Auth Models ---
data class AuthResponse(
    val token: String,
    val user: User? = null
)

data class User(
    val id: String, // <--- CHANGED FROM Int TO String
    val email: String,
    val name: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

// --- Rental Models ---

data class RentalsResponse(
    val data: List<Rental>
)

data class Rental(
    val film: RentalFilmSummary,
    val status: String?,
    @SerializedName("purchased_at") val purchasedAt: String?,
    @SerializedName("expires_at") val expiresAt: String?
)

data class RentalFilmSummary(
    val id: String,
    val title: String,
    val slug: String?,
    val genre: String?,
    @SerializedName("poster_url") val posterUrl: String?,
    @SerializedName("hls_url") val hlsUrl: String?,
    // We try to grab "description" (long) first, but now also "short_description"
    @SerializedName("description") val longDescription: String?,
    @SerializedName("short_description") val shortDescription: String?,
    @SerializedName("duration_minutes") val durationMinutes: Int?
)
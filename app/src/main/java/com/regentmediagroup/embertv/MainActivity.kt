package com.regentmediagroup.embertv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.regentmediagroup.embertv.data.EmberApiClient
import com.regentmediagroup.embertv.data.Rental
import com.regentmediagroup.embertv.ui.screens.LoginScreen
import com.regentmediagroup.embertv.ui.screens.MyRentalsScreen
import com.regentmediagroup.embertv.ui.screens.PlayerScreen // Import the new screen
import com.regentmediagroup.embertv.ui.screens.RentalDetailScreen
import com.regentmediagroup.embertv.ui.theme.EmberTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current

            // Navigation States: "login", "rentals", "detail", "player"
            var currentScreen by remember { mutableStateOf("login") }
            var selectedRental by remember { mutableStateOf<Rental?>(null) }

            LaunchedEffect(Unit) {
                if (EmberApiClient.getToken(context) != null) {
                    currentScreen = "rentals"
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EmberTheme.Background)
            ) {
                when (currentScreen) {
                    "login" -> {
                        LoginScreen(
                            onLoginSuccess = {
                                currentScreen = "rentals"
                                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    "rentals" -> {
                        MyRentalsScreen(
                            onLogout = { currentScreen = "login" },
                            onRentalClick = { rental ->
                                selectedRental = rental
                                currentScreen = "detail"
                            }
                        )
                    }
                    "detail" -> {
                        if (selectedRental != null) {
                            RentalDetailScreen(
                                rental = selectedRental!!,
                                onPlayClick = {
                                    // Check if HLS URL exists before playing
                                    if (!selectedRental!!.film.hlsUrl.isNullOrEmpty()) {
                                        currentScreen = "player"
                                    } else {
                                        Toast.makeText(context, "Error: No video URL found.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onBack = {
                                    currentScreen = "rentals"
                                    selectedRental = null
                                }
                            )
                        } else {
                            currentScreen = "rentals"
                        }
                    }
                    "player" -> {
                        if (selectedRental != null && !selectedRental!!.film.hlsUrl.isNullOrEmpty()) {
                            PlayerScreen(
                                videoUrl = selectedRental!!.film.hlsUrl!!,
                                onBack = {
                                    currentScreen = "detail"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
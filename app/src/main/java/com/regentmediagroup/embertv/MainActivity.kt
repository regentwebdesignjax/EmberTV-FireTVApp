package com.regentmediagroup.embertv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.regentmediagroup.embertv.data.EmberApiClient
import com.regentmediagroup.embertv.data.Rental
import com.regentmediagroup.embertv.ui.screens.ActivationScreen
import com.regentmediagroup.embertv.ui.screens.MyRentalsScreen
import com.regentmediagroup.embertv.ui.screens.PlayerScreen
import com.regentmediagroup.embertv.ui.screens.RentalDetailScreen
import com.regentmediagroup.embertv.ui.theme.EmberTheme

private enum class Screen { Rentals, Detail, Player }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EmberApiClient.init(applicationContext)

        setContent {
            val signedIn by EmberApiClient.signedIn.collectAsState()

            var screen by remember { mutableStateOf(Screen.Rentals) }
            var selectedRental by remember { mutableStateOf<Rental?>(null) }
            // Shared resume point for the selected rental, updated after each viewing.
            var resumeSeconds by remember { mutableStateOf<Long?>(null) }
            var startAtSeconds by remember { mutableStateOf<Long?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EmberTheme.Background)
            ) {
                val rental = selectedRental
                if (!signedIn) {
                    ActivationScreen(
                        onSignedIn = {
                            screen = Screen.Rentals
                            selectedRental = null
                        }
                    )
                } else if (screen == Screen.Rentals || rental == null) {
                    MyRentalsScreen(
                        onSignOut = {
                            screen = Screen.Rentals
                            selectedRental = null
                        },
                        onRentalClick = { clicked ->
                            selectedRental = clicked
                            resumeSeconds = clicked.resume?.positionSeconds?.toLong()
                            screen = Screen.Detail
                        }
                    )
                } else if (screen == Screen.Detail) {
                    RentalDetailScreen(
                        rental = rental,
                        resumeSeconds = resumeSeconds,
                        onPlay = { start ->
                            startAtSeconds = start
                            screen = Screen.Player
                        },
                        onBack = {
                            screen = Screen.Rentals
                            selectedRental = null
                        }
                    )
                } else {
                    PlayerScreen(
                        rental = rental,
                        startAtSeconds = startAtSeconds,
                        onClose = { position ->
                            if (position != null) resumeSeconds = position
                            screen = Screen.Detail
                        }
                    )
                }
            }
        }
    }
}

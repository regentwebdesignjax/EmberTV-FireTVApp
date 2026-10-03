package com.regentmediagroup.embertv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.regentmediagroup.embertv.data.EmberApiClient
import com.regentmediagroup.embertv.data.EmberConfig
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

                // Every screen but the player, so staging is never mistaken for the store app.
                if (EmberConfig.IS_STAGING && !(signedIn && screen == Screen.Player)) {
                    StagingBadge(Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}

@Composable
private fun StagingBadge(modifier: Modifier = Modifier) {
    Text(
        text = "STAGING",
        style = EmberTheme.bodySemibold(18),
        color = Color.Black,
        modifier = modifier
            .padding(top = 24.dp, end = 48.dp)
            .background(Color(0xFFFFD60A), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

package com.regentmediagroup.embertv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.regentmediagroup.embertv.R
import com.regentmediagroup.embertv.data.EmberApiClient
import com.regentmediagroup.embertv.data.EmberApiException
import com.regentmediagroup.embertv.data.Rental
import com.regentmediagroup.embertv.ui.theme.EmberTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MyRentalsScreen(
    onSignOut: () -> Unit,
    onRentalClick: (Rental) -> Unit
) {
    val scope = rememberCoroutineScope()

    var rentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadRentals() {
        scope.launch {
            isLoading = true
            error = null
            try {
                rentals = EmberApiClient.fetchLibrary()
            } catch (e: CancellationException) {
                throw e
            } catch (e: EmberApiException.SignedOut) {
                // The session was revoked; MainActivity shows the sign-in screen.
            } catch (e: Exception) {
                error = e.message ?: "Couldn't load your library."
            }
            isLoading = false
        }
    }

    // Reloads each time the screen appears, so resume points and time left stay current.
    LaunchedEffect(Unit) { loadRentals() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberTheme.Background)
    ) {
        // --- Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 50.dp, end = 50.dp, bottom = 20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ember_tv_logo),
                contentDescription = "Ember TV",
                modifier = Modifier
                    .height(54.dp)
                    .align(Alignment.CenterStart),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "My Rentals",
                style = EmberTheme.titleFont(30),
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center)
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { loadRentals() },
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = EmberTheme.Primary,
                        contentColor = Color.White
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh", style = EmberTheme.bodyFont(14))
                }

                Button(
                    onClick = {
                        EmberApiClient.signOut()
                        onSignOut()
                    },
                    colors = ButtonDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = EmberTheme.Primary,
                        contentColor = Color.White
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(50))
                ) {
                    Text("Sign Out", style = EmberTheme.bodyFont(14))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Main content ---
        val message = error
        when {
            isLoading && rentals.isEmpty() -> {
                CenteredMessage("Loading your library...")
            }
            message != null -> {
                CenteredMessage("Couldn't load your library", message)
            }
            rentals.isEmpty() -> {
                CenteredMessage(
                    "Your library is empty",
                    "Rentals on your Ember TV account will appear here."
                )
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    contentPadding = PaddingValues(horizontal = 50.dp, vertical = 40.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rentals, key = { it.entitlement.id ?: it.film.id }) { rental ->
                        PosterCard(rental = rental, onClick = { onRentalClick(rental) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CenteredMessage(title: String, detail: String? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = EmberTheme.bodySemibold(24))
            if (detail != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    detail,
                    style = EmberTheme.bodyFont(18),
                    color = EmberTheme.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterCard(
    rental: Rental,
    onClick: () -> Unit
) {
    val watchable = rental.isWatchable()
    Column(
        modifier = Modifier.width(150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = androidx.compose.foundation.BorderStroke(3.dp, EmberTheme.Primary),
                    shape = RoundedCornerShape(12.dp)
                )
            ),
            modifier = Modifier.aspectRatio(2f / 3f)
        ) {
            AsyncImage(
                model = rental.film.posterUrl,
                contentDescription = rental.film.title,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (watchable) 1f else 0.4f),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = rental.film.title,
            style = EmberTheme.bodySemibold(16),
            color = EmberTheme.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = rental.statusLabel(),
            style = EmberTheme.bodyFont(13),
            color = if (watchable) EmberTheme.Primary else EmberTheme.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

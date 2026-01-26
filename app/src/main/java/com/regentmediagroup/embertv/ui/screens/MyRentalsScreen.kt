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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.regentmediagroup.embertv.data.Rental
import com.regentmediagroup.embertv.ui.theme.EmberTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MyRentalsScreen(
    onLogout: () -> Unit,
    onRentalClick: (Rental) -> Unit // <--- Added this parameter
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Function to load data
    fun loadRentals() {
        scope.launch {
            isLoading = true
            rentals = EmberApiClient.fetchMyRentals(context)
            isLoading = false
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        loadRentals()
    }

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
            // 1. Logo (Top Left)
            Image(
                painter = painterResource(id = R.drawable.ember_tv_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .height(60.dp)
                    .align(Alignment.CenterStart),
                contentScale = ContentScale.Fit
            )

            // 2. Title (Top Center)
            Text(
                text = "My Rentals",
                style = EmberTheme.titleFont(30),
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center)
            )

            // 3. Action Buttons (Top Right)
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Refresh Button
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

                // Sign Out Button
                Button(
                    onClick = {
                        EmberApiClient.logout(context)
                        onLogout()
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

        // --- Main Content (Horizontal Scroll) ---
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading Library...", style = EmberTheme.bodyFont(24))
            }
        } else if (rentals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No rentals found.", style = EmberTheme.bodyFont(24), color = EmberTheme.TextSecondary)
            }
        } else {
            // Horizontal List
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(
                    horizontal = 50.dp,
                    vertical = 40.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                items(rentals) { rental ->
                    PosterCard(
                        rental = rental,
                        onClick = {
                            onRentalClick(rental) // <--- Calls the navigation callback
                        }
                    )
                }
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
    Column(
        modifier = Modifier.width(150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Focusable Image Card ---
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(
                shape = RoundedCornerShape(12.dp)
            ),
            scale = ClickableSurfaceDefaults.scale(
                focusedScale = 1.1f
            ),
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
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Title ---
        Text(
            text = rental.film.title,
            style = EmberTheme.bodySemibold(16),
            color = EmberTheme.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
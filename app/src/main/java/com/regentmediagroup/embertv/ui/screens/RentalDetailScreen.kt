package com.regentmediagroup.embertv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.regentmediagroup.embertv.data.Rental
import com.regentmediagroup.embertv.ui.theme.EmberTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RentalDetailScreen(
    rental: Rental,
    onPlayClick: () -> Unit,
    onBack: () -> Unit
) {
    // 1. Handle Remote "Back" Button
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberTheme.Background)
    ) {
        // --- BACKGROUND LAYER ---
        AsyncImage(
            model = rental.film.posterUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.3f), // Dim the background
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            EmberTheme.Background,
                            EmberTheme.Background.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )

        // --- CONTENT LAYER ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: The Poster
            AsyncImage(
                model = rental.film.posterUrl,
                contentDescription = rental.film.title,
                modifier = Modifier
                    .height(450.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(60.dp))

            // RIGHT: Title & Button Only
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = rental.film.title,
                    style = EmberTheme.titleFont(52),
                    color = Color.White,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Watch Now Button
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.colors(
                        containerColor = EmberTheme.Primary,
                        focusedContainerColor = EmberTheme.Primary.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(50))
                ) {
                    Text(
                        text = "Watch Now",
                        style = EmberTheme.bodySemibold(22),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
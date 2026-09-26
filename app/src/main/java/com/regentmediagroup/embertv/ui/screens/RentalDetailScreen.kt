package com.regentmediagroup.embertv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.regentmediagroup.embertv.data.EmberApiClient
import com.regentmediagroup.embertv.data.Rental
import com.regentmediagroup.embertv.ui.theme.EmberTheme

/** Offer "Resume" once the viewer is at least this far in. */
private const val MIN_RESUME_SECONDS = 60L

/**
 * One rental: artwork, title, description and Play. [resumeSeconds] is the
 * shared resume point (from the library, updated after each viewing).
 * [onPlay] receives where to start, or null to start from the beginning.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RentalDetailScreen(
    rental: Rental,
    resumeSeconds: Long?,
    onPlay: (startAtSeconds: Long?) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val film = rental.film
    val watchable = rental.isWatchable()
    val canResume = watchable && (resumeSeconds ?: 0L) >= MIN_RESUME_SECONDS
    val playFocus = remember { FocusRequester() }
    var description by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(film.slug) {
        description = runCatching { EmberApiClient.fetchFilm(film.slug) }
            .getOrNull()
            ?.let { it.longDescription ?: it.shortDescription }
    }

    LaunchedEffect(watchable) {
        if (watchable) runCatching { playFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberTheme.Background)
    ) {
        // --- Background ---
        AsyncImage(
            model = film.backdropUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.3f),
            contentScale = ContentScale.Crop
        )
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

        // --- Content ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = film.posterUrl,
                contentDescription = film.title,
                modifier = Modifier
                    .height(420.dp)
                    .aspectRatio(2f / 3f)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(60.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = film.title,
                    style = EmberTheme.titleFont(48),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val facts = listOfNotNull(
                    film.releaseYear?.toString(),
                    film.rating,
                    film.durationMinutes?.let { "$it min" },
                    rental.statusLabel()
                )
                if (facts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = facts.joinToString("  •  "),
                        style = EmberTheme.bodyFont(16),
                        color = EmberTheme.TextSecondary
                    )
                }

                val text = description
                if (!text.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = text,
                        style = EmberTheme.bodyFont(16),
                        color = EmberTheme.TextSecondary,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (watchable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailButton(
                            label = if (canResume) "Resume" else "Watch Now",
                            primary = true,
                            modifier = Modifier.focusRequester(playFocus),
                            onClick = { onPlay(if (canResume) resumeSeconds else null) }
                        )
                        if (canResume) {
                            DetailButton(
                                label = "Start Over",
                                primary = false,
                                onClick = { onPlay(null) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (rental.isUpcoming()) {
                            "This screening isn't available to play yet."
                        } else {
                            "This rental has ended."
                        },
                        style = EmberTheme.bodySemibold(18),
                        color = EmberTheme.TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = if (primary) EmberTheme.Primary else Color.White.copy(alpha = 0.1f),
            focusedContainerColor = if (primary) EmberTheme.Primary.copy(alpha = 0.8f) else EmberTheme.Primary,
            contentColor = Color.White
        ),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(50))
    ) {
        Text(
            text = label,
            style = EmberTheme.bodySemibold(20),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

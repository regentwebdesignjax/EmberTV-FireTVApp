package com.regentmediagroup.embertv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.regentmediagroup.embertv.R
import com.regentmediagroup.embertv.data.ActivationPoll
import com.regentmediagroup.embertv.data.DeviceCodeResponse
import com.regentmediagroup.embertv.data.EmberApiClient
import com.regentmediagroup.embertv.data.EmberApiException
import com.regentmediagroup.embertv.data.EmberConfig
import com.regentmediagroup.embertv.ui.theme.EmberTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Sign in with an activation code: the TV shows a short code, the viewer
 * approves it on their phone or computer, and the TV polls until it receives
 * a session. A new code is fetched when one expires.
 *
 * No QR code on Fire TV: Amazon's review treats a scannable link to the
 * website (where films are rented) as directing customers to an outside
 * payment method. The Apple TV and Roku apps keep theirs.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ActivationScreen(onSignedIn: () -> Unit) {
    var code by remember { mutableStateOf<DeviceCodeResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    val retryFocus = remember { FocusRequester() }

    LaunchedEffect(attempt) {
        error = null
        try {
            while (true) {
                val current = EmberApiClient.startActivation()
                code = current
                val expiresAt = System.currentTimeMillis() + (current.expiresIn ?: 600) * 1000L
                var interval = (current.interval ?: 5).coerceAtLeast(1)
                var restart = false
                while (!restart) {
                    delay(interval * 1000L)
                    if (System.currentTimeMillis() >= expiresAt) break
                    val poll = try {
                        EmberApiClient.pollActivation(current.deviceCode!!)
                    } catch (e: EmberApiException.Network) {
                        // A blip on the network: keep the code and poll again.
                        ActivationPoll.Pending(interval)
                    }
                    when (poll) {
                        is ActivationPoll.Pending -> interval = poll.interval.coerceAtLeast(1)
                        ActivationPoll.Approved -> {
                            onSignedIn()
                            return@LaunchedEffect
                        }
                        ActivationPoll.Restart -> restart = true
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            code = null
            error = e.message ?: "Something went wrong. Please try again in a moment."
        }
    }

    LaunchedEffect(error) {
        if (error != null) runCatching { retryFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberTheme.Background)
            .padding(horizontal = 64.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(520.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ember_tv_logo),
                    contentDescription = "Ember TV",
                    modifier = Modifier.height(56.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = "Sign in to Ember TV", style = EmberTheme.titleFont(34))
                Spacer(modifier = Modifier.height(24.dp))

                val current = code
                val message = error
                when {
                    message != null -> {
                        Text(
                            text = message,
                            style = EmberTheme.bodyFont(18),
                            color = EmberTheme.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { attempt++ },
                            modifier = Modifier.focusRequester(retryFocus),
                            colors = ButtonDefaults.colors(
                                containerColor = EmberTheme.Primary,
                                focusedContainerColor = EmberTheme.Primary.copy(alpha = 0.8f),
                                contentColor = Color.White
                            ),
                            shape = ButtonDefaults.shape(shape = CircleShape)
                        ) {
                            Text("Try Again", style = EmberTheme.bodySemibold(18))
                        }
                    }
                    current == null -> {
                        Text(
                            text = "Getting your code...",
                            style = EmberTheme.bodyFont(18),
                            color = EmberTheme.TextSecondary
                        )
                    }
                    else -> {
                        Step(1, "On your phone or computer, go to")
                        Text(
                            text = "${EmberConfig.WEBSITE_DISPLAY_NAME}/activate",
                            style = EmberTheme.bodySemibold(22),
                            color = EmberTheme.Primary,
                            modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 16.dp)
                        )
                        Step(2, "Sign in to your Ember TV account")
                        Spacer(modifier = Modifier.height(16.dp))
                        Step(3, "Enter this code:")
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .padding(start = 36.dp)
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 28.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = current.userCode.orEmpty(),
                                style = EmberTheme.titleFont(44).copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 6.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Waiting for you to approve...",
                            style = EmberTheme.bodyFont(16),
                            color = EmberTheme.TextSecondary,
                            modifier = Modifier.padding(start = 36.dp)
                        )
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Step(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "$number.",
            style = EmberTheme.bodySemibold(18),
            color = EmberTheme.Primary,
            modifier = Modifier.width(24.dp)
        )
        Text(text = text, style = EmberTheme.bodyFont(18))
    }
}

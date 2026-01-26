package com.regentmediagroup.embertv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.* import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

import com.regentmediagroup.embertv.R
import com.regentmediagroup.embertv.data.EmberApiClient
import com.regentmediagroup.embertv.ui.theme.EmberTheme

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // We keep the scroll state just in case, but the goal is to not need it!
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val buttonFocus = remember { FocusRequester() }

    var isEmailFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberTheme.Background),
        contentAlignment = Alignment.Center // Centers the entire form on screen
    ) {
        Column(
            modifier = Modifier
                .width(600.dp) // Restrict width for cleaner TV look
                .verticalScroll(scrollState)
                .padding(vertical = 20.dp), // Safe margin for overscan
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. COMPACT LOGO (Standard App Icon Size)
            Image(
                painter = painterResource(id = R.drawable.ember_tv_logo),
                contentDescription = "Logo",
                modifier = Modifier.height(64.dp), // Reduced from 80dp
                contentScale = ContentScale.Fit
            )

            // 2. TIGHTER SPACING
            Spacer(modifier = Modifier.height(24.dp)) // Reduced from 40dp

            // 3. STANDARD TV HEADLINE SIZE
            Text(
                text = "Sign in to EmberTV",
                style = EmberTheme.titleFont(34) // Reduced from 44sp
            )

            Spacer(modifier = Modifier.height(8.dp)) // Reduced from 12dp

            // 4. STANDARD TV BODY SIZE
            Text(
                text = "Use your EmberStreaming.app email & password.",
                style = EmberTheme.bodyFont(16), // Reduced from 22sp
                color = EmberTheme.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp)) // Reduced from 40dp

            // --- LOGIN CARD ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                    .padding(24.dp), // Reduced padding from 40dp
                verticalArrangement = Arrangement.spacedBy(16.dp), // Reduced gap from 24dp
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Email Field
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Email", color = EmberTheme.TextSecondary, style = EmberTheme.bodyFont(14))
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(emailFocus)
                            .onFocusChanged { isEmailFocused = it.isFocused }
                            .border(
                                width = if (isEmailFocused) 3.dp else 1.dp,
                                color = if (isEmailFocused) EmberTheme.Primary else Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = EmberTheme.Primary,
                            cursorColor = EmberTheme.Primary,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                        singleLine = true // Ensures it doesn't grow vertically
                    )
                }

                // Password Field
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Password", color = EmberTheme.TextSecondary, style = EmberTheme.bodyFont(14))
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocus)
                            .onFocusChanged { isPasswordFocused = it.isFocused }
                            .border(
                                width = if (isPasswordFocused) 3.dp else 1.dp,
                                color = if (isPasswordFocused) EmberTheme.Primary else Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = EmberTheme.Primary,
                            cursorColor = EmberTheme.Primary,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { buttonFocus.requestFocus() }),
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        style = EmberTheme.bodyFont(14),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sign In Button
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            val success = EmberApiClient.login(context, email, password)
                            isLoading = false
                            if (success) {
                                onLoginSuccess()
                            } else {
                                errorMessage = "Sign-in failed. Check credentials."
                            }
                        }
                    },
                    modifier = Modifier
                        .width(280.dp) // Standard Button Width
                        .height(48.dp) // Standard Button Height
                        .focusRequester(buttonFocus),
                    colors = ButtonDefaults.colors(
                        containerColor = EmberTheme.Primary,
                        focusedContainerColor = EmberTheme.Primary.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    shape = ButtonDefaults.shape(shape = CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isLoading) "Signing In..." else "Sign In",
                            style = EmberTheme.bodySemibold(18), // Reduced from 24sp
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        emailFocus.requestFocus()
    }
}
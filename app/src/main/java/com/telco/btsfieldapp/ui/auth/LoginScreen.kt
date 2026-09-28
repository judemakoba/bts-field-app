package com.telco.btsfieldapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telco.btsfieldapp.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    // Handle one-shot navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LoginEvent.NavigateToSites -> onLoginSuccess()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // warm cream BackgroundLight
    ) {
        // ── Decorative coral blob in top-right corner ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // Main decorative circle - top right
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(x = 120.dp, y = (-60).dp)
                    .background(
                        color = PrimaryCoralLight.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50)
                    )
            )
            // Secondary smaller circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = 260.dp, y = 20.dp)
                    .background(
                        color = PrimaryCoralLight.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        // ── Main scrollable content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Logo circle with coral background ──
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = PrimaryCoral.copy(alpha = 0.25f),
                        spotColor = PrimaryCoral.copy(alpha = 0.25f)
                    ),
                shape = RoundedCornerShape(50),
                color = PrimaryCoralLight
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── App title ──
            Text(
                text = "BTS Site Audit",
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurfaceLight,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Field Engineer App",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariantLight
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Login card with warm styling ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0x40000000),
                        spotColor = Color(0x20000000)
                    )
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = SurfaceLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceLight
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Email field ──
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("Email") },
                        placeholder = { Text("engineer@telco.com", color = OnSurfaceVariantLight.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = PrimaryCoral.copy(alpha = 0.7f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCoral,
                            unfocusedBorderColor = OnSurfaceVariantLight.copy(alpha = 0.3f),
                            focusedLabelColor = PrimaryCoral,
                            unfocusedLabelColor = OnSurfaceVariantLight,
                            cursorColor = PrimaryCoral,
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Password field ──
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = PrimaryCoral.copy(alpha = 0.7f)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = OnSurfaceVariantLight.copy(alpha = 0.7f)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login()
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCoral,
                            unfocusedBorderColor = OnSurfaceVariantLight.copy(alpha = 0.3f),
                            focusedLabelColor = PrimaryCoral,
                            unfocusedLabelColor = OnSurfaceVariantLight,
                            cursorColor = PrimaryCoral,
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight
                        )
                    )

                    // ── Error / warning message ──
                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ErrorColor.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (uiState.isLockedOut) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = WarningColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (uiState.isLockedOut)
                                        "Locked — retry in ${uiState.lockoutSeconds}s"
                                    else
                                        uiState.error!!,
                                    color = ErrorColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // ── Attempts remaining warning ──
                    if (!uiState.isLockedOut && (uiState.attemptsLeft ?: 3) <= 2 && uiState.error == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.attemptsLeft} attempt${if (uiState.attemptsLeft == 1) "" else "s"} remaining",
                            color = WarningColor.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Primary Sign In button ──
                    Button(
                        onClick = viewModel::login,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading && !uiState.isLockedOut,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCoral,
                            contentColor = Color.White,
                            disabledContainerColor = PrimaryCoral.copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else if (uiState.isLockedOut) {
                            Text(
                                text = "Wait ${uiState.lockoutSeconds}s",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ── Bottom decorative gradient bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .offset(y = 24.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                PrimaryCoral,
                                SecondaryAmber,
                                PrimaryCoralLight
                            )
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

package com.nutriscan.app.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutriscan.app.data.model.AuthState
import com.nutriscan.app.ui.components.NutriButton
import com.nutriscan.app.ui.components.NutriPasswordField
import com.nutriscan.app.ui.components.NutriTextField
import com.nutriscan.app.ui.theme.*

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> onLoginSuccess()
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GradientStart,
                        GradientMiddle,
                        GradientEnd.copy(alpha = 0.5f)
                    )
                )
            )
    ) {
        // Decorative circles
        DecorativeBackground()

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            if (isLandscape) {
                // Landscape Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Branding
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(600)) + slideInHorizontally(
                            initialOffsetX = { -100 },
                            animationSpec = tween(600)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        BrandingSection(compact = true)
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Right side - Form
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInHorizontally(
                            initialOffsetX = { 100 },
                            animationSpec = tween(600, delayMillis = 200)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        LoginFormCard(
                            email = email,
                            password = password,
                            onEmailChange = {
                                email = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            onPasswordChange = {
                                password = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            authState = authState,
                            emailError = emailError,
                            passwordError = passwordError,
                            onLogin = { viewModel.signIn(email, password) },
                            onNavigateToRegister = {
                                viewModel.resetState()
                                onNavigateToRegister()
                            }
                        )
                    }
                }
            } else {
                // Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))

                    // Animated Logo Section
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(600)
                        )
                    ) {
                        BrandingSection(compact = false)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Animated Form Card
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 300)) + slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(600, delayMillis = 300)
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        LoginFormCard(
                            email = email,
                            password = password,
                            onEmailChange = {
                                email = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            onPasswordChange = {
                                password = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            authState = authState,
                            emailError = emailError,
                            passwordError = passwordError,
                            onLogin = { viewModel.signIn(email, password) },
                            onNavigateToRegister = {
                                viewModel.resetState()
                                onNavigateToRegister()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun DecorativeBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top right circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 150.dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GreenLight.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Bottom left circle
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TealLight.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun BrandingSection(compact: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated Logo
        Box(
            modifier = Modifier
                .size(if (compact) 100.dp else 120.dp)
                .scale(scale)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = GreenPrimary.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GreenPrimary, GreenLight, TealPrimary),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Eco,
                contentDescription = "NutriScan Logo",
                modifier = Modifier.size(if (compact) 56.dp else 64.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 16.dp else 24.dp))

        // App Name
        Text(
            text = "NutriScan",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 28.sp else 32.sp
            ),
            color = GreenDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tagline
        Text(
            text = "Scan & Analisis Nutrisi Makanan",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoginFormCard(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    authState: AuthState,
    emailError: String?,
    passwordError: String?,
    onLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Selamat Datang! 👋",
                style = MaterialTheme.typography.titleLarge,
                color = GreenDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Masuk untuk melanjutkan",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error Message
            AnimatedVisibility(
                visible = authState is AuthState.Error,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ErrorLight
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (authState as? AuthState.Error)?.message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }
                }
            }

            // Email Field
            NutriTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Email",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                isError = emailError != null,
                errorMessage = emailError,
                enabled = authState !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            NutriPasswordField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        onLogin()
                    }
                },
                isError = passwordError != null,
                errorMessage = passwordError,
                enabled = authState !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Button
            NutriButton(
                text = "Masuk",
                onClick = onLogin,
                isLoading = authState is AuthState.Loading,
                enabled = email.isNotBlank() && password.isNotBlank()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text(
                    text = "  atau  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register Link
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Belum punya akun?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = "Daftar",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GreenPrimary
                    )
                }
            }
        }
    }
}

// Easing function
private val EaseInOutCubic: Easing = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
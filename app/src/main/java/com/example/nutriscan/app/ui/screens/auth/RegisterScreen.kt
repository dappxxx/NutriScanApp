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
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val confirmPasswordError by viewModel.confirmPasswordError.collectAsState()
    val needsEmailVerification by viewModel.needsEmailVerification.collectAsState()

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
            is AuthState.Authenticated -> onRegisterSuccess()
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
        RegisterDecorativeBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.resetState()
                                onNavigateToLogin()
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = GreenPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
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
                        modifier = Modifier.weight(0.8f)
                    ) {
                        RegisterBrandingSection()
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Right side - Form (scrollable)
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInHorizontally(
                            initialOffsetX = { 100 },
                            animationSpec = tween(600, delayMillis = 200)
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        RegisterFormCard(
                            fullName = fullName,
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword,
                            onFullNameChange = {
                                fullName = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            onEmailChange = {
                                email = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            onPasswordChange = {
                                password = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            onConfirmPasswordChange = {
                                confirmPassword = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            authState = authState,
                            nameError = nameError,
                            emailError = emailError,
                            passwordError = passwordError,
                            confirmPasswordError = confirmPasswordError,
                            needsEmailVerification = needsEmailVerification,
                            onRegister = {
                                viewModel.signUp(fullName, email, password, confirmPassword)
                            },
                            onNavigateToLogin = {
                                viewModel.resetState()
                                viewModel.clearEmailVerificationFlag()
                                onNavigateToLogin()
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
                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated Header
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(600)
                        )
                    ) {
                        RegisterBrandingSection()
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Animated Form Card
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(600, delayMillis = 300)) + slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(600, delayMillis = 300)
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        RegisterFormCard(
                            fullName = fullName,
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword,
                            onFullNameChange = {
                                fullName = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            onEmailChange = {
                                email = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            onPasswordChange = {
                                password = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            onConfirmPasswordChange = {
                                confirmPassword = it
                                if (authState is AuthState.Error) viewModel.resetState()
                            },
                            authState = authState,
                            nameError = nameError,
                            emailError = emailError,
                            passwordError = passwordError,
                            confirmPasswordError = confirmPasswordError,
                            needsEmailVerification = needsEmailVerification,
                            onRegister = {
                                viewModel.signUp(fullName, email, password, confirmPassword)
                            },
                            onNavigateToLogin = {
                                viewModel.resetState()
                                viewModel.clearEmailVerificationFlag()
                                onNavigateToLogin()
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
private fun RegisterDecorativeBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top left circle
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-60).dp, y = (-30).dp)
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

        // Bottom right circle
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GreenLight.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Middle accent circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            OrangeLight.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun RegisterBrandingSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = GreenPrimary.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GreenPrimary, TealPrimary),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Eco,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Buat Akun Baru",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = GreenDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Daftar untuk mulai menganalisis\nnutrisi makanan Anda",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RegisterFormCard(
    fullName: String,
    email: String,
    password: String,
    confirmPassword: String,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    authState: AuthState,
    nameError: String?,
    emailError: String?,
    passwordError: String?,
    confirmPasswordError: String?,
    needsEmailVerification: Boolean,
    onRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
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
            // Success Message (Email Verification Required)
            AnimatedVisibility(
                visible = needsEmailVerification && authState is AuthState.Error,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SuccessVerificationCard(onNavigateToLogin = onNavigateToLogin)
            }

            // Error Message (not email verification)
            AnimatedVisibility(
                visible = authState is AuthState.Error && !needsEmailVerification,
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

            // Form Fields (hide if email verification required)
            if (!needsEmailVerification) {
                // Full Name Field
                NutriTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = "Nama Lengkap",
                    leadingIcon = Icons.Default.Person,
                    imeAction = ImeAction.Next,
                    isError = nameError != null,
                    errorMessage = nameError,
                    enabled = authState !is AuthState.Loading
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    imeAction = ImeAction.Next,
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    enabled = authState !is AuthState.Loading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field
                NutriPasswordField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Konfirmasi Password",
                    leadingIcon = Icons.Default.Lock,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (fullName.isNotBlank() && email.isNotBlank() &&
                            password.isNotBlank() && confirmPassword.isNotBlank()
                        ) {
                            onRegister()
                        }
                    },
                    isError = confirmPasswordError != null,
                    errorMessage = confirmPasswordError,
                    enabled = authState !is AuthState.Loading
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password requirements hint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextHint
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Password minimal 6 karakter",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Register Button
                NutriButton(
                    text = "Daftar",
                    onClick = onRegister,
                    isLoading = authState is AuthState.Loading,
                    enabled = fullName.isNotBlank() && email.isNotBlank() &&
                            password.isNotBlank() && confirmPassword.isNotBlank()
                )

                Spacer(modifier = Modifier.height(20.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                // Login Link
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sudah punya akun?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    TextButton(onClick = onNavigateToLogin) {
                        Text(
                            text = "Masuk",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = GreenPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessVerificationCard(onNavigateToLogin: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = GradientStart
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GreenPrimary, TealPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Registrasi Berhasil! 🎉",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GreenDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Silakan cek email Anda untuk verifikasi akun, kemudian login.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            NutriButton(
                text = "Pergi ke Login",
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}
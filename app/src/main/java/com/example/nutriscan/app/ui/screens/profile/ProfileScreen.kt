package com.nutriscan.app.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutriscan.app.data.model.UiState
import com.nutriscan.app.ui.components.LoadingScreen
import com.nutriscan.app.ui.components.NutriPasswordField
import com.nutriscan.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val userState by viewModel.userState.collectAsState()
    val profileState by viewModel.profileState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val passwordState by viewModel.passwordState.collectAsState()
    val logoutState by viewModel.logoutState.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Handle logout state
    LaunchedEffect(logoutState) {
        when (logoutState) {
            is UiState.Success -> {
                onLogout()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar((logoutState as UiState.Error).message)
            }
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
                        GradientMiddle.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Decorative Background
        ProfileDecorativeBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ProfileTopBar(
                    onNavigateBack = onNavigateBack,
                    isVisible = isVisible
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            when (userState) {
                is UiState.Loading -> {
                    LoadingScreen(message = "Memuat profil...")
                }

                is UiState.Success -> {
                    val user = (userState as UiState.Success).data
                    val profile = (profileState as? UiState.Success)?.data

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Profile Header Card
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(400)) + scaleIn(
                                initialScale = 0.9f,
                                animationSpec = tween(400)
                            )
                        ) {
                            ProfileHeaderCard(
                                fullName = profile?.fullName ?: user.fullName ?: "User",
                                email = user.email,
                                healthCondition = profile?.healthCondition
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Menu Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Account Settings Section
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(
                                    initialOffsetY = { 30 },
                                    animationSpec = tween(400, delayMillis = 100)
                                )
                            ) {
                                SectionHeader(
                                    icon = Icons.Outlined.Settings,
                                    title = "Pengaturan Akun"
                                )
                            }

                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(400, delayMillis = 150)) + slideInVertically(
                                    initialOffsetY = { 30 },
                                    animationSpec = tween(400, delayMillis = 150)
                                )
                            ) {
                                ProfileMenuItem(
                                    icon = Icons.Outlined.Person,
                                    title = "Edit Profil",
                                    subtitle = "Ubah nama dan kondisi kesehatan",
                                    gradientColors = listOf(GreenPrimary, GreenLight),
                                    onClick = { showEditProfileDialog = true }
                                )
                            }

                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(
                                    initialOffsetY = { 30 },
                                    animationSpec = tween(400, delayMillis = 200)
                                )
                            ) {
                                ProfileMenuItem(
                                    icon = Icons.Outlined.Lock,
                                    title = "Ubah Password",
                                    subtitle = "Ganti password akun Anda",
                                    gradientColors = listOf(TealPrimary, TealLight),
                                    onClick = { showChangePasswordDialog = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Other Section
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(400, delayMillis = 250)) + slideInVertically(
                                    initialOffsetY = { 30 },
                                    animationSpec = tween(400, delayMillis = 250)
                                )
                            ) {
                                SectionHeader(
                                    icon = Icons.Outlined.MoreHoriz,
                                    title = "Lainnya"
                                )
                            }

                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(
                                    initialOffsetY = { 30 },
                                    animationSpec = tween(400, delayMillis = 300)
                                )
                            ) {
                                ProfileMenuItem(
                                    icon = Icons.Outlined.Info,
                                    title = "Tentang Aplikasi",
                                    subtitle = "NutriScan v1.0.0",
                                    gradientColors = listOf(InfoBlue, InfoBlue.copy(alpha = 0.7f)),
                                    onClick = { }
                                )
                            }

                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(400, delayMillis = 350)) + slideInVertically(
                                    initialOffsetY = { 30 },
                                    animationSpec = tween(400, delayMillis = 350)
                                )
                            ) {
                                ProfileMenuItem(
                                    icon = Icons.Outlined.Logout,
                                    title = "Keluar",
                                    subtitle = "Logout dari akun",
                                    gradientColors = listOf(ErrorRed, ErrorRed.copy(alpha = 0.7f)),
                                    onClick = { showLogoutDialog = true },
                                    isDestructive = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Footer
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(400, delayMillis = 400))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 32.dp)
                            ) {
                                Text(
                                    text = "Made with ❤️ for healthy eating",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "© 2024 NutriScan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextHint
                                )
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = ErrorLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = (userState as UiState.Error).message,
                                    color = ErrorRed,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        val profile = (profileState as? UiState.Success)?.data
        val user = (userState as? UiState.Success)?.data

        EditProfileDialog(
            currentName = profile?.fullName ?: user?.fullName ?: "",
            currentHealthCondition = profile?.healthCondition ?: "",
            updateState = updateState,
            onDismiss = {
                showEditProfileDialog = false
                viewModel.resetUpdateState()
            },
            onSave = { name, condition ->
                viewModel.updateProfile(name, condition)
            },
            onSuccess = {
                showEditProfileDialog = false
                viewModel.resetUpdateState()
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            passwordState = passwordState,
            onDismiss = {
                showChangePasswordDialog = false
                viewModel.resetPasswordState()
            },
            onSave = { newPassword, confirmPassword ->
                viewModel.changePassword(newPassword, confirmPassword)
            },
            onSuccess = {
                showChangePasswordDialog = false
                viewModel.resetPasswordState()
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = { viewModel.logout() }
        )
    }
}

@Composable
private fun ProfileDecorativeBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-50).dp, y = (-50).dp)
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

        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TealLight.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            OrangeLight.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    onNavigateBack: () -> Unit,
    isVisible: Boolean
) {
    TopAppBar(
        title = {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + slideInHorizontally(
                    initialOffsetX = { -30 },
                    animationSpec = tween(400)
                )
            ) {
                Text(
                    text = "Profil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenDark
                )
            }
        },
        navigationIcon = {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
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

@Composable
private fun ProfileHeaderCard(
    fullName: String,
    email: String,
    healthCondition: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = GreenPrimary.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(8.dp, CircleShape, spotColor = GreenPrimary.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GreenPrimary, GreenLight, TealPrimary)
                        )
                    )
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Text(
                        text = fullName.firstOrNull()?.uppercase()?.toString() ?: "U",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp
                        ),
                        color = GreenPrimary,
                        modifier = Modifier.scale(scale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = GreenDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (!healthCondition.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = GreenLight.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = GreenPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = healthCondition,
                            style = MaterialTheme.typography.labelLarge,
                            color = GreenDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = if (isDestructive) ErrorRed.copy(alpha = 0.2f) else gradientColors.first().copy(alpha = 0.2f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) ErrorLight.copy(alpha = 0.4f) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = gradientColors.map { it.copy(alpha = 0.15f) }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = gradientColors.first(),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) ErrorRed else GreenDark
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDestructive) ErrorRed.copy(alpha = 0.5f) else TextSecondary
            )
        }
    }
}

// ==================== EDIT PROFILE DIALOG ====================
@Composable
private fun EditProfileDialog(
    currentName: String,
    currentHealthCondition: String,
    updateState: UiState<Unit>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onSuccess: () -> Unit
) {
    var fullName by remember { mutableStateOf(currentName) }
    var healthCondition by remember { mutableStateOf(currentHealthCondition) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // Handle success
    LaunchedEffect(updateState) {
        if (updateState is UiState.Success) {
            showSuccessMessage = true
            delay(1500) // Show success for 1.5 seconds
            onSuccess()
        }
    }

    Dialog(
        onDismissRequest = { if (updateState !is UiState.Loading) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = updateState !is UiState.Loading,
            dismissOnClickOutside = updateState !is UiState.Loading
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(16.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .imePadding() // Handle keyboard
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GreenLight.copy(alpha = 0.3f), TealLight.copy(alpha = 0.3f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = GreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Edit Profil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Success Message - Di dalam dialog, di atas form
                AnimatedVisibility(
                    visible = showSuccessMessage,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SuccessGreen.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Profil berhasil diperbarui!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Error Message - Di dalam dialog
                AnimatedVisibility(
                    visible = updateState is UiState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (updateState as? UiState.Error)?.message ?: "Gagal memperbarui profil",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRed
                            )
                        }
                    }
                }

                // Form Fields (hide when success)
                if (!showSuccessMessage) {
                    // Name Field
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        enabled = updateState !is UiState.Loading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            focusedLabelColor = GreenPrimary,
                            cursorColor = GreenPrimary
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Badge,
                                contentDescription = null,
                                tint = GreenPrimary
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Health Condition Field
                    OutlinedTextField(
                        value = healthCondition,
                        onValueChange = { healthCondition = it },
                        label = { Text("Kondisi Kesehatan (opsional)") },
                        placeholder = { Text("Contoh: Diabetes, Alergi Kacang") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        enabled = updateState !is UiState.Loading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            focusedLabelColor = GreenPrimary,
                            cursorColor = GreenPrimary
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.HealthAndSafety,
                                contentDescription = null,
                                tint = GreenPrimary
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = updateState !is UiState.Loading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            )
                        ) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onSave(fullName, healthCondition) },
                            enabled = updateState !is UiState.Loading && fullName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenPrimary
                            )
                        ) {
                            if (updateState is UiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simpan")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== CHANGE PASSWORD DIALOG ====================
@Composable
private fun ChangePasswordDialog(
    passwordState: UiState<Unit>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onSuccess: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // Handle success
    LaunchedEffect(passwordState) {
        if (passwordState is UiState.Success) {
            showSuccessMessage = true
            delay(1500)
            onSuccess()
        }
    }

    Dialog(
        onDismissRequest = { if (passwordState !is UiState.Loading) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = passwordState !is UiState.Loading,
            dismissOnClickOutside = passwordState !is UiState.Loading
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(16.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .imePadding()
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(TealLight.copy(alpha = 0.3f), GreenLight.copy(alpha = 0.3f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = TealPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Ubah Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Success Message
                AnimatedVisibility(
                    visible = showSuccessMessage,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SuccessGreen.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Password berhasil diubah!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Error Message
                AnimatedVisibility(
                    visible = passwordState is UiState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (passwordState as? UiState.Error)?.message ?: "Gagal mengubah password",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRed
                            )
                        }
                    }
                }

                // Form (hide when success)
                if (!showSuccessMessage) {
                    NutriPasswordField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "Password Baru",
                        leadingIcon = Icons.Default.Lock,
                        imeAction = ImeAction.Next,
                        enabled = passwordState !is UiState.Loading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NutriPasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Konfirmasi Password",
                        leadingIcon = Icons.Default.Lock,
                        imeAction = ImeAction.Done,
                        enabled = passwordState !is UiState.Loading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hint
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = TextHint
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Password minimal 6 karakter",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHint
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = passwordState !is UiState.Loading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            )
                        ) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onSave(newPassword, confirmPassword) },
                            enabled = passwordState !is UiState.Loading &&
                                    newPassword.isNotBlank() &&
                                    confirmPassword.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary
                            )
                        ) {
                            if (passwordState is UiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ubah")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== LOGOUT DIALOG ====================
@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(16.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "warning")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(ErrorLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Keluar dari Akun?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Anda akan keluar dari akun NutriScan. Pastikan data Anda sudah tersimpan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed
                        )
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keluar")
                    }
                }
            }
        }
    }
}

private val EaseInOutCubic: Easing = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
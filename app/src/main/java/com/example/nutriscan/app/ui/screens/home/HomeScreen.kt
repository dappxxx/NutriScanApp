package com.nutriscan.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nutriscan.app.data.model.ScanSession
import com.nutriscan.app.data.model.UiState
import com.nutriscan.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val recentScans by viewModel.recentScans.collectAsState()
    val healthConditions by viewModel.healthConditions.collectAsState()
    val personalizedTips by viewModel.personalizedTips.collectAsState()
    val updateHealthState by viewModel.updateHealthState.collectAsState()

    var showHealthDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
        viewModel.refresh()
    }

    LaunchedEffect(updateHealthState) {
        when (updateHealthState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("Riwayat kesehatan berhasil disimpan!")
                viewModel.resetUpdateState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar((updateHealthState as UiState.Error).message)
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    // Health Condition Dialog
    if (showHealthDialog) {
        HealthConditionDialog(
            currentConditions = healthConditions,
            onDismiss = { showHealthDialog = false },
            onSave = { conditions ->
                viewModel.updateHealthConditions(conditions)
                showHealthDialog = false
            }
        )
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
        HomeDecorativeBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HomeTopBar(
                    userName = userName,
                    onProfileClick = onNavigateToProfile,
                    isVisible = isVisible
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = scaleIn(animationSpec = tween(400, delayMillis = 600)) + fadeIn()
                ) {
                    GradientFAB(
                        onClick = onNavigateToCamera,
                        icon = Icons.Default.CameraAlt,
                        text = "Scan Produk"
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Health Profile Card
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400)) + slideInVertically(
                            initialOffsetY = { 30 },
                            animationSpec = tween(400)
                        )
                    ) {
                        HealthProfileCard(
                            conditions = healthConditions,
                            onEditClick = { showHealthDialog = true }
                        )
                    }
                }

                // Hero Card
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(
                            initialOffsetY = { 30 },
                            animationSpec = tween(400, delayMillis = 100)
                        )
                    ) {
                        HeroCard(onScanClick = onNavigateToCamera)
                    }
                }

                // Recent Scans Header
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400, delayMillis = 300))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scan Terakhir",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GreenDark
                            )
                            if (recentScans is UiState.Success && (recentScans as UiState.Success<List<ScanSession>>).data.isNotEmpty()) {
                                TextButton(onClick = onNavigateToHistory) {
                                    Text(
                                        text = "Lihat Semua",
                                        color = GreenPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Scans Content
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400, delayMillis = 350)) + slideInVertically(
                            initialOffsetY = { 30 },
                            animationSpec = tween(400, delayMillis = 350)
                        )
                    ) {
                        when (val state = recentScans) {
                            is UiState.Loading -> {
                                RecentScansLoading()
                            }
                            is UiState.Success -> {
                                if (state.data.isEmpty()) {
                                    EmptyRecentScans(onScanClick = onNavigateToCamera)
                                } else {
                                    RecentScansRow(
                                        scans = state.data,
                                        onScanClick = onNavigateToChat
                                    )
                                }
                            }
                            is UiState.Error -> {
                                ErrorCard(
                                    message = state.message,
                                    onRetry = { viewModel.loadRecentScans() }
                                )
                            }
                            else -> {}
                        }
                    }
                }

                // Personalized Tips Header
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400, delayMillis = 400))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (healthConditions.isNotEmpty()) "Tips Khusus Untuk Anda" else "Tips Kesehatan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GreenDark
                            )
                            if (healthConditions.isNotEmpty()) {
                                AssistChip(
                                    onClick = { showHealthDialog = true },
                                    label = {
                                        Text(
                                            "Personalisasi",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = GreenLight.copy(alpha = 0.2f),
                                        labelColor = GreenPrimary,
                                        leadingIconContentColor = GreenPrimary
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                // Personalized Tips
                items(personalizedTips.size) { index ->
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(400, delayMillis = 450 + (index * 50))) + slideInVertically(
                            initialOffsetY = { 30 },
                            animationSpec = tween(400, delayMillis = 450 + (index * 50))
                        )
                    ) {
                        PersonalizedTipCard(tip = personalizedTips[index])
                    }
                }

                // Spacer for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeDecorativeBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top right circle
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = 150.dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GreenLight.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Bottom left circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 50.dp)
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

        // Middle right accent
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 70.dp)
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
private fun HomeTopBar(
    userName: String,
    onProfileClick: () -> Unit,
    isVisible: Boolean
) {
    val displayName = userName.split(" ").firstOrNull() ?: "User"

    TopAppBar(
        title = {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + slideInHorizontally(
                    initialOffsetX = { -30 },
                    animationSpec = tween(400)
                )
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Halo, $displayName",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = GreenDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "👋",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "Siap scan nutrisi hari ini?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        actions = {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(400)
                )
            ) {
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = GreenPrimary,
                        modifier = Modifier.size(24.dp)
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
fun GradientFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fabScale"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = GreenPrimary.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.Transparent,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GreenPrimary, GreenLight, TealPrimary),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun HealthProfileCard(
    conditions: List<String>,
    onEditClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (conditions.isEmpty()) Color.Gray.copy(alpha = 0.2f) else GreenPrimary.copy(alpha = 0.2f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onEditClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = if (conditions.isEmpty()) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(GreenLight.copy(alpha = 0.3f), TealLight.copy(alpha = 0.3f))
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (conditions.isEmpty()) Icons.Outlined.HealthAndSafety else Icons.Filled.HealthAndSafety,
                    contentDescription = null,
                    tint = if (conditions.isEmpty()) TextSecondary else GreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (conditions.isEmpty()) "Tambah Riwayat Kesehatan" else "Riwayat Kesehatan Anda",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (conditions.isEmpty()) {
                    Text(
                        text = "Tap untuk mendapatkan tips yang dipersonalisasi",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(conditions.take(3)) { condition ->
                            SuggestionChip(
                                onClick = onEditClick,
                                label = {
                                    Text(
                                        condition,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = GreenLight.copy(alpha = 0.15f),
                                    labelColor = GreenDark
                                ),
                                border = null
                            )
                        }
                        if (conditions.size > 3) {
                            item {
                                SuggestionChip(
                                    onClick = onEditClick,
                                    label = { Text("+${conditions.size - 3}") },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = TealLight.copy(alpha = 0.2f),
                                        labelColor = TealDark
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun HeroCard(onScanClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = GreenPrimary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GreenPrimary, GreenLight, TealPrimary),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            // Decorative circles
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-30).dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-20).dp, y = 20.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kenali Apa yang\nKamu Makan",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 32.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Scan label nutrisi dan dapatkan analisis AI untuk pola makan lebih sehat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Animated emoji
                    val infiniteTransition = rememberInfiniteTransition(label = "emoji")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Text(
                        text = "🍎",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.scale(scale)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}


@Composable
fun RecentScansLoading() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(3) {
            Card(
                modifier = Modifier
                    .width(160.dp)
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = GreenPrimary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
fun RecentScansRow(scans: List<ScanSession>, onScanClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(scans) { scan ->
            RecentScanCard(scan = scan, onClick = { onScanClick(scan.id) })
        }
    }
}

@Composable
fun RecentScanCard(scan: ScanSession, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .width(160.dp)
            .scale(scale)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = GreenPrimary.copy(alpha = 0.2f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientMiddle)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (scan.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = scan.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = GreenPrimary.copy(alpha = 0.5f)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = scan.productName ?: "Produk",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = GreenDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDate(scan.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun EmptyRecentScans(onScanClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Gray.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientMiddle)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = GreenPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Belum Ada Scan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = GreenDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mulai scan produk pertamamu!",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onScanClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GreenPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    GreenPrimary
                )
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Sekarang")
            }
        }
    }
}

@Composable
fun PersonalizedTipCard(tip: HealthTip) {
    val (backgroundColor, borderColor, iconTint) = when (tip.priority) {
        HealthTipPriority.HIGH -> Triple(
            ErrorLight.copy(alpha = 0.3f),
            ErrorRed.copy(alpha = 0.3f),
            ErrorRed
        )
        HealthTipPriority.MEDIUM -> Triple(
            GreenLight.copy(alpha = 0.15f),
            GreenPrimary.copy(alpha = 0.2f),
            GreenPrimary
        )
        HealthTipPriority.LOW -> Triple(
            GradientStart,
            Color.Transparent,
            TextSecondary
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = iconTint.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (tip.priority == HealthTipPriority.HIGH) {
            androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tip.emoji,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tip.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenDark
                    )

                    if (tip.priority == HealthTipPriority.HIGH) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    ErrorRed.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Penting",
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = tip.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorLight)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRetry,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ErrorRed
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Coba Lagi")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HealthConditionDialog(
    currentConditions: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val predefinedConditions = listOf(
        "Diabetes", "Hipertensi", "Kolesterol Tinggi", "Asam Urat",
        "Maag/GERD", "Obesitas", "Anemia", "Alergi Kacang",
        "Alergi Susu", "Alergi Seafood", "Alergi Gluten",
        "Penyakit Jantung", "Penyakit Ginjal", "Hamil", "Menyusui"
    )

    val selectedConditions = remember {
        mutableStateListOf<String>().apply { addAll(currentConditions) }
    }
    var customCondition by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(GreenPrimary, TealPrimary)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Riwayat Kesehatan",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pilih kondisi kesehatan untuk tips yang dipersonalisasi",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Pilih kondisi:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenDark
                        )
                    }

                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            predefinedConditions.forEach { condition ->
                                FilterChip(
                                    selected = selectedConditions.contains(condition),
                                    onClick = {
                                        if (selectedConditions.contains(condition)) {
                                            selectedConditions.remove(condition)
                                        } else {
                                            selectedConditions.add(condition)
                                        }
                                    },
                                    label = { Text(condition, style = MaterialTheme.typography.labelMedium) },
                                    leadingIcon = if (selectedConditions.contains(condition)) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GreenLight.copy(alpha = 0.3f),
                                        selectedLabelColor = GreenDark,
                                        selectedLeadingIconColor = GreenPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = GreenLight.copy(alpha = 0.5f),
                                        selectedBorderColor = GreenPrimary,
                                        enabled = true,
                                        selected = selectedConditions.contains(condition)
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tambah kondisi lainnya:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenDark
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customCondition,
                            onValueChange = { customCondition = it },
                            label = { Text("Kondisi lainnya") },
                            placeholder = { Text("Ketik kondisi kesehatan...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                focusedLabelColor = GreenPrimary,
                                cursorColor = GreenPrimary
                            ),
                            trailingIcon = {
                                if (customCondition.isNotBlank()) {
                                    IconButton(onClick = {
                                        val trimmed = customCondition.trim()
                                        if (trimmed.isNotBlank() && !selectedConditions.contains(trimmed)) {
                                            selectedConditions.add(trimmed)
                                            customCondition = ""
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Tambah",
                                            tint = GreenPrimary
                                        )
                                    }
                                }
                            }
                        )
                    }

                    val customSelected = selectedConditions.filter { !predefinedConditions.contains(it) }
                    if (customSelected.isNotEmpty()) {
                        item {
                            Text(
                                text = "Kondisi tambahan:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenDark
                            )
                        }
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                customSelected.forEach { condition ->
                                    InputChip(
                                        selected = true,
                                        onClick = { selectedConditions.remove(condition) },
                                        label = { Text(condition) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Hapus",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = TealLight.copy(alpha = 0.3f),
                                            selectedLabelColor = TealDark,
                                            selectedTrailingIconColor = TealDark
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (selectedConditions.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = GreenLight.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${selectedConditions.size} kondisi dipilih",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GreenDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer
                HorizontalDivider(color = GradientMiddle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(selectedConditions.toList()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

private fun formatDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val parts = dateString.split("T")
        if (parts.isNotEmpty()) {
            val datePart = parts[0]
            val dateComponents = datePart.split("-")
            if (dateComponents.size == 3) {
                val months = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                val day = dateComponents[2]
                val month = months.getOrElse(dateComponents[1].toIntOrNull() ?: 0) { "" }
                "$day $month"
            } else datePart
        } else dateString
    } catch (e: Exception) { dateString }
}

private val EaseInOutCubic: Easing = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
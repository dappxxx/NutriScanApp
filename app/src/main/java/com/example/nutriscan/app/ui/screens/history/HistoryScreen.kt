package com.nutriscan.app.ui.screens.history

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutriscan.app.data.model.ScanSession
import com.nutriscan.app.data.model.UiState
import com.nutriscan.app.ui.components.ConfirmDeleteDialog
import com.nutriscan.app.ui.components.HistoryCard
import com.nutriscan.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val historyState by viewModel.historyState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<ScanSession?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(deleteState) {
        when (deleteState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Riwayat berhasil dihapus",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetDeleteState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (deleteState as UiState.Error).message,
                    duration = SnackbarDuration.Short
                )
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && sessionToDelete != null) {
        ModernDeleteDialog(
            productName = sessionToDelete?.productName ?: "Produk",
            onConfirm = {
                sessionToDelete?.let { viewModel.deleteSession(it.id) }
                showDeleteDialog = false
                sessionToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                sessionToDelete = null
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
        HistoryDecorativeBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HistoryTopBar(
                    onNavigateBack = onNavigateBack,
                    onRefresh = { viewModel.loadHistory() },
                    isVisible = isVisible
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            when (val state = historyState) {
                is UiState.Loading -> {
                    HistoryLoadingContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyHistoryContent(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            isVisible = isVisible
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header with count
                            item {
                                AnimatedVisibility(
                                    visible = isVisible,
                                    enter = fadeIn(tween(400)) + slideInVertically(
                                        initialOffsetY = { -20 },
                                        animationSpec = tween(400)
                                    )
                                ) {
                                    HistoryHeaderCard(count = state.data.size)
                                }
                            }

                            // History Items
                            itemsIndexed(
                                items = state.data,
                                key = { _, item -> item.id }
                            ) { index, session ->
                                AnimatedVisibility(
                                    visible = isVisible,
                                    enter = fadeIn(tween(400, delayMillis = 100 + (index * 50))) +
                                            slideInVertically(
                                                initialOffsetY = { 30 },
                                                animationSpec = tween(400, delayMillis = 100 + (index * 50))
                                            )
                                ) {
                                    HistoryCard(
                                        scanSession = session,
                                        onClick = { onNavigateToChat(session.id) },
                                        onDelete = {
                                            sessionToDelete = session
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    ErrorHistoryContent(
                        message = state.message,
                        onRetry = { viewModel.loadHistory() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        isVisible = isVisible
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun HistoryDecorativeBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top right circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 150.dp, y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TealLight.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Bottom left circle
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 100.dp)
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

        // Middle right accent
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 50.dp)
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
private fun HistoryTopBar(
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Riwayat Scan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "📋",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
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
        actions = {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f)
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TealPrimary
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
private fun HistoryHeaderCard(count: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = GreenPrimary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GreenLight.copy(alpha = 0.3f), TealLight.copy(alpha = 0.3f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Riwayat",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenDark
                )
                Text(
                    text = "$count produk telah discan",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Count Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GreenPrimary, TealPrimary)
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HistoryLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GreenLight.copy(alpha = 0.3f), TealLight.copy(alpha = 0.3f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = GreenPrimary,
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Memuat riwayat...",
                style = MaterialTheme.typography.bodyLarge,
                color = GreenDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyHistoryContent(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(400)
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = GreenPrimary.copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Icon
                    val infiniteTransition = rememberInfiniteTransition(label = "empty")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GradientStart, GradientMiddle, GradientEnd.copy(alpha = 0.5f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = GreenPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Belum Ada Riwayat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Riwayat scan produk akan muncul di sini.\nMulai scan produk pertamamu!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Decorative line
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GreenPrimary, TealPrimary)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorHistoryContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(400)
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = ErrorRed.copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Error Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(ErrorLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = ErrorRed
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Terjadi Kesalahan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        ),
                        modifier = Modifier
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = GreenPrimary.copy(alpha = 0.3f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Coba Lagi",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernDeleteDialog(
    productName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
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
                // Warning Icon with animation
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
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Hapus Riwayat?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Apakah Anda yakin ingin menghapus riwayat scan \"$productName\"? Semua chat terkait juga akan dihapus.",
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
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hapus")
                    }
                }
            }
        }
    }
}

private val EaseInOutCubic: Easing = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
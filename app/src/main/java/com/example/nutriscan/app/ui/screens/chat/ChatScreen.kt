package com.nutriscan.app.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nutriscan.app.data.model.UiState
import com.nutriscan.app.ui.components.ChatBubble
import com.nutriscan.app.ui.components.TypingIndicator
import com.nutriscan.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val messagesState by viewModel.messagesState.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showImageDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    // Scroll to bottom when new message arrives
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1 + if (isTyping) 1 else 0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GradientStart.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .systemBarsPadding() // Handle system bars
            .imePadding() // Handle keyboard - di level paling luar
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            ChatTopBar(
                session = (sessionState as? UiState.Success)?.data,
                onNavigateBack = onNavigateBack,
                onImageClick = { showImageDialog = true },
                onEditClick = { showEditNameDialog = true },
                isVisible = isVisible
            )

            // Chat Messages - menggunakan weight agar fleksibel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val state = messagesState) {
                    is UiState.Loading -> {
                        ChatLoadingContent()
                    }

                    is UiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.data,
                                key = { it.id }
                            ) { message ->
                                ChatBubble(
                                    message = message.message,
                                    isUser = message.sender == "user",
                                    timestamp = formatTime(message.createdAt)
                                )
                            }

                            if (isTyping) {
                                item {
                                    TypingIndicator()
                                }
                            }
                        }
                    }

                    is UiState.Error -> {
                        ChatErrorContent(
                            message = state.message,
                            onRetry = { viewModel.loadSession(sessionId) }
                        )
                    }

                    else -> {}
                }
            }

            // Chat Input Bar - di bawah, akan otomatis naik dengan keyboard
            ChatInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = !isTyping && sessionState is UiState.Success
            )
        }
    }

    // Image Dialog
    if (showImageDialog) {
        val session = (sessionState as? UiState.Success)?.data
        ImagePreviewDialog(
            imageUrl = session?.imageUrl,
            onDismiss = { showImageDialog = false }
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        EditProductNameDialog(
            currentName = (sessionState as? UiState.Success)?.data?.productName ?: "",
            onDismiss = { showEditNameDialog = false },
            onSave = { newName ->
                if (newName.isNotBlank()) {
                    viewModel.updateProductName(newName)
                }
                showEditNameDialog = false
            }
        )
    }
}

@Composable
private fun ChatTopBar(
    session: com.nutriscan.app.data.model.ScanSession?,
    onNavigateBack: () -> Unit,
    onImageClick: () -> Unit,
    onEditClick: () -> Unit,
    isVisible: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(GradientStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = GreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Product Info
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400, delayMillis = 100)) + slideInHorizontally(
                    initialOffsetX = { -30 },
                    animationSpec = tween(400, delayMillis = 100)
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Product Image Thumbnail
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GreenLight.copy(alpha = 0.3f), TealLight.copy(alpha = 0.3f))
                                )
                            )
                            .clickable(onClick = onImageClick),
                        contentAlignment = Alignment.Center
                    ) {
                        if (session?.imageUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = session.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = GreenPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = session?.productName ?: "Produk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = GreenDark
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = TextHint
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tap untuk edit nama",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextHint
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // AI Badge
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400, delayMillis = 200)) + scaleIn(initialScale = 0.8f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(GreenPrimary, TealPrimary)
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                    .size(70.dp)
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
                    modifier = Modifier.size(36.dp),
                    color = GreenPrimary,
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Memuat percakapan...",
                style = MaterialTheme.typography.bodyMedium,
                color = GreenDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ChatErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(ErrorLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Coba Lagi")
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    // Surface tanpa padding tambahan - langsung menempel
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Input Field
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Tanya tentang produk...",
                        color = TextHint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(24.dp),
                enabled = enabled,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GradientMiddle,
                    focusedContainerColor = GradientStart.copy(alpha = 0.3f),
                    unfocusedContainerColor = GradientStart.copy(alpha = 0.3f),
                    cursorColor = GreenPrimary,
                    disabledBorderColor = GradientMiddle.copy(alpha = 0.5f),
                    disabledContainerColor = GradientStart.copy(alpha = 0.2f)
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(buttonScale)
                    .shadow(
                        elevation = if (enabled && value.isNotBlank()) 6.dp else 2.dp,
                        shape = CircleShape,
                        spotColor = GreenPrimary.copy(alpha = 0.3f)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = if (enabled && value.isNotBlank()) {
                            Brush.linearGradient(
                                colors = listOf(GreenPrimary, TealPrimary),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(GradientMiddle, GradientEnd)
                            )
                        }
                    )
                    .clickable(
                        enabled = enabled && value.isNotBlank(),
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onSend
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (enabled && value.isNotBlank()) Color.White else TextHint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewDialog(
    imageUrl: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(16.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GreenLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Image,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gambar Produk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenDark
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GradientStart)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image
                if (imageUrl?.isNotEmpty() == true) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GradientStart),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GradientStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.ImageNotSupported,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TextHint
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tidak ada gambar",
                                color = TextHint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProductNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(16.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = GreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Edit Nama Produk",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Input Field
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nama Produk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        focusedLabelColor = GreenPrimary,
                        cursorColor = GreenPrimary
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.LocalOffer,
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(newName) },
                        enabled = newName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
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

private fun formatTime(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val parts = dateString.split("T")
        if (parts.size > 1) {
            val timePart = parts[1].split(".")[0]
            val timeParts = timePart.split(":")
            if (timeParts.size >= 2) {
                "${timeParts[0]}:${timeParts[1]}"
            } else timePart
        } else ""
    } catch (e: Exception) {
        ""
    }
}

private val EaseInOutCubic: Easing = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
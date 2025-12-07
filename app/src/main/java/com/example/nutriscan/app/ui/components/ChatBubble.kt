package com.nutriscan.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nutriscan.app.ui.theme.*

@Composable
fun ChatBubble(
    message: String,
    isUser: Boolean,
    timestamp: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // AI Avatar (left side)
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .shadow(4.dp, CircleShape, spotColor = GreenPrimary.copy(alpha = 0.3f))
                    .clip(CircleShape)
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
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message Content
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Message Bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .shadow(
                        elevation = if (isUser) 6.dp else 4.dp,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        ),
                        spotColor = if (isUser) GreenPrimary.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f)
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        brush = if (isUser) {
                            Brush.linearGradient(
                                colors = listOf(GreenPrimary, GreenLight),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(Color.White, Color.White)
                            )
                        }
                    )
                    .padding(14.dp)
            ) {
                if (isUser) {
                    // User message - plain text
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                } else {
                    // AI message - formatted markdown
                    MarkdownText(
                        text = message,
                        color = AiBubbleText
                    )
                }
            }

            // Timestamp
            if (timestamp != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }
        }

        // User Avatar (right side)
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .shadow(4.dp, CircleShape, spotColor = TealPrimary.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(TealPrimary, TealLight),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // AI Avatar
        Box(
            modifier = Modifier
                .size(34.dp)
                .shadow(4.dp, CircleShape, spotColor = GreenPrimary.copy(alpha = 0.3f))
                .clip(CircleShape)
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
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Typing bubble with animated dots
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                    spotColor = Color.Gray.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated typing dots
                val infiniteTransition = rememberInfiniteTransition(label = "typing")

                repeat(3) { index ->
                    val delay = index * 150
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 600,
                                delayMillis = delay,
                                easing = EaseInOutCubic
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )

                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 600,
                                delayMillis = delay,
                                easing = EaseInOutCubic
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        GreenPrimary.copy(alpha = alpha),
                                        TealPrimary.copy(alpha = alpha)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "AI sedang mengetik",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

private val EaseInOutCubic: Easing = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
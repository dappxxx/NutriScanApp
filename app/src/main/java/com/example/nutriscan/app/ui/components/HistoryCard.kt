package com.nutriscan.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nutriscan.app.data.model.ScanSession
import com.nutriscan.app.ui.theme.*

@Composable
fun HistoryCard(
    scanSession: ScanSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = GreenPrimary.copy(alpha = 0.15f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientMiddle)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (scanSession.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = scanSession.imageUrl,
                        contentDescription = "Product Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = GreenPrimary.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = scanSession.productName ?: "Produk Tidak Dikenal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = GreenDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Clean preview text
                val previewText = getCleanPreview(scanSession.initialAnalysis)
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TealLight.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatDate(scanSession.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = TealDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorLight.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Arrow indicator
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = TextHint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Get clean preview text dari analysis
 */
private fun getCleanPreview(analysis: String?): String {
    if (analysis.isNullOrBlank()) return "Tap untuk melihat detail analisis"

    // Cari bagian kesimpulan atau informasi penting
    val lines = analysis.lines()

    // Coba cari kesimpulan
    val conclusionIndex = lines.indexOfFirst {
        it.contains("KESIMPULAN", ignoreCase = true) ||
                it.contains("✅", ignoreCase = true)
    }

    val previewText = if (conclusionIndex >= 0 && conclusionIndex < lines.size - 1) {
        // Ambil text setelah header kesimpulan
        lines.drop(conclusionIndex + 1)
            .take(2)
            .joinToString(" ")
    } else {
        // Ambil beberapa baris pertama yang bukan header
        lines.filter { line ->
            line.isNotBlank() &&
                    !line.startsWith("📦") &&
                    !line.startsWith("📊") &&
                    !line.startsWith("⚠️") &&
                    !line.startsWith("📅") &&
                    !line.startsWith("💡") &&
                    !line.startsWith("🏷️") &&
                    !line.contains("NAMA PRODUK", ignoreCase = true) &&
                    !line.contains("NILAI GIZI", ignoreCase = true)
        }.take(2).joinToString(" ")
    }

    // Clean up the preview
    return previewText
        .replace(Regex("[*_#]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(100)
        .let { if (it.length >= 100) "$it..." else it }
        .ifBlank { "Tap untuk melihat detail analisis" }
}

private fun formatDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val parts = dateString.split("T")
        if (parts.isNotEmpty()) {
            val datePart = parts[0]
            val dateComponents = datePart.split("-")
            if (dateComponents.size == 3) {
                val months = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
                    "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                val day = dateComponents[2]
                val month = months.getOrElse(dateComponents[1].toIntOrNull() ?: 0) { "" }
                val year = dateComponents[0]
                "$day $month $year"
            } else {
                datePart
            }
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}
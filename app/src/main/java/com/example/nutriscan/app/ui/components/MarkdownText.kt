package com.nutriscan.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val annotatedString = buildAnnotatedString {
        val lines = text.lines()

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            // Cek apakah header (mulai dengan emoji atau huruf kapital semua)
            val isHeader = trimmed.isNotEmpty() && (
                    trimmed[0].code > 127 || // Emoji
                            trimmed.matches(Regex("^[A-Z][A-Z\\s]+.*")) || // ALL CAPS
                            trimmed.startsWith("📦") ||
                            trimmed.startsWith("📊") ||
                            trimmed.startsWith("⚠") ||
                            trimmed.startsWith("📅") ||
                            trimmed.startsWith("💡") ||
                            trimmed.startsWith("✅")
                    )

            if (isHeader) {
                withStyle(SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = primaryColor
                )) {
                    append(trimmed)
                }
            } else {
                withStyle(SpanStyle(color = color)) {
                    append(trimmed)
                }
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
    )
}
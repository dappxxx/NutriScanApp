package com.nutriscan.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MarkdownText - Komponen untuk render markdown dengan format yang rapi
 *
 * Mendukung:
 * - **bold** → Bold text
 * - Headers (📦, 📊, ⚠️, dll)
 * - Bullet points (•, -, 🔸, 🔴, 🟢, 🟡)
 * - Tabel sederhana
 * - Emoji preservation
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val warningColor = Color(0xFFFF9800) // Orange
    val successColor = Color(0xFF4CAF50) // Green

    val annotatedString = buildAnnotatedString {
        val lines = text.lines()

        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()

            when {
                // HEADERS - Baris dengan emoji header utama
                trimmed.isMainHeader() -> {
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = primaryColor
                    )) {
                        append(trimmed.cleanMarkdown())
                    }
                }
                // SUB-HEADERS - Section titles
                trimmed.isSubHeader() -> {
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = secondaryColor
                    )) {
                        append(trimmed.cleanMarkdown())
                    }
                }

                // WARNING ITEMS - 🔴 Red indicators
                trimmed.startsWith("🔴") -> {
                    parseLineWithBold(
                        line = trimmed,
                        defaultColor = color,
                        bulletColor = errorColor,
                        boldColor = errorColor
                    )
                }

                // CAUTION ITEMS - 🟡 Yellow/Orange indicators
                trimmed.startsWith("🟡") || trimmed.startsWith("🟠") -> {
                    parseLineWithBold(
                        line = trimmed,
                        defaultColor = color,
                        bulletColor = warningColor,
                        boldColor = warningColor
                    )
                }

                // POSITIVE ITEMS - 🟢 Green indicators
                trimmed.startsWith("🟢") || trimmed.startsWith("✅") -> {
                    parseLineWithBold(
                        line = trimmed,
                        defaultColor = color,
                        bulletColor = successColor,
                        boldColor = successColor
                    )
                }

                // BULLET POINTS - 🔸, •, -, →
                trimmed.isBulletPoint() -> {
                    parseLineWithBold(
                        line = trimmed,
                        defaultColor = color,
                        bulletColor = primaryColor,
                        boldColor = primaryColor
                    )
                }

                // TABLE ROWS - | Nutrisi | Jumlah |
                trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                    if (trimmed.contains("---")) {
                        // Table separator - skip or minimal
                        withStyle(SpanStyle(color = color.copy(alpha = 0.3f), fontSize = 12.sp)) {
                            append(trimmed)
                        }
                    } else {
                        parseTableRow(trimmed, color, primaryColor)
                    }
                }

                // RATING - ⭐⭐⭐⭐⭐
                trimmed.contains("⭐") -> {
                    withStyle(SpanStyle(
                        fontSize = 15.sp,
                        color = color
                    )) {
                        append(trimmed.cleanMarkdown())
                    }
                }

                // NORMAL TEXT - Parse bold (**text**) dalam text biasa
                else -> {
                    parseLineWithBold(
                        line = trimmed,
                        defaultColor = color,
                        bulletColor = color,
                        boldColor = primaryColor
                    )
                }
            }

            // Add newline (except last line)
            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 24.sp
        )
    )
}

/**
 * Cek apakah line adalah header utama (dengan emoji section)
 */
private fun String.isMainHeader(): Boolean {
    val headerEmojis = listOf("📦", "📊", "📈", "⚠️", "⚠", "📅", "💡", "✅", "🏥", "═")
    return headerEmojis.any { this.startsWith(it) } ||
            this.matches(Regex("^[A-Z][A-Z\\s]{3,}.*")) // ALL CAPS dengan min 4 karakter
}

/**
 * Cek apakah line adalah sub-header
 */
private fun String.isSubHeader(): Boolean {
    val subHeaderStarts = listOf("Takaran", "Jumlah sajian", "🕐", "📏", "⏰", "📝", "📊 Rating")
    return subHeaderStarts.any { this.startsWith(it, ignoreCase = true) }
}

/**
 * Cek apakah line adalah bullet point
 */
private fun String.isBulletPoint(): Boolean {
    val bulletStarts = listOf("•", "-", "→", "🔸", "▸", "▹", "‣", "⁃")
    val numberedPattern = Regex("^\\d+\\.\\s")
    return bulletStarts.any { this.trimStart().startsWith(it) } ||
            numberedPattern.containsMatchIn(this)
}

/**
 * Bersihkan markdown artifacts (* dan **) dari text
 */
private fun String.cleanMarkdown(): String {
    return this
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // **bold** → bold
        .replace(Regex("\\*([^*]+)\\*"), "$1")       // *italic* → italic
        .replace(Regex("^#+\\s*"), "")               // ### Header → Header
        .trim()
}

/**
 * Parse line dengan bold formatting (**text**)
 */
private fun androidx.compose.ui.text.AnnotatedString.Builder.parseLineWithBold(
    line: String,
    defaultColor: Color,
    bulletColor: Color,
    boldColor: Color
) {
    // Pattern untuk **bold text**
    val boldPattern = Regex("\\*\\*([^*]+)\\*\\*")
    var currentIndex = 0
    val cleanLine = line.trim()

    // Cek apakah ada bullet di awal
    val bulletMatch = Regex("^([•\\-→🔸🔴🟢🟡🟠▸▹‣⁃]|\\d+\\.)\\s*").find(cleanLine)

    if (bulletMatch != null) {
        // Render bullet dengan warna khusus
        withStyle(SpanStyle(color = bulletColor, fontWeight = FontWeight.Medium)) {
            append(bulletMatch.value)
        }
        currentIndex = bulletMatch.range.last + 1
    }

    val textAfterBullet = cleanLine.substring(currentIndex)
    var textIndex = 0

    // Find all bold patterns
    val matches = boldPattern.findAll(textAfterBullet).toList()

    if (matches.isEmpty()) {
        // No bold, just append clean text
        withStyle(SpanStyle(color = defaultColor)) {
            append(textAfterBullet.cleanMarkdown())
        }
    } else {
        // Has bold patterns - parse them
        for (match in matches) {
            // Text before bold
            if (match.range.first > textIndex) {
                val beforeText = textAfterBullet.substring(textIndex, match.range.first)
                withStyle(SpanStyle(color = defaultColor)) {
                    append(beforeText)
                }
            }

            // Bold text
            val boldText = match.groupValues[1]
            withStyle(SpanStyle(
                color = boldColor,
                fontWeight = FontWeight.Bold
            )) {
                append(boldText)
            }

            textIndex = match.range.last + 1
        }

        // Remaining text after last bold
        if (textIndex < textAfterBullet.length) {
            val remaining = textAfterBullet.substring(textIndex)
            withStyle(SpanStyle(color = defaultColor)) {
                append(remaining.cleanMarkdown())
            }
        }
    }
}

/**
 * Parse table row dengan formatting khusus
 */
private fun androidx.compose.ui.text.AnnotatedString.Builder.parseTableRow(
    line: String,
    defaultColor: Color,
    headerColor: Color
) {
    val cells = line.split("|").filter { it.isNotBlank() }.map { it.trim() }

    if (cells.isEmpty()) {
        append(line)
        return
    }

    // Cek apakah ini header row (biasanya row pertama atau mengandung "Nutrisi", "Jumlah")
    val isHeaderRow = cells.any { cell ->
        listOf("Nutrisi", "Jumlah", "% AKG", "Nilai").any {
            cell.contains(it, ignoreCase = true)
        }
    }

    append("  ") // Indent

    cells.forEachIndexed { index, cell ->
        val cleanCell = cell.cleanMarkdown()

        if (isHeaderRow) {
            withStyle(SpanStyle(
                fontWeight = FontWeight.Bold,
                color = headerColor,
                fontSize = 13.sp
            )) {
                append(cleanCell)
            }
        } else {
            // First column (nutrient name) - semi-bold
            if (index == 0) {
                withStyle(SpanStyle(
                    fontWeight = FontWeight.Medium,
                    color = defaultColor
                )) {
                    append(cleanCell)
                }
            } else {
                withStyle(SpanStyle(color = defaultColor)) {
                    append(cleanCell)
                }
            }
        }

        // Add separator between cells
        if (index < cells.size - 1) {
            withStyle(SpanStyle(color = defaultColor.copy(alpha = 0.5f))) {
                append("  •  ")
            }
        }
    }
}
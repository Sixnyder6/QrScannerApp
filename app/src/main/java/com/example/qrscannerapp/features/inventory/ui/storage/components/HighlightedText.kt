package com.example.qrscannerapp.features.inventory.ui.storage.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.example.qrscannerapp.StardustSuccess

@Composable
fun HighlightedText(
    text: String,
    highlight: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (highlight.isBlank()) {
        Text(text = text, modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow)
        return
    }
    val annotatedString = buildAnnotatedString {
        var startIndex = 0
        while (startIndex < text.length) {
            val index = text.indexOf(highlight, startIndex, ignoreCase = true)
            if (index == -1) {
                append(text.substring(startIndex))
                break
            }
            append(text.substring(startIndex, index))
            withStyle(style = SpanStyle(background = StardustSuccess.copy(alpha = 0.3f), fontWeight = fontWeight ?: FontWeight.Normal)) {
                append(text.substring(index, index + highlight.length))
            }
            startIndex = index + highlight.length
        }
    }
    Text(text = annotatedString, modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow)
}

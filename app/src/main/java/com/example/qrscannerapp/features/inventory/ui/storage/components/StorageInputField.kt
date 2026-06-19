package com.example.qrscannerapp.features.inventory.ui.storage.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary

@Composable
fun StorageInputField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue   = if (focused) StardustPrimary.copy(alpha = 0.65f) else Color.Transparent,
        animationSpec = tween(180), label = "border"
    )
    val iconTint by animateColorAsState(
        targetValue   = if (focused) StardustPrimary else StardustTextSecondary.copy(alpha = 0.4f),
        animationSpec = tween(180), label = "icon"
    )
    Box(
        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(13.dp)).background(StardustGlassBg)
            .drawBehind {
                drawRoundRect(color = borderColor, cornerRadius = CornerRadius(13.dp.toPx()), style = Stroke(width = 1.5.dp.toPx()))
            }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value, onValueChange = { onChange(it) }, singleLine = true,
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                textStyle = TextStyle(color = StardustTextPrimary, fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = Brush.verticalGradient(listOf(StardustPrimary, StardustPrimary)),
                decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, color = StardustTextSecondary.copy(alpha = 0.38f), fontSize = 15.sp); inner() } }
            )
        }
    }
}

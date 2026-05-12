package com.example.qrscannerapp.features.homescreen.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

// Apple-style continuous corner squircle — same as before, one shared instance.
private val IconSquircle = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = size.minDimension * 0.32f
    moveTo(0f, h * 0.5f)
    cubicTo(0f, r * 0.55f, r * 0.55f, 0f, w * 0.5f, 0f)
    cubicTo(w - r * 0.55f, 0f, w, r * 0.55f, w, h * 0.5f)
    cubicTo(w, h - r * 0.55f, w - r * 0.55f, h, w * 0.5f, h)
    cubicTo(r * 0.55f, h, 0f, h - r * 0.55f, 0f, h * 0.5f)
    close()
}

/**
 * iOS/visionOS app icon tile with BREATHING glow.
 *
 * Each icon receives a [phaseOffset] (0f..1f) so it starts at a different point
 * in the breath cycle. This makes the whole grid feel organic — not like a room
 * of lightbulbs blinking in sync.
 *
 * Breath math: glow alpha = base ± amplitude * sin(2π * (t + phase))
 *   base      = 0.36f   (resting glow, brighter than before at +30%)
 *   amplitude = 0.10f   (±10% = 6-8% of visible range per spec)
 *   period    = 3-4s    (slow, biological feel)
 *
 * @param phaseOffset 0f..1f — caller derives from icon index or label hash.
 */
@Composable
fun AppIconTile(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = accentColor,
    iconSize: Int = 56,
    phaseOffset: Float = 0f,
    breathPhase: Float = 0f,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "tile_scale"
    )

    val breathAlpha = 0.42f + 0.13f * sin((breathPhase + phaseOffset) * 2f * PI.toFloat()).coerceIn(-1f, 1f)
    val glowAlpha = if (pressed) 0.65f else breathAlpha

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try { tryAwaitRelease() } finally { pressed = false }
                    },
                    onTap = { onClick() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size((iconSize + 24).dp)
                .drawBehind {
                    // External radial glow — the breathing happens entirely here.
                    // The squircle body stays static; only the halo pulsates.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = glowAlpha),
                                accentColor.copy(alpha = glowAlpha * 0.45f),
                                accentColor.copy(alpha = glowAlpha * 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension * 0.68f
                        ),
                        radius = size.minDimension * 0.68f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Squircle body
            Box(
                modifier = Modifier
                    .size(iconSize.dp)
                    .clip(IconSquircle)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.60f),
                                accentColor.copy(alpha = 0.22f)
                            )
                        )
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = IconSquircle
                    )
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.30f),
                            start = Offset(size.width * 0.18f, 1.5f),
                            end = Offset(size.width * 0.82f, 1.5f),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = accentColor.copy(alpha = 0.75f),
                            start = Offset(size.width * 0.15f, size.height - 0.5f),
                            end = Offset(size.width * 0.85f, size.height - 0.5f),
                            strokeWidth = 1f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size((iconSize * 0.42f).dp)
                )
            }

            // Badge
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .graphicsLayer {
                            translationX = -10f
                            translationY = 10f
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeColor)
                        .border(
                            width = 1.dp,
                            color = Color(0xFF02020A),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeText.length > 2) "•" else badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
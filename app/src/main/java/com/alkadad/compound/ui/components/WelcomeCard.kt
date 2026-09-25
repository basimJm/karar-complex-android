package com.alkadad.compound.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** Animated greeting shown at the top of the dashboard. */
@Composable
fun WelcomeCard(
    userName: String?,
    tableCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val greeting = remember {
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 12) "صباح الخير" else "مساء الخير"
    }
    val headline = if (userName.isNullOrBlank()) "أهلاً بك" else "أهلاً بك، $userName"
    val typed by rememberTypewriterText(headline, startDelayMs = 350)

    // Waving hand
    val wave = rememberInfiniteTransition(label = "wave")
    val waveAngle by wave.animateFloat(
        initialValue = -12f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "waveAngle"
    )

    // Count up the number of tables
    var countTarget by remember { mutableIntStateOf(0) }
    LaunchedEffect(tableCount) { countTarget = tableCount }
    val animatedCount by animateIntAsState(
        targetValue = countTarget,
        animationSpec = tween(900, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "tableCount"
    )
    val showSubtitle by rememberDelayedVisible(450)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(colors.primaryContainer, colors.secondaryContainer.copy(alpha = 0.7f))
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "👋",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = waveAngle
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.7f, 0.9f)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Reserve the final height so the card doesn't grow while typing.
                    Box {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.onPrimaryContainer.copy(alpha = 0f)
                        )
                        Text(
                            text = typed,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.onPrimaryContainer
                        )
                    }
                    AnimatedVisibility(
                        visible = showSubtitle,
                        enter = fadeIn(tween(AppMotion.Long)) + slideInVertically(tween(AppMotion.Long, easing = AppMotion.EmphasizedDecelerate)) { it / 2 }
                    ) {
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = colors.primary, shape = CircleShape) {
                                Text(
                                    text = "$animatedCount",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "جداول في النظام",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .floating(amplitude = 4.dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerLowest.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Apartment,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

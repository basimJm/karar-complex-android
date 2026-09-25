package com.alkadad.compound.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Shared motion tokens so every animation in the app feels like one system. */
object AppMotion {
    const val Short = 180
    const val Medium = 320
    const val Long = 480
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    fun <T> bouncy() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    fun <T> gentle() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

/** Shrinks slightly while pressed, springs back on release. Pass the same source given to the clickable. */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = AppMotion.bouncy(),
        label = "pressScale"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Fades + slides content up when it first enters composition. [index] staggers the first
 * screenful; items scrolled into view later animate immediately.
 */
fun Modifier.enterAnimation(index: Int = 0, offsetY: Dp = 28.dp, staggerMs: Long = 45): Modifier = composed {
    val offsetPx = with(LocalDensity.current) { offsetY.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceIn(0, 8)) * staggerMs)
        progress.animateTo(1f, tween(AppMotion.Long, easing = AppMotion.EmphasizedDecelerate))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * offsetPx
        val s = 0.94f + 0.06f * p
        scaleX = s
        scaleY = s
    }
}

/** Gentle infinite up/down float, for hero icons and illustrations. */
fun Modifier.floating(amplitude: Dp = 6.dp, durationMs: Int = 2200): Modifier = composed {
    val amplitudePx = with(LocalDensity.current) { amplitude.toPx() }
    val transition = rememberInfiniteTransition(label = "floating")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatingOffset"
    )
    graphicsLayer { translationY = offset * amplitudePx }
}

/** Slightly lifts a text field while it has focus. */
fun Modifier.focusLift(): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.015f else 1f, AppMotion.gentle(), label = "focusLift")
    onFocusChanged { focused = it.isFocused }
        .graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Horizontal shake, e.g. on a failed login. Call [ShakeController.shake] to trigger. */
class ShakeController {
    internal val offset = Animatable(0f)
    suspend fun shake() {
        offset.snapTo(0f)
        for (target in listOf(-18f, 16f, -12f, 9f, -5f, 0f)) {
            offset.animateTo(target, tween(55, easing = LinearEasing))
        }
    }
}

@Composable
fun rememberShakeController() = remember { ShakeController() }

fun Modifier.shake(controller: ShakeController): Modifier =
    graphicsLayer { translationX = controller.offset.value }

/** Reveals [text] character by character. */
@Composable
fun rememberTypewriterText(text: String, startDelayMs: Long = 250, charDelayMs: Long = 38): State<String> {
    val shown = remember(text) { mutableStateOf("") }
    LaunchedEffect(text) {
        delay(startDelayMs)
        for (i in 1..text.length) {
            shown.value = text.take(i)
            delay(charDelayMs)
        }
    }
    return shown
}

/** Becomes true after [delayMs] — for choreographing sequential entrances. */
@Composable
fun rememberDelayedVisible(delayMs: Long): State<Boolean> {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        visible.value = true
    }
    return visible
}

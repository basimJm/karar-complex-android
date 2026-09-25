package com.alkadad.compound.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/**
 * Full-screen swipeable image preview. Pinch to zoom, double-tap to toggle zoom.
 * [onDelete] (optional) receives the index of the image currently shown.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
    onDelete: ((Int) -> Unit)? = null,
) {
    if (imageUrls.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, imageUrls.lastIndex),
        pageCount = { imageUrls.size }
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                var targetScale by remember { mutableFloatStateOf(1f) }
                val scale by animateFloatAsState(targetScale, AppMotion.gentle(), label = "viewerZoom")
                LaunchedEffect(pagerState.currentPage) { if (pagerState.currentPage != page) targetScale = 1f }

                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current).data(imageUrls[page]).crossfade(AppMotion.Long).build()
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .pointerInput(page) {
                            detectTapGestures(onDoubleTap = { targetScale = if (targetScale > 1.1f) 1f else 2.5f })
                        }
                        .pointerInput(page) {
                            // Only two-finger gestures zoom; one-finger swipes stay with the pager.
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.count { it.pressed } >= 2) {
                                        targetScale = (targetScale * event.calculateZoom()).coerceIn(1f, 5f)
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = { onDelete(pagerState.currentPage) },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الصورة", tint = Color(0xFFFCA5A5))
                    }
                }
            }

            AnimatedContent(
                targetState = pagerState.currentPage,
                transitionSpec = { (fadeIn(tween(AppMotion.Short)) + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut(tween(AppMotion.Short))) },
                label = "viewerCounter",
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { page ->
                Text(
                    text = "${page + 1} / ${imageUrls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }
    }
}

package com.alkadad.compound.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alkadad.compound.data.repository.AuthRepository
import com.alkadad.compound.ui.components.*
import com.alkadad.compound.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // UI-only state
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val header = LocalHeaderColors.current
    val colors = MaterialTheme.colorScheme

    // AppMotion
    val shakeController = rememberShakeController()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) shakeController.shake()
    }
    val logoScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
    }
    val ambient = rememberInfiniteTransition(label = "loginAmbient")
    val drift by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val buttonInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(header.container, header.containerEnd, colors.background),
                    startY = 0f
                )
            )
    ) {
        // Soft decorative circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.06f), radius = size.width * (0.52f + 0.06f * drift), center = Offset(size.width * (0.95f - 0.08f * drift), size.height * (0.05f + 0.03f * drift)))
            drawCircle(Mint400.copy(alpha = 0.14f), radius = size.width * (0.35f - 0.04f * drift), center = Offset(size.width * (0.02f + 0.10f * drift), size.height * (0.30f - 0.04f * drift)))
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                BrandMark(
                    size = 96.dp,
                    modifier = Modifier
                        .floating(amplitude = 5.dp, durationMs = 2600)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            rotationZ = (1f - logoScale.value) * -90f
                        }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = "مجمعات النداء السكنية",
                    style = MaterialTheme.typography.headlineMedium,
                    color = header.content,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.enterAnimation(index = 2)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "نظام إدارة الجداول",
                    style = MaterialTheme.typography.bodyMedium,
                    color = header.content.copy(alpha = 0.8f),
                    modifier = Modifier.enterAnimation(index = 3)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Login Card
                Card(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .enterAnimation(index = 4, offsetY = 64.dp)
                        .shake(shakeController),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تسجيل الدخول",
                            modifier = Modifier.enterAnimation(index = 5),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أدخل بياناتك للمتابعة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("البريد الإلكتروني") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth().enterAnimation(index = 6).focusLift(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Crossfade(targetState = passwordVisible, animationSpec = tween(AppMotion.Short), label = "passwordIcon") { visible ->
                                        Icon(
                                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (visible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().enterAnimation(index = 7).focusLift(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        // Error Message
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                                color = colors.errorContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = colors.onErrorContainer, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        color = colors.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    val result = authRepository.login(email, password)
                                    isLoading = false
                                    result.onSuccess {
                                        onLoginSuccess()
                                    }
                                    result.onFailure {
                                        errorMessage = "خطأ في تسجيل الدخول"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .enterAnimation(index = 8)
                                .pressScale(buttonInteraction),
                            interactionSource = buttonInteraction,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                        ) {
                            AnimatedContent(
                                targetState = isLoading,
                                transitionSpec = {
                                    (fadeIn(tween(AppMotion.Medium)) + scaleIn(initialScale = 0.7f, animationSpec = tween(AppMotion.Medium)))
                                        .togetherWith(fadeOut(tween(AppMotion.Short)) + scaleOut(targetScale = 0.7f, animationSpec = tween(AppMotion.Short)))
                                },
                                label = "loginButtonContent"
                            ) { loading ->
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = colors.primary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("تسجيل الدخول", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier.enterAnimation(index = 9),
                    color = colors.surfaceContainerHigh.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "للمسؤولين فقط - تواصل مع المدير للحصول على حساب",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

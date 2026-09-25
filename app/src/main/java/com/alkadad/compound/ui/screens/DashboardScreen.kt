package com.alkadad.compound.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.request.ImageRequest
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.alkadad.compound.data.model.Table
import com.alkadad.compound.data.repository.AuthRepository
import com.alkadad.compound.data.repository.TableRepository
import com.alkadad.compound.ui.components.*
import com.alkadad.compound.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onTableClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tableRepository = remember { TableRepository(context) }
    val authRepository = remember { AuthRepository(context) }

    var tables by remember { mutableStateOf<List<Table>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newTableName by remember { mutableStateOf("") }
    var newTableDescription by remember { mutableStateOf("") }

    fun loadTables() {
        scope.launch {
            isLoading = true
            val result = tableRepository.getTables()
            result.onSuccess { tables = it }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadTables()
    }

    var userName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        authRepository.getMe().onSuccess { userName = it.name }
    }

    val colors = MaterialTheme.colorScheme
    val header = LocalHeaderColors.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val gridState = rememberLazyGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }
    val fabInteraction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.headerBackground(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(size = 40.dp, modifier = Modifier.enterAnimation(offsetY = 0.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مجمعات النداء السكنية",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "نظام إدارة الجداول",
                                style = MaterialTheme.typography.labelMedium,
                                color = header.content.copy(alpha = 0.75f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            authRepository.logout()
                            onLogout()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "تسجيل الخروج")
                    }
                },
                colors = headerTopAppBarColors(),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCreateDialog = true
                },
                expanded = fabExpanded,
                interactionSource = fabInteraction,
                modifier = Modifier.enterAnimation(index = 4, offsetY = 48.dp).pressScale(fabInteraction, 0.92f),
                containerColor = colors.secondary,
                contentColor = colors.onSecondary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("جدول جديد", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { paddingValues ->
        val screenState = when {
            isLoading -> "loading"
            tables.isEmpty() -> "empty"
            else -> "content"
        }
        Crossfade(
            targetState = screenState,
            animationSpec = tween(AppMotion.Medium),
            label = "dashboardState",
            modifier = Modifier.padding(paddingValues)
        ) { state ->
            when (state) {
                "loading" -> LoadingState()
                "empty" -> EmptyState(
                    icon = Icons.Default.GridView,
                    title = "\u0644\u0627 \u062a\u0648\u062c\u062f \u062c\u062f\u0627\u0648\u0644",
                    subtitle = "\u0627\u0636\u063a\u0637 + \u0644\u0625\u0646\u0634\u0627\u0621 \u0623\u0648\u0644 \u062c\u062f\u0648\u0644"
                )
                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "welcome") {
                        WelcomeCard(
                            userName = userName,
                            tableCount = tables.size,
                            modifier = Modifier.enterAnimation(index = 0)
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "section") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).enterAnimation(index = 1),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\u0627\u0644\u062c\u062f\u0627\u0648\u0644",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            AnimatedContent(
                                targetState = tables.size,
                                transitionSpec = {
                                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                                },
                                label = "tableCountBadge"
                            ) { count ->
                                Surface(color = colors.primaryContainer, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    itemsIndexed(tables, key = { _, table -> table.id }) { index, table ->
                        TableCard(
                            table = table,
                            onClick = { onTableClick(table.id) },
                            modifier = Modifier
                                .animateItemPlacement(tween(AppMotion.Medium, easing = AppMotion.Emphasized))
                                .enterAnimation(index = index + 2)
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            icon = { DialogIcon(Icons.Default.PostAdd, colors.primaryContainer, colors.onPrimaryContainer) },
            title = { Text(text = "إضافة جدول جديد", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTableName,
                        onValueChange = { newTableName = it },
                        label = { Text("اسم الجدول") },
                        modifier = Modifier.fillMaxWidth().focusLift(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTableDescription,
                        onValueChange = { newTableDescription = it },
                        label = { Text("الوصف (اختياري)") },
                        modifier = Modifier.fillMaxWidth().focusLift(),
                        maxLines = 3,
                        shape = MaterialTheme.shapes.small
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = tableRepository.createTable(
                                com.alkadad.compound.data.model.CreateTableRequest(
                                    name = newTableName,
                                    description = newTableDescription.ifBlank { null }
                                )
                            )
                            result.onSuccess {
                                newTableName = ""
                                newTableDescription = ""
                                showCreateDialog = false
                                loadTables()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                ) {
                    Text("إنشاء")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newTableName = ""
                    newTableDescription = ""
                }) {
                    Text("إلغاء", color = colors.onSurfaceVariant)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableCard(
    table: Table,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().pressScale(interaction),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 11f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(colors.surfaceContainerHigh)
            ) {
                if (table.houseCardImage != null) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(table.houseCardImage)
                                .crossfade(AppMotion.Long)
                                .build()
                        ),
                        contentDescription = table.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(colors.primaryContainer, colors.primaryContainer.copy(alpha = 0.45f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HomeWork,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = colors.onPrimaryContainer.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (table.description != null) {
                    Text(
                        text = table.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = colors.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = try { table.createdAt.substring(0, 10) } catch (_: Exception) { table.createdAt },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.outline
                    )
                }
            }
        }
    }
}

package com.alkadad.compound.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.alkadad.compound.ui.components.BrandMark
import com.alkadad.compound.ui.components.DialogIcon
import com.alkadad.compound.ui.components.EmptyState
import com.alkadad.compound.ui.components.LoadingState
import com.alkadad.compound.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    val colors = MaterialTheme.colorScheme
    val header = LocalHeaderColors.current

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.headerBackground(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(size = 40.dp)
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
                colors = headerTopAppBarColors()
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = colors.secondary,
                contentColor = colors.onSecondary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("جدول جديد", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            LoadingState(modifier = Modifier.padding(paddingValues))
        } else if (tables.isEmpty()) {
            EmptyState(
                icon = Icons.Default.GridView,
                title = "لا توجد جداول",
                subtitle = "اضغط + لإنشاء أول جدول",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الجداول",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(color = colors.primaryContainer, shape = RoundedCornerShape(50)) {
                            Text(
                                text = "${tables.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                items(tables) { table ->
                    TableCard(
                        table = table,
                        onClick = { onTableClick(table.id) }
                    )
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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTableDescription,
                        onValueChange = { newTableDescription = it },
                        label = { Text("الوصف (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
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
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                        painter = rememberAsyncImagePainter(model = table.houseCardImage),
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

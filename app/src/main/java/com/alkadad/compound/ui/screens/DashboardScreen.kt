package com.alkadad.compound.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.alkadad.compound.data.model.Table
import com.alkadad.compound.data.repository.AuthRepository
import com.alkadad.compound.data.repository.TableRepository
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Gold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "\u0645",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "\u0645\u062c\u0645\u0639 \u0627\u0644\u0643\u062f\u0627\u062f \u0627\u0644\u0633\u0643\u0646\u064a",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\u0646\u0638\u0627\u0645 \u0625\u062f\u0627\u0631\u0629 \u0627\u0644\u062c\u062f\u0627\u0648\u0644",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
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
                        Icon(Icons.Default.Logout, contentDescription = "\u062a\u0633\u062c\u064a\u0644 \u0627\u0644\u062e\u0631\u0648\u062c")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = White,
                    actionIconContentColor = White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Gold,
                contentColor = White
            ) {
                Icon(Icons.Default.Add, contentDescription = "\u0625\u0636\u0627\u0641\u0629 \u062c\u062f\u0648\u0644")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (tables.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.GridOn,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Gray300
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "\u0644\u0627 \u062a\u0648\u062c\u062f \u062c\u062f\u0627\u0648\u0644",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Gray600
                    )
                    Text(
                        text = "\u0627\u0636\u063a\u0637 + \u0644\u0625\u0646\u0634\u0627\u0621 \u0623\u0648\u0644 \u062c\u062f\u0648\u0644",
                        fontSize = 14.sp,
                        color = Gray400
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
            title = { Text(text = "\u0625\u0636\u0627\u0641\u0629 \u062c\u062f\u0648\u0644 \u062c\u062f\u064a\u062f", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTableName,
                        onValueChange = { newTableName = it },
                        label = { Text("\u0627\u0633\u0645 \u0627\u0644\u062c\u062f\u0648\u0644") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTableDescription,
                        onValueChange = { newTableDescription = it },
                        label = { Text("\u0627\u0644\u0648\u0635\u0641 (\u0627\u062e\u062a\u064a\u0627\u0631\u064a)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
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
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("\u0625\u0646\u0634\u0627\u0621")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newTableName = ""
                    newTableDescription = ""
                }) {
                    Text("\u0625\u0644\u063a\u0627\u0621", color = Gray600)
                }
            }
        )
    }
}

@Composable
fun TableCard(
    table: Table,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (table.houseCardImage != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = table.houseCardImage),
                    contentDescription = table.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(PrimaryLight.copy(alpha = 0.2f), Primary.copy(alpha = 0.1f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Primary.copy(alpha = 0.5f)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = table.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (table.description != null) {
                    Text(
                        text = table.description,
                        fontSize = 12.sp,
                        color = Gray500,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = try { table.createdAt.substring(0, 10) } catch (_: Exception) { table.createdAt },
                    fontSize = 10.sp,
                    color = Gray400
                )
            }
        }
    }
}

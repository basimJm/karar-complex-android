package com.alkadad.compound.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.alkadad.compound.data.model.*
import com.alkadad.compound.data.repository.RowRepository
import com.alkadad.compound.data.repository.TableRepository
import com.alkadad.compound.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val DEBOUNCE_MS = 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDetailScreen(
    tableId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tableRepository = remember { TableRepository(context) }
    val rowRepository = remember { RowRepository(context) }

    var table by remember { mutableStateOf<Table?>(null) }
    var rows by remember { mutableStateOf<List<Row>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showRowDialog by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<Row?>(null) }
    var rowFormData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var rowName by remember { mutableStateOf("") }
    var selectedRowForImages by remember { mutableStateOf<Row?>(null) }
    var showImageOptions by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteImageDialog by remember { mutableStateOf(false) }
    var pendingDeleteImageIndex by remember { mutableStateOf(0) }
    var pendingDeleteRowId by remember { mutableStateOf("") }
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedRow by remember { mutableStateOf<Row?>(null) }

    val imageUri = remember { mutableStateOf<Uri?>(null) }

    // Debounce search
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(searchInput) {
        debounceJob?.cancel()
        debounceJob = launch {
            delay(DEBOUNCE_MS)
            searchQuery = searchInput
        }
    }

    fun refreshTable() {
        scope.launch {
            isLoading = true
            val result = rowRepository.getRows(tableId, searchQuery.ifBlank { null })
            result.onSuccess { rows = it }
            val tableResult = tableRepository.getTable(tableId)
            tableResult.onSuccess { table = it.first }
            isLoading = false
        }
    }

    fun handleCameraResult(success: Boolean) {
        if (success) {
            imageUri.value?.let {
                scope.launch {
                    val file = File(context.cacheDir, "temp_image.jpg")
                    if (file.exists()) {
                        if (showImageOptions) {
                            selectedRowForImages?.let { row ->
                                rowRepository.uploadRowImages(tableId, row.id, listOf(file))
                                refreshTable()
                            }
                        } else {
                            tableRepository.uploadTableImage(tableId, file)
                            refreshTable()
                        }
                    }
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> handleCameraResult(success) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val tempFile = File(context.cacheDir, "temp_image.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            imageUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri.value = it
            scope.launch {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "temp_image.jpg")
                inputStream?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                if (showImageOptions) {
                    selectedRowForImages?.let { row ->
                        rowRepository.uploadRowImages(tableId, row.id, listOf(file))
                        refreshTable()
                    }
                } else {
                    tableRepository.uploadTableImage(tableId, file)
                    refreshTable()
                }
            }
        }
    }

    LaunchedEffect(tableId, searchQuery) {
        refreshTable()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = table?.name ?: "\u0627\u0644\u062c\u062f\u0648\u0644", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "\u0631\u062c\u0648\u0639")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showImageOptions = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val tempFile = File(context.cacheDir, "temp_image.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                            imageUri.value = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                    IconButton(onClick = {
                        showImageOptions = false
                        galleryLauncher.launch("image/*")
                    }) { Icon(Icons.Default.PhotoLibrary, contentDescription = null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = White, navigationIconContentColor = White, actionIconContentColor = White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingRow = null; rowFormData = emptyMap(); rowName = ""; showRowDialog = true },
                containerColor = Gold, contentColor = White
            ) { Icon(Icons.Default.Add, contentDescription = null) }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("\u0628\u062d\u062b \u0628\u0627\u0644\u0627\u0633\u0645...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = { searchInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            table?.houseCardImage?.let { imageUrl ->
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                    Column {
                        Text(text = "\u0635\u0648\u0631\u0629 \u0628\u0637\u0627\u0642\u0629 \u0627\u0644\u0645\u0646\u0632\u0644", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = Gray800)
                        Image(
                            painter = rememberAsyncImagePainter(model = imageUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (rows.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.List, null, modifier = Modifier.size(64.dp), tint = Gray300)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("\u0644\u0627 \u062a\u0648\u062c\u062f \u0635\u0641\u0648\u0641", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Gray600)
                        Text("\u0627\u0636\u063a\u0637 + \u0644\u0625\u0636\u0627\u0641\u0629 \u0623\u0648\u0644 \u0635\u0641", fontSize = 14.sp, color = Gray400)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(rows) { _, row ->
                        RowCard(
                            row = row, table = table,
                            onRowClick = { selectedRow = row; showDetailDialog = true },
                            onEdit = { editingRow = row; rowName = row.name; rowFormData = row.data ?: emptyMap(); showRowDialog = true },
                            onDelete = { scope.launch { rowRepository.deleteRow(tableId, row.id); refreshTable() } },
                            onAddImages = { selectedRowForImages = row; showImageOptions = true; galleryLauncher.launch("image/*") },
                            onDeleteImage = { idx ->
                                pendingDeleteImageIndex = idx
                                pendingDeleteRowId = row.id
                                showDeleteImageDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRowDialog) {
        AlertDialog(
            onDismissRequest = { showRowDialog = false },
            title = { Text(if (editingRow != null) "\u062a\u0639\u062f\u064a\u0644 \u0627\u0644\u0635\u0641" else "\u0625\u0636\u0627\u0641\u0629 \u0635\u0641 \u062c\u062f\u064a\u062f", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = rowName,
                        onValueChange = { rowName = it },
                        label = { Text("\u0627\u0633\u0645 \u0627\u0644\u0635\u0641 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    table?.columns?.forEach { column ->
                        OutlinedTextField(
                            value = rowFormData[column.key] ?: "",
                            onValueChange = { rowFormData = rowFormData.toMutableMap().apply { put(column.key, it) } },
                            label = { Text(column.label) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rowName.isNotBlank()) {
                            scope.launch {
                                if (editingRow != null) {
                                    rowRepository.updateRow(tableId, editingRow!!.id, rowName, rowFormData)
                                } else {
                                    rowRepository.createRow(tableId, rowName, rowFormData)
                                }
                                showRowDialog = false
                                refreshTable()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = rowName.isNotBlank()
                ) { Text(if (editingRow != null) "\u062a\u062d\u062f\u064a\u062b" else "\u0625\u0636\u0627\u0641\u0629") }
            },
            dismissButton = { TextButton(onClick = { showRowDialog = false }) { Text("\u0625\u0644\u063a\u0627\u0621", color = Gray600) } }
        )
    }

    if (showDeleteImageDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteImageDialog = false },
            title = { Text("\u062a\u0623\u0643\u064a\u062f \u062d\u0630\u0641 \u0627\u0644\u0635\u0648\u0631\u0629", fontWeight = FontWeight.Bold) },
            text = { Text("\u0647\u0644 \u0623\u0646\u062a \u0645\u062a\u0623\u0643\u062f \u0645\u0646 \u062d\u0630\u0641 \u0647\u0630\u0647 \u0627\u0644\u0635\u0648\u0631\u0629\u0131") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            rowRepository.deleteRowImage(tableId, pendingDeleteRowId, pendingDeleteImageIndex)
                            showDeleteImageDialog = false
                            refreshTable()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text("\u062d\u0630\u0641") }
            },
            dismissButton = { TextButton(onClick = { showDeleteImageDialog = false }) { Text("\u0625\u0644\u063a\u0627\u0621") } }
        )
    }

    // Row Detail Dialog
    if (showDetailDialog && selectedRow != null) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false; selectedRow = null },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("\u062a\u0641\u0627\u0635\u064a\u0644 \u0627\u0644\u0635\u0641", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("\u0627\u0633\u0645 \u0627\u0644\u062c\u062f\u0648\u0644", fontSize = 12.sp, color = Gray500)
                            Text(table?.name ?: "", fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Gray100),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("\u0627\u0633\u0645 \u0627\u0644\u0635\u0641", fontSize = 12.sp, color = Gray500)
                            Text(selectedRow!!.name.ifBlank { "\u0635\u0641 \u062c\u062f\u064a\u062f" }, fontWeight = FontWeight.Bold, color = Gray800, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    table?.columns?.forEach { column ->
                        val value = selectedRow!!.data?.get(column.key)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Gray100),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(column.label, fontSize = 12.sp, color = Gray500)
                                Text(value ?: "-", fontWeight = FontWeight.Medium, color = Gray800)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    selectedRow?.images?.let { images ->
                        if (images.isNotEmpty()) {
                            Text("\u0627\u0644\u0635\u0648\u0631 (${images.size})", fontWeight = FontWeight.Medium, color = Gray600, modifier = Modifier.padding(vertical = 4.dp))
                            images.forEachIndexed { idx, img ->
                                Image(
                                    painter = rememberAsyncImagePainter(model = img.url),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDetailDialog = false
                        editingRow = selectedRow
                        rowName = selectedRow!!.name
                        rowFormData = selectedRow!!.data ?: emptyMap()
                        showRowDialog = true
                        selectedRow = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("\u062a\u0639\u062f\u064a\u0644") }
            },
            dismissButton = {
                TextButton(onClick = { showDetailDialog = false; selectedRow = null }) {
                    Text("\u0625\u063a\u0644\u0627\u0621", color = Gray600)
                }
            }
        )
    }
}

@Composable
fun RowCard(
    row: Row, table: Table?,
    onRowClick: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit, onAddImages: () -> Unit, onDeleteImage: (Int) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onRowClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = row.name.ifBlank { "\u0635\u0641 \u062c\u062f\u064a\u062f" }, fontWeight = FontWeight.Bold, color = Primary, fontSize = 16.sp)
                Row {
                    IconButton(onClick = { onEdit() }) { Icon(Icons.Default.Edit, null, tint = Primary) }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, null, tint = Red) }
                }
            }

            table?.columns?.forEach { column ->
                val value = row.data?.get(column.key)
                if (!value.isNullOrBlank()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(text = "${column.label}: ", fontWeight = FontWeight.Medium, color = Gray600)
                        Text(text = value, color = Gray800)
                    }
                }
            }

            if (!row.images.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("\u0627\u0644\u0635\u0648\u0631 (${row.images.size})", fontWeight = FontWeight.Medium, color = Gray600)
                    IconButton(onClick = onAddImages) { Icon(Icons.Default.AddPhotoAlternate, null, tint = Primary) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.images.take(3).forEachIndexed { idx, img ->
                        Image(
                            painter = rememberAsyncImagePainter(model = img.url), contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).clickable { onDeleteImage(idx) },
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (row.images.size > 3) {
                        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Gray100), contentAlignment = Alignment.Center) {
                            Text("+${row.images.size - 3}", color = Gray600, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onAddImages) { Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.padding(start = 8.dp)); Text("\u0625\u0636\u0627\u0641\u0629 \u0635\u0648\u0631") }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("\u062a\u0623\u0643\u064a\u062f \u0627\u0644\u062d\u0630\u0641") },
            text = { Text("\u0647\u0644 \u0623\u0646\u062a \u0645\u062a\u0623\u0643\u062f \u0645\u0646 \u062d\u0630\u0641 \u0647\u0630\u0647 \u0627\u0644\u0635\u0641\u013f") },
            confirmButton = { Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("\u062d\u0630\u0641") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("\u0625\u0644\u063a\u0627\u0621") } }
        )
    }
}

package com.alkadad.compound.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.alkadad.compound.data.image.ImageCompressor
import com.alkadad.compound.data.model.Row
import com.alkadad.compound.data.model.Table
import com.alkadad.compound.data.repository.RowRepository
import com.alkadad.compound.data.repository.TableRepository
import com.alkadad.compound.ui.components.*
import com.alkadad.compound.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RowDetailScreen(
    tableId: String,
    rowId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tableRepository = remember { TableRepository(context) }
    val rowRepository = remember { RowRepository(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    var row by remember { mutableStateOf<Row?>(null) }
    var table by remember { mutableStateOf<Table?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }

    var isUploading by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isSaving by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun load() {
        scope.launch {
            isLoading = true
            rowRepository.getRow(tableId, rowId)
                .onSuccess { row = it; loadFailed = false }
                .onFailure {
                    Log.w("RowDetailScreen", "Failed to load row", it)
                    if (row == null) loadFailed = true
                }
            tableRepository.getTable(tableId).onSuccess { table = it.first }
            isLoading = false
        }
    }

    LaunchedEffect(tableId, rowId) { load() }

    fun uploadImage(source: Uri) {
        scope.launch {
            isUploading = true
            val file = ImageCompressor.prepareForUpload(context, source)
            val result: Result<Any> = if (file == null) {
                Result.failure(Exception("Could not read image"))
            } else {
                rowRepository.uploadRowImages(tableId, rowId, listOf(file)).also { file.delete() }
            }
            context.cacheDir.listFiles { f -> f.name.startsWith("camera_") }?.forEach { it.delete() }
            isUploading = false
            result
                .onSuccess {
                    showMessage("تم رفع الصورة بنجاح")
                    load()
                }
                .onFailure {
                    Log.w("RowDetailScreen", "Image upload failed", it)
                    showMessage("تعذر رفع الصورة، حاول مرة أخرى")
                }
        }
    }

    val imagePicker = rememberImagePicker(
        onImagePicked = { uploadImage(it) },
        onCameraPermissionDenied = { showMessage("يلزم السماح باستخدام الكاميرا") }
    )

    fun deleteImage(index: Int) {
        scope.launch {
            isDeleting = true
            val result = rowRepository.deleteRowImage(tableId, rowId, index)
            isDeleting = false
            pendingDeleteIndex = null
            result
                .onSuccess {
                    viewerIndex = null
                    showMessage("تم حذف الصورة")
                    load()
                }
                .onFailure {
                    Log.w("RowDetailScreen", "Image delete failed", it)
                    showMessage("تعذر حذف الصورة، حاول مرة أخرى")
                }
        }
    }

    fun openEdit() {
        val current = row ?: return
        editName = current.name
        editData = current.data ?: emptyMap()
        showEditDialog = true
    }

    val colors = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val fabInteraction = remember { MutableInteractionSource() }
    val images = row?.images.orEmpty()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.headerBackground(),
                title = {
                    Column {
                        Text(
                            text = row?.name?.ifBlank { null } ?: "تفاصيل الصف",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        AnimatedVisibility(visible = table != null, enter = fadeIn() + expandVertically()) {
                            Text(
                                text = table?.name ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = LocalHeaderColors.current.content.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    AnimatedVisibility(visible = row != null, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                        IconButton(onClick = { openEdit() }) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل الصف")
                        }
                    }
                },
                colors = headerTopAppBarColors(),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = row != null, enter = fadeIn() + scaleIn(initialScale = 0.6f), exit = fadeOut() + scaleOut()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!isUploading) showSourceSheet = true
                    },
                    expanded = fabExpanded,
                    interactionSource = fabInteraction,
                    modifier = Modifier.pressScale(fabInteraction, 0.92f),
                    containerColor = colors.secondary,
                    contentColor = colors.onSecondary,
                    icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                    text = { Text("إضافة صورة", style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
    ) { paddingValues ->
        val screenState = when {
            row != null -> "content"
            loadFailed && !isLoading -> "error"
            else -> "loading"
        }
        Crossfade(
            targetState = screenState,
            animationSpec = tween(AppMotion.Medium),
            label = "rowDetailState",
            modifier = Modifier.padding(paddingValues)
        ) { state ->
            when (state) {
                "loading" -> LoadingState()
                "error" -> EmptyState(
                    icon = Icons.Default.CloudOff,
                    title = "تعذر تحميل الصف",
                    subtitle = "تحقق من الاتصال ثم حاول مرة أخرى",
                    action = {
                        Button(onClick = { load() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إعادة المحاولة")
                        }
                    }
                )
                else -> {
                    val current = row ?: return@Crossfade
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(key = "progress") {
                            AnimatedVisibility(
                                visible = isLoading || isDeleting,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                                    color = colors.primary,
                                    trackColor = colors.primary.copy(alpha = 0.15f)
                                )
                            }
                        }
                        item(key = "hero") {
                            RowHeroCard(row = current, tableName = table?.name, modifier = Modifier.enterAnimation(index = 0))
                        }
                        item(key = "details") {
                            RowFieldsCard(
                                row = current,
                                table = table,
                                onEdit = { openEdit() },
                                modifier = Modifier.enterAnimation(index = 1)
                            )
                        }
                        item(key = "imagesHeader") {
                            Row(
                                modifier = Modifier.fillMaxWidth().enterAnimation(index = 2),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الصور", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                                Spacer(modifier = Modifier.width(8.dp))
                                AnimatedContent(
                                    targetState = images.size,
                                    transitionSpec = { (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut()) },
                                    label = "imageCount"
                                ) { count ->
                                    Surface(color = colors.primaryContainer, shape = RoundedCornerShape(50)) {
                                        Text(
                                            "$count",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colors.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                FilledTonalButton(
                                    onClick = { showSourceSheet = true },
                                    enabled = !isUploading,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة")
                                }
                            }
                        }
                        item(key = "uploading") {
                            AnimatedVisibility(
                                visible = isUploading,
                                enter = fadeIn(tween(AppMotion.Medium)) + expandVertically(tween(AppMotion.Medium, easing = AppMotion.Emphasized)),
                                exit = fadeOut(tween(AppMotion.Short)) + shrinkVertically(tween(AppMotion.Medium, easing = AppMotion.Emphasized))
                            ) {
                                UploadingBanner()
                            }
                        }
                        if (images.isEmpty()) {
                            item(key = "noImages") {
                                NoImagesCard(
                                    onCamera = { imagePicker.openCamera() },
                                    onGallery = { imagePicker.openGallery() },
                                    enabled = !isUploading,
                                    modifier = Modifier.enterAnimation(index = 3)
                                )
                            }
                        } else {
                            val chunks = images.chunked(3)
                            items(chunks.size, key = { "images_$it" }) { chunkIndex ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    chunks[chunkIndex].forEachIndexed { i, image ->
                                        val index = chunkIndex * 3 + i
                                        ImageTile(
                                            url = image.url,
                                            onClick = { viewerIndex = index },
                                            onDelete = { pendingDeleteIndex = index },
                                            modifier = Modifier.weight(1f).enterAnimation(index = index + 3, offsetY = 16.dp)
                                        )
                                    }
                                    repeat(3 - chunks[chunkIndex].size) { Spacer(modifier = Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Image source picker
    if (showSourceSheet) {
        ModalBottomSheet(onDismissRequest = { showSourceSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(
                    "إضافة صورة",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SheetOption(Icons.Default.CameraAlt, "التقاط صورة", "استخدم الكاميرا", Modifier.enterAnimation(index = 0)) {
                    showSourceSheet = false
                    imagePicker.openCamera()
                }
                SheetOption(Icons.Default.PhotoLibrary, "اختيار من المعرض", "صورة من جهازك", Modifier.enterAnimation(index = 1)) {
                    showSourceSheet = false
                    imagePicker.openGallery()
                }
            }
        }
    }

    // Preview
    viewerIndex?.let { start ->
        FullScreenImageViewer(
            imageUrls = images.map { it.url },
            initialPage = start,
            onDismiss = { viewerIndex = null },
            onDelete = { pendingDeleteIndex = it }
        )
    }

    // Delete confirmation
    pendingDeleteIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) pendingDeleteIndex = null },
            icon = { DialogIcon(Icons.Default.DeleteOutline, colors.errorContainer, colors.onErrorContainer) },
            title = { Text("حذف الصورة", style = MaterialTheme.typography.titleLarge) },
            text = { Text("هل أنت متأكد من حذف هذه الصورة؟ لا يمكن التراجع عن هذا الإجراء.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { deleteImage(index) },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError)
                ) {
                    AnimatedContent(targetState = isDeleting, label = "deleteButton") { deleting ->
                        if (deleting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.onError)
                        else Text("حذف")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIndex = null }, enabled = !isDeleting) {
                    Text("إلغاء", color = colors.onSurfaceVariant)
                }
            }
        )
    }

    // Edit row
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditDialog = false },
            icon = { DialogIcon(Icons.Default.Edit, colors.primaryContainer, colors.onPrimaryContainer) },
            title = { Text("تعديل الصف", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم الصف *") },
                        modifier = Modifier.fillMaxWidth().focusLift(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    table?.columns?.forEach { column ->
                        OutlinedTextField(
                            value = editData[column.key] ?: "",
                            onValueChange = { editData = editData.toMutableMap().apply { put(column.key, it) } },
                            label = { Text(column.label) },
                            modifier = Modifier.fillMaxWidth().focusLift(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            val result = rowRepository.updateRow(tableId, rowId, editName, editData)
                            isSaving = false
                            result
                                .onSuccess {
                                    showEditDialog = false
                                    showMessage("تم حفظ التعديلات")
                                    load()
                                }
                                .onFailure {
                                    Log.w("RowDetailScreen", "Row update failed", it)
                                    showMessage("تعذر حفظ التعديلات")
                                }
                        }
                    },
                    enabled = editName.isNotBlank() && !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                ) {
                    AnimatedContent(targetState = isSaving, label = "saveButton") { saving ->
                        if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primary)
                        else Text("حفظ")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }, enabled = !isSaving) {
                    Text("إلغاء", color = colors.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun RowHeroCard(row: Row, tableName: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val displayName = row.name.ifBlank { "صف جديد" }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(displayName.take(1), style = MaterialTheme.typography.headlineSmall, color = colors.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                if (!tableName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(color = colors.secondaryContainer, shape = RoundedCornerShape(50)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tableName, style = MaterialTheme.typography.labelMedium, color = colors.onSecondaryContainer)
                        }
                    }
                }
                val date = runCatching { row.createdAt.substring(0, 10) }.getOrNull()
                if (date != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colors.outline, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(date, style = MaterialTheme.typography.labelSmall, color = colors.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowFieldsCard(row: Row, table: Table?, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val columns = table?.columns.orEmpty()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("البيانات", style = MaterialTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تعديل")
                }
            }
            if (columns.isEmpty()) {
                Text(
                    "لا توجد حقول في هذا الجدول",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            } else {
                columns.forEachIndexed { i, column ->
                    if (i > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), color = colors.outlineVariant.copy(alpha = 0.6f))
                    val value = row.data?.get(column.key)
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Text(
                            column.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.weight(0.4f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (value.isNullOrBlank()) "—" else value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (value.isNullOrBlank()) colors.outline else colors.onSurface,
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageTile(url: String, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pressScale(interaction, 0.94f)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceContainerHigh)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current).data(url).crossfade(AppMotion.Long).build()
            ),
            contentDescription = "معاينة الصورة",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(34.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الصورة", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun NoImagesCard(onCamera: () -> Unit, onGallery: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(1.5.dp, colors.outlineVariant, MaterialTheme.shapes.large)
            .background(colors.surfaceContainerLow)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.floating(amplitude = 4.dp).size(64.dp).clip(CircleShape).background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("لا توجد صور بعد", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text("أضف صوراً من الكاميرا أو المعرض", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onCamera, enabled = enabled, shape = MaterialTheme.shapes.small) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("الكاميرا")
            }
            OutlinedButton(onClick = onGallery, enabled = enabled, shape = MaterialTheme.shapes.small) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("المعرض")
            }
        }
    }
}

@Composable
private fun UploadingBanner() {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth(), color = colors.primaryContainer, shape = MaterialTheme.shapes.small) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("جارٍ رفع الصورة...", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun SheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    ListItem(
        modifier = modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer)
            }
        },
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
    )
}

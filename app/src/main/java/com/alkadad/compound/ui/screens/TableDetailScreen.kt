package com.alkadad.compound.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.request.ImageRequest
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTransformGestures
import coil.compose.rememberAsyncImagePainter
import com.alkadad.compound.data.image.ImageCompressor
import com.alkadad.compound.data.model.*
import com.alkadad.compound.data.repository.RowRepository
import com.alkadad.compound.data.repository.TableRepository
import com.alkadad.compound.ui.components.*
import com.alkadad.compound.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val DEBOUNCE_MS = 1000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TableDetailScreen(
    tableId: String,
    onBack: () -> Unit,
    onOpenRow: (String) -> Unit
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
    // Upload target survives activity recreation while the camera/gallery app is in front.
    // null = table cover image, otherwise the id of the row receiving the image.
    var uploadRowId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var searchInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteImageDialog by remember { mutableStateOf(false) }
    var pendingDeleteImageIndex by remember { mutableStateOf(0) }
    var pendingDeleteRowId by remember { mutableStateOf("") }
    var showImageViewer by remember { mutableStateOf(false) }
    var viewerImages by remember { mutableStateOf<List<Image>>(emptyList()) }
    var viewerInitialPage by remember { mutableStateOf(0) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

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

    fun uploadImage(source: Uri) {
        val targetRowId = uploadRowId
        scope.launch {
            isUploading = true
            val file = ImageCompressor.prepareForUpload(context, source)
            val uploaded = if (file == null) {
                false
            } else {
                val result = if (targetRowId != null) {
                    rowRepository.uploadRowImages(tableId, targetRowId, listOf(file)).map { }
                } else {
                    tableRepository.uploadTableImage(tableId, file).map { }
                }
                result.onFailure { Log.w("TableDetailScreen", "Image upload failed", it) }
                file.delete()
                result.isSuccess
            }
            context.cacheDir.listFiles { f -> f.name.startsWith("camera_") }?.forEach { it.delete() }
            isUploading = false
            if (uploaded) refreshTable()
            launch {
                snackbarHostState.showSnackbar(
                    if (uploaded) "\u062a\u0645 \u0631\u0641\u0639 \u0627\u0644\u0635\u0648\u0631\u0629 \u0628\u0646\u062c\u0627\u062d"
                    else "\u062a\u0639\u0630\u0631 \u0631\u0641\u0639 \u0627\u0644\u0635\u0648\u0631\u0629\u060c \u062d\u0627\u0648\u0644 \u0645\u0631\u0629 \u0623\u062e\u0631\u0649"
                )
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) uploadImage(uri)
    }

    fun launchCamera() {
        val photoFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            scope.launch { snackbarHostState.showSnackbar("\u064a\u0644\u0632\u0645 \u0627\u0644\u0633\u0645\u0627\u062d \u0628\u0627\u0633\u062a\u062e\u062f\u0627\u0645 \u0627\u0644\u0643\u0627\u0645\u064a\u0631\u0627") }
        }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uploadImage(it) }
    }

    LaunchedEffect(tableId, searchQuery) {
        refreshTable()
    }

    val colors = MaterialTheme.colorScheme
    val header = LocalHeaderColors.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val fabInteraction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current

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
                            text = table?.name ?: "الجدول",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        AnimatedVisibility(visible = table != null, enter = fadeIn() + expandVertically()) {
                            AnimatedContent(
                                targetState = rows.size,
                                transitionSpec = {
                                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                                },
                                label = "rowCount"
                            ) { count ->
                                Text(
                                    text = "$count صف",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = header.content.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(enabled = !isUploading, onClick = {
                        uploadRowId = null
                        openCamera()
                    }) { Icon(Icons.Default.CameraAlt, contentDescription = "التقاط صورة") }
                    IconButton(enabled = !isUploading, onClick = {
                        uploadRowId = null
                        galleryLauncher.launch("image/*")
                    }) { Icon(Icons.Default.PhotoLibrary, contentDescription = "اختيار صورة من المحفظة") }
                },
                colors = headerTopAppBarColors(),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    editingRow = null; rowFormData = emptyMap(); rowName = ""; showRowDialog = true
                },
                expanded = fabExpanded,
                interactionSource = fabInteraction,
                modifier = Modifier.enterAnimation(index = 3, offsetY = 48.dp).pressScale(fabInteraction, 0.92f),
                containerColor = colors.secondary,
                contentColor = colors.onSecondary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("صف جديد", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                    .enterAnimation(index = 0)
                    .focusLift(),
                placeholder = { Text("بحث بالاسم...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchInput.isNotEmpty(),
                        enter = fadeIn(tween(AppMotion.Short)) + scaleIn(initialScale = 0.5f),
                        exit = fadeOut(tween(AppMotion.Short)) + scaleOut(targetScale = 0.5f)
                    ) {
                        IconButton(onClick = { searchInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerHighest,
                    unfocusedContainerColor = colors.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedLeadingIconColor = colors.primary,
                    unfocusedLeadingIconColor = colors.onSurfaceVariant
                )
            )

            AnimatedVisibility(
                visible = isUploading,
                enter = fadeIn(tween(AppMotion.Medium)) + expandVertically(tween(AppMotion.Medium, easing = AppMotion.Emphasized)),
                exit = fadeOut(tween(AppMotion.Short)) + shrinkVertically(tween(AppMotion.Medium, easing = AppMotion.Emphasized))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    color = colors.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("\u062c\u0627\u0631\u064d \u0631\u0641\u0639 \u0627\u0644\u0635\u0648\u0631\u0629...", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
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
            AnimatedVisibility(
                visible = !isUploading && isLoading && rows.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(50)),
                    color = colors.primary,
                    trackColor = colors.primary.copy(alpha = 0.15f)
                )
            }

            val listContentState = when {
                isLoading && rows.isEmpty() -> "loading"
                rows.isEmpty() -> "empty"
                else -> "content"
            }
            Crossfade(targetState = listContentState, animationSpec = tween(AppMotion.Medium), label = "rowsState") { state ->
            if (state == "loading") {
                LoadingState()
            } else if (state == "empty") {
                EmptyState(
                    icon = Icons.Default.TableRows,
                    title = "لا توجد صفوف",
                    subtitle = "اضغط + لإضافة أول صف"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                        RowCard(
                            modifier = Modifier
                                .animateItemPlacement(tween(AppMotion.Medium, easing = AppMotion.Emphasized))
                                .enterAnimation(index = index + 1),
                            row = row, table = table,
                            onRowClick = { onOpenRow(row.id) },
                            onEdit = { editingRow = row; rowName = row.name; rowFormData = row.data ?: emptyMap(); showRowDialog = true },
                            onDelete = { scope.launch { rowRepository.deleteRow(tableId, row.id); refreshTable() } },
                            onAddImages = { if (!isUploading) { uploadRowId = row.id; showImageSourceDialog = true } },
                            onDeleteImage = { idx ->
                                pendingDeleteImageIndex = idx
                                pendingDeleteRowId = row.id
                                showDeleteImageDialog = true
                            },
                            onImageClick = { images, idx ->
                                viewerImages = images
                                viewerInitialPage = idx
                                showImageViewer = true
                            }
                        )
                    }
                }
            }
            }
        }
    }

    if (showRowDialog) {
        AlertDialog(
            onDismissRequest = { showRowDialog = false },
            icon = {
                DialogIcon(
                    if (editingRow != null) Icons.Default.Edit else Icons.Default.PlaylistAdd,
                    colors.primaryContainer, colors.onPrimaryContainer
                )
            },
            title = { Text(if (editingRow != null) "تعديل الصف" else "إضافة صف جديد", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = rowName,
                        onValueChange = { rowName = it },
                        label = { Text("اسم الصف *") },
                        modifier = Modifier.fillMaxWidth().focusLift(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    table?.columns?.forEach { column ->
                        OutlinedTextField(
                            value = rowFormData[column.key] ?: "",
                            onValueChange = { rowFormData = rowFormData.toMutableMap().apply { put(column.key, it) } },
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
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    enabled = rowName.isNotBlank()
                ) { Text(if (editingRow != null) "تحديث" else "إضافة") }
            },
            dismissButton = { TextButton(onClick = { showRowDialog = false }) { Text("إلغاء", color = colors.onSurfaceVariant) } }
        )
    }

    if (showDeleteImageDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteImageDialog = false },
            icon = { DialogIcon(Icons.Default.DeleteOutline, colors.errorContainer, colors.onErrorContainer) },
            title = { Text("تأكيد حذف الصورة", style = MaterialTheme.typography.titleLarge) },
            text = { Text("هل أنت متأكد من حذف هذه الصورة؟", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            rowRepository.deleteRowImage(tableId, pendingDeleteRowId, pendingDeleteImageIndex)
                            showDeleteImageDialog = false
                            refreshTable()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError)
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { showDeleteImageDialog = false }) { Text("إلغاء", color = colors.onSurfaceVariant) } }
        )
    }

    // Image Source Chooser
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false; uploadRowId = null },
            icon = { DialogIcon(Icons.Default.AddPhotoAlternate, colors.primaryContainer, colors.onPrimaryContainer) },
            title = { Text("إضافة صورة", style = MaterialTheme.typography.titleLarge) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImageSourceOption(
                        icon = Icons.Default.CameraAlt,
                        label = "الكاميرا",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showImageSourceDialog = false
                            openCamera()
                        }
                    )
                    ImageSourceOption(
                        icon = Icons.Default.PhotoLibrary,
                        label = "المحفظة",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showImageSourceDialog = false; uploadRowId = null }) { Text("إلغاء", color = colors.onSurfaceVariant) } }
        )
    }

    // Image Viewer Full Screen
    if (showImageViewer && viewerImages.isNotEmpty()) {
        val pagerState = rememberPagerState(initialPage = viewerInitialPage, pageCount = { viewerImages.size })
        var scale by remember { mutableFloatStateOf(1f) }

        LaunchedEffect(pagerState.currentPage) {
            scale = 1f
        }

        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = rememberAsyncImagePainter(model = crossfadeRequest(viewerImages[page].url)),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .pointerInput(page) {
                                    detectTransformGestures { _, _, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                                    }
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                IconButton(
                    onClick = { showImageViewer = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    transitionSpec = { (fadeIn(tween(AppMotion.Short)) + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut(tween(AppMotion.Short))) },
                    label = "viewerCounter",
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) { page ->
                Text(
                    text = "${page + 1} / ${viewerImages.size}",
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSourceOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction, 0.94f),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowCard(
    row: Row, table: Table?,
    modifier: Modifier = Modifier,
    onRowClick: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit, onAddImages: () -> Unit, onDeleteImage: (Int) -> Unit,
    onImageClick: (List<Image>, Int) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val displayName = row.name.ifBlank { "صف جديد" }

    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onRowClick,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().pressScale(interaction, 0.97f).animateContentSize(tween(AppMotion.Medium, easing = AppMotion.Emphasized)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onEdit() }) { Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = colors.primary) }
                IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = colors.error) }
            }

            val filledColumns = table?.columns?.filter { !row.data?.get(it.key).isNullOrBlank() }.orEmpty()
            if (filledColumns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    color = colors.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        filledColumns.forEach { column ->
                            val value = row.data?.get(column.key)
                            if (!value.isNullOrBlank()) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(
                                        text = column.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.weight(0.4f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        color = colors.onSurface,
                                        modifier = Modifier.weight(0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!row.images.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "الصور (${row.images.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onAddImages) { Icon(Icons.Default.AddPhotoAlternate, contentDescription = "إضافة صور", tint = colors.primary) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.images.take(3).forEachIndexed { idx, img ->
                        Image(
                            painter = rememberAsyncImagePainter(model = crossfadeRequest(img.url)), contentDescription = null,
                            modifier = Modifier
                                .enterAnimation(index = idx, offsetY = 12.dp)
                                .size(72.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(colors.surfaceContainerHigh)
                                .clickable { onImageClick(row.images, idx) },
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (row.images.size > 3) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small).background(colors.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${row.images.size - 3}", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onAddImages,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.primaryContainer.copy(alpha = 0.55f),
                        contentColor = colors.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة صور")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { DialogIcon(Icons.Default.DeleteOutline, colors.errorContainer, colors.onErrorContainer) },
            title = { Text("تأكيد الحذف", style = MaterialTheme.typography.titleLarge) },
            text = { Text("هل أنت متأكد من حذف هذا الصف؟", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError)) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("إلغاء", color = colors.onSurfaceVariant) } }
        )
    }
}

@Composable
private fun crossfadeRequest(url: String?): ImageRequest =
    ImageRequest.Builder(LocalContext.current)
        .data(url)
        .crossfade(AppMotion.Long)
        .build()

package com.lovely.bakingrecipes.ui.screens.add

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.lovely.bakingrecipes.data.IngredientUnit
import com.lovely.bakingrecipes.data.MediaType
import com.lovely.bakingrecipes.data.PastryCategories
import com.lovely.bakingrecipes.ui.components.brandedTopAppBarColors
import com.lovely.bakingrecipes.viewmodel.AddPastryViewModel
import com.lovely.bakingrecipes.viewmodel.IngredientDraft
import com.lovely.bakingrecipes.viewmodel.MediaDraft
import com.lovely.bakingrecipes.viewmodel.StepDraft
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPastryScreen(
    onBackClick: () -> Unit,
    onSaveComplete: (edited: Boolean, name: String, category: String) -> Unit,
    viewModel: AddPastryViewModel = viewModel()
) {

    // Once the pastry is saved, leave the form and return to the list.
    LaunchedEffect(viewModel.saveComplete) {
        if (viewModel.saveComplete) {
            onSaveComplete(viewModel.isEditMode, viewModel.pastryName, viewModel.category)
        }
    }

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Reveal a newly added row by scrolling to the bottom — only on user adds, not on data load.
    LaunchedEffect(viewModel.addRowTick) {
        if (viewModel.addRowTick > 0) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showReorderSteps by remember { mutableStateOf(false) }
    var procedureReordered by remember { mutableStateOf(false) }
    val attemptBack: () -> Unit = {
        if (viewModel.hasUnsavedChanges()) showDiscardDialog = true else onBackClick()
    }
    BackHandler { attemptBack() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }
    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addPhotos(uris)
    }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.addVideo(it) }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = brandedTopAppBarColors(),
                title = {
                    Text(if (viewModel.isEditMode) "Edit Recipe" else "Add Recipe")
                },
                navigationIcon = {
                    IconButton(
                        onClick = attemptBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recipe Name *"
            )

            OutlinedTextField(
                value = viewModel.pastryName,
                onValueChange = {
                    viewModel.onPastryNameChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                isError = viewModel.nameError != null,
                supportingText = {
                    viewModel.nameError?.let { Text(it) }
                },
                placeholder = {
                    Text("e.g. Chocolate Moist Cake")
                }
            )

            Text(
                text = "Category"
            )

            CategoryDropdown(
                selected = viewModel.category,
                onSelected = { viewModel.onCategoryChange(it) }
            )

            TagsInput(
                tags = viewModel.tags,
                onAddTag = { viewModel.addTag(it) },
                onRemoveTag = { viewModel.removeTag(it) }
            )

            Text(
                text = "Short Description"
            )

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = {
                    viewModel.onDescriptionChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Tell us a little about this recipe")
                },
                minLines = 3
            )

            Text(
                text = "Details"
            )

            OutlinedTextField(
                value = viewModel.servings,
                onValueChange = viewModel::onServingsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Servings (how many it makes)") },
                placeholder = { Text("e.g. 12") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.prepMinutes,
                    onValueChange = viewModel::onPrepMinutesChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Prep time (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = viewModel.cookMinutes,
                    onValueChange = viewModel::onCookMinutesChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Bake time (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            DifficultyDropdown(
                selected = viewModel.difficulty,
                onSelected = { viewModel.onDifficultyChange(it) }
            )

            Text(
                text = if (viewModel.ingredients.isEmpty()) {
                    "Ingredients"
                } else {
                    "Ingredients (${viewModel.ingredients.size})"
                }
            )

            viewModel.ingredients.forEach { ingredient ->
                IngredientRow(
                    ingredient = ingredient,
                    onNameChange = { viewModel.onIngredientNameChange(ingredient.key, it) },
                    onAmountChange = { viewModel.onIngredientAmountChange(ingredient.key, it) },
                    onUnitChange = { viewModel.onIngredientUnitChange(ingredient.key, it) },
                    onRemove = { viewModel.removeIngredientRow(ingredient.key) }
                )
            }

            OutlinedButton(
                onClick = {
                    viewModel.addIngredientRow()
                    scope.launch {
                        snackbarHostState.showSnackbar("Ingredient added")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )
                Text("  Add ingredient")
            }

            Text(
                text = if (viewModel.steps.isEmpty()) {
                    "Baking Procedure"
                } else {
                    "Baking Procedure (${viewModel.steps.size})"
                }
            )

            viewModel.steps.forEachIndexed { index, step ->
                StepRow(
                    number = index + 1,
                    step = step,
                    onTextChange = { viewModel.onStepChange(step.key, it) },
                    onRemove = { viewModel.removeStepRow(step.key) }
                )
            }

            OutlinedButton(
                onClick = { viewModel.addStepRow() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )
                Text("  Add step")
            }

            if (viewModel.steps.size > 1) {
                OutlinedButton(
                    onClick = { showReorderSteps = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reorder procedure")
                }
            }

            Text(
                text = "Recipe Photo"
            )

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose Photo")
            }

            viewModel.selectedImageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected recipe photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Text(
                text = if (viewModel.mediaItems.isEmpty()) {
                    "More Photos & Video"
                } else {
                    "More Photos & Video (${viewModel.mediaItems.size})"
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        multiPhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Photos")
                }
                OutlinedButton(
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Video")
                }
            }

            if (viewModel.mediaItems.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(
                        viewModel.mediaItems,
                        key = { _, m -> m.key }
                    ) { _, media ->
                        MediaThumbnail(
                            media = media,
                            onRemove = { viewModel.removeMedia(media.key) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.savePastry()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.isEditMode) "Update Recipe" else "Save Recipe")
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved changes will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    if (showReorderSteps) {
        ReorderStepsDialog(
            steps = viewModel.steps,
            onMoveUp = {
                viewModel.moveStepUp(it)
                procedureReordered = true
            },
            onMoveDown = {
                viewModel.moveStepDown(it)
                procedureReordered = true
            },
            onDismiss = {
                showReorderSteps = false
                if (procedureReordered) {
                    procedureReordered = false
                    scope.launch { snackbarHostState.showSnackbar("Procedure reordered") }
                }
            }
        )
    }
}

@Composable
private fun StepRow(
    number: Int,
    step: StepDraft,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "$number.",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, end = 8.dp)
            )
            OutlinedTextField(
                value = step.text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                label = { Text("Instruction") },
                placeholder = { Text("e.g. Preheat oven to 180°C") },
                minLines = 2
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove step"
                )
            }
        }
    }
}

@Composable
private fun ReorderStepsDialog(
    steps: List<StepDraft>,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var draggingKey by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reorder procedure",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
                Text(
                    text = "Hold and drag a card to move it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = draggingKey == null
                ) {
                    itemsIndexed(steps, key = { _, s -> s.key }) { index, step ->
                        val isDragging = draggingKey == step.key
                        ReorderableStepItem(
                            number = index + 1,
                            text = step.text,
                            isDragging = isDragging,
                            dragOffset = if (isDragging) dragOffset else 0f,
                            onMeasured = { height -> if (itemHeightPx == 0) itemHeightPx = height },
                            onDragStart = {
                                draggingKey = step.key
                                dragOffset = 0f
                            },
                            onDrag = { dy ->
                                dragOffset += dy
                                if (itemHeightPx > 0) {
                                    if (dragOffset > itemHeightPx) {
                                        onMoveDown(step.key)
                                        dragOffset -= itemHeightPx
                                    } else if (dragOffset < -itemHeightPx) {
                                        onMoveUp(step.key)
                                        dragOffset += itemHeightPx
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingKey = null
                                dragOffset = 0f
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableStepItem(
    number: Int,
    text: String,
    isDragging: Boolean,
    dragOffset: Float,
    onMeasured: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 1.dp,
        label = "dragElevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1f,
        label = "dragScale"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onMeasured(it.height) }
            .zIndex(if (isDragging) 1f else 0f)
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .scale(scale)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = "$number.",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = text.ifBlank { "(empty step)" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultyDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Easy", "Medium", "Hard")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Difficulty") },
            placeholder = { Text("Select difficulty") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelected("")
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            placeholder = { Text("Select a category") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PastryCategories.all.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    selected: IngredientUnit,
    onSelected: (IngredientUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Unit") },
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            IngredientUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text("${unit.displayName} (${unit.label})") },
                    onClick = {
                        onSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: IngredientDraft,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (IngredientUnit) -> Unit,
    onRemove: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ingredient.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Flour") },
                    singleLine = true
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove ingredient"
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = ingredient.amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Amount") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    isError = ingredient.amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                UnitDropdown(
                    selected = ingredient.unit,
                    onSelected = onUnitChange,
                    modifier = Modifier.weight(1f)
                )
            }

            if (ingredient.amountError) {
                Text(
                    text = "Enter an amount greater than 0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsInput(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (tags.isEmpty()) "Tags" else "Tags (${tags.size})",
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Add a tag") },
            placeholder = { Text("e.g. Chocolate, Vegan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        onAddTag(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add tag")
                }
            }
        )
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveTag(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove $tag"
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(
    media: MediaDraft,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        if (media.type == MediaType.VIDEO) {
            // Decode a frame so the video shows a preview instead of a black box.
            val request = ImageRequest.Builder(context)
                .data(media.uri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .videoFrameMillis(1000)
                .crossfade(true)
                .build()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = request,
                    contentDescription = "Video",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White
                )
            }
        } else {
            AsyncImage(
                model = media.uri,
                contentDescription = "Recipe photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

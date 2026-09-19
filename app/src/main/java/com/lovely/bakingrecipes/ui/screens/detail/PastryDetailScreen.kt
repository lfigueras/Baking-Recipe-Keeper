package com.lovely.bakingrecipes.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.lovely.bakingrecipes.data.MediaItem
import com.lovely.bakingrecipes.data.MediaType
import com.lovely.bakingrecipes.data.Pastry
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.data.formatAmount
import com.lovely.bakingrecipes.ui.components.brandedTopAppBarColors
import com.lovely.bakingrecipes.ui.screens.media.FullScreenVideoDialog
import com.lovely.bakingrecipes.ui.screens.media.PhotoThumbnail
import com.lovely.bakingrecipes.ui.screens.media.VideoThumbnail
import com.lovely.bakingrecipes.util.Analytics
import com.lovely.bakingrecipes.util.RecipeExporter
import com.lovely.bakingrecipes.util.UnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastryDetailScreen(
    pastry: PastryWithIngredients?,
    onBackClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    onDeleteConfirmed: (Pastry) -> Unit,
    onToggleFavorite: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onAddToShoppingList: (Double) -> Unit = {},
    onStartBaking: (Int) -> Unit = {},
    onSeeAllMedia: (Int) -> Unit = {},
    editedMessage: String? = null,
    onEditedMessageShown: () -> Unit = {},
    duplicatedMessage: String? = null,
    onDuplicatedMessageShown: () -> Unit = {},
    addedToListMessage: String? = null,
    onAddedToListMessageShown: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var fullScreenPhoto by remember { mutableStateOf<String?>(null) }
    var fullScreenVideo by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Holds the generated export until the user picks a save location.
    var pendingExport by remember { mutableStateOf<RecipeExporter.ExportContent?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val export = pendingExport
        pendingExport = null
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null && export != null) {
            val ok = RecipeExporter.writeToUri(context, uri, export.bytes)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (ok) "Saved ${export.fileName}" else "Couldn't save file"
                )
            }
        }
    }

    fun saveExport(export: RecipeExporter.ExportContent) {
        pendingExport = export
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = export.mimeType
            putExtra(Intent.EXTRA_TITLE, export.fileName)
        }
        saveLauncher.launch(intent)
    }

    val baseServings = pastry?.pastry?.servings ?: 0
    var servings by remember(pastry?.pastry?.id) { mutableIntStateOf(baseServings) }
    val scale = if (baseServings > 0) servings.toDouble() / baseServings else 1.0

    // Show a one-time confirmation after returning from an edit.
    LaunchedEffect(editedMessage) {
        editedMessage?.let {
            snackbarHostState.showSnackbar("$it has been edited")
            onEditedMessageShown()
        }
    }
    LaunchedEffect(duplicatedMessage) {
        duplicatedMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDuplicatedMessageShown()
        }
    }
    LaunchedEffect(addedToListMessage) {
        addedToListMessage?.let {
            snackbarHostState.showSnackbar(it)
            onAddedToListMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = brandedTopAppBarColors(),
                title = {
                    Text(pastry?.pastry?.name ?: "Recipe")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (pastry != null) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (pastry.pastry.isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                                contentDescription = if (pastry.pastry.isFavorite) {
                                    "Remove from favorites"
                                } else {
                                    "Add to favorites"
                                }
                            )
                        }
                        IconButton(onClick = {
                            val text = buildShareText(pastry, scale, servings)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, pastry.pastry.name)
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share recipe"))
                            Analytics.recipeShared()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share recipe"
                            )
                        }
                        IconButton(onClick = { onEditClick(pastry.pastry.id) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit recipe"
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More actions"
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Start Baking") },
                                    onClick = {
                                        showMenu = false
                                        Analytics.startBaking()
                                        onStartBaking(pastry.pastry.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to shopping list") },
                                    onClick = {
                                        showMenu = false
                                        onAddToShoppingList(scale)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate") },
                                    onClick = {
                                        showMenu = false
                                        onDuplicate()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as PDF") },
                                    onClick = {
                                        showMenu = false
                                        Analytics.recipeExported("pdf")
                                        saveExport(RecipeExporter.buildPdf(context, pastry, scale, servings))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as CSV") },
                                    onClick = {
                                        showMenu = false
                                        Analytics.recipeExported("csv")
                                        saveExport(RecipeExporter.buildCsv(pastry, scale, servings))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as Excel") },
                                    onClick = {
                                        showMenu = false
                                        Analytics.recipeExported("xlsx")
                                        saveExport(RecipeExporter.buildXlsx(pastry, scale, servings))
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        if (pastry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Recipe not found")
            }
            return@Scaffold
        }

        val details = pastry.pastry

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (details.imageUri != null) {
                AsyncImage(
                    model = details.imageUri,
                    contentDescription = details.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showImageViewer = true }
                )
            }

            Text(
                text = details.name,
                style = MaterialTheme.typography.headlineSmall
            )

            RecipeMeta(details)

            if (details.category.isNotBlank()) {
                DetailSection(label = "Category", value = details.category)
            }

            if (pastry.tags.isNotEmpty()) {
                TagsSection(pastry.tags.map { it.name })
            }

            if (details.description.isNotBlank()) {
                DetailSection(label = "Description", value = details.description)
            }

            if (baseServings > 0) {
                ServingsStepper(
                    servings = servings,
                    onDecrease = { if (servings > 1) servings-- },
                    onIncrease = { servings++ }
                )
            }

            IngredientsSection(pastry.ingredients, scale)

            if (pastry.steps.isNotEmpty()) {
                StepsSection(pastry.steps.sortedBy { it.position })
            }

            if (pastry.media.isNotEmpty()) {
                MediaGallery(
                    media = pastry.media.sortedBy { it.position },
                    onPhotoClick = { fullScreenPhoto = it },
                    onVideoClick = { fullScreenVideo = it },
                    onSeeAll = { onSeeAllMedia(pastry.pastry.id) }
                )
            }
        }
    }

    val fullImageUri = pastry?.pastry?.imageUri
    if (showImageViewer && fullImageUri != null) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showImageViewer = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fullImageUri,
                    contentDescription = pastry.pastry.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    fullScreenPhoto?.let { photoUri ->
        Dialog(
            onDismissRequest = { fullScreenPhoto = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenPhoto = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    fullScreenVideo?.let { videoUri ->
        FullScreenVideoDialog(uri = videoUri, onDismiss = { fullScreenVideo = null })
    }

    if (showDeleteDialog && pastry != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete recipe?") },
            text = { Text("Are you sure you want to permanently remove \"${pastry.pastry.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirmed(pastry.pastry)
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun IngredientsSection(
    ingredients: List<com.lovely.bakingrecipes.data.Ingredient>,
    scale: Double = 1.0
) {
    var system by remember { mutableStateOf(UnitConverter.System.ORIGINAL) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (ingredients.any { UnitConverter.dimensionOf(it.unit) != UnitConverter.Dimension.COUNT }) {
                UnitSystemSelector(
                    selected = system,
                    onSelected = { system = it },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (ingredients.isEmpty()) {
                Text(
                    text = "No ingredients added",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                ingredients.forEach { ingredient ->
                    val (amount, unit) = UnitConverter.display(
                        ingredient.amount * scale,
                        ingredient.unit,
                        system
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ingredient.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${formatMeasure(amount)} ${unit.label}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSystemSelector(
    selected: UnitConverter.System,
    onSelected: (UnitConverter.System) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        UnitConverter.System.ORIGINAL to "Original",
        UnitConverter.System.METRIC to "Metric",
        UnitConverter.System.US to "US"
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label) }
            )
        }
    }
}

// Rounds converted amounts to at most two decimals, trimming trailing zeros.
private fun formatMeasure(amount: Double): String {
    val rounded = Math.round(amount * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

// Builds a plain-text version of the recipe for sharing.
private fun buildShareText(
    data: PastryWithIngredients,
    scale: Double,
    displayServings: Int
): String {
    val p = data.pastry
    return buildString {
        appendLine(p.name)
        if (p.category.isNotBlank()) appendLine(p.category)
        val meta = buildList {
            if (p.servings > 0) add("$displayServings servings")
            if (p.prepMinutes > 0) add("Prep ${p.prepMinutes} min")
            if (p.cookMinutes > 0) add("Bake ${p.cookMinutes} min")
            if (p.difficulty.isNotBlank()) add(p.difficulty)
        }
        if (meta.isNotEmpty()) appendLine(meta.joinToString(" · "))
        if (p.description.isNotBlank()) {
            appendLine()
            appendLine(p.description)
        }
        if (data.ingredients.isNotEmpty()) {
            appendLine()
            appendLine("Ingredients:")
            data.ingredients.forEach { ing ->
                appendLine("- ${formatAmount(ing.amount * scale)} ${ing.unit.label} ${ing.name}")
            }
        }
        if (data.steps.isNotEmpty()) {
            appendLine()
            appendLine("Baking Procedure:")
            data.steps.sortedBy { it.position }.forEachIndexed { index, step ->
                appendLine("${index + 1}. ${step.instruction}")
            }
        }
    }.trim()
}

@Composable
private fun StepsSection(
    steps: List<com.lovely.bakingrecipes.data.Step>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Baking Procedure",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = step.instruction,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeMeta(pastry: com.lovely.bakingrecipes.data.Pastry) {
    val items = buildList {
        if (pastry.prepMinutes > 0) add("Prep ${pastry.prepMinutes} min")
        if (pastry.cookMinutes > 0) add("Bake ${pastry.cookMinutes} min")
        if (pastry.difficulty.isNotBlank()) add(pastry.difficulty)
    }
    if (items.isEmpty()) return
    Text(
        text = items.joinToString("  •  "),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ServingsStepper(
    servings: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Servings",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDecrease) {
                Icon(Icons.Filled.Remove, contentDescription = "Fewer servings")
            }
            Text(
                text = "$servings",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onIncrease) {
                Icon(Icons.Filled.Add, contentDescription = "More servings")
            }
        }
    }
}

@Composable
private fun DetailSection(
    label: String,
    value: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsSection(tags: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    AssistChip(
                        onClick = {},
                        label = { Text(tag) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaGallery(
    media: List<MediaItem>,
    onPhotoClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onSeeAll: () -> Unit
) {
    val photos = media.filter { it.type == MediaType.PHOTO }.sortedBy { it.position }
    val videos = media.filter { it.type == MediaType.VIDEO }.sortedBy { it.position }
    val hasMore = photos.size > 3 || videos.size > 3

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Photos & Video",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (hasMore) {
                    TextButton(onClick = onSeeAll) { Text("See all") }
                }
            }

            if (photos.isNotEmpty()) {
                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                SquareThumbnailRow(
                    items = photos.take(3),
                    isVideo = false,
                    onClick = { onPhotoClick(it) }
                )
            }

            if (videos.isNotEmpty()) {
                Text(
                    text = "Videos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                SquareThumbnailRow(
                    items = videos.take(3),
                    isVideo = true,
                    onClick = { onVideoClick(it) }
                )
            }
        }
    }
}

// A row of up to three equal square thumbnails.
@Composable
private fun SquareThumbnailRow(
    items: List<MediaItem>,
    isVideo: Boolean,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val squareModifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
            if (isVideo) {
                VideoThumbnail(uri = item.uri, modifier = squareModifier, onClick = { onClick(item.uri) })
            } else {
                PhotoThumbnail(uri = item.uri, modifier = squareModifier, onClick = { onClick(item.uri) })
            }
        }
        // Keep squares the same size when fewer than three are present.
        repeat(3 - items.size) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        }
    }
}

package com.lovely.bakingrecipes.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lovely.bakingrecipes.R
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.data.PastryCategories
import com.lovely.bakingrecipes.ui.components.DashboardTileCard
import com.lovely.bakingrecipes.ui.components.brandedTopAppBarColors
import com.lovely.bakingrecipes.viewmodel.DashboardTarget
import com.lovely.bakingrecipes.viewmodel.HomeUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onAddPastryClick: () -> Unit,
    onPastryClick: (Int) -> Unit,
    onTileClick: (DashboardTarget) -> Unit,
    onSeeAllClick: () -> Unit,
    onSettingsClick: () -> Unit,
    userName: String? = null,
    userPhotoUrl: String? = null,
    onAccountClick: () -> Unit = {},
    addedMessage: String? = null,
    onAddedMessageShown: () -> Unit = {},
    deletedMessage: String? = null,
    onDeletedMessageShown: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show a one-time confirmation after a new recipe is added.
    LaunchedEffect(addedMessage) {
        addedMessage?.let {
            snackbarHostState.showSnackbar(it)
            onAddedMessageShown()
        }
    }

    LaunchedEffect(deletedMessage) {
        deletedMessage?.let {
            snackbarHostState.showSnackbar("$it deleted")
            onDeletedMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = brandedTopAppBarColors(),
                title = {
                    Text("Baking Recipe Keeper")
                },
                actions = {
                    ProfileAvatar(
                        name = userName,
                        photoUrl = userPhotoUrl,
                        onClick = onAccountClick
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPastryClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add recipe"
                )
            }
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GreetingHeader(name = userName)
            }

            item {
                DashboardTiles(
                    tiles = uiState.tiles,
                    onTileClick = onTileClick,
                    onSeeAllClick = onSeeAllClick
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search recipes") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            }

            item {
                CategoryFilters(
                    categories = PastryCategories.all,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = onCategorySelected
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.pastries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BakeryDining,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = when {
                                uiState.totalPastries == 0 ->
                                    "No recipes yet. Tap + to add one."
                                uiState.selectedCategory != null ->
                                    "No ${uiState.selectedCategory} yet."
                                else ->
                                    "No recipes match your search."
                            }
                        )
                    }
                }
            } else {
                items(
                    items = uiState.pastries,
                    key = { it.pastry.id }
                ) { item ->
                    PastryCard(
                        item = item,
                        onClick = { onPastryClick(item.pastry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTiles(
    tiles: List<com.lovely.bakingrecipes.viewmodel.DashboardTile>,
    onTileClick: (DashboardTarget) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (tiles.size > 4) {
                TextButton(onClick = onSeeAllClick) {
                    Text("See all")
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
        }
        tiles.take(4).chunked(2).forEach { rowTiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTiles.forEach { tile ->
                    DashboardTileCard(
                        label = tile.label,
                        count = tile.count,
                        onClick = { onTileClick(tile.target) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowTiles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilters(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ElevatedFilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        items(categories) { category ->
            ElevatedFilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
private fun PastryCard(
    item: PastryWithIngredients,
    onClick: () -> Unit
) {
    val pastry = item.pastry
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 6.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pastry.imageUri != null) {
                AsyncImage(
                    model = pastry.imageUri,
                    contentDescription = pastry.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.BakeryDining,
                        contentDescription = null
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = pastry.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (pastry.category.isNotBlank()) {
                    Text(
                        text = pastry.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (pastry.description.isNotBlank()) {
                    Text(
                        text = pastry.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val count = item.ingredients.size
                if (count > 0) {
                    Text(
                        text = if (count == 1) "1 ingredient" else "$count ingredients",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

            if (pastry.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

// A friendly greeting that names the signed-in baker.
@Composable
private fun GreetingHeader(name: String?) {
    val firstName = name?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
    Text(
        text = if (firstName != null) "Hello, $firstName 👋" else "Hello, Baker 👋",
        style = MaterialTheme.typography.headlineSmall
    )
    Text(
        text = "What are we baking today?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Circular profile avatar: photo when available, otherwise the baker's initial.
@Composable
private fun ProfileAvatar(
    name: String?,
    photoUrl: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Account",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}


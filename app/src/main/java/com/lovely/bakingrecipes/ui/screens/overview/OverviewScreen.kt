package com.lovely.bakingrecipes.ui.screens.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lovely.bakingrecipes.ui.components.DashboardTileCard
import com.lovely.bakingrecipes.ui.components.brandedTopAppBarColors
import com.lovely.bakingrecipes.viewmodel.DashboardTarget
import com.lovely.bakingrecipes.viewmodel.DashboardTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    tiles: List<DashboardTile>,
    onBackClick: (() -> Unit)? = null,
    title: String = "Overview",
    onTileClick: (DashboardTarget) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = brandedTopAppBarColors(),
                title = { Text(title) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tiles, key = { it.label }) { tile ->
                DashboardTileCard(
                    label = tile.label,
                    count = tile.count,
                    onClick = { onTileClick(tile.target) }
                )
            }
        }
    }
}

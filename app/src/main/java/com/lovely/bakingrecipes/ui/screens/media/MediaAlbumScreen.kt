package com.lovely.bakingrecipes.ui.screens.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lovely.bakingrecipes.data.MediaItem
import com.lovely.bakingrecipes.data.MediaType
import com.lovely.bakingrecipes.ui.components.brandedTopAppBarColors

private enum class Folder { NONE, IMAGES, VIDEOS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaAlbumScreen(
    media: List<MediaItem>,
    onBackClick: () -> Unit
) {
    val photos = remember(media) { media.filter { it.type == MediaType.PHOTO }.sortedBy { it.position } }
    val videos = remember(media) { media.filter { it.type == MediaType.VIDEO }.sortedBy { it.position } }

    var folder by remember { mutableStateOf(Folder.NONE) }
    var fullScreenPhoto by remember { mutableStateOf<String?>(null) }
    var fullScreenVideo by remember { mutableStateOf<String?>(null) }

    val title = when (folder) {
        Folder.IMAGES -> "Images"
        Folder.VIDEOS -> "Videos"
        Folder.NONE -> "All Media"
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                colors = brandedTopAppBarColors(),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (folder == Folder.NONE) onBackClick() else folder = Folder.NONE
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (folder) {
            Folder.NONE -> FolderList(
                photoCount = photos.size,
                videoCount = videos.size,
                photoCover = photos.firstOrNull()?.uri,
                videoCover = videos.firstOrNull()?.uri,
                onOpenImages = { folder = Folder.IMAGES },
                onOpenVideos = { folder = Folder.VIDEOS },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            Folder.IMAGES -> MediaGrid(
                items = photos,
                onItemClick = { fullScreenPhoto = it.uri },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            Folder.VIDEOS -> MediaGrid(
                items = videos,
                onItemClick = { fullScreenVideo = it.uri },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    fullScreenPhoto?.let { FullScreenPhotoDialog(it) { fullScreenPhoto = null } }
    fullScreenVideo?.let { FullScreenVideoDialog(it) { fullScreenVideo = null } }
}

@Composable
private fun FolderList(
    photoCount: Int,
    videoCount: Int,
    photoCover: String?,
    videoCover: String?,
    onOpenImages: () -> Unit,
    onOpenVideos: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (photoCount > 0) {
            item {
                FolderCard("Images", photoCount, photoCover, isVideo = false, onClick = onOpenImages)
            }
        }
        if (videoCount > 0) {
            item {
                FolderCard("Videos", videoCount, videoCover, isVideo = true, onClick = onOpenVideos)
            }
        }
    }
}

@Composable
private fun FolderCard(
    label: String,
    count: Int,
    coverUri: String?,
    isVideo: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
            ) {
                if (coverUri != null) {
                    if (isVideo) {
                        VideoThumbnail(uri = coverUri, modifier = Modifier.fillMaxSize(), onClick = onClick)
                    } else {
                        PhotoThumbnail(uri = coverUri, modifier = Modifier.fillMaxSize(), onClick = onClick)
                    }
                }
            }
            Text(
                text = "$label ($count)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Nothing here yet",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            if (item.type == MediaType.VIDEO) {
                VideoThumbnail(
                    uri = item.uri,
                    modifier = Modifier.aspectRatio(1f),
                    onClick = { onItemClick(item) }
                )
            } else {
                PhotoThumbnail(
                    uri = item.uri,
                    modifier = Modifier.aspectRatio(1f),
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

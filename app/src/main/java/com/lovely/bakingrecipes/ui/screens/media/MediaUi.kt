package com.lovely.bakingrecipes.ui.screens.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis

@Composable
fun PhotoThumbnail(
    uri: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AsyncImage(
        model = uri,
        contentDescription = "Photo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    )
}

// Square video preview showing a decoded frame with a play badge.
@Composable
fun VideoThumbnail(
    uri: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(uri)
        .decoderFactory(VideoFrameDecoder.Factory())
        .videoFrameMillis(1000)
        .crossfade(true)
        .build()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = request,
            contentDescription = "Video",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            imageVector = Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.35f)
        )
    }
}

@Composable
fun FullScreenPhotoDialog(
    uri: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun FullScreenVideoDialog(
    uri: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(uri)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(uri) {
        onDispose { exoPlayer.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                }
            )
        }
    }
}

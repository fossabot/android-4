package uk.trigpointing.android.logging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

// Helper to interop Compose inside Java Activity
@JvmName("LogPhotosRowInterop")
@Composable
fun LogPhotosRow(
    photos: java.util.List<UiPhoto>,
    onAddClick: () -> Unit,
    onPhotoClick: (Long) -> Unit,
) {
    LogPhotosRow(photos = photos.toList(), onAddClick = onAddClick, onPhotoClick = onPhotoClick)
}

data class UiPhoto(
    val id: Long,
    val thumbnailPath: String,
)

@Composable
fun LogPhotosRow(
    photos: List<UiPhoto>,
    onAddClick: () -> Unit,
    onPhotoClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                AddTile(onClick = onAddClick)
            }
            items(photos, key = { it.id }) { photo ->
                PhotoTile(photo = photo, onClick = { onPhotoClick(photo.id) })
            }
        }
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(96.dp)
            .clickable { onClick() }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("+", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
private fun PhotoTile(photo: UiPhoto, onClick: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(96.dp)
            .clickable { onClick() }
    ) {
        val model: Any = if (photo.thumbnailPath.startsWith("/")) File(photo.thumbnailPath) else photo.thumbnailPath
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}



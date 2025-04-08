package fin.phoenix.flix.ui.product

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.ui.colors.RoseRed

@Composable
fun PhotoSelectionGrid(
    images: List<Uri>, onAddPhoto: () -> Unit, onRemovePhoto: (Uri) -> Unit, maxPhotos: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "添加照片（${images.size}/$maxPhotos）", modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (images.isEmpty()) 100.dp else ((images.size / 3 + 1) * 100).dp)
                .clip(RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add existing images
            items(images) { uri ->
                PhotoItem(uri = uri, onRemove = { onRemovePhoto(uri) })
            }

            // Add photo button (if not at max capacity)
            if (images.size < maxPhotos) {
                item {
                    AddPhotoButton(onClick = onAddPhoto)
                }
            }
        }

        if (images.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请至少添加一张照片",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "1：1 比例的照片效果最佳",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhotoItem(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = "Selected photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Remove button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove photo",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F8F8))
            .clickable(onClick = onClick), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Add photo",
                tint = RoseRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "添加照片", color = Color.Gray, fontSize = 12.sp
            )
        }
    }
}
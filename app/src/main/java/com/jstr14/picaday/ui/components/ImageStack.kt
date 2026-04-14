package com.jstr14.picaday.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ImageStack(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    circleSize: Dp = 20.dp
) {
    val overlapOffset = circleSize * 0.6f

    val showCounter = imageUrls.size > MAX_IMAGE_STACK_SIZE
    val displayCount = if (showCounter) MAX_IMAGE_STACK_SIZE - 1 else imageUrls.size
    val displayImages = imageUrls.take(displayCount)
    val extraCount = imageUrls.size - displayCount

    val totalVisualElements = if (showCounter) MAX_IMAGE_STACK_SIZE else displayImages.size
    val totalWidth = if (imageUrls.isEmpty()) {
        0.dp
    } else {
        circleSize + (overlapOffset * (totalVisualElements - 1))
    }

    Box(
        modifier = modifier.width(totalWidth),
        contentAlignment = Alignment.CenterStart
    ) {
        displayImages.forEachIndexed { index, url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = overlapOffset * index)
                    .size(circleSize)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        if (showCounter) {
            Box(
                modifier = Modifier
                    .offset(x = overlapOffset * (MAX_IMAGE_STACK_SIZE - 1))
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$extraCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImageStack() {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // One Image
        ImageStack(imageUrls = listOf("https://picsum.photos/200"))
        // Three Images
        ImageStack(
            imageUrls = listOf(
                "https://picsum.photos/200",
                "https://picsum.photos/201",
                "https://picsum.photos/202"
            )
        )
        // More than 3
        ImageStack(imageUrls = List(15) { "https://picsum.photos/200" })
    }
}

private const val MAX_IMAGE_STACK_SIZE = 3
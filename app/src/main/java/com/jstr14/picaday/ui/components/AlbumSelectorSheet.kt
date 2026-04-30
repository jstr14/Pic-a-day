package com.jstr14.picaday.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jstr14.picaday.domain.model.Album

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumSelectorSheet(
    albums: List<Album>,
    onSelectPersonal: () -> Unit,
    onSelectAlbum: (Album) -> Unit,
    onDismiss: () -> Unit,
    showPersonalOption: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Text(
            text = "Añadir a...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

        if (showPersonalOption) {
            ListItem(
                headlineContent = { Text("Mi diario personal") },
                leadingContent = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                modifier = Modifier.clickable { onSelectPersonal() }
            )
        }

        albums.forEach { album ->
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(album.name) },
                supportingContent = {
                    val count = album.members.size
                    Text("$count ${if (count == 1) "miembro" else "miembros"}")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.PhotoAlbum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                modifier = Modifier.clickable { onSelectAlbum(album) }
            )
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}

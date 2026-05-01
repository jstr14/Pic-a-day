package com.jstr14.picaday.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.jstr14.picaday.R

enum class BottomNavTab { HOME, ALBUMS, FAVORITES }

private data class NavItem(
    val tab: BottomNavTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    val navItems = listOf(
        NavItem(BottomNavTab.HOME, stringResource(R.string.nav_home), Icons.Filled.Home, Icons.Outlined.Home),
        NavItem(BottomNavTab.ALBUMS, stringResource(R.string.nav_albums), Icons.Filled.PhotoAlbum, Icons.Outlined.PhotoAlbum),
        NavItem(BottomNavTab.FAVORITES, stringResource(R.string.nav_favorites), Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    )
    NavigationBar {
        navItems.forEach { item ->
            val selected = item.tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}

package com.jstr14.picaday.ui.albums

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jstr14.picaday.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jstr14.picaday.domain.model.AlbumMember
import com.jstr14.picaday.domain.model.AlbumRole
import com.jstr14.picaday.domain.model.DayEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val album by viewModel.album.collectAsState()
    val isInviting by viewModel.isInviting.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val inviteResult by viewModel.inviteResult.collectAsState()
    val entries by viewModel.entries.collectAsState()
    var inviteEmail by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedUrls by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedUrls.isNotEmpty()
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.uploadPhotos(uris)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun launchPickerWithLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    val msgInviteSent = stringResource(R.string.invite_sent)
    val msgInvitePending = stringResource(R.string.invite_pending_account)
    val msgInviteError = stringResource(R.string.invite_error)

    LaunchedEffect(inviteResult) {
        val result = inviteResult ?: return@LaunchedEffect
        val message = when (result) {
            is InviteResult.Success -> {
                inviteEmail = ""
                msgInviteSent
            }
            is InviteResult.PendingInvite -> {
                inviteEmail = ""
                msgInvitePending
            }
            is InviteResult.Error -> msgInviteError
        }
        snackbarHostState.showSnackbar(message)
        viewModel.clearInviteResult()
    }

    val currentUserRole = album?.members
        ?.find { it.userId == viewModel.currentUserId }?.role
    val canManageMembers = currentUserRole == AlbumRole.OWNER || currentUserRole == AlbumRole.ADMIN
    if (showRenameDialog) {
        RenameAlbumDialog(
            currentName = album?.name ?: "",
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                viewModel.renameAlbum(newName)
                showRenameDialog = false
            }
        )
    }

    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text(pluralStringResource(R.plurals.delete_photos_title, selectedUrls.size, selectedUrls.size)) },
            text = { Text(stringResource(R.string.delete_album_photos_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val photosByDate = entries
                        .flatMap { entry -> entry.photos.map { entry.date to it } }
                        .filter { (_, photo) -> photo.url in selectedUrls }
                        .groupBy({ it.first }, { it.second.url })
                    viewModel.deletePhotos(photosByDate)
                    selectedUrls = emptySet()
                    showBatchDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_album_title)) },
            text = { Text(stringResource(R.string.delete_album_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAlbumAndMovePhotos(onComplete = onBack)
                    }
                ) {
                    Text(stringResource(R.string.move_my_photos_and_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAlbum(onComplete = onBack)
                    }
                ) {
                    Text(stringResource(R.string.delete_all_album), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSelectionMode) pluralStringResource(R.plurals.selected_photos, selectedUrls.size, selectedUrls.size) else album?.name ?: "",
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (isSelectionMode) selectedUrls = emptySet() else onBack() }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isSelectionMode) stringResource(R.string.cd_cancel_selection) else stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { showBatchDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_selection), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        if (canManageMembers) {
                            IconButton(onClick = { showRenameDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_rename_album), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        if (currentUserRole == AlbumRole.OWNER) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_album), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { if (!isUploading) launchPickerWithLocationPermission() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = stringResource(R.string.cd_add_photos))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_photos)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; selectedUrls = emptySet() },
                    text = { Text(stringResource(R.string.tab_members)) }
                )
            }

            AnimatedVisibility(
                visible = isUploading,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.saving_album_photos),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> PhotosTab(
                    entries = entries,
                    selectedUrls = selectedUrls,
                    onSelectionChanged = { selectedUrls = it },
                    canDeletePhoto = { uploadedByUid -> viewModel.canDeletePhoto(uploadedByUid) },
                )
                1 -> MembersTab(
                    album = album,
                    canManageMembers = canManageMembers,
                    isInviting = isInviting,
                    inviteEmail = inviteEmail,
                    onInviteEmailChange = { inviteEmail = it },
                    onInvite = { viewModel.inviteByEmail(inviteEmail.trim()) },
                    onRemoveMember = { viewModel.removeMember(it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotosTab(
    entries: List<DayEntry>,
    selectedUrls: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    canDeletePhoto: (uploadedByUid: String?) -> Boolean,
) {
    val context = LocalContext.current
    val allPhotos = entries.flatMap { entry -> entry.photos.map { entry.date to it } }
    val isSelectionMode = selectedUrls.isNotEmpty()

    if (allPhotos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.PhotoAlbum,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.no_photos_yet),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(allPhotos, key = { (_, photo) -> photo.url }) { (_, photo) ->
                val isSelected = photo.url in selectedUrls
                val isDeletable = canDeletePhoto(photo.uploadedByUid)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary)
                            else Modifier
                        )
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    if (isDeletable) {
                                        onSelectionChanged(
                                            if (isSelected) selectedUrls - photo.url
                                            else selectedUrls + photo.url
                                        )
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.only_your_photos),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onLongClick = {
                                if (isDeletable) {
                                    onSelectionChanged(selectedUrls + photo.url)
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.only_your_photos),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                ) {
                    AsyncImage(
                        model = photo.url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isSelectionMode && !isDeletable)
                                    Modifier.graphicsLayer { alpha = 0.4f }
                                else Modifier
                            ),
                        contentScale = ContentScale.Crop,
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MembersTab(
    album: com.jstr14.picaday.domain.model.Album?,
    canManageMembers: Boolean,
    isInviting: Boolean,
    inviteEmail: String,
    onInviteEmailChange: (String) -> Unit,
    onInvite: () -> Unit,
    onRemoveMember: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canManageMembers) {
            item {
                Text(
                    stringResource(R.string.invite_person_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = onInviteEmailChange,
                        label = { Text(stringResource(R.string.email_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { if (android.util.Patterns.EMAIL_ADDRESS.matcher(inviteEmail.trim()).matches() && !isInviting) onInvite() }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onInvite,
                        enabled = android.util.Patterns.EMAIL_ADDRESS.matcher(inviteEmail.trim()).matches() && !isInviting,
                    ) {
                        if (isInviting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.invite))
                        }
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.members_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        val members = (album?.members ?: emptyList())
            .sortedBy { when (it.role) { AlbumRole.OWNER -> 0; AlbumRole.ADMIN -> 1; AlbumRole.MEMBER -> 2 } }
        items(members, key = { it.userId }) { member ->
            MemberRow(
                member = member,
                canRemove = canManageMembers && member.role != AlbumRole.OWNER,
                onRemove = { onRemoveMember(member.userId) }
            )
        }

        val pendingInvites = album?.pendingInvites ?: emptyList()
        if (pendingInvites.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.pending_invites_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            items(pendingInvites, key = { "pending_$it" }) { email ->
                PendingInviteRow(email = email)
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: AlbumMember,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    val cardColor = lerp(Color.White, MaterialTheme.colorScheme.primary, 0.18f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!member.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = member.photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val displayLabel = member.displayName?.takeIf { it.isNotBlank() }
                    ?: member.email
                    ?: member.userId
                Text(
                    displayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (member.displayName?.isNotBlank() == true && member.email != null) {
                    Text(
                        member.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
            RoleBadge(role = member.role)
            if (canRemove) {
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = stringResource(R.string.cd_remove_member),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingInviteRow(email: String) {
    val cardColor = lerp(Color.White, MaterialTheme.colorScheme.tertiary, 0.12f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    stringResource(R.string.pending_badge),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun RenameAlbumDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_album_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.album_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name.trim()) },
                enabled = name.isNotBlank() && name.trim() != currentName
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun RoleBadge(role: AlbumRole) {
    val (label, containerColor) = when (role) {
        AlbumRole.OWNER -> stringResource(R.string.role_owner) to MaterialTheme.colorScheme.primaryContainer
        AlbumRole.ADMIN -> stringResource(R.string.role_admin) to MaterialTheme.colorScheme.secondaryContainer
        AlbumRole.MEMBER -> stringResource(R.string.role_member) to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

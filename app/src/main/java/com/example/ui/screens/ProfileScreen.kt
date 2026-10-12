package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.ProfileEditState
import com.example.ui.components.SectionHeader
import com.example.ui.components.UnexTopBar
import com.example.ui.theme.AvatarColors

@Composable
fun ProfileScreen(
    state: ProfileEditState,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onAvatarColorChange: (Int) -> Unit,
    onSaveProfile: () -> Unit,
    onDismissSaveSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = stringResource(R.string.profile_saved_success)

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar(successMessage)
            onDismissSaveSuccess()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        topBar = {
            UnexTopBar(
                title = stringResource(R.string.profile_title),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Preview Card
            SectionHeader(
                title = stringResource(R.string.profile_preview),
                icon = Icons.Default.Visibility
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val avatarColor = AvatarColors.getOrElse(state.avatarColorIndex) { AvatarColors[0] }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(avatarColor)
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.displayName.trim().take(1).uppercase().ifEmpty { "U" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = state.displayName.ifBlank { stringResource(R.string.profile_name_hint) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = state.bio.ifBlank { stringResource(R.string.profile_desc_hint) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            // Avatar Color Accent Selector
            SectionHeader(
                title = stringResource(R.string.profile_avatar_color),
                icon = Icons.Default.Palette
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarColors.forEachIndexed { index, color ->
                        val isSelected = state.avatarColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onAvatarColorChange(index) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Display Name Input
            SectionHeader(
                title = stringResource(R.string.profile_edit_title),
                icon = Icons.Default.Person
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    placeholder = { Text(stringResource(R.string.profile_name_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    supportingText = {
                        Text(
                            text = "${state.displayName.length} / 30",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    isError = state.nameError != null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_display_name")
                )

                if (state.nameError != null) {
                    val errorText = when (state.nameError) {
                        "EMPTY" -> stringResource(R.string.profile_name_error_empty)
                        "TOO_LONG" -> stringResource(R.string.profile_name_error_too_long)
                        else -> stringResource(R.string.profile_name_error_empty)
                    }
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Bio / Device Description Input
            OutlinedTextField(
                value = state.bio,
                onValueChange = onBioChange,
                label = { Text(stringResource(R.string.profile_desc_label)) },
                placeholder = { Text(stringResource(R.string.profile_desc_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                supportingText = {
                    Text(
                        text = "${state.bio.length} / 120",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_bio")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Save Button
            Button(
                onClick = onSaveProfile,
                enabled = !state.isSaving && state.nameError == null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_save_profile")
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.profile_save),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

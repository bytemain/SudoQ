/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2025  Jiacheng Li
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.menus.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.sudoq.R

/**
 * Data class to hold player preferences
 */
data class PlayerPreferencesData(
    val profileName: String,
    val gestureActive: Boolean,
    val autoAdjustNotes: Boolean,
    val markRowColumn: Boolean,
    val markWrongSymbol: Boolean,
    val restrictCandidates: Boolean,
    val autoFillUniqueCandidates: Boolean,
    val showCompletedDigits: Boolean,
    val provideHints: Boolean,
    val canDeleteProfile: Boolean,
    val canSwitchProfile: Boolean
)

/**
 * Player preferences screen using Jetpack Compose
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerPreferencesScreen(
    data: PlayerPreferencesData,
    onProfileNameChange: (String) -> Unit,
    onGestureChange: (Boolean) -> Unit,
    onAutoAdjustNotesChange: (Boolean) -> Unit,
    onMarkRowColumnChange: (Boolean) -> Unit,
    onMarkWrongSymbolChange: (Boolean) -> Unit,
    onRestrictCandidatesChange: (Boolean) -> Unit,
    onAutoFillUniqueCandidatesChange: (Boolean) -> Unit,
    onShowCompletedDigitsChange: (Boolean) -> Unit,
    onProvideHintsChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onNewProfileClick: () -> Unit,
    onDeleteProfileClick: () -> Unit,
    onSwitchProfileClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onAdvancedClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_preference_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // New profile button
                    IconButton(onClick = onNewProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.action_new_profile)
                        )
                    }
                    // Delete profile button (only show if multiple profiles exist)
                    if (data.canDeleteProfile) {
                        IconButton(onClick = onDeleteProfileClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete_profile)
                            )
                        }
                    }
                    // Switch profile button (only show if multiple profiles exist)
                    if (data.canSwitchProfile) {
                        IconButton(onClick = onSwitchProfileClick) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.action_switch_profile)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile name section
            Text(
                text = stringResource(R.string.profile_preference_title_name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = data.profileName,
                onValueChange = onProfileNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Assistances section
            Text(
                text = stringResource(R.string.profile_preference_title_cat_assistances),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            PreferenceCheckbox(
                label = stringResource(R.string.profile_preference_title_gesture),
                checked = data.gestureActive,
                onCheckedChange = onGestureChange
            )
            
            PreferenceCheckbox(
                label = stringResource(R.string.profile_preference_title_assistance_autoAdjustNotes),
                checked = data.autoAdjustNotes,
                onCheckedChange = onAutoAdjustNotesChange
            )
            
            PreferenceCheckbox(
                label = stringResource(R.string.profile_preference_title_assistance_markRowColumn),
                checked = data.markRowColumn,
                onCheckedChange = onMarkRowColumnChange
            )
            
            PreferenceCheckbox(
                label = stringResource(R.string.profile_preference_title_assistance_markWrongSymbol),
                checked = data.markWrongSymbol,
                onCheckedChange = onMarkWrongSymbolChange
            )
            
            PreferenceCheckbox(
                label = stringResource(R.string.profile_preference_title_assistance_restrictCandidates),
                checked = data.restrictCandidates,
                onCheckedChange = onRestrictCandidatesChange
            )
            
            PreferenceCheckbox(
                label = stringResource(R.string.profile_preference_title_assistance_autoFillUniqueCandidates),
                checked = data.autoFillUniqueCandidates,
                onCheckedChange = onAutoFillUniqueCandidatesChange
            )
            
            PreferenceCheckbox(
                label = stringResource(R.string.profile_preference_title_assistance_showCompletedDigits),
                checked = data.showCompletedDigits,
                onCheckedChange = onShowCompletedDigitsChange
            )
            
            PreferenceCheckbox(
                label = stringResource(R.string.profile_preference_title_intelligent_assistant),
                checked = data.provideHints,
                onCheckedChange = onProvideHintsChange
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Action buttons
            Button(
                onClick = onStatisticsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.profile_preference_button_statistics))
            }
            
            Button(
                onClick = onAdvancedClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.profile_preference_button_advanced_preferences))
            }
        }
    }
}

/**
 * Reusable checkbox preference item
 */
@Composable
private fun PreferenceCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Preview function
 */
@Preview(showBackground = true)
@Composable
private fun PlayerPreferencesScreenPreview() {
    MaterialTheme {
        PlayerPreferencesScreen(
            data = PlayerPreferencesData(
                profileName = "Profile 1",
                gestureActive = true,
                autoAdjustNotes = true,
                markRowColumn = false,
                markWrongSymbol = true,
                restrictCandidates = false,
                autoFillUniqueCandidates = true,
                showCompletedDigits = false,
                provideHints = true,
                canDeleteProfile = true,
                canSwitchProfile = true
            ),
            onProfileNameChange = {},
            onGestureChange = {},
            onAutoAdjustNotesChange = {},
            onMarkRowColumnChange = {},
            onMarkWrongSymbolChange = {},
            onRestrictCandidatesChange = {},
            onAutoFillUniqueCandidatesChange = {},
            onShowCompletedDigitsChange = {},
            onProvideHintsChange = {},
            onBackClick = {},
            onNewProfileClick = {},
            onDeleteProfileClick = {},
            onSwitchProfileClick = {},
            onStatisticsClick = {},
            onAdvancedClick = {}
        )
    }
}

/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.view.actionTree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.sudoq.model.actionTree.ActionTreeElement

/** Mode for action when clicking a historical node */
enum class ActionMode {
    CHECKOUT, // Checkout to this node (create new branch)
    REVERT, // Revert to this node (add reverse actions)
    PREVIEW // Preview this state in a dialog with navigation
}

/**
 * Dialog for selecting action mode when clicking a historical node
 *
 * @param node The node that was clicked
 * @param onModeSelected Callback when a mode is selected with optional branch name
 * @param onDismiss Callback to dismiss the dialog
 */
@Composable
fun ActionModeDialog(
        node: ActionTreeElement,
        onModeSelected: (ActionMode, String?) -> Unit,
        onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf<ActionMode?>(null) }
    var branchName by remember { mutableStateOf("") }
    var showBranchNameField by remember { mutableStateOf(false) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Choose Action", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                    // Checkout option
                    RadioButtonOption(
                            selected = selectedMode == ActionMode.CHECKOUT,
                            onClick = {
                                selectedMode = ActionMode.CHECKOUT
                                showBranchNameField = true
                            },
                            title = "Checkout",
                            description =
                                    "Create new branch from here. Your current branch remains unchanged."
                    )

                    // Show branch name input for checkout
                    if (selectedMode == ActionMode.CHECKOUT && showBranchNameField) {
                        OutlinedTextField(
                                value = branchName,
                                onValueChange = { branchName = it },
                                label = { Text("Branch name (optional)") },
                                placeholder = { Text("Leave empty for auto-generated name") },
                                singleLine = true,
                                modifier =
                                        Modifier.fillMaxWidth().padding(start = 32.dp, top = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Revert option
                    RadioButtonOption(
                            selected = selectedMode == ActionMode.REVERT,
                            onClick = {
                                selectedMode = ActionMode.REVERT
                                showBranchNameField = false
                            },
                            title = "Revert",
                            description =
                                    "Undo to this state by adding reverse actions. Keeps linear history."
                    )

                    Spacer(Modifier.height(16.dp))

                    // Preview button (not a radio option)
                    OutlinedButton(
                            onClick = { onModeSelected(ActionMode.PREVIEW, null) },
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Preview State")
                    }

                    // Show action info
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))

                    Text(
                            "Action #${node.id}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                            getActionDescription(node),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            selectedMode?.let { mode ->
                                val name =
                                        if (mode == ActionMode.CHECKOUT && branchName.isNotBlank()
                                        ) {
                                            branchName
                                        } else {
                                            null
                                        }
                                onModeSelected(mode, name)
                            }
                        },
                        enabled = selectedMode != null
                ) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Radio button option with title and description */
@Composable
private fun RadioButtonOption(
        selected: Boolean,
        onClick: () -> Unit,
        title: String,
        description: String,
        modifier: Modifier = Modifier
) {
    Row(
            modifier =
                    modifier.fillMaxWidth()
                            .selectable(
                                    selected = selected,
                                    onClick = onClick,
                                    role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
    ) {
        RadioButton(
                selected = selected,
                onClick = null // Handled by Row's selectable
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
            )

            Spacer(Modifier.height(4.dp))

            Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Get human-readable description for an action */
private fun getActionDescription(node: ActionTreeElement): String {
    val action = node.action
    val cellId = action.cell.id

    return when (action) {
        is de.sudoq.model.actionTree.SolveAction -> {
            val oldValue = action.cell.currentValue - action.diff
            val newValue = action.cell.currentValue
            "Cell #$cellId: $oldValue → $newValue"
        }
        is de.sudoq.model.actionTree.NoteAction -> {
            "Cell #$cellId: Note ${action.actionType} ${action.diff}"
        }
        else -> "Cell #$cellId"
    }
}

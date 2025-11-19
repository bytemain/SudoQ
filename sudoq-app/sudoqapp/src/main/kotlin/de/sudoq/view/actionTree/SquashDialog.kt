/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.view.actionTree

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.sudoq.model.actionTree.Action
import de.sudoq.model.actionTree.ActionTreeElement
import de.sudoq.model.actionTree.CompoundAction
import de.sudoq.model.actionTree.NoteAction
import de.sudoq.model.actionTree.SolveAction

/**
 * Dialog for selecting multiple consecutive nodes to squash/rebase into a single compound action
 *
 * @param startNode The starting node (earliest in history)
 * @param endNode The ending node (latest in history)
 * @param onSquash Callback when user confirms the squash with optional description
 * @param onDismiss Callback to dismiss the dialog
 */
@Composable
fun SquashDialog(
        startNode: ActionTreeElement,
        endNode: ActionTreeElement,
        onSquash: (description: String?) -> Unit,
        onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var useDescription by remember { mutableStateOf(false) }

    // Collect actions between start and end
    val actionsToSquash = remember(startNode, endNode) { collectActionsBetween(startNode, endNode) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Title
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = "⊡ Squash Operations",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info card
                Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                                text = "Combining ${actionsToSquash.size} operations into one",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text =
                                        "This will replace the selected range with a single compound action",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.7f
                                        )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions list
                Text(
                        text = "Operations to combine:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                ) { items(actionsToSquash) { action -> ActionSummaryItem(action) } }

                Spacer(modifier = Modifier.height(16.dp))

                // Optional description checkbox
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable { useDescription = !useDescription }
                                        .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = useDescription, onCheckedChange = { useDescription = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = "Add custom description",
                            style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Description input
                if (useDescription) {
                    OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            placeholder = { Text("e.g., 'Filled row 3'") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action buttons
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                            onClick = {
                                val finalDescription =
                                        if (useDescription && description.isNotBlank()) {
                                            description
                                        } else {
                                            null
                                        }
                                onSquash(finalDescription)
                            },
                            enabled = actionsToSquash.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Squash")
                    }
                }
            }
        }
    }
}

/** Display a summary of a single action */
@Composable
private fun ActionSummaryItem(action: Action) {
    Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Action type indicator
            Surface(
                    color =
                            when (action) {
                                is CompoundAction -> MaterialTheme.colorScheme.tertiaryContainer
                                is SolveAction -> MaterialTheme.colorScheme.primaryContainer
                                is NoteAction -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.size(32.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                            text =
                                    when (action) {
                                        is CompoundAction -> action.type.icon
                                        is SolveAction -> "S"
                                        is NoteAction -> "N"
                                        else -> "?"
                                    },
                            style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text =
                                when (action) {
                                    is CompoundAction ->
                                            "${action.type.displayName} (${action.actionCount} ops)"
                                    is SolveAction -> "Solve"
                                    is NoteAction -> "Note"
                                    else -> "Action"
                                },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text =
                                when (action) {
                                    is CompoundAction -> action.description
                                                    ?: action.getActionSummary()
                                    is SolveAction -> {
                                        val cellId = action.cell.id
                                        val oldValue = action.cell.currentValue - action.diff
                                        val newValue = action.cell.currentValue
                                        "Cell #$cellId: $oldValue → $newValue"
                                    }
                                    is NoteAction -> {
                                        "Cell #${action.cell.id}: ${action.actionType}"
                                    }
                                    else -> "Cell #${action.cell.id}"
                                },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/** Collect all actions between two nodes (inclusive) */
private fun collectActionsBetween(
        startNode: ActionTreeElement,
        endNode: ActionTreeElement
): List<Action> {
    val actions = mutableListOf<Action>()

    // Build path from start to end
    var current = endNode
    while (current != startNode && current.parent != null) {
        actions.add(0, current.action)
        current = current.parent!!
    }

    // Add start node's action if we reached it
    if (current == startNode) {
        actions.add(0, startNode.action)
    }

    return actions
}

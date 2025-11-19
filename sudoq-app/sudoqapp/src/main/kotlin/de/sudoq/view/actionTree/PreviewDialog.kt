/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.view.actionTree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.sudoq.model.actionTree.ActionTreeElement
import de.sudoq.model.actionTree.CompoundAction
import de.sudoq.model.actionTree.NoteAction
import de.sudoq.model.actionTree.SolveAction
import de.sudoq.model.game.GameStateHandler

/**
 * Preview Dialog - Shows a preview of the game state at a specific node Users can navigate through
 * history with left/right buttons
 *
 * Note: The dialog shows a placeholder instead of the actual board because the SudokuLayout view is
 * already attached to the main game screen and cannot be reused. The game board in the background
 * will show the preview state when you close this dialog.
 *
 * @param startNode The initial node to preview
 * @param gameStateHandler The game state handler
 * @param onCheckout Callback when user wants to checkout to the current preview node
 * @param onDismiss Callback when dialog is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewDialog(
        startNode: ActionTreeElement,
        gameStateHandler: GameStateHandler,
        onCheckout: (ActionTreeElement) -> Unit,
        onDismiss: () -> Unit
) {
    var currentPreviewNode by remember { mutableStateOf(startNode) }
    val originalState = remember { gameStateHandler.currentState }

    // Enter preview mode when dialog opens
    LaunchedEffect(startNode) { gameStateHandler.viewNode(startNode) }

    // Clean up when dialog closes
    DisposableEffect(Unit) { onDispose { gameStateHandler.exitViewMode() } }

    // Navigate to different node in preview
    fun navigateToNode(node: ActionTreeElement) {
        currentPreviewNode = node
        gameStateHandler.viewNode(node)
    }

    // Find previous node (parent)
    val hasPrevious = currentPreviewNode.parent != null
    fun navigatePrevious() {
        currentPreviewNode.parent?.let { navigateToNode(it) }
    }

    // Find next node (first child if exists)
    val hasNext = currentPreviewNode.hasChildren()
    fun navigateNext() {
        if (currentPreviewNode.hasChildren()) {
            navigateToNode(currentPreviewNode.childrenList.first())
        }
    }

    Dialog(
            onDismissRequest = onDismiss,
            properties =
                    DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = false
                    )
    ) {
        Surface(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with title and close button
                TopAppBar(
                        title = {
                            Column {
                                Text("Preview Mode")
                                Text(
                                        text = getNodeDescription(currentPreviewNode),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                )
                )

                // Info banner
                Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                text = "👁️ Previewing state - changes not saved",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Sudoku board preview - Show placeholder
                // Note: Cannot reuse sudokuLayout as it's already attached to main screen
                Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Card(
                            modifier = Modifier.fillMaxSize(),
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.surfaceVariant
                                    )
                    ) {
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                        Icons.Default.GridOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.5f
                                                )
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                        "Preview Mode",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                        "Node #${currentPreviewNode.id}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.7f
                                                )
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                        "Close this dialog to see the game board\nin the preview state",
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.6f
                                                ),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Navigation controls at bottom
                Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous button
                        Button(
                                onClick = { navigatePrevious() },
                                enabled = hasPrevious,
                                modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous"
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Previous")
                        }

                        Spacer(Modifier.width(16.dp))

                        // Node info
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                    "Step ${getNodeDepth(currentPreviewNode)}",
                                    style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                    "ID: ${currentPreviewNode.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        // Next button
                        Button(
                                onClick = { navigateNext() },
                                enabled = hasNext,
                                modifier = Modifier.weight(1f)
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next"
                            )
                        }
                    }
                }

                // Action buttons at bottom
                Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }

                    Button(
                            onClick = { onCheckout(currentPreviewNode) },
                            modifier = Modifier.weight(1f)
                    ) { Text("Checkout Here") }
                }
            }
        }
    }
}

/** Get a human-readable description of a node */
private fun getNodeDescription(node: ActionTreeElement): String {
    val action = node.action
    return when (action) {
        is CompoundAction ->
                "${action.type.icon} ${action.type.displayName}: ${action.description ?: action.getActionSummary()}"
        is SolveAction -> {
            val cellId = action.cell.id
            val oldValue = action.cell.currentValue - action.diff
            val newValue = action.cell.currentValue
            "Solve: Cell #$cellId ($oldValue → $newValue)"
        }
        is NoteAction -> {
            val cellId = action.cell.id
            "Note: Cell #$cellId (${action.actionType})"
        }
        else -> "Action on Cell #${action.cell.id}"
    }
}

/** Get the depth of a node from root */
private fun getNodeDepth(node: ActionTreeElement): Int {
    var depth = 0
    var current: ActionTreeElement? = node
    while (current?.parent != null) {
        depth++
        current = current.parent
    }
    return depth
}

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.sudoq.model.actionTree.ActionBranch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Branch picker bottom sheet for selecting and managing branches
 *
 * @param branches List of all branches
 * @param currentBranch The currently active branch
 * @param onBranchSelected Callback when a branch is selected
 * @param onCreateBranch Callback to create a new branch
 * @param onDismiss Callback to dismiss the sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchPickerSheet(
        branches: List<ActionBranch>,
        currentBranch: ActionBranch?,
        onBranchSelected: (ActionBranch) -> Unit,
        onCreateBranch: () -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        BranchPickerContent(
                branches = branches,
                currentBranch = currentBranch,
                onBranchSelected = {
                    onBranchSelected(it)
                    onDismiss()
                },
                onCreateBranch = {
                    onCreateBranch()
                    onDismiss()
                }
        )
    }
}

/** Content of the branch picker */
@Composable
private fun BranchPickerContent(
        branches: List<ActionBranch>,
        currentBranch: ActionBranch?,
        onBranchSelected: (ActionBranch) -> Unit,
        onCreateBranch: () -> Unit,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        // Header
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    "Branches",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onCreateBranch) {
                Icon(
                        Icons.Default.Add,
                        contentDescription = "Create branch",
                        tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Branch list
        LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
        ) {
            items(branches) { branch ->
                BranchListItem(
                        branch = branch,
                        isCurrent = branch.id == currentBranch?.id,
                        onClick = { onBranchSelected(branch) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** Individual branch list item */
@Composable
private fun BranchListItem(
        branch: ActionBranch,
        isCurrent: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Card(
            modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (isCurrent) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 1.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Icon(
                                Icons.Default.Check,
                                contentDescription = "Current branch",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(
                                Icons.Default.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    Text(
                            text = branch.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color =
                                    if (isCurrent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                    )

                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                                "(current)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Branch statistics
                Text(
                        "${branch.getActionCount()} actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                        formatTimestamp(branch.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Format timestamp to relative time */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} min ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))
    }
}

/** Dialog for creating a new branch */
@Composable
fun CreateBranchDialog(onCreateBranch: (String) -> Unit, onDismiss: () -> Unit) {
    var branchName by remember { mutableStateOf("") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Create New Branch") },
            text = {
                Column {
                    Text(
                            "Create a new branch from current state",
                            style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                            value = branchName,
                            onValueChange = { branchName = it },
                            label = { Text("Branch name") },
                            placeholder = { Text("e.g., 'try-different-approach'") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                        onClick = { onCreateBranch(branchName) },
                        enabled = branchName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

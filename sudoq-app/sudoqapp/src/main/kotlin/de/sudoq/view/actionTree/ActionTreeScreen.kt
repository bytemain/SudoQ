/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.view.actionTree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.sudoq.R
import de.sudoq.model.actionTree.Action
import de.sudoq.model.actionTree.ActionTreeElement
import de.sudoq.model.actionTree.NoteAction
import de.sudoq.model.actionTree.SolveAction
import de.sudoq.model.sudoku.Cell

/**
 * Data class representing an action in the timeline
 */
data class ActionItem(
    val id: Int,
    val action: Action,
    val depth: Int,
    val isCurrent: Boolean,
    val isMistake: Boolean,
    val isCorrect: Boolean,
    val hasBranches: Boolean,
    val branchIndex: Int = 0
)

/**
 * Action Tree Screen - displays operation history in a git-like vertical timeline
 *
 * @param actionTree The root of the action tree
 * @param currentElement The currently active action element
 * @param onActionClick Callback when an action is clicked to navigate to that state
 * @param onClose Callback to close the action tree view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionTreeScreen(
    actionTree: ActionTreeElement?,
    currentElement: ActionTreeElement?,
    onActionClick: (ActionTreeElement) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sf_sudoku_title_action_tree)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (actionTree == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No action history available")
            }
        } else {
            val actions = remember(actionTree, currentElement) {
                buildActionList(actionTree, currentElement)
            }
            
            ActionTreeTimeline(
                actions = actions,
                currentElementId = currentElement?.id,
                onActionClick = { actionId ->
                    findActionElement(actionTree, actionId)?.let(onActionClick)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

/**
 * Timeline view displaying actions vertically like git commits
 */
@Composable
private fun ActionTreeTimeline(
    actions: List<ActionItem>,
    currentElementId: Int?,
    onActionClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to current action
    LaunchedEffect(currentElementId) {
        val currentIndex = actions.indexOfFirst { it.id == currentElementId }
        if (currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex)
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)
    ) {
        items(actions.size) { index ->
            val action = actions[index]
            val isFirst = index == 0
            val isLast = index == actions.size - 1
            
            ActionTimelineItem(
                action = action,
                isSelected = action.id == currentElementId,
                isFirst = isFirst,
                isLast = isLast,
                onClick = { onActionClick(action.id) }
            )
        }
    }
}

/**
 * Single action item in the timeline
 */
@Composable
private fun ActionTimelineItem(
    action: ActionItem,
    isSelected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Timeline connector and dot (Git style)
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
        ) {
            val lineColor = MaterialTheme.colorScheme.outline
            
            // Vertical line connector
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Draw line to previous (if not first)
                if (!isFirst) {
                    drawLine(
                        color = lineColor,
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, centerY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                
                // Draw line to next (if not last)
                if (!isLast) {
                    drawLine(
                        color = lineColor,
                        start = Offset(centerX, centerY),
                        end = Offset(centerX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                
                // Draw branch indicator if has branches
                if (action.hasBranches) {
                    drawLine(
                        color = lineColor,
                        start = Offset(centerX, centerY),
                        end = Offset(size.width * 2, centerY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                    )
                }
            }
            
            // Center dot
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(if (isSelected) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            action.isMistake -> MaterialTheme.colorScheme.error
                            action.isCorrect -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Right side: Action info in one line
        Row(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        Color.Transparent
                    },
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left part: Action description
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getActionTitle(action.action),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = getActionDescription(action.action),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            // Right part: Status badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (action.isMistake) {
                    Text(
                        text = "✗",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                CircleShape
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (action.isCorrect) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                CircleShape
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (action.hasBranches) {
                    Text(
                        text = "⑂",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Get human-readable title for an action
 */
private fun getActionTitle(action: Action): String {
    return when (action) {
        is SolveAction -> "Solve"
        is NoteAction -> "Note"
        else -> "Action"
    }
}

/**
 * Get human-readable description for an action
 */
private fun getActionDescription(action: Action): String {
    val cellId = action.cell.id
    
    return when (action) {
        is SolveAction -> {
            val oldValue = action.cell.currentValue - action.diff
            val newValue = action.cell.currentValue
            "Cell #$cellId: $oldValue → $newValue"
        }
        is NoteAction -> {
            "Cell #$cellId: Note toggle ${action.diff}"
        }
        else -> "Cell #$cellId"
    }
}

/**
 * Build a linear list of actions from the tree structure
 * Traverses from root to current element, then shows siblings/branches
 */
private fun buildActionList(
    root: ActionTreeElement,
    current: ActionTreeElement?
): List<ActionItem> {
    val result = mutableListOf<ActionItem>()
    
    // Build path from root to current
    val pathToCurrent = mutableListOf<ActionTreeElement>()
    var node = current
    while (node != null && node != root) {
        pathToCurrent.add(0, node)
        node = node.parent
    }
    pathToCurrent.add(0, root)
    
    // Convert path to ActionItems
    pathToCurrent.forEachIndexed { index, element ->
        result.add(
            ActionItem(
                id = element.id,
                action = element.action,
                depth = index,
                isCurrent = element == current,
                isMistake = element.isMistake,
                isCorrect = element.isCorrect,
                hasBranches = element.hasChildren() && element.childrenList.size > 1
            )
        )
    }
    
    return result
}

/**
 * Find an action element by ID in the tree
 */
private fun findActionElement(root: ActionTreeElement, id: Int): ActionTreeElement? {
    if (root.id == id) return root
    for (child in root.childrenList) {
        findActionElement(child, id)?.let { return it }
    }
    return null
}

// ==================== Previews ====================

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun PreviewActionTreeScreen() {
    MaterialTheme {
        // Create mock action tree
        val mockActions = listOf(
            ActionItem(
                id = 1,
                action = createMockSolveAction(5, 5),
                depth = 0,
                isCurrent = false,
                isMistake = false,
                isCorrect = true,
                hasBranches = false
            ),
            ActionItem(
                id = 2,
                action = createMockNoteAction(12, 3),
                depth = 1,
                isCurrent = false,
                isMistake = false,
                isCorrect = false,
                hasBranches = false
            ),
            ActionItem(
                id = 3,
                action = createMockSolveAction(23, 7),
                depth = 2,
                isCurrent = true,
                isMistake = false,
                isCorrect = false,
                hasBranches = true
            ),
            ActionItem(
                id = 4,
                action = createMockSolveAction(34, 2),
                depth = 3,
                isCurrent = false,
                isMistake = true,
                isCorrect = false,
                hasBranches = false
            ),
            ActionItem(
                id = 5,
                action = createMockNoteAction(45, 6),
                depth = 4,
                isCurrent = false,
                isMistake = false,
                isCorrect = true,
                hasBranches = false
            )
        )
        
        ActionTreeTimeline(
            actions = mockActions,
            currentElementId = 3,
            onActionClick = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionTimelineItemNormal() {
    MaterialTheme {
        ActionTimelineItem(
            action = ActionItem(
                id = 1,
                action = createMockSolveAction(25, 5),
                depth = 0,
                isCurrent = false,
                isMistake = false,
                isCorrect = false,
                hasBranches = false
            ),
            isSelected = false,
            isFirst = false,
            isLast = false,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionTimelineItemSelected() {
    MaterialTheme {
        ActionTimelineItem(
            action = ActionItem(
                id = 2,
                action = createMockSolveAction(48, 8),
                depth = 1,
                isCurrent = true,
                isMistake = false,
                isCorrect = true,
                hasBranches = false
            ),
            isSelected = true,
            isFirst = false,
            isLast = false,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionTimelineItemMistake() {
    MaterialTheme {
        ActionTimelineItem(
            action = ActionItem(
                id = 3,
                action = createMockSolveAction(15, 3),
                depth = 2,
                isCurrent = false,
                isMistake = true,
                isCorrect = false,
                hasBranches = true
            ),
            isSelected = false,
            isFirst = false,
            isLast = false,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionTimelineItemNote() {
    MaterialTheme {
        ActionTimelineItem(
            action = ActionItem(
                id = 4,
                action = createMockNoteAction(67, 4),
                depth = 3,
                isCurrent = false,
                isMistake = false,
                isCorrect = false,
                hasBranches = false
            ),
            isSelected = false,
            isFirst = false,
            isLast = false,
            onClick = {}
        )
    }
}

// Mock data creators for preview
private fun createMockSolveAction(cellId: Int, value: Int): SolveAction {
    val cell = Cell(cellId, 9)
    cell.currentValue = value
    return SolveAction(value, cell)
}

private fun createMockNoteAction(cellId: Int, note: Int): NoteAction {
    val cell = Cell(cellId, 9)
    return NoteAction(note, NoteAction.Action.SET, cell)
}

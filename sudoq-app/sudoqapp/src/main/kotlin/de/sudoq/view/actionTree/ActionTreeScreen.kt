/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.view.actionTree

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import de.sudoq.model.actionTree.ActionBranch
import de.sudoq.model.actionTree.ActionTreeElement
import de.sudoq.model.actionTree.CompoundAction
import de.sudoq.model.actionTree.NoteAction
import de.sudoq.model.actionTree.SolveAction
import de.sudoq.model.game.GameStateHandler
import de.sudoq.model.sudoku.Cell

/** Data class representing an action in the timeline */
data class ActionItem(
        val element: ActionTreeElement,
        val action: Action,
        val depth: Int,
        val isCurrent: Boolean,
        val isMistake: Boolean,
        val isCorrect: Boolean,
        val isMarked: Boolean, // Bookmark
        val branches: List<ActionTreeElement> = emptyList() // All child branches
)

/**
 * Action Tree Screen with Branch Support - displays operation history in a git-like vertical
 * timeline
 *
 * @param gameStateHandler The game state handler with branch support
 * @param onClose Callback to close the action tree view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionTreeScreenWithBranches(gameStateHandler: GameStateHandler, onClose: () -> Unit) {
    var showBranchPicker by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf<ActionTreeElement?>(null) }
    var showCreateBranchDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf<ActionTreeElement?>(null) }

    val currentBranch = gameStateHandler.currentBranch
    val currentElement = gameStateHandler.currentState
    val actionTree = gameStateHandler.actionTree.root

    // Handle back button to close action tree
    BackHandler {
        if (gameStateHandler.branchManager.isInViewMode) {
            gameStateHandler.exitViewMode()
        } else {
            onClose()
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Row(
                                    modifier =
                                            Modifier.clickable { showBranchPicker = true }
                                                    .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentBranch?.name ?: "main")
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch branch",
                                        modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            // Show branches button
                            IconButton(onClick = { showBranchPicker = true }) {
                                Icon(Icons.Default.AccountTree, contentDescription = "Branches")
                            }
                        }
                )
            },
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        val actions =
                remember(actionTree, currentElement, currentBranch) {
                    buildActionList(actionTree, currentElement, currentBranch)
                }

        ActionTreeTimeline(
                actions = actions,
                currentElementId = currentElement?.id,
                onActionClick = { actionId ->
                    findActionElement(actionTree, actionId)?.let { element ->
                        showActionDialog = element
                    }
                },
                onToggleBookmark = { actionId ->
                    findActionElement(actionTree, actionId)?.let { element ->
                        element.mark() // Toggle bookmark
                    }
                },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
        )
    }

    // Branch picker bottom sheet
    if (showBranchPicker) {
        BranchPickerSheet(
                branches = gameStateHandler.getAllBranches(),
                currentBranch = currentBranch,
                onBranchSelected = { branch -> gameStateHandler.switchBranch(branch.id) },
                onCreateBranch = {
                    showBranchPicker = false
                    showCreateBranchDialog = true
                },
                onDismiss = { showBranchPicker = false }
        )
    }

    // Create branch dialog
    if (showCreateBranchDialog) {
        CreateBranchDialog(
                onCreateBranch = { name ->
                    gameStateHandler.createBranch(name)
                    showCreateBranchDialog = false
                },
                onDismiss = { showCreateBranchDialog = false }
        )
    }

    // Action mode dialog
    showActionDialog?.let { element ->
        ActionModeDialog(
                node = element,
                onModeSelected = { mode, branchName ->
                    when (mode) {
                        ActionMode.CHECKOUT -> {
                            gameStateHandler.checkoutToNode(element, branchName)
                        }
                        ActionMode.REVERT -> {
                            gameStateHandler.revertToNode(element)
                        }
                        ActionMode.PREVIEW -> {
                            showPreviewDialog = element
                        }
                    }
                    showActionDialog = null
                },
                onDismiss = { showActionDialog = null }
        )
    }

    // Preview dialog
    showPreviewDialog?.let { element ->
        PreviewDialog(
                startNode = element,
                gameStateHandler = gameStateHandler,
                onCheckout = { node ->
                    showPreviewDialog = null
                    gameStateHandler.checkoutToNode(node)
                },
                onDismiss = { showPreviewDialog = null }
        )
    }
}

/**
 * Legacy Action Tree Screen - for backward compatibility
 *
 * @param actionTree The root of the action tree
 * @param currentElement The currently active action element
 * @param onActionClick Callback when an action is clicked to navigate to that state
 * @param onToggleBookmark Callback to toggle bookmark on an element
 * @param onClose Callback to close the action tree view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionTreeScreen(
        actionTree: ActionTreeElement?,
        currentElement: ActionTreeElement?,
        onActionClick: (ActionTreeElement) -> Unit,
        onToggleBookmark: ((ActionTreeElement) -> Unit)? = null,
        onClose: () -> Unit
) {
    // Handle back button to close action tree
    BackHandler { onClose() }

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
            },
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        if (actionTree == null) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
            ) { Text("No action history available") }
        } else {
            val actions =
                    remember(actionTree, currentElement) {
                        buildActionList(actionTree, currentElement, null)
                    }

            ActionTreeTimeline(
                    actions = actions,
                    currentElementId = currentElement?.id,
                    onActionClick = { actionId ->
                        findActionElement(actionTree, actionId)?.let(onActionClick)
                    },
                    onToggleBookmark =
                            onToggleBookmark?.let { callback ->
                                { actionId: Int ->
                                    findActionElement(actionTree, actionId)?.let(callback)
                                }
                            },
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
        }
    }
}

/** Timeline view displaying actions vertically like git commits */
@Composable
private fun ActionTreeTimeline(
        actions: List<ActionItem>,
        currentElementId: Int?,
        onActionClick: (Int) -> Unit,
        onToggleBookmark: ((Int) -> Unit)?,
        modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to current action
    LaunchedEffect(currentElementId) {
        val currentIndex = actions.indexOfFirst { it.element.id == currentElementId }
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
                    isSelected = action.element.id == currentElementId,
                    isFirst = isFirst,
                    isLast = isLast,
                    onClick = { onActionClick(action.element.id) },
                    onBranchClick = { branchElement -> onActionClick(branchElement.id) },
                    onToggleBookmark = onToggleBookmark?.let { { it(action.element.id) } }
            )
        }
    }
}

/** Single action item in the timeline */
@Composable
private fun ActionTimelineItem(
        action: ActionItem,
        isSelected: Boolean,
        isFirst: Boolean,
        isLast: Boolean,
        onClick: () -> Unit,
        onBranchClick: (ActionTreeElement) -> Unit,
        onToggleBookmark: (() -> Unit)?,
        initialBranchPickerExpanded: Boolean = false // For preview/testing
) {
    val hasBranches = action.branches.size > 1
    var showBranchPicker by remember { mutableStateOf(initialBranchPickerExpanded) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).height(48.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Timeline connector and dot
            Box(modifier = Modifier.width(32.dp).fillMaxHeight()) {
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
                    if (hasBranches) {
                        drawLine(
                                color = lineColor,
                                start = Offset(centerX, centerY),
                                end = Offset(size.width * 1.5f, centerY),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                        )
                    }
                }

                // Center dot - different styles for bookmarked vs branching vs normal
                Box(
                        modifier =
                                Modifier.align(Alignment.Center)
                                        .size(
                                                if (isSelected) 16.dp
                                                else if (hasBranches) 14.dp else 10.dp
                                        )
                                        .then(
                                                if (hasBranches && !action.isMarked) {
                                                    // Branching element: hollow circle (ring)
                                                    Modifier.border(
                                                            width = 3.dp,
                                                            color =
                                                                    when {
                                                                        isSelected ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                        action.isMistake ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                                        action.isCorrect ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .tertiary
                                                                        else ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .outline
                                                                    },
                                                            shape = CircleShape
                                                    )
                                                } else {
                                                    // Normal or bookmarked: filled circle
                                                    Modifier.clip(CircleShape)
                                                            .background(
                                                                    when {
                                                                        isSelected ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                        action.isMistake ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .error
                                                                        action.isCorrect ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .tertiary
                                                                        action.isMarked ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .secondary // Bookmark color
                                                                        else ->
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .outline
                                                                    }
                                                            )
                                                }
                                        )
                                        .clickable(enabled = hasBranches) {
                                            showBranchPicker = !showBranchPicker
                                        }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: Action info
            Row(
                    modifier =
                            Modifier.weight(1f)
                                    .background(
                                            color =
                                                    if (isSelected) {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                                .copy(alpha = 0.3f)
                                                    } else {
                                                        Color.Transparent
                                                    },
                                            shape = MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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

                // Right part: Status badges and bookmark button
                Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (action.isMistake) {
                        Text(
                                text = "✗",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier =
                                        Modifier.background(
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
                                modifier =
                                        Modifier.background(
                                                        MaterialTheme.colorScheme.tertiaryContainer,
                                                        CircleShape
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (hasBranches) {
                        Text(
                                text = "${action.branches.size}⑂",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Bookmark toggle button
                    if (onToggleBookmark != null) {
                        IconButton(onClick = onToggleBookmark, modifier = Modifier.size(24.dp)) {
                            Icon(
                                    imageVector =
                                            if (action.isMarked) {
                                                Icons.Default.Star
                                            } else {
                                                Icons.Default.StarBorder
                                            },
                                    contentDescription =
                                            if (action.isMarked) "Remove bookmark"
                                            else "Add bookmark",
                                    tint =
                                            if (action.isMarked) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Branch picker dropdown
        if (showBranchPicker && hasBranches) {
            Card(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(start = 44.dp, end = 16.dp, bottom = 8.dp),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                            text = "Select branch:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                    )
                    action.branches.forEachIndexed { index, branch ->
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clickable {
                                                    onBranchClick(
                                                            branch
                                                    ) // Navigate to selected branch
                                                    showBranchPicker = false
                                                }
                                                .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    text = "Branch ${index + 1}:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                            )
                            Text(
                                    text = getActionDescription(branch.action),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Get human-readable title for an action */
private fun getActionTitle(action: Action): String {
    return when (action) {
        is CompoundAction -> "${action.type.icon} ${action.type.displayName}"
        is SolveAction -> "Solve"
        is NoteAction -> "Note"
        else -> "Action"
    }
}

/** Get human-readable description for an action */
private fun getActionDescription(action: Action): String {
    return when (action) {
        is CompoundAction -> {
            action.description ?: action.getActionSummary()
        }
        is SolveAction -> {
            val cellId = action.cell.id
            val oldValue = action.cell.currentValue - action.diff + 1
            val newValue = action.cell.currentValue + 1
            "Cell #$cellId: $oldValue → $newValue"
        }
        is NoteAction -> {
            val cellId = action.cell.id
            "Cell #$cellId: Note toggle ${action.diff}"
        }
        else -> {
            val cellId = action.cell.id
            "Cell #$cellId"
        }
    }
}

/**
 * Build a linear list of actions for the current branch only. Shows the path from root to the
 * branch's head, without other branches.
 *
 * @param root The root of the action tree
 * @param current The currently active element
 * @param currentBranch The current branch (null means show all like before)
 */
private fun buildActionList(
        root: ActionTreeElement,
        current: ActionTreeElement?,
        currentBranch: ActionBranch?
): List<ActionItem> {
    val result = mutableListOf<ActionItem>()

    // If no branch specified, show everything (backward compatibility)
    if (currentBranch == null) {
        fun traverseTree(node: ActionTreeElement, depth: Int) {
            val branches =
                    if (node.hasChildren()) {
                        node.childrenList.toList()
                    } else {
                        emptyList()
                    }

            result.add(
                    ActionItem(
                            element = node,
                            action = node.action,
                            depth = depth,
                            isCurrent = node == current,
                            isMistake = node.isMistake,
                            isCorrect = node.isCorrect,
                            isMarked = node.isMarked,
                            branches = branches
                    )
            )

            branches.forEach { child -> traverseTree(child, depth + 1) }
        }
        traverseTree(root, 0)
        return result
    }

    // Build the path from root to current branch's head
    val branchPath = mutableListOf<ActionTreeElement>()
    var node: ActionTreeElement? = currentBranch.head

    // Trace back from head to root
    while (node != null) {
        branchPath.add(0, node) // Insert at beginning to maintain order
        node = node.parent
    }

    // Convert path to ActionItems
    branchPath.forEachIndexed { index, element ->
        // Check if this node has children that are NOT in the current branch path
        val otherBranches =
                if (element.hasChildren()) {
                    element.childrenList.filter { child ->
                        // Include child if it's not the next node in our current branch path
                        val nextInPath = branchPath.getOrNull(index + 1)
                        child.id != nextInPath?.id
                    }
                } else {
                    emptyList()
                }

        result.add(
                ActionItem(
                        element = element,
                        action = element.action,
                        depth = index,
                        isCurrent = element == current,
                        isMistake = element.isMistake,
                        isCorrect = element.isCorrect,
                        isMarked = element.isMarked,
                        branches = otherBranches // Show branch points but not the children
                )
        )
    }

    return result
}

/** Find an action element by ID in the tree */
private fun findActionElement(root: ActionTreeElement, id: Int): ActionTreeElement? {
    if (root.id == id) return root
    for (child in root.childrenList) {
        findActionElement(child, id)?.let {
            return it
        }
    }
    return null
}

// ==================== Previews ====================

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun PreviewActionTreeScreen() {
    MaterialTheme {
        // Create mock action tree elements
        val mockElement1 = createMockActionTreeElement(1, createMockSolveAction(5, 5))
        val mockElement2 = createMockActionTreeElement(2, createMockNoteAction(12, 3))
        val mockElement3 = createMockActionTreeElement(3, createMockSolveAction(23, 7))
        val mockElement4 = createMockActionTreeElement(4, createMockSolveAction(34, 2))
        val mockElement5 = createMockActionTreeElement(5, createMockNoteAction(45, 6))

        val mockActions =
                listOf(
                        ActionItem(
                                element = mockElement1,
                                action = mockElement1.action,
                                depth = 0,
                                isCurrent = false,
                                isMistake = false,
                                isCorrect = true,
                                isMarked = false,
                                branches = emptyList()
                        ),
                        ActionItem(
                                element = mockElement2,
                                action = mockElement2.action,
                                depth = 1,
                                isCurrent = false,
                                isMistake = false,
                                isCorrect = false,
                                isMarked = false,
                                branches = emptyList()
                        ),
                        ActionItem(
                                element = mockElement3,
                                action = mockElement3.action,
                                depth = 2,
                                isCurrent = true,
                                isMistake = false,
                                isCorrect = false,
                                isMarked = false,
                                branches = listOf(mockElement4, mockElement5)
                        ),
                        ActionItem(
                                element = mockElement4,
                                action = mockElement4.action,
                                depth = 3,
                                isCurrent = false,
                                isMistake = true,
                                isCorrect = false,
                                isMarked = false,
                                branches = emptyList()
                        ),
                        ActionItem(
                                element = mockElement5,
                                action = mockElement5.action,
                                depth = 4,
                                isCurrent = false,
                                isMistake = false,
                                isCorrect = true,
                                isMarked = true,
                                branches = emptyList()
                        )
                )

        ActionTreeTimeline(
                actions = mockActions,
                currentElementId = 3,
                onActionClick = {},
                onToggleBookmark = {},
                modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionTimelineItemNormal() {
    MaterialTheme {
        val mockElement = createMockActionTreeElement(1, createMockSolveAction(25, 5))
        ActionTimelineItem(
                action =
                        ActionItem(
                                element = mockElement,
                                action = mockElement.action,
                                depth = 0,
                                isCurrent = false,
                                isMistake = false,
                                isCorrect = false,
                                isMarked = false,
                                branches = emptyList()
                        ),
                isSelected = false,
                isFirst = false,
                isLast = false,
                onClick = {},
                onBranchClick = {},
                onToggleBookmark = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionTimelineItemSelected() {
    MaterialTheme {
        val mockElement = createMockActionTreeElement(2, createMockSolveAction(48, 8))
        ActionTimelineItem(
                action =
                        ActionItem(
                                element = mockElement,
                                action = mockElement.action,
                                depth = 1,
                                isCurrent = true,
                                isMistake = false,
                                isCorrect = true,
                                isMarked = false,
                                branches = emptyList()
                        ),
                isSelected = true,
                isFirst = false,
                isLast = false,
                onClick = {},
                onBranchClick = {},
                onToggleBookmark = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionTimelineItemMistake() {
    MaterialTheme {
        val mockElement = createMockActionTreeElement(3, createMockSolveAction(15, 3))
        ActionTimelineItem(
                action =
                        ActionItem(
                                element = mockElement,
                                action = mockElement.action,
                                depth = 2,
                                isCurrent = false,
                                isMistake = true,
                                isCorrect = false,
                                isMarked = false,
                                branches =
                                        listOf(
                                                createMockActionTreeElement(
                                                        31,
                                                        createMockSolveAction(15, 4)
                                                ),
                                                createMockActionTreeElement(
                                                        32,
                                                        createMockSolveAction(15, 5)
                                                )
                                        )
                        ),
                isSelected = false,
                isFirst = false,
                isLast = false,
                onClick = {},
                onBranchClick = {},
                onToggleBookmark = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionTimelineItemNote() {
    MaterialTheme {
        val mockElement = createMockActionTreeElement(4, createMockNoteAction(67, 4))
        ActionTimelineItem(
                action =
                        ActionItem(
                                element = mockElement,
                                action = mockElement.action,
                                depth = 3,
                                isCurrent = false,
                                isMistake = false,
                                isCorrect = false,
                                isMarked = true,
                                branches = emptyList()
                        ),
                isSelected = false,
                isFirst = false,
                isLast = false,
                onClick = {},
                onBranchClick = {},
                onToggleBookmark = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionTimelineItemWithBranches() {
    MaterialTheme {
        val mockElement = createMockActionTreeElement(5, createMockSolveAction(34, 6))
        ActionTimelineItem(
                action =
                        ActionItem(
                                element = mockElement,
                                action = mockElement.action,
                                depth = 2,
                                isCurrent = false,
                                isMistake = false,
                                isCorrect = false,
                                isMarked = false,
                                branches =
                                        listOf(
                                                createMockActionTreeElement(
                                                        51,
                                                        createMockSolveAction(34, 6)
                                                ),
                                                createMockActionTreeElement(
                                                        52,
                                                        createMockSolveAction(34, 7)
                                                ),
                                                createMockActionTreeElement(
                                                        53,
                                                        createMockSolveAction(34, 8)
                                                )
                                        )
                        ),
                isSelected = false,
                isFirst = false,
                isLast = false,
                onClick = {},
                onBranchClick = {},
                onToggleBookmark = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 200)
@Composable
private fun PreviewActionTimelineItemWithBranchesExpanded() {
    MaterialTheme {
        // This preview shows the expanded branch picker state
        val mockElement = createMockActionTreeElement(6, createMockSolveAction(42, 5))
        val branches =
                listOf(
                        createMockActionTreeElement(61, createMockSolveAction(42, 6)),
                        createMockActionTreeElement(62, createMockSolveAction(42, 7)),
                        createMockActionTreeElement(63, createMockSolveAction(42, 8)),
                        createMockActionTreeElement(64, createMockSolveAction(42, 1))
                )

        ActionTimelineItem(
                action =
                        ActionItem(
                                element = mockElement,
                                action = mockElement.action,
                                depth = 2,
                                isCurrent = false,
                                isMistake = false,
                                isCorrect = false,
                                isMarked = false,
                                branches = branches
                        ),
                isSelected = false,
                isFirst = false,
                isLast = false,
                onClick = {},
                onBranchClick = {},
                onToggleBookmark = {},
                initialBranchPickerExpanded = true // Show expanded state
        )
    }
}

// Mock data creators for preview

// Mock data creators for preview
private fun createMockActionTreeElement(id: Int, action: Action): ActionTreeElement {
    return ActionTreeElement(id, action, null)
}

private fun createMockSolveAction(cellId: Int, value: Int): SolveAction {
    val cell = Cell(cellId, 9)
    // Ensure value is within valid range (0 to maxValue which is 8 for a 9x9 sudoku)
    val safeValue = value.coerceIn(0, cell.maxValue)
    cell.currentValue = safeValue
    return SolveAction(safeValue, cell)
}

private fun createMockNoteAction(cellId: Int, note: Int): NoteAction {
    val cell = Cell(cellId, 9)
    return NoteAction(note, NoteAction.Action.SET, cell)
}

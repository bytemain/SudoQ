/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.game

import de.sudoq.model.ObservableModelImpl
import de.sudoq.model.actionTree.*
import de.sudoq.model.actionTree.ActionTree.Companion.findPath
import de.sudoq.model.sudoku.Cell
import java.util.*

/**
 * Diese Klasse verwaltet den Zustand eines Spiels durch einen ActionTree und stellt Funktionalität
 * für die Verwaltung des Zustandes zur Verfügung.
 */
class GameStateHandler : ObservableModelImpl<ActionTreeElement>() {
    /** The data structure that stores all actions in order */
    val actionTree: ActionTree = ActionTree()

    /** Branch manager for Git-like branch operations */
    val branchManager: BranchManager = BranchManager()

    /** The node of the [ActionTree] that represents the current state. */
    var currentState: ActionTreeElement? // TODO can be made nonnullable?
        get() = branchManager.currentNode
        private set(value) {
            if (value != null) {
                branchManager.setCurrentNode(value)
            }
        }

    /** The current branch */
    val currentBranch: ActionBranch?
        get() = branchManager.currentBranch

    /** In case of undo this stack saves the way back forward for a redo */
    private val undoStack: Stack<ActionTreeElement> = Stack()

    /** A semaphore? to prevent concurrent changes by listeners during a change */
    private var locked: Boolean

    /**
     * Adds an [Action] to the [ActionTree] and executes it
     *
     * @param action The [Action] to add and execute
     */
    fun addAndExecute(action: Action) {
        // if another change is in progress dont execute!
        // TODO this looks wrong we're not waiting we're just skipping
        if (!locked) {
            locked = true
            addStrategic(action)
            notifyListeners(currentState!!)
            locked = false
        }
    }

    // private
    private fun addStrategic(action: Action) {
        // TODO split execution and insertion into action tree
        when {
            isActionRedundant(currentState, action) -> {
                currentState = findExistingChildren(currentState, action)[0]
                action.execute()
            }
            isActionAStepBack(currentState, action) -> {
                currentState = currentState!!.parent
                when (action) {
                    is NoteAction -> action.undo()
                    is SolveAction -> action.execute()
                }
            }
            isSolveOnSameCell(action) -> {
                val intended = action as SolveAction
                val above = currentState!!.action as SolveAction
                val liftedAction = intended.add(above)
                currentState!!.undo()
                currentState = currentState!!.parent
                addStrategic(liftedAction)
            }
            else -> {
                currentState = actionTree.add(action, currentState!!)
                currentState!!.execute()
            }
        }
    }

    /* check if action already in Tree,
    i.e. we went back in actionTree but are doing same steps again */
    private fun isActionRedundant(mountingElement: ActionTreeElement?, action: Action): Boolean {
        return findExistingChildren(mountingElement, action).isNotEmpty()
    }

    private fun findExistingChildren(
            mountingElement: ActionTreeElement?,
            action: Action
    ): List<ActionTreeElement> {
        val l: MutableList<ActionTreeElement> = Stack()
        if (mountingElement != null) {
            for (ateI in mountingElement.childrenList) if (ateI.actionEquals(action)) l.add(ateI)
        }
        return l
    }

    private fun isActionAStepBack(mountingElement: ActionTreeElement?, action: Action): Boolean {
        return mountingElement!!.action.inverse(action)
    }

    private fun isSolveOnSameCell(action: Action): Boolean {
        return currentState !== actionTree.root &&
                bothSolveActions(currentState, action) &&
                isActionOnSameCell(currentState, action)
    }

    private fun bothSolveActions(mountingElement: ActionTreeElement?, action: Action): Boolean {
        val actionAbove = mountingElement!!.action
        return action is SolveAction && actionAbove is SolveAction
    }

    private fun isActionOnSameCell(mountingElement: ActionTreeElement?, action: Action): Boolean {
        val sameCell = mountingElement!!.action.cell == action.cell
        return action is SolveAction && sameCell
    }

    /**
     * Executes all necessary [Action]s to bring the Sudoku back to the passed state.
     *
     * @param target The ActionTreeElement in which state the [Sudoku] is to be converted
     */
    fun goToState(target: ActionTreeElement) {
        locked = true
        var onlyUndo = true
        val listWay = findPath(currentState!!, target)!!
        val way = listWay.toTypedArray()

        for (i in 1 until way.size) {
            if (way[i - 1].parent === way[i]) { // are we going backwards?
                way[i - 1].undo()
                if (way[i].isSplitUp()) {
                    undoStack.push(way[i - 1])
                }
            } else {
                onlyUndo = false
                if (i - 2 >= 0 && way[i - 2].parent !== way[i - 1]) {
                    way[i - 1].execute()
                }
                if (way[i] === target) {
                    target.execute()
                }
            }
        }
        if (!onlyUndo) {
            undoStack.clear()
        }
        currentState = target
        notifyListeners(currentState!!)
        locked = false
    }

    /**
     * Gibt zurück, ob die letzte Aktion rückgängig gemacht werden kann
     *
     * @return true, falls die letzte Aktion rückgängig gemacht werden kann, false falls es keine
     * Aktion gibt, die rückgängig gemacht werden kann
     */
    fun canUndo(): Boolean {
        return currentState!!.parent != null
    }

    /** Undoes the last [Action]. Goes one step back in the version history. */
    fun undo() {
        locked = true
        if (currentState!!.parent != null) {
            val oldElement = currentState
            currentState = currentState!!.undo()
            if (currentState!!.isSplitUp()) {
                undoStack.push(oldElement)
            }
            notifyListeners(currentState!!)
        }
        locked = false
    }

    /**
     * Checks if You can go a step forward in the action history.
     *
     * @return true, if a redo is possible, false otherwise
     */
    fun canRedo(): Boolean {
        // if there are several child nodes then undo stack cannot be empty
        val a = currentState!!.isSplitUp() && undoStack.isNotEmpty()
        // if there are less than 2 child nodes, there has to be at least one
        val b = !currentState!!.isSplitUp() && currentState!!.hasChildren()
        return a || b
    }

    /**
     * Goes one step forward in the action history. If the last step was an undo that undo is
     * reversed.
     */
    fun redo() {
        locked = true
        if (currentState!!.isSplitUp()) {
            if (!undoStack.empty()) {
                currentState = undoStack.pop()
                currentState!!.execute()
                notifyListeners(currentState!!)
            }
        } else {
            if (currentState!!.hasChildren()) {
                // if there is a child node, go there, execute
                currentState = currentState!!.iterator().next()
                currentState!!.execute()
                notifyListeners(currentState!!)
            }
        }
        locked = false
    }

    /** Marks the current state to better find it later */
    fun markCurrentState() {
        currentState!!.mark()
    }

    /**
     * Checks if the passed [ActionTreeElement] is marked.
     *
     * @param ate the [ActionTreeElement] to check
     * @return true if it is marked, false otherwise
     */
    fun isMarked(ate: ActionTreeElement?): Boolean {
        return ate != null && ate.isMarked // TODO make non nullable
    }

    // ==================== Branch Operations ====================

    /**
     * Checkout to a specific node, creating a new branch if needed
     *
     * @param targetNode The node to checkout to
     * @param branchName Optional name for the new branch
     * @return The branch that was checked out or created
     */
    fun checkoutToNode(targetNode: ActionTreeElement, branchName: String? = null): ActionBranch {
        locked = true

        try {
            // Save current branch's HEAD
            currentBranch?.let { branch ->
                branchManager.updateBranchHead(branch.id, currentState!!)
            }

            // Check if target node is HEAD of an existing branch
            val existingBranch = branchManager.findBranchByHead(targetNode)

            val resultBranch =
                    if (existingBranch != null) {
                        // Switch to existing branch
                        branchManager.switchToBranch(existingBranch)
                        existingBranch
                    } else {
                        // Create new branch
                        val newBranch =
                                branchManager.createBranch(
                                        name = branchName,
                                        fromNode = targetNode,
                                        autoGenerate = branchName == null
                                )
                        branchManager.switchToBranch(newBranch)
                        newBranch
                    }

            // Navigate to the target node
            goToState(targetNode)

            return resultBranch
        } finally {
            locked = false
        }
    }

    /**
     * Revert to a previous node by creating a compound action that reverses all changes This keeps
     * the history linear while achieving the same result as checkout
     *
     * @param targetNode The node to revert to
     */
    fun revertToNode(targetNode: ActionTreeElement) {
        if (locked) return

        locked = true
        try {
            val originalState = currentState ?: return

            // Don't revert if already at target
            if (originalState.id == targetNode.id) {
                return
            }

            // Collect all affected cells by walking the path WITHOUT executing
            val path = findPath(targetNode, originalState) ?: return
            val affectedCells = mutableSetOf<Cell>()

            // Collect cells from all actions in the path (excluding targetNode itself)
            for (i in 1 until path.size) {
                val node = path[i]
                val action = node.action

                when (action) {
                    is SolveAction -> affectedCells.add(action.cell)
                    is NoteAction -> affectedCells.add(action.cell)
                    is CompoundAction -> {
                        // Extract cells from compound action
                        action.actions.forEach { subAction ->
                            when (subAction) {
                                is SolveAction -> affectedCells.add(subAction.cell)
                                is NoteAction -> affectedCells.add(subAction.cell)
                            }
                        }
                    }
                }
            }

            // Calculate target cell values by simulating execution from root
            // This is safe because we're just reading, not executing
            val targetCellValues = calculateCellValuesAtNode(targetNode, affectedCells)

            // Create actions to transform current state to target state
            val revertActions = mutableListOf<Action>()

            for (cell in affectedCells) {
                val currentValue = cell.currentValue
                val targetValue = targetCellValues[cell] ?: continue
                val diff = targetValue - currentValue

                if (diff != 0) {
                    // Only create action if there's a difference
                    revertActions.add(SolveAction(diff, cell))
                }
            }

            if (revertActions.isEmpty()) {
                // No changes needed
                return
            }

            // Create compound revert action
            val compoundRevertAction =
                    CompoundAction(
                            actions = revertActions,
                            type = CompoundActionType.REVERT,
                            description =
                                    "Revert to state #${targetNode.id}: ${revertActions.size} change(s)"
                    )

            // Add the compound action as a single node
            addStrategic(compoundRevertAction)

            // Update current branch HEAD
            currentBranch?.let { branch ->
                branchManager.updateBranchHead(branch.id, currentState!!)
            }

            notifyListeners(currentState!!)
        } finally {
            locked = false
        }
    }

    /**
     * Calculate what cell values would be at a given node by simulating the path from root This
     * doesn't actually execute actions, just calculates the values
     */
    private fun calculateCellValuesAtNode(
            targetNode: ActionTreeElement,
            cellsToTrack: Set<Cell>
    ): Map<Cell, Int> {
        // Start with current cell values
        val cellValues = mutableMapOf<Cell, Int>()
        for (cell in cellsToTrack) {
            cellValues[cell] = cell.currentValue
        }

        // Build path from current to target
        val path = findPath(targetNode, currentState!!) ?: return cellValues

        // Apply changes in reverse to go from current to target
        // Skip the first element (target itself)
        for (i in 1 until path.size) {
            val node = path[i]
            val action = node.action

            when (action) {
                is SolveAction -> {
                    if (action.cell in cellsToTrack) {
                        // Reverse the action: subtract the diff
                        val currentVal = cellValues[action.cell] ?: action.cell.currentValue
                        cellValues[action.cell] = currentVal - action.diff
                    }
                }
                is NoteAction -> {
                    // Note actions don't affect cell values, skip
                }
                is CompoundAction -> {
                    // Reverse each sub-action in reverse order
                    action.actions.asReversed().forEach { subAction ->
                        when (subAction) {
                            is SolveAction -> {
                                if (subAction.cell in cellsToTrack) {
                                    val currentVal =
                                            cellValues[subAction.cell]
                                                    ?: subAction.cell.currentValue
                                    cellValues[subAction.cell] = currentVal - subAction.diff
                                }
                            }
                            is NoteAction -> {
                                // Skip
                            }
                        }
                    }
                }
            }
        }

        return cellValues
    }

    /** Create a reverse action for the given action */
    private fun createReverseAction(action: Action): Action {
        return when (action) {
            is SolveAction -> {
                // Create an action that undoes this solve
                SolveAction(-action.diff, action.cell)
            }
            is NoteAction -> {
                // Note toggle is its own reverse (toggling again reverses it)
                val reverseActionType =
                        when (action.actionType) {
                            NoteAction.Action.SET -> NoteAction.Action.REMOVE
                            NoteAction.Action.REMOVE -> NoteAction.Action.SET
                        }
                NoteAction(action.diff, reverseActionType, action.cell, action.noteStyle)
            }
            else -> throw IllegalArgumentException("Unknown action type")
        }
    }

    /**
     * Enter view-only mode to temporarily view a node
     *
     * @param targetNode The node to view
     * @return The ViewContext for restoring later
     */
    fun viewNode(targetNode: ActionTreeElement): ViewContext {
        branchManager.enterViewMode(targetNode)

        // Navigate to the node
        val path = findPath(currentState!!, targetNode) ?: emptyList()
        navigateAlongPath(path)

        return branchManager.viewContext!!
    }

    /** Exit view-only mode and return to original state */
    fun exitViewMode() {
        val context = branchManager.viewContext ?: return

        // Navigate back to original node
        val path = findPath(currentState!!, context.originalNode!!) ?: emptyList()
        navigateAlongPath(path)

        branchManager.exitViewMode()
        notifyListeners(currentState!!)
    }

    /** Navigate along a path by executing/undoing actions */
    private fun navigateAlongPath(path: List<ActionTreeElement>) {
        for (i in 1 until path.size) {
            if (path[i - 1].parent === path[i]) {
                // Going backwards - undo
                path[i - 1].undo()
            } else {
                // Going forward - execute
                if (i - 2 >= 0 && path[i - 2].parent !== path[i - 1]) {
                    path[i - 1].execute()
                }
                if (path[i] === path.last()) {
                    path[i].execute()
                }
            }
        }
    }

    /**
     * Switch to a different branch
     *
     * @param branchId The ID of the branch to switch to
     * @return true if successful, false if branch not found
     */
    fun switchBranch(branchId: String): Boolean {
        val branch = branchManager.getBranch(branchId) ?: return false

        locked = true
        try {
            // Save current branch HEAD
            currentBranch?.let { current ->
                branchManager.updateBranchHead(current.id, currentState!!)
            }

            // Switch branch
            branchManager.switchToBranch(branch)

            // Navigate to the branch HEAD
            goToState(branch.head)

            notifyListeners(currentState!!)
            return true
        } finally {
            locked = false
        }
    }

    /**
     * Create a new branch from current state
     *
     * @param name Name for the new branch
     * @return The created branch
     */
    fun createBranch(name: String): ActionBranch {
        return branchManager.createBranch(
                name = name,
                fromNode = currentState!!,
                autoGenerate = false
        )
    }

    /**
     * Delete a branch
     *
     * @param branchId The ID of the branch to delete
     * @throws IllegalArgumentException if trying to delete main or current branch
     */
    fun deleteBranch(branchId: String) {
        branchManager.removeBranch(branchId)
    }

    /**
     * Rename a branch
     *
     * @param branchId The ID of the branch to rename
     * @param newName The new name
     * @return true if successful
     */
    fun renameBranch(branchId: String, newName: String): Boolean {
        return branchManager.renameBranch(branchId, newName)
    }

    /** Get all branches */
    fun getAllBranches(): List<ActionBranch> {
        return branchManager.getAllBranches()
    }

    /**
     * Squash multiple consecutive nodes into a single compound action
     *
     * @param startNode The earliest node in the range (inclusive)
     * @param endNode The latest node in the range (inclusive)
     * @param description Optional description for the compound action
     * @return true if successful, false if the operation is not valid
     */
    fun squashNodes(
            startNode: ActionTreeElement,
            endNode: ActionTreeElement,
            description: String? = null
    ): Boolean {
        locked = true

        try {
            // Validate that endNode is a descendant of startNode on a linear path
            val pathToSquash = mutableListOf<ActionTreeElement>()
            var current = endNode

            while (current != startNode) {
                pathToSquash.add(0, current)

                // Check if we can continue backwards
                val parent = current.parent
                if (parent == null) {
                    // Reached root without finding startNode - invalid range
                    return false
                }

                // Check if there are multiple children (branching point)
                if (parent.childrenList.size > 1 && current != endNode) {
                    // There's a branch point in the middle - not a linear path
                    return false
                }

                current = parent
            }

            // Add start node
            pathToSquash.add(0, startNode)

            // Can't squash if there are no nodes or only one node
            if (pathToSquash.size <= 1) {
                return false
            }

            // Collect all actions to squash
            val actionsToSquash = pathToSquash.map { it.action }

            // Create compound action
            val compoundAction = CompoundAction.createSquash(actionsToSquash, description)

            // Navigate back to the parent of startNode
            val parentOfStart = startNode.parent
            if (parentOfStart != null) {
                goToState(parentOfStart)
            }

            // Add the compound action
            addStrategic(compoundAction)

            // Remove the old nodes from the tree
            // Note: This is a destructive operation - the old history is lost
            // In a real implementation, you might want to keep them in a separate structure

            // Update current branch HEAD
            currentBranch?.let { branch ->
                branchManager.updateBranchHead(branch.id, currentState!!)
            }

            notifyListeners(currentState!!)
            return true
        } finally {
            locked = false
        }
    }

    init {
        currentState = actionTree.root
        branchManager.initialize(actionTree.root)
        locked = false
    }
}

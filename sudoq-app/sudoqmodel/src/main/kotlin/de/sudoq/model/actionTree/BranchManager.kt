/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.actionTree

import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages all branches in the action tree. Tracks the current branch and provides operations for
 * branch management.
 */
class BranchManager {
    private val branches = mutableMapOf<String, ActionBranch>()

    /** The currently active branch */
    var currentBranch: ActionBranch? = null
        private set

    /** The current node pointer (may differ from currentBranch.head during navigation) */
    var currentNode: ActionTreeElement? = null
        private set

    /** Context for view-only mode, null if not in view mode */
    var viewContext: ViewContext? = null
        private set

    /** Check if currently in view-only mode */
    val isInViewMode: Boolean
        get() = viewContext != null

    /** Initialize with the main branch */
    fun initialize(root: ActionTreeElement) {
        val mainBranch = ActionBranch.createMainBranch(root)
        branches[mainBranch.id] = mainBranch
        currentBranch = mainBranch
        currentNode = root
    }

    /** Add a new branch to the manager */
    fun addBranch(branch: ActionBranch) {
        branches[branch.id] = branch
    }

    /**
     * Remove a branch by ID
     * @throws IllegalArgumentException if trying to delete the main branch
     */
    fun removeBranch(branchId: String) {
        if (branchId == ActionBranch.MAIN_BRANCH_ID) {
            throw IllegalArgumentException("Cannot delete main branch")
        }
        if (currentBranch?.id == branchId) {
            throw IllegalArgumentException("Cannot delete current branch")
        }
        branches.remove(branchId)
    }

    /** Switch to a specific branch */
    fun switchToBranch(branch: ActionBranch) {
        // Exit view mode if active
        exitViewMode()

        currentBranch = branch
        currentNode = branch.head
    }

    /** Switch to a branch by ID */
    fun switchToBranch(branchId: String): Boolean {
        val branch = branches[branchId] ?: return false
        switchToBranch(branch)
        return true
    }

    /** Update the current branch's HEAD to point to a new node */
    fun updateCurrentBranchHead(newHead: ActionTreeElement) {
        currentBranch?.let { branch ->
            val updatedBranch = branch.copy(head = newHead)
            branches[branch.id] = updatedBranch
            currentBranch = updatedBranch
        }
        currentNode = newHead
    }

    /** Update a specific branch's HEAD */
    fun updateBranchHead(branchId: String, newHead: ActionTreeElement) {
        branches[branchId]?.let { branch ->
            val updatedBranch = branch.copy(head = newHead)
            branches[branchId] = updatedBranch

            // Update currentBranch reference if it's the current one
            if (currentBranch?.id == branchId) {
                currentBranch = updatedBranch
            }
        }
    }

    /** Set the current node without changing branches. Used during navigation. */
    fun setCurrentNode(node: ActionTreeElement) {
        currentNode = node

        // Also update the current branch's head if we're moving forward
        // (i.e., if the new node is a child of the current branch's head)
        currentBranch?.let { branch ->
            // Only update if node is strictly ahead (not equal to head)
            if (node.id != branch.head.id && isNodeAheadOfHead(node, branch.head)) {
                updateBranchHead(branch.id, node)
            }
        }
    }

    /** Check if a node is ahead of (descendant of) another node */
    private fun isNodeAheadOfHead(node: ActionTreeElement, head: ActionTreeElement): Boolean {
        var current: ActionTreeElement? = node.parent // Start from parent to exclude node itself
        while (current != null) {
            if (current.id == head.id) {
                // node is a descendant of head (but not head itself)
                return true
            }
            current = current.parent
        }
        return false
    }

    /** Get all branches */
    fun getAllBranches(): List<ActionBranch> = branches.values.toList()

    /** Get a branch by ID */
    fun getBranch(id: String): ActionBranch? = branches[id]

    /** Find which branch contains the given node at its HEAD */
    fun findBranchByHead(node: ActionTreeElement): ActionBranch? {
        return branches.values.find { it.head.id == node.id }
    }

    /** Find all branches that contain the given node in their history */
    fun findBranchesContaining(node: ActionTreeElement): List<ActionBranch> {
        return branches.values.filter { it.containsNode(node) }
    }

    /** Generate a unique branch name */
    fun generateBranchName(fromNode: ActionTreeElement): String {
        val timestamp = SimpleDateFormat("MMdd-HHmm", Locale.US).format(Date())
        val action = fromNode.action
        val hint =
                when (action) {
                    is SolveAction -> "solve-${action.cell.id}"
                    is NoteAction -> "note-${action.cell.id}"
                    else -> "action"
                }

        var baseName = "branch-$hint-$timestamp"
        var counter = 1
        var finalName = baseName

        // Ensure unique name
        while (branches.values.any { it.name == finalName }) {
            finalName = "$baseName-$counter"
            counter++
        }

        return finalName
    }

    /** Generate a unique branch ID */
    fun generateBranchId(): String {
        return "branch-${UUID.randomUUID()}"
    }

    /** Create a new branch from the given node */
    fun createBranch(
            name: String? = null,
            fromNode: ActionTreeElement,
            autoGenerate: Boolean = true
    ): ActionBranch {
        val branchName = name ?: if (autoGenerate) generateBranchName(fromNode) else "new-branch"
        val branchId = generateBranchId()

        val newBranch =
                ActionBranch(
                        id = branchId,
                        name = branchName,
                        head = fromNode,
                        createdAt = System.currentTimeMillis(),
                        createdFrom = fromNode
                )

        addBranch(newBranch)
        return newBranch
    }

    /** Rename a branch */
    fun renameBranch(branchId: String, newName: String): Boolean {
        val branch = branches[branchId] ?: return false
        val renamedBranch = branch.copy(name = newName)
        branches[branchId] = renamedBranch

        // Update currentBranch reference if it's the current one
        if (currentBranch?.id == branchId) {
            currentBranch = renamedBranch
        }

        return true
    }

    /** Enter view-only mode for the given node */
    fun enterViewMode(targetNode: ActionTreeElement) {
        if (viewContext == null) {
            viewContext =
                    ViewContext(
                            originalBranch = currentBranch,
                            originalNode = currentNode,
                            viewingNode = targetNode
                    )
        }
        currentNode = targetNode
    }

    /** Exit view-only mode and restore original state */
    fun exitViewMode() {
        viewContext?.let { context ->
            currentBranch = context.originalBranch
            currentNode = context.originalNode
            viewContext = null
        }
    }

    /** Clear all branches except main and reinitialize */
    fun reset(root: ActionTreeElement) {
        branches.clear()
        viewContext = null
        initialize(root)
    }
}

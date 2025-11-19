/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.actionTree

/**
 * Represents a branch in the action tree, similar to Git branches. Each branch has a name and
 * points to a specific ActionTreeElement as its HEAD.
 *
 * @property id Unique identifier for the branch
 * @property name User-visible name of the branch
 * @property head The latest ActionTreeElement in this branch
 * @property createdAt Timestamp when the branch was created
 * @property createdFrom The ActionTreeElement from which this branch was created
 */
data class ActionBranch(
        val id: String,
        val name: String,
        val head: ActionTreeElement,
        val createdAt: Long,
        val createdFrom: ActionTreeElement
) {
    /** Get the number of actions in this branch from root to head */
    fun getActionCount(): Int {
        var count = 0
        var current: ActionTreeElement? = head
        while (current != null) {
            count++
            current = current.parent
        }
        return count
    }

    /** Check if this branch contains the given node */
    fun containsNode(node: ActionTreeElement): Boolean {
        var current: ActionTreeElement? = head
        while (current != null) {
            if (current.id == node.id) return true
            current = current.parent
        }
        return false
    }

    companion object {
        const val MAIN_BRANCH_ID = "main"
        const val MAIN_BRANCH_NAME = "main"

        /** Create the default main branch */
        fun createMainBranch(root: ActionTreeElement): ActionBranch {
            return ActionBranch(
                    id = MAIN_BRANCH_ID,
                    name = MAIN_BRANCH_NAME,
                    head = root,
                    createdAt = System.currentTimeMillis(),
                    createdFrom = root
            )
        }
    }
}

/**
 * Context for viewing a node temporarily without modifying branches
 *
 * @property originalBranch The branch the user was on before viewing
 * @property originalNode The node the user was on before viewing
 * @property viewingNode The node being viewed
 */
data class ViewContext(
        val originalBranch: ActionBranch?,
        val originalNode: ActionTreeElement?,
        val viewingNode: ActionTreeElement
) {
    val isViewing: Boolean
        get() = originalNode != null && originalNode.id != viewingNode.id
}

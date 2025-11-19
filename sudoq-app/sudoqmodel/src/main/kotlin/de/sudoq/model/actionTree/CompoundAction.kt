/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.actionTree

/**
 * CompoundAction represents multiple actions combined into a single operation. This is used for
 * operations like:
 * - Revert: Combining multiple reverse actions into one node
 * - Rebase/Squash: Merging multiple sequential operations
 *
 * @property actions The list of actions to execute in order
 * @property type The type of compound action (for display purposes)
 * @property description Optional description of what this compound action does
 */
class CompoundAction(
        internal val actions: List<Action>,
        val type: CompoundActionType,
        val description: String? = null
) :
        Action(
                diff = 0, // Not applicable for compound actions
                cell = actions.firstOrNull()?.cell
                                ?: throw IllegalArgumentException("Actions list cannot be empty")
        ) {

    init {
        if (actions.isEmpty()) {
            throw IllegalArgumentException("CompoundAction must contain at least one action")
        }
    }

    /** The number of individual actions contained in this compound action */
    val actionCount: Int
        get() = actions.size

    /** Get a summary of all actions for display */
    fun getActionSummary(): String {
        val counts = actions.groupBy { it::class.simpleName }.mapValues { it.value.size }
        return counts.entries.joinToString(", ") { (type, count) -> "$count × $type" }
    }

    /** Execute all contained actions in order */
    override fun execute() {
        actions.forEach { it.execute() }
    }

    /** Undo all contained actions in reverse order */
    override fun undo() {
        actions.asReversed().forEach { it.undo() }
    }

    /** Check if another action is the inverse of this compound action */
    override fun inverse(a: Action): Boolean {
        if (a !is CompoundAction) return false
        if (actions.size != a.actions.size) return false

        // Check if all actions are inverse in reverse order
        return actions.zip(a.actions.asReversed()).all { (action1, action2) ->
            action1.inverse(action2)
        }
    }

    /** Get detailed information about all contained actions */
    fun getDetailedInfo(): String {
        return buildString {
            append("$type (${actions.size} actions)")
            description?.let { append(": $it") }
            append("\n")
            actions.forEachIndexed { index, action ->
                append("  ${index + 1}. ${action.javaClass.simpleName}")
                when (action) {
                    is SolveAction -> append(" - Cell ${action.cellId}: ${action.diff}")
                    is NoteAction -> append(" - Cell ${action.cellId}: ${action.actionType}")
                }
                append("\n")
            }
        }
    }

    /** Create a copy with different description */
    fun withDescription(newDescription: String): CompoundAction {
        return CompoundAction(actions, type, newDescription)
    }

    companion object {

        /**
         * Create a squash compound action from multiple sequential actions
         * @param actionsToSquash The list of actions to combine into one
         * @param description Optional description for the squashed operation
         */
        fun createSquash(
                actionsToSquash: List<Action>,
                description: String? = null
        ): CompoundAction {
            return CompoundAction(
                    actions = actionsToSquash,
                    type = CompoundActionType.SQUASH,
                    description = description ?: "Squash ${actionsToSquash.size} operation(s)"
            )
        }
    }
}

/** Types of compound actions for display and behavior differentiation */
enum class CompoundActionType(val displayName: String, val icon: String) {
    REVERT("Revert", "↶"),
    SQUASH("Squash", "⊡"),
    MERGE("Merge", "⑂")
}

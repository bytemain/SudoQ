/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.actionTree

import de.sudoq.model.sudoku.Cell

/**
 * A composite action that groups multiple actions together as a single undoable operation.
 * This is useful for multi-selection scenarios where multiple cells are modified at once.
 * 
 * When undoing, all actions are reversed in reverse order.
 * When executing, all actions are performed in order.
 * 
 * @property actions The list of actions to be executed/undone as a batch
 */
class BatchAction(
    private val actions: List<Action>
) : Action(
    0, // diff is not meaningful for batch actions
    actions.firstOrNull()?.cell ?: Cell(-1, 1) // dummy cell if empty
) {
    
    init {
        XML_ATTRIBUTE_NAME = "BatchAction"
    }
    
    /**
     * Executes all actions in order
     */
    override fun execute() {
        for (action in actions) {
            action.execute()
        }
    }
    
    /**
     * Undoes all actions in reverse order
     */
    override fun undo() {
        for (action in actions.reversed()) {
            action.undo()
        }
    }
    
    /**
     * Batch actions don't inverse with other actions
     */
    fun inverse(a: Action): Boolean = false
    
    /**
     * Returns the number of actions in this batch
     */
    fun size(): Int = actions.size
    
    /**
     * Returns the list of actions
     */
    fun getActions(): List<Action> = actions
    
    /**
     * Returns true if this batch is empty
     */
    fun isEmpty(): Boolean = actions.isEmpty()
    
    override fun toString(): String {
        return "BatchAction(size=${actions.size}, actions=${actions.map { it.javaClass.simpleName }})"
    }
}

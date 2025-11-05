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
 * This class represents a composite action that fills candidates (notes) for multiple cells.
 * It stores the state changes for all affected cells and can undo them as a single operation.
 * 
 * @property cellChanges A list of changes to be applied, each containing cell, candidate number, and whether to add or remove
 */
class FillCandidatesAction(
    private val cellChanges: List<CellChange>
) : Action(0, if (cellChanges.isNotEmpty()) cellChanges[0].cell else Cell(-1, 1)) {

    data class CellChange(
        val cell: Cell,
        val candidate: Int,
        val shouldSet: Boolean // true to add note, false to remove note
    )

    init {
        XML_ATTRIBUTE_NAME = "FillCandidatesAction"
    }

    /**
     * Executes the action by toggling all notes that need to change.
     */
    override fun execute() {
        for (change in cellChanges) {
            val isCurrentlySet = change.cell.isNoteSet(change.candidate)
            // Only toggle if the state needs to change
            if (change.shouldSet != isCurrentlySet) {
                change.cell.toggleNote(change.candidate)
            }
        }
    }

    /**
     * Reverses the action by toggling all notes back to their original state.
     * This undoes what execute() did by reversing each toggle.
     */
    override fun undo() {
        for (change in cellChanges) {
            val isCurrentlySet = change.cell.isNoteSet(change.candidate)
            // Reverse the change: if we set it in execute, remove it now; if we removed it, add it back
            // After execute: isCurrentlySet == shouldSet (they match)
            // So to undo, we toggle when they match (to make them not match again)
            if (change.shouldSet == isCurrentlySet) {
                change.cell.toggleNote(change.candidate)
            }
        }
    }

    /**
     * Checks if another action is the inverse of this one.
     * For FillCandidatesAction, it is not self-inverse, so this always returns false.
     * The undo mechanism handles reverting the changes.
     */
    override fun inverse(a: Action): Boolean {
        // FillCandidatesAction is not self-inverse
        // Undo is handled by the undo() method which toggles notes back
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FillCandidatesAction) return false
        if (!super.equals(other)) return false

        return cellChanges == other.cellChanges
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + cellChanges.hashCode()
        return result
    }
}

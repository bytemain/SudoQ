/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.actionTree

import de.sudoq.model.sudoku.Cell
import de.sudoq.model.sudoku.Sudoku

/**
 * Extended SolveAction that also handles automatic note updates in related cells.
 * When a cell is filled, this action automatically removes the filled value from 
 * notes in all cells in the same constraints (row, column, block).
 * 
 * @property sudoku The sudoku instance to update notes in
 * @property autoAdjustNotes Whether to automatically adjust notes in related cells
 */
class SolveActionWithNoteUpdate(
    diff: Int, 
    cell: Cell,
    private val sudoku: Sudoku,
    private val autoAdjustNotes: Boolean
) : SolveAction(diff, cell) {

    /**
     * Stores which cells had notes removed, so we can restore them on undo
     * Map: cell ID -> set of candidate values that were removed
     */
    private val removedNotes: MutableMap<Int, MutableSet<Int>> = mutableMapOf()

    init {
        XML_ATTRIBUTE_NAME = "SolveActionWithNoteUpdate"
    }

    /**
     * Executes the solve action and automatically adjusts notes in related cells
     */
    override fun execute() {
        // First execute the main solve action
        super.execute()

        // Then handle note updates if enabled
        if (autoAdjustNotes && cell.isSolved) {
            updateRelatedNotes(true)
        }
    }

    /**
     * Undoes the solve action and restores the notes that were automatically removed
     */
    override fun undo() {
        // First restore the notes that were removed
        if (autoAdjustNotes) {
            updateRelatedNotes(false)
        }

        // Then undo the main solve action
        super.undo()
    }

    /**
     * Updates or restores notes in cells related by constraints
     * @param isExecuting true when executing (remove notes), false when undoing (restore notes)
     */
    private fun updateRelatedNotes(isExecuting: Boolean) {
        val editedPos = sudoku.getPosition(cell.id) ?: return
        val value = cell.currentValue

        if (isExecuting) {
            // Remove notes from related cells
            removedNotes.clear()
            
            for (constraint in sudoku.sudokuType!!) {
                if (constraint.includes(editedPos)) {
                    for (constraintPos in constraint) {
                        val affectedCell = sudoku.getCell(constraintPos) ?: continue
                        
                        if (affectedCell.isNoteSet(value)) {
                            // Record that we're removing this note
                            removedNotes.getOrPut(affectedCell.id) { mutableSetOf() }.add(value)
                            // Remove the note
                            affectedCell.toggleNote(value)
                        }
                    }
                }
            }
        } else {
            // Restore notes that were removed
            for ((cellId, candidates) in removedNotes) {
                val affectedCell = sudoku.getCell(cellId) ?: continue
                for (candidate in candidates) {
                    if (!affectedCell.isNoteSet(candidate)) {
                        affectedCell.toggleNote(candidate)
                    }
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SolveActionWithNoteUpdate) return false
        if (!super.equals(other)) return false

        return sudoku == other.sudoku && autoAdjustNotes == other.autoAdjustNotes
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + sudoku.hashCode()
        result = 31 * result + autoAdjustNotes.hashCode()
        return result
    }
}

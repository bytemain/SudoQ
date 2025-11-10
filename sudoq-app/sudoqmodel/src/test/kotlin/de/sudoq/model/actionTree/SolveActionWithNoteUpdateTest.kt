package de.sudoq.model.actionTree

import de.sudoq.model.sudoku.*
import de.sudoq.model.sudoku.sudokuTypes.SudokuType
import de.sudoq.model.sudoku.sudokuTypes.ComplexityConstraintBuilder
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test for SolveActionWithNoteUpdate to verify that note adjustments are 
 * properly undone when the solve action is undone
 */
class SolveActionWithNoteUpdateTest {

    private lateinit var sudoku: Sudoku
    private lateinit var cell1: Cell
    private lateinit var cell2: Cell
    private lateinit var cell3: Cell

    @BeforeEach
    fun setUp() {
        // Create a simple 9x9 Sudoku with a minimal SudokuType
        val sudokuType = createSimple9x9Type()
        val solutionMap = PositionMap<Int>(sudokuType.size!!)
        val setValuesMap = PositionMap<Boolean>(sudokuType.size!!)
        
        sudoku = Sudoku(sudokuType, solutionMap, setValuesMap)
        
        // Get some cells in the same row (constraint)
        cell1 = sudoku.getCell(Position.get(0, 0))!! // Cell to fill
        cell2 = sudoku.getCell(Position.get(1, 0))!! // Related cell in same row
        cell3 = sudoku.getCell(Position.get(2, 0))!! // Another related cell
    }
    
    private fun createSimple9x9Type(): SudokuType {
        // Create a minimal 9x9 sudoku type for testing
        val type = SudokuType(9, 9, 9)
        
        // Add row constraints
        for (row in 0..8) {
            val constraint = Constraint()
            for (col in 0..8) {
                constraint.addPosition(Position.get(col, row))
            }
            type.constraints.add(constraint)
        }
        
        // Add column constraints
        for (col in 0..8) {
            val constraint = Constraint()
            for (row in 0..8) {
                constraint.addPosition(Position.get(col, row))
            }
            type.constraints.add(constraint)
        }
        
        // Add 3x3 block constraints
        for (blockRow in 0..2) {
            for (blockCol in 0..2) {
                val constraint = Constraint()
                for (row in 0..2) {
                    for (col in 0..2) {
                        constraint.addPosition(
                            Position.get(
                                blockCol * 3 + col,
                                blockRow * 3 + row
                            )
                        )
                    }
                }
                type.constraints.add(constraint)
            }
        }
        
        return type
    }

    @Test
    fun testExecute_RemovesNotesFromRelatedCells() {
        // Set notes in related cells
        cell2.toggleNote(5)
        cell3.toggleNote(5)
        cell3.toggleNote(7)
        
        // Verify initial state
        assertTrue(cell2.isNoteSet(5))
        assertTrue(cell3.isNoteSet(5))
        assertTrue(cell3.isNoteSet(7))
        
        // Create action to fill cell1 with value 5
        val action = SolveActionWithNoteUpdate(5, cell1, sudoku, true)
        
        // Execute the action
        action.execute()
        
        // Verify cell1 is filled
        assertEquals(5, cell1.currentValue)
        
        // Verify note 5 is removed from related cells
        assertFalse(cell2.isNoteSet(5), "Note 5 should be removed from cell2")
        assertFalse(cell3.isNoteSet(5), "Note 5 should be removed from cell3")
        
        // Note 7 should remain (different value)
        assertTrue(cell3.isNoteSet(7), "Note 7 should remain in cell3")
    }

    @Test
    fun testUndo_RestoresNotesInRelatedCells() {
        // Set notes in related cells
        cell2.toggleNote(5)
        cell3.toggleNote(5)
        cell3.toggleNote(7)
        
        // Create and execute action
        val action = SolveActionWithNoteUpdate(5, cell1, sudoku, true)
        action.execute()
        
        // Verify notes were removed
        assertFalse(cell2.isNoteSet(5))
        assertFalse(cell3.isNoteSet(5))
        
        // Undo the action
        action.undo()
        
        // Verify cell1 is cleared
        assertEquals(Cell.EMPTYVAL, cell1.currentValue)
        
        // Verify notes are restored
        assertTrue(cell2.isNoteSet(5), "Note 5 should be restored in cell2")
        assertTrue(cell3.isNoteSet(5), "Note 5 should be restored in cell3")
        assertTrue(cell3.isNoteSet(7), "Note 7 should still be present in cell3")
    }

    @Test
    fun testWithAutoAdjustNotesDisabled() {
        // Set notes in related cells
        cell2.toggleNote(5)
        cell3.toggleNote(5)
        
        // Create action with autoAdjustNotes disabled
        val action = SolveActionWithNoteUpdate(5, cell1, sudoku, false)
        
        // Execute
        action.execute()
        
        // Cell should be filled
        assertEquals(5, cell1.currentValue)
        
        // But notes should NOT be removed (autoAdjustNotes is false)
        assertTrue(cell2.isNoteSet(5), "Note should not be removed when autoAdjustNotes is false")
        assertTrue(cell3.isNoteSet(5), "Note should not be removed when autoAdjustNotes is false")
        
        // Undo
        action.undo()
        
        // Cell should be cleared
        assertEquals(Cell.EMPTYVAL, cell1.currentValue)
        
        // Notes should still be there
        assertTrue(cell2.isNoteSet(5))
        assertTrue(cell3.isNoteSet(5))
    }

    @Test
    fun testMultipleExecuteUndoCycles() {
        // Set notes in related cells
        cell2.toggleNote(5)
        cell3.toggleNote(5)
        
        val action = SolveActionWithNoteUpdate(5, cell1, sudoku, true)
        
        // First cycle: execute and undo
        action.execute()
        assertFalse(cell2.isNoteSet(5))
        action.undo()
        assertTrue(cell2.isNoteSet(5))
        
        // Second cycle: execute and undo again
        action.execute()
        assertFalse(cell2.isNoteSet(5))
        action.undo()
        assertTrue(cell2.isNoteSet(5))
        
        // Verify final state is correct
        assertEquals(Cell.EMPTYVAL, cell1.currentValue)
        assertTrue(cell2.isNoteSet(5))
        assertTrue(cell3.isNoteSet(5))
    }

    @Test
    fun testDeleteEntry_RestoresNotes() {
        // First, fill a cell (which removes notes from related cells)
        cell2.toggleNote(5)
        cell3.toggleNote(5)
        
        val fillAction = SolveActionWithNoteUpdate(5, cell1, sudoku, true)
        fillAction.execute()
        
        // Notes are removed
        assertFalse(cell2.isNoteSet(5))
        assertFalse(cell3.isNoteSet(5))
        
        // Now delete the entry (fill with EMPTYVAL)
        // This should restore the notes
        fillAction.undo()
        
        // Verify notes are restored
        assertTrue(cell2.isNoteSet(5))
        assertTrue(cell3.isNoteSet(5))
    }
}

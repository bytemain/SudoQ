package de.sudoq.model.actionTree

import de.sudoq.model.sudoku.Cell
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Test for FillCandidatesAction to verify execute and undo work correctly
 */
class FillCandidatesActionTest {

    @Test
    fun testExecuteAndUndo_SingleCell() {
        // Create a cell
        val cell = Cell(1, 9)
        
        // Initially, no notes should be set
        assertFalse(cell.isNoteSet(0))
        assertFalse(cell.isNoteSet(1))
        assertFalse(cell.isNoteSet(2))
        
        // Create changes to set notes 0, 1, 2
        val changes = listOf(
            FillCandidatesAction.CellChange(cell, 0, true),
            FillCandidatesAction.CellChange(cell, 1, true),
            FillCandidatesAction.CellChange(cell, 2, true)
        )
        
        val action = FillCandidatesAction(changes)
        
        // Execute the action
        action.execute()
        
        // Verify notes are set
        assertTrue(cell.isNoteSet(0))
        assertTrue(cell.isNoteSet(1))
        assertTrue(cell.isNoteSet(2))
        
        // Undo the action
        action.undo()
        
        // Verify notes are removed
        assertFalse(cell.isNoteSet(0))
        assertFalse(cell.isNoteSet(1))
        assertFalse(cell.isNoteSet(2))
    }

    @Test
    fun testExecuteAndUndo_RemoveNotes() {
        // Create a cell with some notes already set
        val cell = Cell(1, 9)
        cell.toggleNote(3)
        cell.toggleNote(4)
        cell.toggleNote(5)
        
        // Verify initial state
        assertTrue(cell.isNoteSet(3))
        assertTrue(cell.isNoteSet(4))
        assertTrue(cell.isNoteSet(5))
        
        // Create changes to remove notes 3, 4
        val changes = listOf(
            FillCandidatesAction.CellChange(cell, 3, false),
            FillCandidatesAction.CellChange(cell, 4, false)
        )
        
        val action = FillCandidatesAction(changes)
        
        // Execute the action
        action.execute()
        
        // Verify notes are removed
        assertFalse(cell.isNoteSet(3))
        assertFalse(cell.isNoteSet(4))
        assertTrue(cell.isNoteSet(5)) // This one should remain
        
        // Undo the action
        action.undo()
        
        // Verify notes are restored
        assertTrue(cell.isNoteSet(3))
        assertTrue(cell.isNoteSet(4))
        assertTrue(cell.isNoteSet(5))
    }

    @Test
    fun testExecuteAndUndo_MultipleCells() {
        // Create multiple cells
        val cell1 = Cell(1, 9)
        val cell2 = Cell(2, 9)
        val cell3 = Cell(3, 9)
        
        // Create changes for multiple cells
        val changes = listOf(
            FillCandidatesAction.CellChange(cell1, 0, true),
            FillCandidatesAction.CellChange(cell1, 1, true),
            FillCandidatesAction.CellChange(cell2, 2, true),
            FillCandidatesAction.CellChange(cell2, 3, true),
            FillCandidatesAction.CellChange(cell3, 4, true)
        )
        
        val action = FillCandidatesAction(changes)
        
        // Execute
        action.execute()
        
        // Verify all changes applied
        assertTrue(cell1.isNoteSet(0))
        assertTrue(cell1.isNoteSet(1))
        assertTrue(cell2.isNoteSet(2))
        assertTrue(cell2.isNoteSet(3))
        assertTrue(cell3.isNoteSet(4))
        
        // Undo
        action.undo()
        
        // Verify all changes reverted
        assertFalse(cell1.isNoteSet(0))
        assertFalse(cell1.isNoteSet(1))
        assertFalse(cell2.isNoteSet(2))
        assertFalse(cell2.isNoteSet(3))
        assertFalse(cell3.isNoteSet(4))
    }

    @Test
    fun testExecute_NoChangeIfAlreadyInDesiredState() {
        // Create a cell with note already set
        val cell = Cell(1, 9)
        cell.toggleNote(5)
        
        assertTrue(cell.isNoteSet(5))
        
        // Create a change that says "should be set" but it's already set
        val changes = listOf(
            FillCandidatesAction.CellChange(cell, 5, true)
        )
        
        val action = FillCandidatesAction(changes)
        
        // Execute - should not toggle since it's already in the desired state
        action.execute()
        
        // Note should still be set
        assertTrue(cell.isNoteSet(5))
        
        // Undo should toggle it off
        action.undo()
        
        // Note should be removed
        assertFalse(cell.isNoteSet(5))
    }
}

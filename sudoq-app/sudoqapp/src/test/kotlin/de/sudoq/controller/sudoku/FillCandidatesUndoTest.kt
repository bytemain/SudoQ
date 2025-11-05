package de.sudoq.controller.sudoku

import de.sudoq.model.game.Game
import de.sudoq.model.sudoku.*
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Test to verify that fillAllCandidates() can be undone.
 */
class FillCandidatesUndoTest {

    private lateinit var sudoku: Sudoku
    private lateinit var game: Game
    private lateinit var controller: SudokuController
    private lateinit var activity: SudokuActivity

    @Before
    fun setup() {
        val sudokuType = TypeBuilder.getType(SudokuTypes.standard9x9)
        val solutionMap = PositionMap<Int>(sudokuType.size!!)
        val setValuesMap = PositionMap<Boolean>(sudokuType.size!!)
        
        // Set up a simple pattern with some prefilled cells
        // Fill first row: values 0-8 (internal representation; displayed as 1-9 to users)
        for (i in 0 until 9) {
            val pos = Position[0, i]
            solutionMap[pos] = i
            setValuesMap[pos] = true // Mark as prefilled (not editable)
        }
        
        sudoku = Sudoku(sudokuType, solutionMap, setValuesMap)
        sudoku.complexity = Complexity.arbitrary
        
        game = Game(1, sudoku)
        
        // Mock the activity
        activity = mock(SudokuActivity::class.java)
        
        controller = SudokuController(game, activity)
    }

    @Test
    fun testFillAllCandidates_CanBeUndone() {
        // Verify initial state - cells should have no candidates
        val testCell = sudoku.getCell(Position[1, 0])
        assertNotNull(testCell)
        assertEquals(0, testCell!!.getNotesCount())
        
        // Check that we can't undo before any action
        assertFalse("Should not be able to undo before any action", game.stateHandler!!.canUndo())
        
        // Execute fill candidates
        controller.fillAllCandidates()
        
        // Verify that candidates were filled
        assertTrue("Test cell should have candidates after fill", testCell.getNotesCount() > 0)
        val candidatesAfterFill = testCell.getNotesCount()
        
        // Verify that we can now undo
        assertTrue("Should be able to undo after fillAllCandidates", game.stateHandler!!.canUndo())
        
        // Execute undo
        game.stateHandler!!.undo()
        
        // Verify that candidates were removed (back to initial state)
        assertEquals("Test cell should have no candidates after undo", 0, testCell.getNotesCount())
        
        // Verify that we can redo
        assertTrue("Should be able to redo after undo", game.stateHandler!!.canRedo())
        
        // Execute redo
        game.stateHandler!!.redo()
        
        // Verify that candidates were restored
        assertEquals("Test cell should have candidates restored after redo", 
                     candidatesAfterFill, testCell.getNotesCount())
    }

    @Test
    fun testFillAllCandidates_MultipleEmptyCells() {
        // Check multiple cells
        val cell1 = sudoku.getCell(Position[1, 0])
        val cell2 = sudoku.getCell(Position[1, 1])
        val cell3 = sudoku.getCell(Position[2, 0])
        
        assertNotNull(cell1)
        assertNotNull(cell2)
        assertNotNull(cell3)
        
        // Initial state
        assertEquals(0, cell1!!.getNotesCount())
        assertEquals(0, cell2!!.getNotesCount())
        assertEquals(0, cell3!!.getNotesCount())
        
        // Fill candidates
        controller.fillAllCandidates()
        
        // All should have candidates
        assertTrue(cell1.getNotesCount() > 0)
        assertTrue(cell2.getNotesCount() > 0)
        assertTrue(cell3.getNotesCount() > 0)
        
        // Undo should remove all candidates
        game.stateHandler!!.undo()
        
        assertEquals(0, cell1.getNotesCount())
        assertEquals(0, cell2.getNotesCount())
        assertEquals(0, cell3.getNotesCount())
    }

    @Test
    fun testFillAllCandidates_DoesNotAffectPrefilledCells() {
        // Fill candidates
        controller.fillAllCandidates()
        
        // Check that prefilled cells (first row) are not affected
        for (i in 0 until 9) {
            val prefilledCell = sudoku.getCell(Position[0, i])
            assertNotNull(prefilledCell)
            assertEquals("Prefilled cell should have no notes", 0, prefilledCell!!.getNotesCount())
            assertTrue("Prefilled cell should be solved", prefilledCell.isSolved)
        }
    }
}

package de.sudoq.controller.sudoku

import de.sudoq.model.game.Game
import de.sudoq.model.sudoku.Position
import de.sudoq.model.sudoku.PositionMap
import de.sudoq.model.sudoku.Sudoku
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.StandardSudokuType9x9
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class SudokuControllerFillCandidatesTest {

    private lateinit var sudoku: Sudoku
    private lateinit var game: Game
    private lateinit var activity: SudokuActivity
    private lateinit var controller: SudokuController

    @BeforeEach
    fun setUp() {
        // Create a simple 9x9 Sudoku for testing
        val sudokuType = StandardSudokuType9x9()
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
    fun testFillAllCandidates_EmptyCellsGetValidCandidates() {
        // Execute fill candidates
        controller.fillAllCandidates()
        
        // Check that empty editable cells now have candidates set
        // Cell at position (1,0) should have candidates excluding what's in column 0
        val cell = sudoku.getCell(Position[1, 0])
        assertNotNull(cell)
        
        if (cell != null && cell.isEditable && cell.isNotSolved) {
            // The cell should have some notes set
            assertTrue(cell.getNotesCount() > 0, "Empty editable cell should have candidates filled")
            
            // Verify that the value from the same column is NOT a candidate
            // Column 0, row 0 has value 0, so cell at (1,0) in the same column should not have 0 as candidate
            assertFalse(cell.isNoteSet(0), "Value from same column should not be a candidate")
        }
    }

    @Test
    fun testFillAllCandidates_PrefilledCellsUnchanged() {
        // Execute fill candidates
        controller.fillAllCandidates()
        
        // Check that prefilled cells remain unchanged and have no notes
        val prefilledCell = sudoku.getCell(Position[0, 0])
        assertNotNull(prefilledCell)
        
        if (prefilledCell != null) {
            assertFalse(prefilledCell.isEditable, "First cell should be prefilled (not editable)")
            assertEquals(0, prefilledCell.getNotesCount(), "Prefilled cell should have no notes")
        }
    }

    @Test
    fun testFillAllCandidates_DoesNotChangeFilledEditableCells() {
        // Manually fill an editable cell
        val editableCell = sudoku.getCell(Position[1, 1])
        if (editableCell != null && editableCell.isEditable) {
            editableCell.currentValue = 5
            
            // Execute fill candidates
            controller.fillAllCandidates()
            
            // Verify the filled cell still has its value and no notes were added
            assertEquals(5, editableCell.currentValue, "Filled cell value should remain")
            assertEquals(0, editableCell.getNotesCount(), "Filled editable cell should have no notes")
        }
    }
}

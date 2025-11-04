package de.sudoq.model.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import de.sudoq.model.Utility;
import de.sudoq.model.actionTree.NoteActionFactory;
import de.sudoq.model.actionTree.SolveActionFactory;
import de.sudoq.model.game.Assistances;
import de.sudoq.model.game.GameSettings;
import de.sudoq.model.persistence.IRepo;
import de.sudoq.model.sudoku.Cell;
import de.sudoq.model.sudoku.Position;
import de.sudoq.model.sudoku.Sudoku;
import de.sudoq.model.sudoku.SudokuBuilder;
import de.sudoq.model.sudoku.sudokuTypes.SudokuType;
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes;
import de.sudoq.model.sudoku.sudokuTypes.TypeBuilder;

public class AutoFillUniqueCandidatesTests {

    private static IRepo<SudokuType> sudokuTypeRepo;

    @BeforeClass
    public static void beforeClass() {
        Utility.copySudokus();
        TypeBuilder.get99(); // just to force initialization of filemanager
    }

    @Test
    public void testAutoFillWithOneUniqueCandidate() {
        // Create a simple sudoku and set up a cell with only one note
        Game game = new Game(1, new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku());
        
        Position pos = Position.get(0, 0);
        Cell cell = game.getSudoku().getCell(pos);
        
        // Set only one note on the cell (value 5)
        game.addAndExecute(new NoteActionFactory().createAction(5, cell));
        
        assertEquals(1, cell.getNotesCount());
        assertEquals(5, cell.getSingleNote());
        assertTrue(cell.isNotSolved());
        
        // Auto-fill cells with unique candidates
        int filled = game.autoFillUniqueCandidates();
        
        assertEquals(1, filled);
        assertEquals(5, cell.getCurrentValue());
        assertTrue(cell.isSolved());
    }

    @Test
    public void testAutoFillWithMultipleUniqueCandidates() {
        // Create a sudoku and set up multiple cells with single notes
        Game game = new Game(1, new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku());
        
        Position pos1 = Position.get(0, 0);
        Position pos2 = Position.get(1, 1);
        Position pos3 = Position.get(2, 2);
        
        Cell cell1 = game.getSudoku().getCell(pos1);
        Cell cell2 = game.getSudoku().getCell(pos2);
        Cell cell3 = game.getSudoku().getCell(pos3);
        
        // Set single notes on multiple cells
        game.addAndExecute(new NoteActionFactory().createAction(3, cell1));
        game.addAndExecute(new NoteActionFactory().createAction(7, cell2));
        game.addAndExecute(new NoteActionFactory().createAction(2, cell3));
        
        assertEquals(1, cell1.getNotesCount());
        assertEquals(1, cell2.getNotesCount());
        assertEquals(1, cell3.getNotesCount());
        
        // Auto-fill cells with unique candidates
        int filled = game.autoFillUniqueCandidates();
        
        assertEquals(3, filled);
        assertEquals(3, cell1.getCurrentValue());
        assertEquals(7, cell2.getCurrentValue());
        assertEquals(2, cell3.getCurrentValue());
    }

    @Test
    public void testAutoFillWithNoUniqueCandidates() {
        // Create a sudoku without any cells having unique candidates
        Game game = new Game(1, new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku());
        
        Position pos = Position.get(0, 0);
        Cell cell = game.getSudoku().getCell(pos);
        
        // Set multiple notes on the cell
        game.addAndExecute(new NoteActionFactory().createAction(3, cell));
        game.addAndExecute(new NoteActionFactory().createAction(5, cell));
        game.addAndExecute(new NoteActionFactory().createAction(7, cell));
        
        assertEquals(3, cell.getNotesCount());
        assertEquals(-1, cell.getSingleNote());
        
        // Auto-fill should not fill any cells
        int filled = game.autoFillUniqueCandidates();
        
        assertEquals(0, filled);
        assertTrue(cell.isNotSolved());
    }

    @Test
    public void testAutoFillWithRecursiveFillings() {
        // Create a sudoku where filling one cell creates another unique candidate
        Game game = new Game(1, new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku());
        
        // Enable auto-adjust notes assistance to simulate recursive behavior
        game.setAssistances(new GameSettings() {
            @Override
            public boolean getAssistance(Assistances assistance) {
                return assistance == Assistances.autoAdjustNotes;
            }
        });
        
        Position pos1 = Position.get(0, 0);
        Position pos2 = Position.get(0, 1);
        
        Cell cell1 = game.getSudoku().getCell(pos1);
        Cell cell2 = game.getSudoku().getCell(pos2);
        
        // Set single note on cell1
        game.addAndExecute(new NoteActionFactory().createAction(4, cell1));
        
        // Set notes on cell2 including 4 (which will be removed when cell1 is filled if autoAdjustNotes is active)
        game.addAndExecute(new NoteActionFactory().createAction(4, cell2));
        game.addAndExecute(new NoteActionFactory().createAction(6, cell2));
        
        assertEquals(1, cell1.getNotesCount());
        assertEquals(2, cell2.getNotesCount());
        
        // Auto-fill cells with unique candidates
        int filled = game.autoFillUniqueCandidates();
        
        // At least cell1 should be filled
        assertTrue(filled >= 1);
        assertEquals(4, cell1.getCurrentValue());
    }

    @Test
    public void testAutoFillDoesNotFillSolvedCells() {
        // Create a sudoku with a pre-filled cell
        Game game = new Game(1, new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku());
        
        Position pos = Position.get(0, 0);
        Cell cell = game.getSudoku().getCell(pos);
        
        // First solve the cell manually
        game.addAndExecute(new NoteActionFactory().createAction(8, cell));
        int firstFill = game.autoFillUniqueCandidates();
        assertEquals(1, firstFill);
        assertEquals(8, cell.getCurrentValue());
        
        // Add another note (should not be possible on a solved cell, but let's verify auto-fill handles it)
        int secondFill = game.autoFillUniqueCandidates();
        assertEquals(0, secondFill);
        assertEquals(8, cell.getCurrentValue());
    }

    @Test
    public void testFindCellsWithUniqueCandidate() {
        // Test the Sudoku.findCellsWithUniqueCandidate method directly
        Sudoku sudoku = new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku();
        Game game = new Game(1, sudoku);
        
        Position pos1 = Position.get(0, 0);
        Position pos2 = Position.get(1, 1);
        
        Cell cell1 = sudoku.getCell(pos1);
        Cell cell2 = sudoku.getCell(pos2);
        
        // Initially, no cells should have unique candidates
        assertEquals(0, sudoku.findCellsWithUniqueCandidate().size());
        
        // Add a single note to cell1
        game.addAndExecute(new NoteActionFactory().createAction(5, cell1));
        assertEquals(1, sudoku.findCellsWithUniqueCandidate().size());
        
        // Add multiple notes to cell2 (not a unique candidate)
        game.addAndExecute(new NoteActionFactory().createAction(3, cell2));
        game.addAndExecute(new NoteActionFactory().createAction(7, cell2));
        assertEquals(1, sudoku.findCellsWithUniqueCandidate().size());
        
        // Remove one note from cell2 to make it a unique candidate
        game.addAndExecute(new NoteActionFactory().createAction(3, cell2)); // Toggle off
        assertEquals(2, sudoku.findCellsWithUniqueCandidate().size());
    }

    @Test
    public void testCellGetSingleNote() {
        // Test the Cell.getSingleNote method
        Sudoku sudoku = new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku();
        Game game = new Game(1, sudoku);
        
        Position pos = Position.get(0, 0);
        Cell cell = sudoku.getCell(pos);
        
        // Initially, no notes should be set
        assertEquals(-1, cell.getSingleNote());
        
        // Add a single note
        game.addAndExecute(new NoteActionFactory().createAction(4, cell));
        assertEquals(4, cell.getSingleNote());
        
        // Add another note
        game.addAndExecute(new NoteActionFactory().createAction(8, cell));
        assertEquals(-1, cell.getSingleNote()); // Multiple notes, so -1
    }

    @Test
    public void testCellGetNotesCount() {
        // Test the Cell.getNotesCount method
        Sudoku sudoku = new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku();
        Game game = new Game(1, sudoku);
        
        Position pos = Position.get(0, 0);
        Cell cell = sudoku.getCell(pos);
        
        // Initially, no notes should be set
        assertEquals(0, cell.getNotesCount());
        
        // Add notes one by one
        game.addAndExecute(new NoteActionFactory().createAction(2, cell));
        assertEquals(1, cell.getNotesCount());
        
        game.addAndExecute(new NoteActionFactory().createAction(5, cell));
        assertEquals(2, cell.getNotesCount());
        
        game.addAndExecute(new NoteActionFactory().createAction(9, cell));
        assertEquals(3, cell.getNotesCount());
        
        // Remove a note
        game.addAndExecute(new NoteActionFactory().createAction(5, cell)); // Toggle off
        assertEquals(2, cell.getNotesCount());
    }

    @Test
    public void testAutoFillTriggeredAutomaticallyWhenEnabled() {
        // Test that auto-fill is triggered automatically when a cell is filled and assistance is enabled
        Game game = new Game(1, new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku());
        
        // Enable auto-fill assistance
        game.setAssistances(new GameSettings() {
            @Override
            public boolean getAssistance(Assistances assistance) {
                return assistance == Assistances.autoFillUniqueCandidates;
            }
        });
        
        Position pos1 = Position.get(0, 0);
        Position pos2 = Position.get(1, 1);
        
        Cell cell1 = game.getSudoku().getCell(pos1);
        Cell cell2 = game.getSudoku().getCell(pos2);
        
        // Set single notes on both cells
        game.addAndExecute(new NoteActionFactory().createAction(3, cell1));
        game.addAndExecute(new NoteActionFactory().createAction(7, cell2));
        
        assertEquals(1, cell1.getNotesCount());
        assertEquals(1, cell2.getNotesCount());
        assertTrue(cell1.isNotSolved());
        assertTrue(cell2.isNotSolved());
        
        // Now fill cell1 manually - this should trigger auto-fill for both cells
        game.addAndExecute(new SolveActionFactory().createAction(3, cell1));
        
        // Cell1 should be filled
        assertEquals(3, cell1.getCurrentValue());
        assertTrue(cell1.isSolved());
        
        // Cell2 should also be auto-filled because it had a unique candidate
        assertEquals(7, cell2.getCurrentValue());
        assertTrue(cell2.isSolved());
    }

    @Test
    public void testAutoFillNotTriggeredWhenDisabled() {
        // Test that auto-fill is NOT triggered automatically when assistance is disabled
        Game game = new Game(1, new SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku());
        
        // Disable auto-fill assistance
        game.setAssistances(new GameSettings() {
            @Override
            public boolean getAssistance(Assistances assistance) {
                return false;
            }
        });
        
        Position pos1 = Position.get(0, 0);
        Position pos2 = Position.get(1, 1);
        
        Cell cell1 = game.getSudoku().getCell(pos1);
        Cell cell2 = game.getSudoku().getCell(pos2);
        
        // Set single notes on both cells
        game.addAndExecute(new NoteActionFactory().createAction(3, cell1));
        game.addAndExecute(new NoteActionFactory().createAction(7, cell2));
        
        assertEquals(1, cell1.getNotesCount());
        assertEquals(1, cell2.getNotesCount());
        
        // Fill cell1 manually - auto-fill should NOT trigger
        game.addAndExecute(new SolveActionFactory().createAction(3, cell1));
        
        // Cell1 should be filled
        assertEquals(3, cell1.getCurrentValue());
        
        // Cell2 should NOT be auto-filled
        assertTrue(cell2.isNotSolved());
        assertEquals(1, cell2.getNotesCount());
    }
}

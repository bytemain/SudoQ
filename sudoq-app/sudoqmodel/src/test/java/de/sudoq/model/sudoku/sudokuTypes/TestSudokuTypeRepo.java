package de.sudoq.model.sudoku.sudokuTypes;

import de.sudoq.model.persistence.IRepo;
import de.sudoq.model.sudoku.Position;
import de.sudoq.model.solverGenerator.solver.helper.Helpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple test repository for SudokuTypes that creates basic instances
 * without needing XML resources.
 */
public class TestSudokuTypeRepo implements IRepo<SudokuType> {

    @Override
    public SudokuType create() {
        throw new UnsupportedOperationException("Not implemented for test repository");
    }

    @Override
    public SudokuType read(int id) {
        SudokuTypes type = SudokuTypes.values()[id];
        return createBasicSudokuType(type);
    }

    private SudokuType createBasicSudokuType(SudokuTypes type) {
        // Create a minimal SudokuType for testing purposes
        switch (type) {
            case standard9x9:
                return createStandardType(type, 9, 9, 9, 0.33f);
            case standard16x16:
                return createStandardType(type, 16, 16, 16, 0.33f);
            case standard4x4:
                return createStandardType(type, 4, 4, 4, 0.4f);
            case standard6x6:
                return createStandardType(type, 6, 6, 6, 0.35f);
            case samurai:
                return createStandardType(type, 21, 21, 9, 0.3f);
            case Xsudoku:
                return createStandardType(type, 9, 9, 9, 0.25f);
            case HyperSudoku:
                return createStandardType(type, 9, 9, 9, 0.3f);
            case squigglya:
            case squigglyb:
                return createStandardType(type, 9, 9, 9, 0.3f);
            case stairstep:
                return createStandardType(type, 9, 9, 9, 0.3f);
            default:
                return createStandardType(type, 9, 9, 9, 0.33f);
        }
    }

    private SudokuType createStandardType(SudokuTypes enumType, int width, int height, int numberOfSymbols, float allocationFactor) {
        // Use the full constructor to properly initialize all fields including enumType
        return new SudokuType(
            enumType,
            numberOfSymbols,
            allocationFactor,
            Position.get(width, height),
            Position.get(3, 3),  // default block size for standard sudokus
            new ArrayList<>(),   // constraints - empty for basic test types
            new ArrayList<>(),   // permutation properties
            new ArrayList<>(),   // helper list
            new ComplexityConstraintBuilder()
        );
    }

    @Override
    public SudokuType update(SudokuType t) {
        throw new UnsupportedOperationException("Not implemented for test repository");
    }

    @Override
    public void delete(int id) {
        throw new UnsupportedOperationException("Not implemented for test repository");
    }

    @Override
    public List<Integer> ids() {
        throw new UnsupportedOperationException("Not implemented for test repository");
    }
}

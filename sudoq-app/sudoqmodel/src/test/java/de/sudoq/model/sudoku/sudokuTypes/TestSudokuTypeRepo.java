package de.sudoq.model.sudoku.sudokuTypes;

import de.sudoq.model.persistence.IRepo;
import de.sudoq.model.sudoku.Constraint;
import de.sudoq.model.sudoku.Position;
import de.sudoq.model.sudoku.complexity.Complexity;
import de.sudoq.model.sudoku.complexity.ComplexityConstraint;
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

    private SudokuType createStandardType(SudokuTypes enumType, int width, int height, int numberOfSymbols,
            float allocationFactor) {
        // Create ComplexityConstraintBuilder with typical constraints for all
        // complexity levels
        ComplexityConstraintBuilder ccb = new ComplexityConstraintBuilder();

        // Add standard complexity constraints based on sudoku size
        int cellCount = width * height;
        if (cellCount == 81) { // 9x9
            ccb.getSpecimen().put(Complexity.easy,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.easy, 40, 500, 1500, 2));
            ccb.getSpecimen().put(Complexity.medium,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.medium, 32, 1500, 3500, 3));
            ccb.getSpecimen().put(Complexity.difficult,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.difficult, 28, 3500, 6000,
                            Integer.MAX_VALUE));
            ccb.getSpecimen().put(Complexity.infernal,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.infernal, 24, 6000, 10000,
                            Integer.MAX_VALUE));
        } else if (cellCount == 16) { // 4x4
            ccb.getSpecimen().put(Complexity.easy,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.easy, 8, 100, 300, 2));
            ccb.getSpecimen().put(Complexity.medium,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.medium, 6, 300, 600, 3));
            ccb.getSpecimen().put(Complexity.difficult,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.difficult, 5, 600, 1000,
                            Integer.MAX_VALUE));
        } else if (cellCount == 36) { // 6x6
            ccb.getSpecimen().put(Complexity.easy,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.easy, 18, 200, 500, 2));
            ccb.getSpecimen().put(Complexity.medium,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.medium, 14, 500, 1000, 3));
            ccb.getSpecimen().put(Complexity.difficult,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.difficult, 12, 1000, 2000,
                            Integer.MAX_VALUE));
        } else if (cellCount == 256) { // 16x16
            ccb.getSpecimen().put(Complexity.easy,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.easy, 120, 1000, 3000, 2));
            ccb.getSpecimen().put(Complexity.medium,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.medium, 100, 3000, 6000, 3));
            ccb.getSpecimen().put(Complexity.difficult,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.difficult, 80, 6000, 10000,
                            Integer.MAX_VALUE));
        } else {
            // Default constraints for other sizes
            int avgCells = (int) (cellCount * 0.4f);
            ccb.getSpecimen().put(Complexity.easy,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.easy, avgCells, 500, 1500, 2));
            ccb.getSpecimen().put(Complexity.medium,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.medium,
                            (int) (avgCells * 0.8f), 1500, 3500, 3));
            ccb.getSpecimen().put(Complexity.difficult,
                    new de.sudoq.model.sudoku.complexity.ComplexityConstraint(Complexity.difficult,
                            (int) (avgCells * 0.7f), 3500, 6000, Integer.MAX_VALUE));
        }

        // Build constraints for rows, columns, and blocks
        ArrayList<de.sudoq.model.sudoku.Constraint> constraints = buildStandardConstraints(width, height,
                numberOfSymbols);

        // Use the full constructor to properly initialize all fields including enumType
        return new SudokuType(
                enumType,
                numberOfSymbols,
                allocationFactor,
                Position.get(width, height),
                Position.get((int) Math.sqrt(numberOfSymbols), (int) Math.sqrt(numberOfSymbols)), // block size
                constraints,
                new ArrayList<>(), // permutation properties
                new ArrayList<>(), // helper list
                ccb);
    }

    /**
     * Build standard constraints (rows, columns, and blocks) for a standard sudoku
     */
    private ArrayList<de.sudoq.model.sudoku.Constraint> buildStandardConstraints(int width, int height,
            int numberOfSymbols) {
        ArrayList<de.sudoq.model.sudoku.Constraint> constraints = new ArrayList<>();
        int blockSize = (int) Math.sqrt(numberOfSymbols);

        de.sudoq.model.sudoku.UniqueConstraintBehavior behavior = new de.sudoq.model.sudoku.UniqueConstraintBehavior();

        // Add row constraints
        for (int row = 0; row < height; row++) {
            de.sudoq.model.sudoku.Constraint rowConstraint = new de.sudoq.model.sudoku.Constraint(behavior,
                    de.sudoq.model.sudoku.ConstraintType.LINE, "Row " + row);
            for (int col = 0; col < width; col++) {
                rowConstraint.addPosition(Position.get(col, row));
            }
            constraints.add(rowConstraint);
        }

        // Add column constraints
        for (int col = 0; col < width; col++) {
            de.sudoq.model.sudoku.Constraint colConstraint = new de.sudoq.model.sudoku.Constraint(behavior,
                    de.sudoq.model.sudoku.ConstraintType.LINE, "Column " + col);
            for (int row = 0; row < height; row++) {
                colConstraint.addPosition(Position.get(col, row));
            }
            constraints.add(colConstraint);
        }

        // Add block constraints (for standard sudokus only)
        if (blockSize * blockSize == numberOfSymbols && numberOfSymbols == width && width == height) {
            for (int blockRow = 0; blockRow < blockSize; blockRow++) {
                for (int blockCol = 0; blockCol < blockSize; blockCol++) {
                    de.sudoq.model.sudoku.Constraint blockConstraint = new de.sudoq.model.sudoku.Constraint(
                            behavior,
                            de.sudoq.model.sudoku.ConstraintType.BLOCK,
                            "Block " + (blockRow * blockSize + blockCol));
                    for (int row = 0; row < blockSize; row++) {
                        for (int col = 0; col < blockSize; col++) {
                            blockConstraint.addPosition(
                                    Position.get(
                                            blockCol * blockSize + col,
                                            blockRow * blockSize + row));
                        }
                    }
                    constraints.add(blockConstraint);
                }
            }
        }

        return constraints;
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

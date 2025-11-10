package de.sudoq.model.sudoku

import de.sudoq.model.sudoku.sudokuTypes.SudokuType
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

class SudokuSymbolCompletedTest {

    @Test
    fun `should return false when symbol not filled at all`() {
        // Create a standard 9x9 sudoku
        val sudokuType = createSimple9x9Type()
        val sudoku = Sudoku(sudokuType)
        
        // Symbol 0 (represents "1" in display) should not be completed
        sudoku.isSymbolCompleted(0).`should be false`()
    }

    @Test
    fun `should return false when symbol partially filled`() {
        val sudokuType = createSimple9x9Type()
        val sudoku = Sudoku(sudokuType)
        
        // Fill symbol 0 in 5 cells (less than required 9)
        var count = 0
        for (cell in sudoku.iterator()) {
            if (count >= 5) break
            if (cell.isEditable) {
                cell.setCurrentValue(0, false)
                count++
            }
        }
        
        sudoku.isSymbolCompleted(0).`should be false`()
    }

    @Test
    fun `should return false when symbol filled 9 times but incorrectly`() {
        val sudokuType = createSimple9x9Type()
        
        // Create a map with all cells having solution = 1 (symbol 1)
        val solutionMap = PositionMap<Int>(sudokuType.size!!)
        for (constraint in sudokuType) {
            for (position in constraint) {
                solutionMap.put(position, 1)  // All cells should have solution "1"
            }
        }
        
        val sudoku = Sudoku(sudokuType, solutionMap)
        
        // Fill symbol 0 (wrong solution) in 9 editable cells
        var count = 0
        for (cell in sudoku.iterator()) {
            if (count >= 9) break
            if (cell.isEditable) {
                cell.setCurrentValue(0, false)  // Wrong value
                count++
            }
        }
        
        // Should be false because values don't match solutions
        sudoku.isSymbolCompleted(0).`should be false`()
    }

    @Test
    fun `should return true when symbol filled correctly 9 times`() {
        val sudokuType = createSimple9x9Type()
        
        // Create a sudoku where first 9 cells have solution = 0
        val solutionMap = PositionMap<Int>(sudokuType.size!!)
        var cellsSet = 0
        for (constraint in sudokuType) {
            for (position in constraint) {
                if (cellsSet < 9) {
                    solutionMap.put(position, 0)  // Solution is 0
                    cellsSet++
                } else {
                    solutionMap.put(position, 1)  // Other cells have different solution
                }
            }
        }
        
        val sudoku = Sudoku(sudokuType, solutionMap)
        
        // Fill symbol 0 in the cells where solution is 0
        for (cell in sudoku.iterator()) {
            if (cell.solution == 0 && cell.isEditable) {
                cell.setCurrentValue(0, false)
            }
        }
        
        // Should be true because all 9 instances of symbol 0 are correctly placed
        sudoku.isSymbolCompleted(0).`should be true`()
    }

    @Test
    fun `should return false for invalid symbol values`() {
        val sudokuType = createSimple9x9Type()
        val sudoku = Sudoku(sudokuType)
        
        // Negative symbol
        sudoku.isSymbolCompleted(-1).`should be false`()
        
        // Symbol beyond range
        sudoku.isSymbolCompleted(9).`should be false`()
        sudoku.isSymbolCompleted(100).`should be false`()
    }

    @Test
    fun `should work with different sudoku sizes`() {
        // Test with 4x4 sudoku
        val sudokuType4x4 = createSimple4x4Type()
        
        // Create a sudoku where first 4 cells have solution = 0
        val solutionMap = PositionMap<Int>(sudokuType4x4.size!!)
        var cellsSet = 0
        for (constraint in sudokuType4x4) {
            for (position in constraint) {
                if (cellsSet < 4) {
                    solutionMap.put(position, 0)
                    cellsSet++
                } else {
                    solutionMap.put(position, 1)
                }
            }
        }
        
        val sudoku = Sudoku(sudokuType4x4, solutionMap)
        
        // Fill symbol 0 in the cells where solution is 0
        for (cell in sudoku.iterator()) {
            if (cell.solution == 0 && cell.isEditable) {
                cell.setCurrentValue(0, false)
            }
        }
        
        // Should be true because all 4 instances of symbol 0 are correctly placed
        sudoku.isSymbolCompleted(0).`should be true`()
    }
    
    private fun createSimple9x9Type(): SudokuType {
        // Create a minimal 9x9 sudoku type for testing
        val type = SudokuType(9, 9, 9)
        
        // Add row constraints
        for (row in 0..8) {
            val constraint = Constraint(UniqueConstraintBehavior(), ConstraintType.LINE)
            for (col in 0..8) {
                constraint.addPosition(Position.get(col, row))
            }
            type.constraints.add(constraint)
        }
        
        // Add column constraints
        for (col in 0..8) {
            val constraint = Constraint(UniqueConstraintBehavior(), ConstraintType.LINE)
            for (row in 0..8) {
                constraint.addPosition(Position.get(col, row))
            }
            type.constraints.add(constraint)
        }
        
        // Add 3x3 block constraints
        for (blockRow in 0..2) {
            for (blockCol in 0..2) {
                val constraint = Constraint(UniqueConstraintBehavior(), ConstraintType.BLOCK)
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
    
    private fun createSimple4x4Type(): SudokuType {
        // Create a minimal 4x4 sudoku type for testing
        val type = SudokuType(4, 4, 4)
        
        // Add row constraints
        for (row in 0..3) {
            val constraint = Constraint(UniqueConstraintBehavior(), ConstraintType.LINE)
            for (col in 0..3) {
                constraint.addPosition(Position.get(col, row))
            }
            type.constraints.add(constraint)
        }
        
        // Add column constraints
        for (col in 0..3) {
            val constraint = Constraint(UniqueConstraintBehavior(), ConstraintType.LINE)
            for (row in 0..3) {
                constraint.addPosition(Position.get(col, row))
            }
            type.constraints.add(constraint)
        }
        
        // Add 2x2 block constraints
        for (blockRow in 0..1) {
            for (blockCol in 0..1) {
                val constraint = Constraint(UniqueConstraintBehavior(), ConstraintType.BLOCK)
                for (row in 0..1) {
                    for (col in 0..1) {
                        constraint.addPosition(
                            Position.get(
                                blockCol * 2 + col,
                                blockRow * 2 + row
                            )
                        )
                    }
                }
                type.constraints.add(constraint)
            }
        }
        
        return type
    }
}

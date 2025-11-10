package de.sudoq.model.solverGenerator.utils

import de.sudoq.model.persistence.IRepo
import de.sudoq.model.sudoku.sudokuTypes.SudokuType
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import java.io.File

/**
 * Mock repository for SudokuType that loads from test resources.
 * This is a simplified version that doesn't depend on the app module.
 */
class SudokuTypeRepo2(private val sudokuTypesDir: File) : IRepo<SudokuType> {

    override fun create(): SudokuType {
        TODO("Not yet implemented")
    }

    override fun read(id: Int): SudokuType {
        // For now, return a mock SudokuType
        // In a real implementation, this would load from XML files
        TODO("Not yet implemented - use a proper mock or stub")
    }

    override fun update(t: SudokuType): SudokuType {
        TODO("Not yet implemented")
    }

    override fun delete(id: Int) {
        TODO("Not yet implemented")
    }

    override fun ids(): List<Int> {
        TODO("Not yet implemented")
    }
}
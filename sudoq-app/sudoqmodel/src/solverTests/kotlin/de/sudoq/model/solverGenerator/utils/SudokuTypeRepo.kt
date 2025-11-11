package de.sudoq.model.solverGenerator.utils

import de.sudoq.model.persistence.IRepo
import de.sudoq.model.sudoku.Position
import de.sudoq.model.sudoku.PositionMap
import de.sudoq.model.sudoku.UniqueConstraintBehavior
import de.sudoq.model.sudoku.sudokuTypes.*
import java.io.File

/**
 * Repository for SudokuType that provides basic SudokuTypes for tests.
 * This is a test-specific version that doesn't depend on the app module.
 */
class SudokuTypeRepo(private val sudokuTypesDir: File) : IRepo<SudokuType> {

    override fun create(): SudokuType {
        TODO("Not yet implemented")
    }

    override fun read(id: Int): SudokuType {
        val st: SudokuTypes = SudokuTypes.entries[id]
        return createBasicSudokuType(st)
    }

    private fun createBasicSudokuType(type: SudokuTypes): SudokuType {
        // Create a minimal SudokuType for testing purposes using the simple constructor
        return when (type) {
            SudokuTypes.standard9x9 -> SudokuType(9, 9, 9)
            SudokuTypes.standard16x16 -> SudokuType(16, 16, 16)
            SudokuTypes.standard4x4 -> SudokuType(4, 4, 4)
            SudokuTypes.standard6x6 -> SudokuType(6, 6, 6)
            SudokuTypes.samurai -> SudokuType(21, 21, 9)
            else -> SudokuType(9, 9, 9)
        }
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
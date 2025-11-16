package de.sudoq.model.solverGenerator

import de.sudoq.model.sudoku.SudokuBuilder
import de.sudoq.model.sudoku.Sudoku
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import de.sudoq.model.sudoku.sudokuTypes.TestSudokuTypeRepo
import org.junit.Test
import java.util.*

/**
 * Simple test to debug generation issues
 */
class SimpleGenerationTest : GeneratorCallback {
    
    private var generatedSudoku: Sudoku? = null
    private val lock = Object()

    override fun generationFinished(sudoku: Sudoku) {
        println("[SimpleTest] generationFinished called")
        synchronized(lock) {
            println("[SimpleTest] Setting sudoku and notifying")
            generatedSudoku = sudoku
            lock.notifyAll()
            println("[SimpleTest] Notified")
        }
    }

    override fun generationFinished(sudoku: Sudoku, sl: List<de.sudoq.model.solverGenerator.solution.Solution>) {
        generationFinished(sudoku)
    }

    @Test
    fun testSimpleGeneration() {
        println("=== Simple Generation Test (Original Algo) ===")
        
        val sudokuTypeRepo = TestSudokuTypeRepo()
        val random = Random(12345L)
        val sudoku = SudokuBuilder(SudokuTypes.standard4x4, sudokuTypeRepo).createSudoku()
        sudoku.complexity = Complexity.easy

        println("Creating GenerationAlgo...")
        val algo = GenerationAlgo(sudoku, this, random)
        
        println("Starting thread...")
        val thread = Thread(algo)
        thread.start()

        println("Waiting for result...")
        synchronized(lock) {
            val start = System.currentTimeMillis()
            lock.wait(10000) // 10 seconds
            val elapsed = System.currentTimeMillis() - start
            println("Wait completed after ${elapsed}ms")
        }

        if (generatedSudoku != null) {
            println("SUCCESS: Got sudoku with id=${generatedSudoku!!.id}")
        } else {
            println("FAILURE: No sudoku generated")
            throw IllegalStateException("Generation failed")
        }
    }

    @Test
    fun testImprovedGeneration() {
        println("=== Simple Generation Test (Improved Algo) ===")
        
        generatedSudoku = null
        val sudokuTypeRepo = TestSudokuTypeRepo()
        val random = Random(12345L)
        val sudoku = SudokuBuilder(SudokuTypes.standard4x4, sudokuTypeRepo).createSudoku()
        sudoku.complexity = Complexity.easy

        println("Creating ImprovedGenerationAlgo...")
        val algo = ImprovedGenerationAlgo(sudoku, this, random) { tag, msg ->
            println("[$tag] $msg")
        }
        
        println("Starting thread...")
        val thread = Thread(algo)
        thread.start()

        println("Waiting for result...")
        synchronized(lock) {
            val start = System.currentTimeMillis()
            lock.wait(10000) // 10 seconds
            val elapsed = System.currentTimeMillis() - start
            println("Wait completed after ${elapsed}ms")
        }

        if (generatedSudoku != null) {
            println("SUCCESS: Got sudoku with id=${generatedSudoku!!.id}")
        } else {
            println("FAILURE: No sudoku generated")
            throw IllegalStateException("Generation failed")
        }
    }
}

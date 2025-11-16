package de.sudoq.model.solverGenerator

import de.sudoq.model.solverGenerator.solver.ComplexityRelation
import de.sudoq.model.solverGenerator.solver.Solver
import de.sudoq.model.sudoku.Sudoku
import de.sudoq.model.sudoku.SudokuBuilder
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import de.sudoq.model.sudoku.sudokuTypes.TestSudokuTypeRepo
import org.junit.Test
import java.util.*

/**
 * Tool for generating high-difficulty Sudoku puzzles
 * Run this to test generation and analyze difficulty
 */
class SudokuGeneratorTool : GeneratorCallback {

    private var generatedSudoku: Sudoku? = null
    private val lock = Object()
    private val sudokuTypeRepo = TestSudokuTypeRepo()

    override fun generationFinished(sudoku: Sudoku) {
        synchronized(lock) {
            generatedSudoku = sudoku
            lock.notifyAll()
        }
    }

    override fun generationFinished(sudoku: Sudoku, sl: List<de.sudoq.model.solverGenerator.solution.Solution>) {
        generationFinished(sudoku)
    }

    /**
     * Test generation of extremely difficult 9x9 Sudoku puzzles
     * and analyze their actual difficulty
     */
    @Test
    fun testGenerateExtremelyHard9x9() {
        println("=== Testing Extremely Hard 9x9 Generation ===\n")
        
        val targetDifficulties = mapOf(
            Complexity.difficult to ComplexityRelation.CONSTRAINT_SATURATION,
            Complexity.infernal to ComplexityRelation.CONSTRAINT_SATURATION
        )
        
        for ((targetComplexity, minExpectedDifficulty) in targetDifficulties) {
            println("\n=== Target Complexity: $targetComplexity ===")
            var successCount = 0
            var attempts = 0
            val targetCount = 3
            val maxAttempts = 10
            
            val difficultyDistribution = mutableMapOf<ComplexityRelation, Int>()
            
            while (successCount < targetCount && attempts < maxAttempts) {
                attempts++
                val seed = System.currentTimeMillis() + attempts * 1000L
                
                try {
                    println("\n[Attempt $attempts] Generating with seed $seed...")
                    val sudoku = generate9x9(targetComplexity, seed)
                    
                    val actualDifficulty = validateAndGetDifficulty(sudoku)
                    difficultyDistribution[actualDifficulty] = 
                        difficultyDistribution.getOrDefault(actualDifficulty, 0) + 1
                    
                    val filledCells = countFilledCells(sudoku)
                    println("  Difficulty: $actualDifficulty")
                    println("  Filled cells: $filledCells/81")
                    
                    if (actualDifficulty.ordinal >= minExpectedDifficulty.ordinal) {
                        successCount++
                        println("  ✓ SUCCESS! (${successCount}/$targetCount)")
                        printSudoku(sudoku)
                    } else {
                        println("  ~ Generated but easier than expected")
                    }
                } catch (e: Exception) {
                    println("  ✗ Generation failed: ${e.message}")
                }
            }
            
            println("\n=== Summary for $targetComplexity ===")
            println("Success rate: $successCount/$attempts")
            println("Difficulty distribution:")
            difficultyDistribution.entries.sortedBy { it.key.ordinal }.forEach { (diff, count) ->
                println("  $diff: $count")
            }
        }
    }

    /**
     * Test generation of hard 16x16 puzzles
     */
    @Test
    fun testGenerateHard16x16() {
        println("=== Testing Hard 16x16 Generation ===\n")
        
        val attempts = 3
        val results = mutableListOf<Pair<ComplexityRelation, Int>>()
        
        repeat(attempts) { i ->
            val seed = System.currentTimeMillis() + i * 2000L
            println("\n[Attempt ${i+1}/$attempts] Seed: $seed")
            
            try {
                val sudoku = generate16x16(Complexity.infernal, seed)
                val difficulty = validateAndGetDifficulty(sudoku)
                val filledCells = countFilledCells(sudoku)
                
                results.add(difficulty to filledCells)
                println("  Difficulty: $difficulty")
                println("  Filled cells: $filledCells/256")
            } catch (e: Exception) {
                println("  ✗ Failed: ${e.message}")
            }
        }
        
        println("\n=== Summary ===")
        val byDifficulty = results.groupBy { it.first }
        byDifficulty.forEach { (diff, list) ->
            val avgFilled = list.map { it.second }.average()
            println("$diff: ${list.size} puzzles, avg filled: ${"%.1f".format(avgFilled)}")
        }
    }

    /**
     * Benchmark generation speed for different types
     */
    @Test
    fun benchmarkGenerationSpeed() {
        println("=== Generation Speed Benchmark ===\n")
        
        val testCases = listOf(
            Triple(SudokuTypes.standard4x4, Complexity.easy, 5),
            Triple(SudokuTypes.standard9x9, Complexity.easy, 3),
            Triple(SudokuTypes.standard9x9, Complexity.difficult, 2)
        )
        
        for ((type, complexity, count) in testCases) {
            println("\n=== $type - $complexity ===")
            val times = mutableListOf<Long>()
            
            repeat(count) { i ->
                val seed = System.currentTimeMillis() + i * 500L
                val startTime = System.currentTimeMillis()
                
                try {
                    generateSudoku(type, complexity, seed, timeoutMs = 60000)
                    val elapsed = System.currentTimeMillis() - startTime
                    times.add(elapsed)
                    println("  Run ${i+1}: ${elapsed}ms")
                } catch (e: Exception) {
                    println("  Run ${i+1}: FAILED (${e.message})")
                }
            }
            
            if (times.isNotEmpty()) {
                println("  Average: ${"%.0f".format(times.average())}ms")
                println("  Min: ${times.minOrNull()}ms, Max: ${times.maxOrNull()}ms")
            }
        }
    }

    private fun generate9x9(complexity: Complexity, seed: Long): Sudoku {
        return generateSudoku(SudokuTypes.standard9x9, complexity, seed, timeoutMs = 120000)
    }

    private fun generate16x16(complexity: Complexity, seed: Long): Sudoku {
        return generateSudoku(SudokuTypes.standard16x16, complexity, seed, timeoutMs = 180000)
    }

    private fun generateSudoku(
        type: SudokuTypes,
        complexity: Complexity,
        seed: Long,
        timeoutMs: Long = 60000
    ): Sudoku {
        generatedSudoku = null
        val random = Random(seed)
        val sudoku = SudokuBuilder(type, sudokuTypeRepo).createSudoku()
        sudoku.complexity = complexity

        val algo = GenerationAlgo(sudoku, this, random)
        
        val thread = Thread(algo).apply { 
            isDaemon = true
            start()
        }

        synchronized(lock) {
            lock.wait(timeoutMs)
        }

        if (generatedSudoku == null) {
            thread.interrupt()
            throw IllegalStateException("Generation timed out after ${timeoutMs}ms")
        }

        return generatedSudoku!!
    }

    private fun validateAndGetDifficulty(sudoku: Sudoku): ComplexityRelation {
        val solver = Solver(sudoku)
        return solver.validate(null)
    }

    private fun countFilledCells(sudoku: Sudoku): Int {
        return sudoku.cells?.values?.count { cell ->
            !cell.isNotSolved
        } ?: 0
    }

    private fun printSudoku(sudoku: Sudoku) {
        val size = sudoku.sudokuType!!.size!!.x
        println("\n  Sudoku Grid:")
        for (y in 0 until size) {
            print("  ")
            for (x in 0 until size) {
                val cell = sudoku.getCell(de.sudoq.model.sudoku.Position.get(x, y))
                if (cell != null && !cell.isNotSolved) {
                    print("${cell.currentValue + 1} ")
                } else {
                    print(". ")
                }
            }
            println()
        }
    }
}

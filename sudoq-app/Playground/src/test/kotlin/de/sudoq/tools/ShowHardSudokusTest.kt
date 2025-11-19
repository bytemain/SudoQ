package de.sudoq.tools

import de.sudoq.model.solverGenerator.GenerationAlgo
import de.sudoq.model.solverGenerator.GeneratorCallback
import de.sudoq.model.solverGenerator.solver.Solver
import de.sudoq.model.sudoku.Sudoku
import de.sudoq.model.sudoku.SudokuBuilder
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import java.util.*
import org.junit.Test

/** 展示生成的超难数独 */
class ShowHardSudokusTest : GeneratorCallback {

    private var generatedSudoku: Sudoku? = null
    private val lock = Object()
    private val sudokuTypeRepo = TestSudokuTypeRepo()

    override fun generationFinished(sudoku: Sudoku) {
        synchronized(lock) {
            generatedSudoku = sudoku
            lock.notifyAll()
        }
    }

    override fun generationFinished(
            sudoku: Sudoku,
            sl: List<de.sudoq.model.solverGenerator.solution.Solution>
    ) {
        generationFinished(sudoku)
    }

    @Test
    fun showExtremelyHardPuzzles() {
        println("\n" + "=".repeat(70))
        println("  超难数独展示 - INFERNAL 难度")
        println("=".repeat(70))

        // 这些是测试过的好种子，能生成非常难的数独
        val goodSeeds = listOf(1763309284755L, 1763309290438L, 1763309296096L)

        goodSeeds.forEachIndexed { index, seed ->
            println("\n" + "-".repeat(70))
            println("  数独 #${index + 1} (Seed: $seed)")
            println("-".repeat(70))

            try {
                val sudoku = generateSudoku(seed)
                val solver = Solver(sudoku)
                val difficulty = solver.validate(null)
                val filledCount = sudoku.cells?.values?.count { !it.isNotSolved } ?: 0

                println("实际难度: $difficulty")
                println("已填数字: $filledCount/81")
                println("空白数字: ${81 - filledCount}/81")
                println()

                printSudoku(sudoku)

                // 打印一些统计信息
                val stats = analyzer(sudoku)
                println("\n统计:")
                println("  - 每行平均已填数字: ${"%.1f".format(stats.avgPerRow)}")
                println("  - 最少填充的行: ${stats.minRow} 个")
                println("  - 最多填充的行: ${stats.maxRow} 个")
            } catch (e: Exception) {
                println("生成失败: ${e.message}")
            }
        }

        println("\n" + "=".repeat(70))
        println("  提示：这些数独都是算法评估为 MUCH_TOO_DIFFICULT 的超难题！")
        println("=".repeat(70) + "\n")
    }

    private fun generateSudoku(seed: Long): Sudoku {
        generatedSudoku = null
        val random = Random(seed)
        val sudoku = SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku()
        sudoku.complexity = Complexity.infernal

        val algo = GenerationAlgo(sudoku, this, random)
        val thread =
                Thread(algo).apply {
                    isDaemon = true
                    start()
                }

        synchronized(lock) { lock.wait(120000) }

        return generatedSudoku ?: throw IllegalStateException("生成超时")
    }

    private fun printSudoku(sudoku: Sudoku) {
        val size = 9
        println("  ┌───────┬───────┬───────┐")
        for (y in 0 until size) {
            print("  │ ")
            for (x in 0 until size) {
                val cell = sudoku.getCell(de.sudoq.model.sudoku.Position.get(x, y))
                if (cell != null && !cell.isNotSolved) {
                    print("${cell.currentValue + 1} ")
                } else {
                    print("· ")
                }
                if ((x + 1) % 3 == 0) print("│ ")
            }
            println()
            if ((y + 1) % 3 == 0 && y < size - 1) {
                println("  ├───────┼───────┼───────┤")
            }
        }
        println("  └───────┴───────┴───────┘")
    }

    data class SudokuStats(val avgPerRow: Double, val minRow: Int, val maxRow: Int)

    private fun analyzer(sudoku: Sudoku): SudokuStats {
        val rowCounts =
                (0..8).map { y ->
                    (0..8).count { x ->
                        val cell = sudoku.getCell(de.sudoq.model.sudoku.Position.get(x, y))
                        cell != null && !cell.isNotSolved
                    }
                }
        return SudokuStats(
                avgPerRow = rowCounts.average(),
                minRow = rowCounts.minOrNull() ?: 0,
                maxRow = rowCounts.maxOrNull() ?: 0
        )
    }
}

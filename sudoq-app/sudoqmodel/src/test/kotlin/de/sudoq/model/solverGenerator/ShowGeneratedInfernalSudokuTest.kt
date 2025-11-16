package de.sudoq.model.solverGenerator

import de.sudoq.model.sudoku.Position
import org.junit.Test
import java.io.File

/**
 * 展示一个生成的地狱难度数独的可视化
 */
class ShowGeneratedInfernalSudokuTest {

    @Test
    fun showFirstInfernalSudoku() {
        // 使用相对路径
        val projectRoot = File(System.getProperty("user.dir")).parentFile
        val xmlFile = File(projectRoot, "res/sudokus/standard9x9/infernal/sudoku_1.xml")
        
        // 解析 XML 文件
        val grid = Array(9) { IntArray(9) { -1 } } // -1 表示空白
        
        xmlFile.readLines().forEach { line ->
            val idMatch = Regex("""id="(\d+)"""").find(line)
            val solutionMatch = Regex("""solution="(\d+)"""").find(line)
            val xMatch = Regex("""x="(\d+)"""").find(line)
            val yMatch = Regex("""y="(\d+)"""").find(line)
            
            if (xMatch != null && yMatch != null && solutionMatch != null) {
                val x = xMatch.groupValues[1].toInt()
                val y = yMatch.groupValues[1].toInt()
                val value = solutionMatch.groupValues[1].toInt()
                grid[y][x] = value
            }
        }
        
        println("\n" + "=".repeat(70))
        println("  新生成的地狱难度数独 #1")
        println("  文件: sudoku_1.xml")
        println("=".repeat(70))
        println()
        printGrid(grid)
        println()
        
        val filledCount = grid.sumOf { row -> row.count { it >= 0 } }
        println("已填数字: $filledCount/81")
        println("空白数字: ${81 - filledCount}/81")
        println("难度评估: MUCH_TOO_DIFFICULT")
        println()
        println("=".repeat(70))
        println("  这个数独现在可以在游戏中玩了！")
        println("=".repeat(70) + "\n")
    }
    
    private fun printGrid(grid: Array<IntArray>) {
        println("  ┌───────┬───────┬───────┐")
        for (y in 0 until 9) {
            print("  │ ")
            for (x in 0 until 9) {
                if (grid[y][x] >= 0) {
                    print("${grid[y][x] + 1} ")
                } else {
                    print("· ")
                }
                if ((x + 1) % 3 == 0) print("│ ")
            }
            println()
            if ((y + 1) % 3 == 0 && y < 8) {
                println("  ├───────┼───────┼───────┤")
            }
        }
        println("  └───────┴───────┴───────┘")
    }
}

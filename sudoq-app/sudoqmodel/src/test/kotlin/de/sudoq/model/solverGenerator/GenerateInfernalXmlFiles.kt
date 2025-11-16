package de.sudoq.model.solverGenerator

import de.sudoq.model.solverGenerator.solver.Solver
import de.sudoq.model.sudoku.Sudoku
import de.sudoq.model.sudoku.SudokuBuilder
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import de.sudoq.model.sudoku.sudokuTypes.TestSudokuTypeRepo
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.util.*

/**
 * Generate XML files for infernal difficulty sudokus
 * This will replace the existing infernal sudokus with genuinely harder ones
 */
class GenerateInfernalXmlFiles : GeneratorCallback {

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

    @Test
    fun generateAndSaveInfernalSudokus() {
        // 使用相对路径，从项目根目录查找
        val projectRoot = File(System.getProperty("user.dir")).parentFile
        val outputDir = File(projectRoot, "res/sudokus/standard9x9/infernal")
        
        if (!outputDir.exists()) {
            println("Error: Output directory does not exist: ${outputDir.absolutePath}")
            return
        }

        println("\n" + "=".repeat(70))
        println("  生成 10 个超难数独 XML 文件")
        println("=".repeat(70))
        
        var successCount = 0
        var attemptCount = 0
        val maxAttempts = 30 // 允许一些失败重试
        
        for (sudokuId in 1..10) {
            var generated = false
            
            while (!generated && attemptCount < maxAttempts) {
                attemptCount++
                val seed = System.currentTimeMillis() + attemptCount
                
                try {
                    println("\n尝试生成数独 #$sudokuId (尝试 #$attemptCount, seed: $seed)...")
                    val sudoku = generateSudoku(seed)
                    
                    // 验证难度
                    val solver = Solver(sudoku)
                    val difficulty = solver.validate(null)
                    val filledCount = sudoku.cells?.values?.count { !it.isNotSolved } ?: 0
                    
                    println("  难度评估: $difficulty")
                    println("  已填数字: $filledCount/81")
                    
                    // 只接受 MUCH_TOO_DIFFICULT 或 TOO_DIFFICULT 的数独
                    if (difficulty.toString().contains("TOO_DIFFICULT")) {
                        val xmlFile = File(outputDir, "sudoku_$sudokuId.xml")
                        saveToXml(sudoku, xmlFile, sudokuId)
                        println("  ✓ 成功保存到: ${xmlFile.name}")
                        successCount++
                        generated = true
                    } else {
                        println("  × 难度不够，重试...")
                    }
                    
                } catch (e: Exception) {
                    println("  × 生成失败: ${e.message}")
                }
            }
            
            if (!generated) {
                println("  ⚠ 警告：数独 #$sudokuId 在 $maxAttempts 次尝试后仍未成功生成")
            }
        }
        
        println("\n" + "=".repeat(70))
        println("  完成！成功生成 $successCount/10 个数独")
        println("  总尝试次数: $attemptCount")
        println("=".repeat(70) + "\n")
    }

    private fun generateSudoku(seed: Long): Sudoku {
        generatedSudoku = null
        val random = Random(seed)
        val sudoku = SudokuBuilder(SudokuTypes.standard9x9, sudokuTypeRepo).createSudoku()
        sudoku.complexity = Complexity.infernal

        val algo = GenerationAlgo(sudoku, this, random)
        val thread = Thread(algo).apply { 
            isDaemon = true
            start()
        }

        synchronized(lock) {
            lock.wait(120000) // 2分钟超时
        }

        return generatedSudoku ?: throw IllegalStateException("生成超时")
    }

    private fun saveToXml(sudoku: Sudoku, file: File, sudokuId: Int) {
        val writer = FileWriter(file)
        
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
        writer.write("<!DOCTYPE sudoku SYSTEM \"./sudoku.dtd\">\n")
        
        // 写入 sudoku 标签
        writer.write("<sudoku transformCount=\"0\" type=\"0\" complexity=\"3\" id=\"$sudokuId\">\n")
        
        // 写入所有已填充的 cells
        sudoku.cells?.entries?.sortedBy { it.value.id }?.forEach { (position, cell) ->
            if (!cell.isNotSolved) {
                writer.write("<fieldmap id=\"${cell.id}\" editable=\"true\" solution=\"${cell.solution}\">\n")
                writer.write("<position x=\"${position.x}\" y=\"${position.y}\"></position>\n")
                writer.write("</fieldmap>\n")
            }
        }
        
        writer.write("</sudoku>\n")
        writer.flush()
        writer.close()
    }
}

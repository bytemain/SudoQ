package de.sudoq.persistence.sudoku

import de.sudoq.model.sudoku.sudokuTypes.TestSudokuTypeRepo
import de.sudoq.persistence.XmlHelper
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * 验证新生成的地狱难度数独 XML 文件能否正常加载
 */
class VerifyInfernalSudokusTest {

    @Test
    fun testLoadAllInfernalSudokus() {
        // 使用相对路径
        val projectRoot = File(System.getProperty("user.dir")).parentFile
        val infernalDir = File(projectRoot, "res/sudokus/standard9x9/infernal")
        val sudokuTypeRepo = TestSudokuTypeRepo()
        val xmlHelper = XmlHelper()
        
        println("\n" + "=".repeat(70))
        println("  验证新生成的地狱难度数独")
        println("=".repeat(70))
        
        for (i in 1..10) {
            val xmlFile = File(infernalDir, "sudoku_$i.xml")
            assertTrue("文件应该存在: ${xmlFile.name}", xmlFile.exists())
            
            try {
                // 加载 XML
                val xmlTree = xmlHelper.loadXml(xmlFile)
                assertNotNull("XML 树不应为空: ${xmlFile.name}", xmlTree)
                
                // 转换为 Sudoku 对象
                val sudokuBE = SudokuBE()
                sudokuBE.fillFromXml(xmlTree!!, sudokuTypeRepo)
                
                // 验证基本属性
                assertEquals("ID 应该匹配", i, sudokuBE.id)
                assertEquals("Transform count 应该为 0", 0, sudokuBE.transformCount)
                assertEquals("复杂度应该是 infernal", 3, sudokuBE.complexity!!.ordinal)
                assertNotNull("Cells 不应为空", sudokuBE.cells)
                
                // 统计已填充的 cells
                val filledCells = sudokuBE.cells!!.values.count { !it.isNotSolved }
                
                println("✓ sudoku_$i.xml: $filledCells 个已填数字，加载成功")
                
                // 验证填充数量合理（20-35个之间，这是超难数独的典型范围）
                assertTrue("已填数字应该在合理范围内 (20-35): $filledCells", 
                    filledCells in 20..35)
                
            } catch (e: Exception) {
                fail("加载 ${xmlFile.name} 时失败: ${e.message}")
                e.printStackTrace()
            }
        }
        
        println("=".repeat(70))
        println("  所有 10 个数独验证通过！")
        println("=".repeat(70) + "\n")
    }
}

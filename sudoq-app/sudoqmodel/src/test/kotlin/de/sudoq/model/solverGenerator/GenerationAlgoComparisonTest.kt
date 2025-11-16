package de.sudoq.model.solverGenerator

import de.sudoq.model.persistence.IRepo
import de.sudoq.model.sudoku.Sudoku
import de.sudoq.model.sudoku.SudokuBuilder
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.SudokuType
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import de.sudoq.model.solverGenerator.solution.Solution
import de.sudoq.model.solverGenerator.solver.Solver
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * 对比测试：原始生成算法 vs 改进生成算法
 * 
 * 测试指标：
 * 1. 生成速度
 * 2. 难度准确性
 * 3. 唯一解保证
 * 4. 生成稳定性
 */
class GenerationAlgoComparisonTest : GeneratorCallback {

    private var generatedSudoku: Sudoku? = null
    private val lock = Object()
    private lateinit var sudokuTypeRepo: IRepo<SudokuType>

    @Before
    fun setup() {
        generatedSudoku = null
        // 创建一个简单的 mock SudokuTypeRepo
        sudokuTypeRepo = object : IRepo<SudokuType> {
            override fun create(): SudokuType = throw NotImplementedError()
            override fun read(id: Int): SudokuType = throw NotImplementedError()
            override fun update(obj: SudokuType): SudokuType = obj
            override fun delete(id: Int) {}
            override fun ids(): List<Int> = emptyList()
        }
    }

    override fun generationFinished(sudoku: Sudoku) {
        synchronized(lock) {
            generatedSudoku = sudoku
            lock.notifyAll()
        }
    }

    override fun generationFinished(sudoku: Sudoku, sl: List<Solution>) {
        generationFinished(sudoku)
    }

    /**
     * 测试生成速度对比
     */
    @Test
    fun testGenerationSpeed() {
        val seed = 12345L
        val testCases = listOf(
            SudokuTypes.standard4x4 to Complexity.easy,
            SudokuTypes.standard9x9 to Complexity.medium,
            SudokuTypes.standard9x9 to Complexity.difficult
        )

        println("=== 生成速度对比测试 ===\n")

        for ((type, complexity) in testCases) {
            println("测试配置: $type - $complexity")

            // 测试原始算法
            val originalTime = measureAlgorithm(
                type, complexity, seed, 
                useImproved = false
            )

            // 测试改进算法
            val improvedTime = measureAlgorithm(
                type, complexity, seed, 
                useImproved = true
            )

            val speedup = originalTime.toDouble() / improvedTime
            println("  原始算法: ${originalTime}ms")
            println("  改进算法: ${improvedTime}ms")
            println("  提速倍数: ${"%.2f".format(speedup)}x")
            println()
        }
    }

    /**
     * 测试难度准确性
     */
    @Test
    fun testDifficultyAccuracy() {
        val seed = 54321L
        val iterations = 5

        println("=== 难度准确性测试 ===\n")

        for (complexity in Complexity.values()) {
            if (complexity == Complexity.arbitrary) continue

            println("目标难度: $complexity")
            
            var originalMatches = 0
            var improvedMatches = 0

            repeat(iterations) {
                // 测试原始算法
                val originalSudoku = generateWithAlgorithm(
                    SudokuTypes.standard9x9, complexity, seed,
                    useImproved = false
                )
                if (validateDifficulty(originalSudoku, complexity)) {
                    originalMatches++
                }

                // 测试改进算法  
                val improvedSudoku = generateWithAlgorithm(
                    SudokuTypes.standard9x9, complexity, seed,
                    useImproved = true
                )
                if (validateDifficulty(improvedSudoku, complexity)) {
                    improvedMatches++
                }
            }

            println("  原始算法准确率: ${originalMatches}/$iterations = ${"%.0f".format(originalMatches * 100.0 / iterations)}%")
            println("  改进算法准确率: ${improvedMatches}/$iterations = ${"%.0f".format(improvedMatches * 100.0 / iterations)}%")
            println()
        }
    }

    /**
     * 测试唯一解保证
     */
    @Test
    fun testUniquenessGuarantee() {
        val iterations = 10
        
        println("=== 唯一解保证测试 ===\n")

        var originalUnique = 0
        var improvedUnique = 0

        repeat(iterations) {
            val seed = System.currentTimeMillis() + it

            // 原始算法
            val originalSudoku = generateWithAlgorithm(
                SudokuTypes.standard9x9, Complexity.medium, seed,
                useImproved = false
            )
            if (hasUniqueSolution(originalSudoku)) {
                originalUnique++
            }

            // 改进算法
            val improvedSudoku = generateWithAlgorithm(
                SudokuTypes.standard9x9, Complexity.medium, seed,
                useImproved = true
            )
            if (hasUniqueSolution(improvedSudoku)) {
                improvedUnique++
            }
        }

        println("原始算法唯一解率: ${originalUnique}/$iterations = ${"%.0f".format(originalUnique * 100.0 / iterations)}%")
        println("改进算法唯一解率: ${improvedUnique}/$iterations = ${"%.0f".format(improvedUnique * 100.0 / iterations)}%")
    }

    // ========== 辅助方法 ==========

    private fun measureAlgorithm(
        type: SudokuTypes,
        complexity: Complexity,
        seed: Long,
        useImproved: Boolean
    ): Long {
        return measureTimeMillis {
            generateWithAlgorithm(type, complexity, seed, useImproved)
        }
    }

    private fun generateWithAlgorithm(
        type: SudokuTypes,
        complexity: Complexity,
        seed: Long,
        useImproved: Boolean
    ): Sudoku {
        generatedSudoku = null
        val random = Random(seed)
        val sudoku = SudokuBuilder(type, sudokuTypeRepo).createSudoku()
        sudoku.complexity = complexity

        if (useImproved) {
            ImprovedGenerationAlgo(sudoku, this, random, logger = null).run()
        } else {
            GenerationAlgo(sudoku, this, random).run()
        }

        synchronized(lock) {
            lock.wait(60000) // 最多等待60秒
        }

        return generatedSudoku ?: throw IllegalStateException("生成失败")
    }

    private fun validateDifficulty(sudoku: Sudoku, expectedComplexity: Complexity): Boolean {
        val solver = Solver(sudoku)
        val relation = solver.validate(null)
        
        // 根据期望难度判断是否匹配
        return when (expectedComplexity) {
            Complexity.easy -> relation.ordinal <= 2
            Complexity.medium -> relation.ordinal in 2..3
            Complexity.difficult, Complexity.infernal -> relation.ordinal >= 3
            else -> true
        }
    }

    private fun hasUniqueSolution(sudoku: Sudoku): Boolean {
        val fs = de.sudoq.model.solverGenerator.FastSolver.FastSolverFactory.getSolver(sudoku)
        return !fs.isAmbiguous
    }
}

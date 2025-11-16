/*
 * SudoQ is a Sudoku-App for Android Devices with Version 2.2 at least.
 * Copyright (C) 2025 - Improved Generation Algorithm
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 */
package de.sudoq.model.solverGenerator

import de.sudoq.model.solverGenerator.FastSolver.FastSolverFactory
import de.sudoq.model.solverGenerator.solver.ComplexityRelation
import de.sudoq.model.solverGenerator.solver.Solver
import de.sudoq.model.sudoku.*
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.complexity.ComplexityConstraint
import de.sudoq.model.sudoku.sudokuTypes.SudokuType
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

/**
 * 改进的数独生成算法，提供：
 * 1. 更高效的生成速度
 * 2. 更精确的难度控制
 * 3. 更好的唯一解保证
 * 4. 渐进式生成策略
 *
 * @property sudoku 要生成的数独对象
 * @property callbackObject 生成完成后的回调对象
 * @property random 随机数生成器
 * @property logger 日志记录函数
 */
class ImprovedGenerationAlgo(
    private val sudoku: Sudoku,
    private val callbackObject: GeneratorCallback,
    private val random: Random = Random(),
    private val logger: ((String, String) -> Unit)? = null
) : Runnable {

    private val solver: Solver = Solver(sudoku)
    private val allPositions: List<Position> = getAllPositions(sudoku)
    private val complexityConstraint: ComplexityConstraint? =
        sudoku.sudokuType!!.buildComplexityConstraint(sudoku.complexity)

    // 统计信息，用于调试和优化
    private var generationAttempts = 0
    private var removalAttempts = 0

    override fun run() {
        try {
            logger?.invoke(TAG, "Started - Type: ${sudoku.sudokuType?.enumType}, Complexity: ${sudoku.complexity}")
            val startTime = System.currentTimeMillis()
            
            // 阶段 1: 生成完整的数独解
            val fullSolution = generateCompleteSolution()
            
            // 阶段 2: 渐进式移除数字，直到达到目标难度
            val puzzle = createPuzzleFromSolution(fullSolution)
            
            // 阶段 3: 构建最终结果并回调
            val result = buildFinalSudoku(fullSolution, puzzle)
            result.complexity = sudoku.complexity
            
            val duration = System.currentTimeMillis() - startTime
            logger?.invoke(TAG, "Completed successfully in ${duration}ms - Attempts: $generationAttempts, Removals: $removalAttempts")
            
            callbackObject.generationFinished(result)
        } catch (e: Exception) {
            // 如果改进算法失败，回退到原始算法
            logger?.invoke(TAG, "Failed, falling back to GenerationAlgo - ${e.message}")
            GenerationAlgo(sudoku, callbackObject, random).run()
        }
    }

    /**
     * 阶段 1: 使用优化的回溯算法生成完整解
     * 使用启发式选择策略：优先填充可选数字最少的格子（MRV - Minimum Remaining Values）
     */
    private fun generateCompleteSolution(): Sudoku {
        generationAttempts = 0
        val builder = SudokuBuilder(sudoku.sudokuType)
        
        // 使用约束传播快速生成解
        if (fillWithConstraintPropagation(builder, 0)) {
            return builder.createSudoku()
        }
        
        throw IllegalStateException("无法生成完整解")
    }

    /**
     * 使用约束传播和启发式搜索填充数独
     * @param builder 数独构建器
     * @param depth 递归深度
     * @return 是否成功填充
     */
    private fun fillWithConstraintPropagation(builder: SudokuBuilder, depth: Int): Boolean {
        generationAttempts++
        
        // 防止无限递归
        if (generationAttempts > 100000) return false
        
        // 找到候选数最少的空格（MRV启发式）
        val bestPosition = findBestPosition(builder) ?: return true // 所有格子都已填充
        
        // 获取该位置的候选数字
        val candidates = getCandidates(builder, bestPosition)
        
        // 随机打乱候选数字顺序，增加生成多样性
        val shuffledCandidates = candidates.shuffled(random)
        
        // 尝试每个候选数字
        for (value in shuffledCandidates) {
            // 保存当前状态
            val backup = saveState(builder)
            
            // 尝试填入数字
            builder.addSolution(bestPosition, value)
            
            // 如果有效，继续递归
            if (isValid(builder, bestPosition, value)) {
                if (fillWithConstraintPropagation(builder, depth + 1)) {
                    return true
                }
            }
            
            // 回溯
            restoreState(builder, backup)
        }
        
        return false
    }

    /**
     * 使用 MRV 启发式找到最佳填充位置
     * 选择候选数字最少的空格
     */
    private fun findBestPosition(builder: SudokuBuilder): Position? {
        var bestPos: Position? = null
        var minCandidates = Int.MAX_VALUE
        
        for (pos in allPositions) {
            if (!builder.hasValueAt(pos)) {
                val candidates = getCandidates(builder, pos)
                if (candidates.size < minCandidates) {
                    minCandidates = candidates.size
                    bestPos = pos
                    
                    // 如果找到只有一个候选的格子，立即返回（单例技巧）
                    if (minCandidates == 1) break
                }
            }
        }
        
        return bestPos
    }

    /**
     * 获取某个位置的所有有效候选数字
     */
    private fun getCandidates(builder: SudokuBuilder, pos: Position): List<Int> {
        val candidates = mutableListOf<Int>()
        val numberOfSymbols = sudoku.sudokuType!!.numberOfSymbols
        
        for (value in 0 until numberOfSymbols) {
            if (isValid(builder, pos, value)) {
                candidates.add(value)
            }
        }
        
        return candidates
    }

    /**
     * 检查在指定位置填入指定值是否有效
     */
    private fun isValid(builder: SudokuBuilder, pos: Position, value: Int): Boolean {
        // 临时填入值进行检查
        builder.addSolution(pos, value)
        
        // 检查所有约束
        val valid = sudoku.sudokuType!!.all { constraint ->
            if (constraint.includes(pos)) {
                // 只检查包含该位置的约束
                constraint.isSaturated(builder.createSudoku())
            } else {
                true
            }
        }
        
        // 移除临时填入的值
        if (!valid) {
            builder.removeSolution(pos)
        }
        
        return valid
    }

    /**
     * 阶段 2: 从完整解创建谜题
     * 使用改进的渐进式移除策略
     */
    private fun createPuzzleFromSolution(solution: Sudoku): Set<Position> {
        removalAttempts = 0
        val givenPositions = allPositions.toMutableSet()
        val targetGivenCount = calculateTargetGivenCount()
        
        // 创建移除顺序：优先移除对称位置的数字（提高美观性）
        val removalOrder = createSymmetricRemovalOrder()
        
        for (pos in removalOrder) {
            if (givenPositions.size <= targetGivenCount) break
            
            removalAttempts++
            
            // 尝试移除该位置
            givenPositions.remove(pos)
            
            // 检查是否仍然有唯一解且难度合适
            if (!validatePuzzle(solution, givenPositions)) {
                // 无效，恢复该位置
                givenPositions.add(pos)
            }
        }
        
        return givenPositions
    }

    /**
     * 计算目标给定数字数量
     */
    private fun calculateTargetGivenCount(): Int {
        val allocationFactor = sudoku.sudokuType!!.getStandardAllocationFactor()
        val totalCells = allPositions.size
        val byType = (totalCells * allocationFactor).toInt()
        val byComplexity = complexityConstraint?.averageCells ?: byType
        
        return min(byType, byComplexity)
    }

    /**
     * 创建对称的移除顺序
     * 优先从中心和对角线开始移除
     */
    private fun createSymmetricRemovalOrder(): List<Position> {
        val order = mutableListOf<Position>()
        val remaining = allPositions.toMutableSet()
        val size = sudoku.sudokuType!!.size!!
        
        // 按对称对添加位置
        while (remaining.isNotEmpty()) {
            // 使用 java.util.Random 的方式获取随机元素
            val pos = remaining.elementAt(random.nextInt(remaining.size))
            order.add(pos)
            remaining.remove(pos)
            
            // 尝试添加对称位置
            val symmetric = Position[size.x - 1 - pos.x, size.y - 1 - pos.y]
            if (symmetric in remaining) {
                order.add(symmetric)
                remaining.remove(symmetric)
            }
        }
        
        return order
    }

    /**
     * 验证谜题的有效性（唯一解 + 难度匹配）
     */
    private fun validatePuzzle(solution: Sudoku, givenPositions: Set<Position>): Boolean {
        // 构建当前谜题
        val builder = SudokuBuilder(sudoku.sudokuType)
        for (pos in givenPositions) {
            val value = solution.getCell(pos)!!.solution
            builder.addSolution(pos, value)
            builder.setFixed(pos)
        }
        val puzzle = builder.createSudoku()
        
        // 快速检查唯一解
        val fs = FastSolverFactory.getSolver(puzzle)
        if (fs.isAmbiguous) return false
        
        // 检查难度是否合适
        return checkComplexity(puzzle)
    }

    /**
     * 检查难度是否符合要求
     */
    private fun checkComplexity(puzzle: Sudoku): Boolean {
        val solver = Solver(puzzle)
        val relation = solver.validate(null)
        
        return when (sudoku.complexity) {
            Complexity.easy -> relation in listOf(
                ComplexityRelation.CONSTRAINT_SATURATION,
                ComplexityRelation.TOO_EASY
            )
            Complexity.medium -> relation == ComplexityRelation.CONSTRAINT_SATURATION
            Complexity.difficult, Complexity.infernal -> relation in listOf(
                ComplexityRelation.CONSTRAINT_SATURATION,
                ComplexityRelation.TOO_DIFFICULT
            )
            else -> relation == ComplexityRelation.CONSTRAINT_SATURATION
        }
    }

    /**
     * 阶段 3: 构建最终数独对象
     */
    private fun buildFinalSudoku(solution: Sudoku, givenPositions: Set<Position>): Sudoku {
        val builder = SudokuBuilder(sudoku.sudokuType)
        
        for (pos in allPositions) {
            val value = solution.getCell(pos)!!.solution
            builder.addSolution(pos, value)
            
            if (pos in givenPositions) {
                builder.setFixed(pos)
            }
        }
        
        return builder.createSudoku()
    }

    // ========== 辅助方法 ==========

    private fun saveState(builder: SudokuBuilder): Map<Position, Int> {
        val state = mutableMapOf<Position, Int>()
        for (pos in allPositions) {
            if (builder.hasValueAt(pos)) {
                state[pos] = builder.getValueAt(pos)
            }
        }
        return state
    }

    private fun restoreState(builder: SudokuBuilder, state: Map<Position, Int>) {
        // 清空当前状态
        for (pos in allPositions) {
            builder.removeSolution(pos)
        }
        
        // 恢复保存的状态
        for ((pos, value) in state) {
            builder.addSolution(pos, value)
        }
    }

    private fun SudokuBuilder.hasValueAt(pos: Position): Boolean {
        // 需要在 SudokuBuilder 中添加此方法
        // 临时实现：尝试创建 Sudoku 并检查
        return try {
            val tempSudoku = this.createSudoku()
            !tempSudoku.getCell(pos)!!.isNotSolved
        } catch (e: Exception) {
            false
        }
    }

    private fun SudokuBuilder.getValueAt(pos: Position): Int {
        val tempSudoku = this.createSudoku()
        return tempSudoku.getCell(pos)!!.currentValue
    }

    private fun SudokuBuilder.removeSolution(pos: Position) {
        // 需要在 SudokuBuilder 中添加此方法
        // 临时实现：设置为空值
        this.addSolution(pos, Cell.EMPTYVAL)
    }

    companion object {
        private const val TAG = "ImprovedGenerationAlgo"
        
        private fun getAllPositions(sudoku: Sudoku): List<Position> {
            val positions = mutableListOf<Position>()
            val size = sudoku.sudokuType!!.size!!
            
            for (x in 0 until size.x) {
                for (y in 0 until size.y) {
                    val pos = Position[x, y]
                    if (sudoku.getCell(pos) != null) {
                        positions.add(pos)
                    }
                }
            }
            
            return positions
        }
    }
}

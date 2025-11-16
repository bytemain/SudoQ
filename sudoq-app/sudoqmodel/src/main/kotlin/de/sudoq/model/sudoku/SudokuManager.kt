/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.sudoku

import de.sudoq.model.persistence.IRepo
import de.sudoq.model.persistence.xml.sudoku.ISudokuRepoProvider
import de.sudoq.model.solverGenerator.Generator
import de.sudoq.model.solverGenerator.GeneratorCallback
import de.sudoq.model.solverGenerator.solution.Solution
import de.sudoq.model.solverGenerator.transformations.Transformer
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.SudokuType
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes

/** Responsible for maintaining existing Sudokus.
 * Implemented as Singleton. */
open class SudokuManager(
    val sudokuTypeRepo: IRepo<SudokuType>,
    private val sudokuRepoProvider: ISudokuRepoProvider,
    private val logger: ((String, String) -> Unit)? = null
) : GeneratorCallback {

    private val generator = Generator(sudokuTypeRepo, logger)

    /** holds the old sudoku while the new sudoku is being generated. */
    private var used: Sudoku? = null

    /**
     * Callback for the Generator
     */
    override fun generationFinished(sudoku: Sudoku) {
        val sudokuRepo = sudokuRepoProvider.getRepo(sudoku)
        val i = sudokuRepo.create().id
        val sudokuWithId = Sudoku(i,
                                  sudoku.transformCount,
                                  sudoku.sudokuType!!,
                                  sudoku.complexity!!,
                                  sudoku.cells!!)
        sudokuRepo.update(sudokuWithId)
        used?.also { sudokuRepo.delete(it.id) }
    }

    override fun generationFinished(sudoku: Sudoku, sl: List<Solution>) {
        //todo is it ever used, if not safely remove/throw not implemented
        generationFinished(sudoku)
    }

    /**
     * Marks a Sudoku as used.
     * If possible it will be transformed, otherwise a new one is generated.
     *
     * @param sudoku the used Sudoku
     * @param useNewAlgorithm whether to use ImprovedGenerationAlgo for new generation
     * @param forceGeneration force generation instead of transformation (used when explicitly requesting new algorithm)
     */
    fun usedSudoku(sudoku: Sudoku, useNewAlgorithm: Boolean = false, forceGeneration: Boolean = false) {
        if (sudoku.transformCount >= 10 || (forceGeneration && useNewAlgorithm)) {
            used = sudoku
            logger?.invoke("SudokuManager", "Generating new sudoku - Type: ${sudoku.sudokuType?.enumType}, Complexity: ${sudoku.complexity}, UseNewAlgo: $useNewAlgorithm")
            generator.generate(sudoku.sudokuType!!.enumType, sudoku.complexity, this, useNewAlgorithm)
        } else {
            logger?.invoke("SudokuManager", "Transforming existing sudoku - Type: ${sudoku.sudokuType?.enumType}, TransformCount: ${sudoku.transformCount}")
            Transformer.transform(sudoku)
            val sudokuRepo = sudokuRepoProvider.getRepo(sudoku)
            sudokuRepo.update(sudoku)
        }
    }

    /**
     * Generate a completely new Sudoku using the specified algorithm.
     * This bypasses the template system and generates from scratch.
     *
     * @param type The type of Sudoku to generate
     * @param complexity The complexity level
     * @param useNewAlgorithm Whether to use ImprovedGenerationAlgo
     */
    fun generateNewSudoku(type: SudokuTypes, complexity: Complexity, useNewAlgorithm: Boolean) {
        generator.generate(type, complexity, this, useNewAlgorithm)
    }

    /**
     * Return a new [Sudoku] of the specified [type][SudokuTypes] and [Complexity]
     *
     * @param t [type][SudokuTypes] of the [Sudoku]
     * @param c [Complexity] of the [Sudoku]
     * @return the new [Sudoku]
     * @throws IllegalStateException if no sudokus are available for the given type and complexity
     */
    fun getNewSudoku(t: SudokuTypes?, c: Complexity?): Sudoku {
        val sudokuRepo = sudokuRepoProvider.getRepo(t!!, c!!)
        val ids = sudokuRepo.ids()
        if (ids.isEmpty()) {
            throw IllegalStateException("No sudokus available for type $t and complexity $c. Please ensure assets are properly copied.")
        }
        val randomId = ids.random()
        return sudokuRepo.read(randomId)
    }


    companion object {

        /**
         * Creates an empty sudoku that has to be filled.
         * @return empty Sudoku
         */
        @Deprecated("DO NOT USE THIS METHOD (if you are not from us)")
        internal val emptySudokuToFillWithXml: Sudoku
            get() = Sudoku()
    }
}

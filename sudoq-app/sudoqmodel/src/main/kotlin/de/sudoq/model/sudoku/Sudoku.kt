/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.sudoku

import de.sudoq.model.ModelChangeListener
import de.sudoq.model.ObservableModelImpl
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.SudokuType
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * This class represents a Sudoku with mit seinem Typ, seinen Feldern und seinem Schwierigkeitsgrad.
 */
open class Sudoku : ObservableModelImpl<Cell>, Iterable<Cell>, ModelChangeListener<Cell> {

    /** An ID uniquely identifying the Sudoku */
    var id: Int = 0

    /** Counts how often the Sudoku was already transformed */
    var transformCount = 0
        private set

    /** A map that assigns a field to each position of the Sudoku */
    @JvmField var cells: HashMap<Position, Cell>? = null

    private var cellPositions: MutableMap<Int, Position>? = null

    /** The Type of the Sudoku */
    var sudokuType: SudokuType? = null
        private set

    /** The Complexity of this Sudoku */
    var complexity: Complexity? = null

    /**
     * All Cells are set as editable.
     *
     * @param type Type of the Sudoku
     * @param map A Map from Positions to solution values. Values in pre-filled Cells are negated.
     * (actually bitwise negated)
     * @param setValues A Map from Position to whether the value is pre-filled.
     */
    @JvmOverloads
    constructor(
            type: SudokuType,
            map: PositionMap<Int>? = PositionMap((type.size)!!),
            setValues: PositionMap<Boolean>? = PositionMap((type.size)!!)
    ) {
        var cellIdCounter = 1
        cellPositions = HashMap()
        sudokuType = type
        cells = HashMap()

        // iterate over the constraints of the type and create the fields
        for (constraint in type) {
            for (position in constraint) {
                if (!cells!!.containsKey(position)) {
                    var f: Cell
                    val solution = map?.get(position)
                    f =
                            if (solution != null) {
                                val editable =
                                        setValues == null ||
                                                setValues[position] == null ||
                                                setValues[position] == false
                                Cell(editable, solution, cellIdCounter, type.numberOfSymbols)
                            } else {
                                Cell(cellIdCounter, type.numberOfSymbols)
                            }
                    cells!![position] = f
                    cellPositions!![cellIdCounter++] = position
                    f.registerListener(this)
                }
            }
        }
    }

    /** Creates a completely empty sudoku that has to be filled */
    @Deprecated("DO NOT USE THIS METHOD (if you are not from us)")
    internal constructor() {
        id = -1
    }

    /*init from basic properties. use this to init from BE */
    constructor(
            id: Int,
            transformCount: Int,
            sudokuType: SudokuType,
            complexity: Complexity,
            cells: HashMap<Position, Cell>
    ) {
        this.id = id
        this.transformCount = transformCount
        this.sudokuType = sudokuType
        this.complexity = complexity
        this.cells = cells

        cellPositions = HashMap()
        cells.forEach { (pos, c) -> cellPositions!![c.id] = pos }

        cells.values.forEach { cell -> cell.registerListener(this) }
    }

    /** increases transform count by one */
    fun increaseTransformCount() {
        transformCount++
    }

    /**
     * Returns the [Cell] at the specified [Position]. If the position is not mapped to a cell, null
     * is returned
     *
     * @param position Position of the cell
     * @return Cell at the [Position] or null if it is not mapped to a [Cell].
     */
    fun getCell(position: Position): Cell? {
        // todo refactor so that a miss leads to illegal args exception
        return cells!![position]
    }

    /**
     * Returns the [Cell] at the id.
     *
     * @param id ID of the [Cell] to return
     * @return the [Cell] at the specified id or null of id not found
     */
    fun getCell(id: Int): Cell? {
        return getCell(cellPositions!![id]!!)
    }

    /**
     * Maps the [Position] to the [Cell] if cell is null nothing happens
     *
     * @param cell the new [Cell]
     * @param position the [Position] for the new Cell
     */
    fun setCell(cell: Cell?, position: Position) {
        // todo cell can be null because samurai transformation needs it -> refactor?
        if (cell == null) return

        cells!![position] = cell
        cellPositions!![cell.id] = position
    }

    /** Checks if the id is mapped to a cell */
    fun hasCell(id: Int): Boolean {
        if (cellPositions == null) return false

        val p: Position = cellPositions!![id] ?: return false

        return cells?.get(p) != null
    }

    /**
     * Returns the [Position] of the [Cell] if the given id. Ist id noch nicht vergeben wird null
     * zurückgegeben
     *
     * @param id ID of the Cell of the Position to return
     * @return the [Position] of the id or null if id not found
     */
    fun getPosition(id: Int): Position? {
        return cellPositions!![id]
    }

    /**
     * Returns an [Iterator] over the [Cell]s.
     *
     * @return An [Iterator] over the [Cell]s
     */
    override fun iterator(): Iterator<Cell> {
        return cells!!.values.iterator()
    }

    /**
     * Checks if the Sudoku is completely filled and solved correctly.
     *
     * @return true, iff Sudoku is fully filled and solved correctly
     */
    open val isFinished: Boolean
        get() {
            // todo doesn't check for completeness
            var allCorrect = true
            for (cell in cells!!.values) if (!cell.isSolvedCorrect) {
                allCorrect = false
                break
            }
            return allCorrect
        }

    /** {@inheritDoc} */
    override fun onModelChanged(changedCell: Cell) {
        notifyListeners(changedCell)
    }

    /** {@inheritDoc} */
    override fun equals(other: Any?): Boolean {
        if (other != null && other is Sudoku) {
            val complexityMatch = complexity === other.complexity
            val typeMatch = sudokuType!!.enumType === other.sudokuType!!.enumType
            var fieldsMatch = true
            for (f in cells!!.values) {
                if (!other.hasCell(f.id) || f != other.getCell(f.id)) {
                    fieldsMatch = false
                    break
                }
            }
            return complexityMatch && typeMatch && fieldsMatch
        }
        return false
    }

    /**
     * Checks if this [Sudoku] has errors, i.e. if there is a [Cell] where the value is not the
     * correct solution.
     *
     * @return true, if there are incorrectly solved cells, false otherwise
     */
    open fun hasErrors(): Boolean {
        for (f in cells!!.values) if (!f.isNotWrong) return true
        return false

        // return this.fields.values().stream().anyMatch(f -> !f.isNotWrong()); //looks weird but be
        // very careful with simplifications!
    }

    /**
     * Checks if all instances of a symbol have been filled in the sudoku. A symbol is considered
     * fully filled when it appears exactly numberOfSymbols times.
     *
     * @param symbol the symbol value to check (0-based index, e.g., 0 for "1" in a standard sudoku)
     * @return true if the symbol appears numberOfSymbols times in the grid, false otherwise
     */
    fun isSymbolFullyFilled(symbol: Int): Boolean {
        if (symbol < 0 || symbol >= sudokuType!!.numberOfSymbols) {
            return false
        }

        var count = 0
        for (cell in cells!!.values) {
            if (cell.currentValue == symbol) {
                count++
            }
        }

        return count == sudokuType!!.numberOfSymbols
    }

    /**
     * Checks if all instances of a symbol have been filled correctly in the sudoku. A symbol is
     * considered completed when it appears exactly numberOfSymbols times and all occurrences are
     * correct (match the solution).
     *
     * @param symbol the symbol value to check (0-based index, e.g., 0 for "1" in a standard sudoku)
     * @return true if the symbol appears numberOfSymbols times correctly, false otherwise
     */
    fun isSymbolCompleted(symbol: Int): Boolean {
        if (symbol < 0 || symbol >= sudokuType!!.numberOfSymbols) {
            return false
        }

        var count = 0
        for (cell in cells!!.values) {
            if (cell.currentValue == symbol) {
                // Check if this cell is correctly solved
                if (!cell.isSolvedCorrect) {
                    return false
                }
                count++
            }
        }

        return count == sudokuType!!.numberOfSymbols
    }

    /**
     * Finds all cells that have a unique candidate (exactly one note set) AND are in a sufficiently
     * analyzed state. A cell is considered sufficiently analyzed if its constraint groups show
     * evidence of thorough analysis.
     *
     * Criteria: A cell with one note is only auto-filled if:
     * 1. It has exactly one note, AND
     * 2. At least one of its constraint groups (row/column/block) has been substantially analyzed
     * ```
     *    (meaning most unsolved cells in that group have notes)
     * ```
     * This prevents premature auto-filling when the user has only marked a few notes.
     *
     * These cells have a unique solution based on the current notes.
     *
     * @return a list of cells with unique candidates that meet the analysis threshold
     */
    fun findCellsWithUniqueCandidate(): List<Cell> {
        val cellsWithUniqueCandidate = mutableListOf<Cell>()
        for (cell in cells!!.values) {
            if (cell.isEditable && cell.isNotSolved && cell.getNotesCount() == 1) {
                // Check if this cell's constraint groups show sufficient analysis
                if (isConstraintGroupSufficientlyAnalyzed(cell)) {
                    cellsWithUniqueCandidate.add(cell)
                }
            }
        }
        return cellsWithUniqueCandidate
    }

    /**
     * Checks if at least one constraint group containing this cell has been sufficiently analyzed.
     * A constraint group is considered sufficiently analyzed if at least 60% of its unsolved cells
     * have notes.
     *
     * @param cell The cell to check
     * @return true if at least one constraint group (row, column, or block) is sufficiently
     * analyzed
     */
    private fun isConstraintGroupSufficientlyAnalyzed(cell: Cell): Boolean {
        val threshold = 0.6 // At least 60% of unsolved cells should have notes
        val cellPosition = getPosition(cell.id) ?: return false

        // Iterate through all constraints in the sudoku type
        for (constraint in sudokuType!!) {
            // Check if this constraint includes the cell's position
            if (!constraint.includes(cellPosition)) {
                continue
            }

            var unsolvedCells = 0
            var cellsWithNotes = 0

            // Iterate through all positions in this constraint
            for (position in constraint) {
                val constraintCell = getCell(position)
                if (constraintCell != null &&
                                constraintCell.isEditable &&
                                constraintCell.isNotSolved
                ) {
                    unsolvedCells++
                    if (constraintCell.getNotesCount() > 0) {
                        cellsWithNotes++
                    }
                }
            }

            // If at least threshold% of unsolved cells have notes, consider this group analyzed
            if (unsolvedCells > 0 && cellsWithNotes.toDouble() / unsolvedCells >= threshold) {
                return true
            }
        }

        return false
    }

    // debug
    override fun toString(): String {
        val sb = StringBuilder()
        val OFFSET = if (sudokuType!!.numberOfSymbols < 10) "" else " "
        val EMPTY = if (sudokuType!!.numberOfSymbols < 10) "x" else "xx"
        val NONE = if (sudokuType!!.numberOfSymbols < 10) " " else "  "
        for (j in 0 until sudokuType!!.size!!.y) {
            for (i in 0 until sudokuType!!.size!!.x) {
                val f = getCell(Position[i, j])
                var op: String
                if (f != null) { // feld existiert
                    val value = f.currentValue
                    op =
                            if (value == -1) EMPTY
                            else if (value < 10) OFFSET + value else value.toString() + ""
                    sb.append(op)
                } else {
                    sb.append(NONE)
                }
                sb.append(" ") // separator
            }
            sb.replace(sb.length - 1, sb.length, "\n")
        }
        sb.delete(sb.length - 1, sb.length)
        return sb.toString()
    }
}

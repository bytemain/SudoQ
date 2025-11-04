/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.game

import de.sudoq.model.actionTree.*
import de.sudoq.model.sudoku.Cell
import de.sudoq.model.sudoku.Position
import de.sudoq.model.sudoku.Sudoku
import de.sudoq.model.sudoku.complexity.Complexity
import java.util.*
import kotlin.math.pow

/**
 * This class represents a sudoku game.
 * Functions as a Facade towards the controller.
 */
class Game {

    /**
     * Unique id for the game
     */
    var id: Int
        private set

    /**
     * The sudoku of the game.
     */
    var sudoku: Sudoku? = null //todo make nonnullable
        private set

    /**
     * manages the game state
     */
    var stateHandler: GameStateHandler? = null //todo make non-nullable
        private set

    /**
     * Passed time since start of the game in seconds
     */
    var time = 0
        private set

    /**
     * Total sum of used assistances in this game.
     */
    var assistancesCost = 0
        private set

    /**
     * game settings
     */
    var gameSettings: GameSettings? = null //TODO make non-nullable

    /**
     * Indicates if game is finished
     */
    private var finished = false

    /**
     * Flag to prevent recursive auto-fill calls
     */
    private var isAutoFilling = false

    /**
     * Optional scheduler for animating auto-fill steps (provided by UI layer). If null, steps run immediately.
     */
    @Volatile
    private var autoFillScheduler: ((delayMs: Long, action: () -> Unit) -> Unit)? = null

    /**
     * Optional listener invoked before a cell is auto-filled (allows UI to highlight/select the cell).
     */
    @Volatile
    private var autoFillListener: ((Cell) -> Unit)? = null

    /** Optional listener invoked right after a cell has been auto-filled (for UI animations). */
    @Volatile
    private var autoFillAfterListener: ((Cell) -> Unit)? = null

    private val autoFillStepDelayMs = 320L
    private val autoFillFillDelayMs = 260L
    private var autoFillOrigin: Position? = null

    /* used by persistence (mapper) */
    constructor(
        id: Int,
        time: Int,
        assistancesCost: Int,
        sudoku: Sudoku,
        stateHandler: GameStateHandler,
        gameSettings: GameSettings,
        finished: Boolean
    ) {

        this.id = id
        this.time = time
        this.assistancesCost = assistancesCost
        this.sudoku = sudoku
        this.stateHandler = stateHandler
        this.gameSettings = gameSettings
        this.finished = finished
    }


    /**
     * Protected constructor to prevent instatiation outside this package.
     * (apparently thats not possible in kotlin...)
     * Available assistances are set from current profile. TODO really? make explicit instead
     *
     * @param id ID of the game
     * @param sudoku Sudoku of the new game
     */
    constructor(id: Int, sudoku: Sudoku) {//todo check visibility - make internal?
        this.id = id
        gameSettings = GameSettings()
        this.sudoku = sudoku
        this.time = 0
        stateHandler = GameStateHandler()
    }

    /**
     * creates a completely empty game
     */
    // package scope!
    internal constructor() {//TODO who uses this? can it be removed?
        id = -1
    }

    /**
     * Adds time to the game
     *
     * @param time Time to add in seconds
     */
    fun addTime(time: Int) {
        this.time += time
    }

    /**
     * The score of the game
     */
    val score: Int
        get() {
            var scoreFactor = 0
            fun power(expo: Double): Int = sudoku!!.sudokuType?.numberOfSymbols?.let {
                it.toDouble().pow(expo).toInt()
            }!!
            when (sudoku!!.complexity) {
                Complexity.infernal -> scoreFactor = power(4.0)
                Complexity.difficult -> scoreFactor = power(3.5)
                Complexity.medium -> scoreFactor = power(3.0)
                Complexity.easy -> scoreFactor = power(2.5)
                else -> {}
            }
            return (scoreFactor * 10 / ((time + assistancesTimeCost) / 60.0f)).toInt()
        }

    val assistancesTimeCost: Int
        get() = assistancesCost * 60

    /**
     * Checks the sudoku for correctness.
     * This is an assistance so the total assistance cost is increased.
     *
     * @return true, if sudoku is correct so far, false otherwise
     */
    fun checkSudoku(): Boolean {
        assistancesCost += 1
        return checkSudokuValidity()
    }

    /**
     * Checks the sudoku for correctness
     *
     * @return true, if sudoku is correct so far, false otherwise
     */
    private fun checkSudokuValidity(): Boolean {
        val correct = !sudoku!!.hasErrors()
        if (correct) {
            currentState.markCorrect()
        } else {
            currentState.markWrong()
        }
        return correct
    }

    /**
     * Executes the passed [Action] and saves it in the [ActionTree].
     *
     * @param action the [Action] to perform.
     */
    fun addAndExecute(action: Action) {
        if (finished) return
        stateHandler!!.addAndExecute(action)
        sudoku!!.getCell(action.cellId)?.let { cell ->
            updateNotes(cell)
            // Auto-fill cells with unique candidates if assistance is enabled and action is a SolveAction
            if (!isAutoFilling && action is SolveAction && cell.isSolved) {
                triggerAutoFillIfEnabled(cell)
            }
        }
        if (isFinished()) finished = true
    }

    /**
     * Triggers auto-fill for cells with unique candidates if the assistance is enabled.
     * This is called automatically after a user fills a cell.
     */
    private fun triggerAutoFillIfEnabled(origin: Cell) {
        if (!isAssistanceAvailable(Assistances.autoFillUniqueCandidates)) return
        if (sudoku!!.hasErrors()) return
        if (isAutoFilling) return

        isAutoFilling = true
        autoFillOrigin = sudoku!!.getPosition(origin.id)
        scheduleNextAutoFillStep()
    }

    // Backward-compat for callers without origin
    private fun triggerAutoFillIfEnabled() {
        if (!isAssistanceAvailable(Assistances.autoFillUniqueCandidates)) return
        if (sudoku!!.hasErrors()) return
        if (isAutoFilling) return
        isAutoFilling = true
        autoFillOrigin = null
        scheduleNextAutoFillStep()
    }

    private fun scheduleNextAutoFillStep() {
        if (sudoku!!.hasErrors()) {
            isAutoFilling = false
            return
        }
        // Find next cell with exactly one candidate
        val candidates = sudoku!!.findCellsWithUniqueCandidate()
        val nextCell = when (sudoku!!.complexity) {
            Complexity.easy -> {
                val origin = autoFillOrigin
                if (origin != null) {
                    // limit to row/column/block (all constraints containing origin)
                    val allowedPositions = HashSet<Position>()
                    for (c in sudoku!!.sudokuType!!) {
                        if (c.includes(origin)) {
                            for (p in c) allowedPositions.add(p)
                        }
                    }
                    candidates.firstOrNull { it.isNotSolved && it.getNotesCount() == 1 && allowedPositions.contains(sudoku!!.getPosition(it.id)) }
                } else {
                    candidates.firstOrNull { it.isNotSolved && it.getNotesCount() == 1 }
                }
            }
            else -> candidates.firstOrNull { it.isNotSolved && it.getNotesCount() == 1 }
        }

        if (nextCell == null) {
            isAutoFilling = false
            return
        }

        val scheduler = autoFillScheduler
        if (scheduler != null) {
            // Phase 1: brief pre-highlight
            scheduler.invoke(autoFillStepDelayMs) {
                autoFillListener?.invoke(nextCell)
                // Phase 2: perform fill after a short delay
                scheduler.invoke(autoFillFillDelayMs) {
                    val uniqueCandidate = nextCell.getSingleNote()
                    addAndExecute(SolveActionFactory().createAction(uniqueCandidate, nextCell))
                    autoFillAfterListener?.invoke(nextCell)
                    // Chain next step
                    scheduler.invoke(0L) { scheduleNextAutoFillStep() }
                }
            }
        } else {
            // No scheduler injected: perform immediately, but still step-by-step
            val uniqueCandidate = nextCell.getSingleNote()
            addAndExecute(SolveActionFactory().createAction(uniqueCandidate, nextCell))
            autoFillAfterListener?.invoke(nextCell)
            scheduleNextAutoFillStep()
        }
    }

    /**
     * Sets a scheduler used to animate auto-fill steps. The scheduler is expected to execute the
     * given action after the specified delay (in milliseconds) on the UI thread.
     */
    fun setAutoFillScheduler(scheduler: (delayMs: Long, action: () -> Unit) -> Unit) {
        this.autoFillScheduler = scheduler
    }

    /**
     * Sets a listener that will be called right before a cell is auto-filled. Useful for UI to
     * highlight/select the cell that is about to be filled.
     */
    fun setAutoFillListener(listener: (Cell) -> Unit) {
        this.autoFillListener = listener
    }

    /** Sets a listener called after a cell has been auto-filled. */
    fun setAutoFillAfterListener(listener: (Cell) -> Unit) {
        this.autoFillAfterListener = listener
    }

    /**
     * Updates the notes in den constraints of the cell by removing the cells current value from the notes.
     * Is only executed if the respective assistance is available.
     *
     * @param cell the modified Cell
     */
    private fun updateNotes(cell: Cell) {
        if (!isAssistanceAvailable(Assistances.autoAdjustNotes)) return
        val editedPos = sudoku!!.getPosition(cell.id)
        val value = cell.currentValue

        /*this.sudoku.getSudokuType().getConstraints().stream().filter(c -> c.includes(editedPos))
                                                             .flatMap(c -> c.getPositions().stream())
                                                             .filter(changePos -> this.sudoku.getField(changePos).isNoteSet(value))
                                                             .forEachOrdered(changePos -> this.addAndExecute(new NoteActionFactory().createAction(value, this.sudoku.getField(changePos))));
        should work, but to tired to try*/

        for (c in sudoku!!.sudokuType!!) {
            if (c.includes(editedPos!!)) {
                for (changePos in c) {
                    if (sudoku!!.getCell(changePos)?.isNoteSet(value)!!) {
                        addAndExecute(
                            NoteActionFactory().createAction(
                                value,
                                sudoku!!.getCell(changePos)!!
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Returns the state of the game to the given node in the action tree.
     * TODO what if the node is not in the action tree?
     *
     * @param ate The ActionTreeElement in which the state of the Sudoku is to be returned.
     *
     */
    fun goToState(ate: ActionTreeElement) {
        stateHandler!!.goToState(ate)
    }

    /**
     * Undoes the last action. Goes one step back in the action tree.
     */
    fun undo() {
        stateHandler!!.undo()
    }

    /**
     * Redo the last [Action]. Goes one step forward in the action tree.
     */
    fun redo() {
        stateHandler!!.redo()
    }

    /**
     * The action tree node of the current state.
     */
    val currentState: ActionTreeElement
        get() = stateHandler!!.currentState!! //todo find a way to ensure it can never be null (the implicit root)

    /**
     * Marks the current state to better find it later.
     */
    fun markCurrentState() {
        stateHandler!!.markCurrentState() //TODO what doe this mean is it a book mark?
    }

    /**
     * Checks if the given [ActionTreeElement] is marked.
     *
     * @param ate the ActionTreeElement to check
     * @return true iff it is marked
     */
    fun isMarked(ate: ActionTreeElement?): Boolean {
        return stateHandler!!.isMarked(ate)
    }

    /**
     * Checks if the sudoku is solved completely and correct.
     *
     * @return true iff sudoku is finished (and correct)
     */
    fun isFinished(): Boolean {
        return finished || sudoku!!.isFinished
    }

    /**
     * Tries to solve the specified [Cell] and returns if that attempt was successful.
     * If the [Sudoku] is invalid or has mistakes false is returned.
     *
     * @param cell The cell to solve
     * @return true, if cell could be solved, false otherwise
     */
    fun solveCell(cell: Cell?): Boolean { //TODO don't accept null
        if (sudoku!!.hasErrors() || cell == null) return false
        assistancesCost += 3
        val solution = cell.solution
        return if (solution != Cell.EMPTYVAL) {
            addAndExecute(SolveActionFactory().createAction(solution, cell))
            true
        } else {
            false
        }
    }

    /**
     * Tries to solve a randomly selected [Cell] and returns whether that was successful.
     *
     * @return true, if a cell could be solved, false otherwise
     */
    fun solveCell(): Boolean {
        if (sudoku!!.hasErrors()) return false
        assistancesCost += 3
        for (f in sudoku!!) {
            if (f.isNotSolved) {
                addAndExecute(SolveActionFactory().createAction(f.solution, f))
                break
            }
        }
        return true

        /*
         * Solution solution = solver.getHint(); if (solution != null) {
         * stateHandler.addAndExecute(solution.getAction()); return true; } else { return false; }
         */
    }

    /**
     * Solves the entire sudoku.
     *
     * @return true, iff the sudoku could be solved, false otherwise
     */
    fun solveAll(): Boolean {
        if (sudoku!!.hasErrors()) return false
        val unsolvedCells: MutableList<Cell> = ArrayList()
        for (f in sudoku!!) {
            if (f.isNotSolved) {
                unsolvedCells.add(f)
            }
        }
        val rnd = Random()
        while (unsolvedCells.isNotEmpty()) {
            val nr = rnd.nextInt(unsolvedCells.size)
            val a = SolveActionFactory().createAction(unsolvedCells[nr].solution, unsolvedCells[nr])
            addAndExecute(a)
            unsolvedCells.removeAt(nr)
        }
        assistancesCost += Int.MAX_VALUE / 80
        return true
        /*
         * if (solver.solveAll(false, false, false) != null) { for (Field f : unsolvedFields) { this.addAndExecute(new
         * SolveActionFactory().createAction(f.getCurrentValue(), f)); } return true; } else { return false; }
         */
    }

    /**
     * Goes back to the last correctly solved state in the action tree.
     * If current state is correct, nothing happens.
     * This is an assistance, so the AssistanceCost is increased.
     */
    fun goToLastCorrectState() {
        assistancesCost += 3
        while (!checkSudokuValidity()) {
            undo()
        }
        currentState.markCorrect()
    }

    /**
     * Goes back to the last book mark in the [ActionTree].
     * If the current state is already bookmarked, nothing happens.
     * Goes back to the root if there is no bookmark. TODO maybe better if nothing happens in that case
     */
    fun goToLastBookmark() {
        while (stateHandler!!.currentState != stateHandler!!.actionTree.root
            && !stateHandler!!.currentState!!.isMarked
        ) {
            undo()
        }
    }

    /**
     * Automatically fills cells with unique candidates (cells that have exactly one note set).
     * Recursively continues filling until no more cells with unique candidates remain.
     * This is an active assistance, so the assistance cost is increased when called manually.
     *
     * @return the number of cells that were filled
     */
    fun autoFillUniqueCandidates(): Int {
        if (sudoku!!.hasErrors()) return 0
        if (isAutoFilling) return 0 // Prevent recursive calls
        
        var totalFilled = 0
        isAutoFilling = true
        
        try {
            var cellsFilled: Int
            
            // Keep filling cells with unique candidates until no more are found
            do {
                cellsFilled = 0
                val cellsWithUniqueCandidate = sudoku!!.findCellsWithUniqueCandidate()
                
                for (cell in cellsWithUniqueCandidate) {
                    // Double-check cell state in case it was modified during iteration
                    if (cell.isNotSolved && cell.getNotesCount() == 1) {
                        val uniqueCandidate = cell.getSingleNote()
                        // Fill the cell with the unique candidate
                        addAndExecute(SolveActionFactory().createAction(uniqueCandidate, cell))
                        cellsFilled++
                        totalFilled++
                    }
                }
            } while (cellsFilled > 0 && !sudoku!!.hasErrors())
            
            // Add assistance cost based on the number of cells filled
            assistancesCost += totalFilled
        } finally {
            isAutoFilling = false
        }
        
        return totalFilled
    }

    /**
     * Sets the available assistances.
     *
     * @param assistances Die Assistances die für dieses Game gesetzt werden soll
     */
    fun setAssistances(assistances: GameSettings) {
        gameSettings = assistances

        /* calculate costs of passive assistances add them to the total assistance cost */
        if (isAssistanceAvailable(Assistances.autoAdjustNotes)) assistancesCost += 4
        if (isAssistanceAvailable(Assistances.markRowColumn)) assistancesCost += 2
        if (isAssistanceAvailable(Assistances.markWrongSymbol)) assistancesCost += 6
        if (isAssistanceAvailable(Assistances.restrictCandidates)) assistancesCost += 12
    }

    /**
     * Checks whether the given assistance is available.
     *
     * @param assist The assistance to check for availability
     *
     * @return true, if the assistance is available
     */
    fun isAssistanceAvailable(assist: Assistances): Boolean {//TODO don't accept null
        return gameSettings!!.getAssistance(assist)
    }

    val isLefthandedModeActive: Boolean
        get() = gameSettings!!.isLefthandModeSet


    /**
     * {@inheritDoc}
     */
    override fun equals(other: Any?): Boolean {//todo refactor
        if (other is Game) {
            return (id == other.id
                    && sudoku == other.sudoku
                    && stateHandler!!.actionTree == other.stateHandler!!.actionTree
                    && currentState == other.currentState)
        }
        return false
    }
}
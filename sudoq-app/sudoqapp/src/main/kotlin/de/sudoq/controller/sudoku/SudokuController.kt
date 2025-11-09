/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.sudoku

import android.content.Context
import de.sudoq.R
import de.sudoq.model.actionTree.Action
import de.sudoq.model.actionTree.NoteActionFactory
import de.sudoq.model.actionTree.SolveActionFactory
import de.sudoq.model.game.Game
import de.sudoq.model.profile.ProfileSingleton
import de.sudoq.model.profile.Statistics
import de.sudoq.model.sudoku.Cell
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.persistence.profile.ProfileRepo
import de.sudoq.persistence.profile.ProfilesListRepo

/**
 * Der SudokuController ist dafür zuständig auf Aktionen des Benutzers mit dem
 * Spielfeld zu reagieren.
 */
class SudokuController(
    /** Hält eine Referenz auf das Game, welches Daten über das aktuelle Spiel enthält */
    private val game: Game,
    /** Die SudokuActivity. */
    private val context: SudokuActivity
) : AssistanceRequestListener, ActionListener {

    /**
     * Debugging
     *
     * @throws IllegalArgumentException
     * Wird geworfen, falls null übergeben wird
     */
    private fun getsucc(illegal: Boolean) {
        require(!illegal) { "tu" }
    }
    /** Methods  */
    /**
     * {@inheritDoc}
     */
    override fun onRedo() {
        game.redo()
    }

    /**
     * {@inheritDoc}
     */
    override fun onUndo() {
        game.undo()
        
        // Clear multi-selection state after undo
        context.sudokuLayout?.let { layout ->
            if (layout.isMultiSelectionMode) {
                layout.clearMultiSelection()
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onNoteAdd(cell: Cell, value: Int) {
        game.addAndExecute(NoteActionFactory().createAction(value, cell))
    }

    /**
     * {@inheritDoc}
     */
    override fun onNoteDelete(cell: Cell, value: Int) {
        game.addAndExecute(
            NoteActionFactory().createAction(
                value,
                cell
            )
        ) //TODO same code as onNoteAdd why?
    }

    /**
     * {@inheritDoc}
     */
    override fun onAddEntry(cell: Cell, value: Int) {
        // Use SolveActionWithNoteUpdate to handle automatic note adjustments
        val action = de.sudoq.model.actionTree.SolveActionWithNoteUpdate(
            value - cell.currentValue, // diff, not absolute value
            cell,
            game.sudoku!!,
            game.isAssistanceAvailable(de.sudoq.model.game.Assistances.autoAdjustNotes)
        )
        game.addAndExecute(action)
        if (game.isFinished()) {
            updateStatistics()
            handleFinish(false)
        }
    }

    fun onHintAction(a: Action) {
        game.addAndExecute(a)
        if (game.isFinished()) {
            updateStatistics()
            handleFinish(false)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onDeleteEntry(cell: Cell) {
        // Use SolveActionWithNoteUpdate to handle automatic note adjustments
        val action = de.sudoq.model.actionTree.SolveActionWithNoteUpdate(
            Cell.EMPTYVAL - cell.currentValue, // diff, not absolute value
            cell,
            game.sudoku!!,
            game.isAssistanceAvailable(de.sudoq.model.game.Assistances.autoAdjustNotes)
        )
        game.addAndExecute(action)
    }

    /**
     * {@inheritDoc}
     */
    override fun onSolveOne(): Boolean {
        val sudoku = game.sudoku ?: return false
        if (sudoku.hasErrors()) return false

        // Pick the next unsolved cell (same behavior as previous implementation)
        val cellToSolve = run {
            var target: Cell? = null
            for (f in sudoku) {
                if (f.isNotSolved) { target = f; break }
            }
            target
        } ?: return false

        // Detect solvability first; if no solution available, fail fast
        val solution = cellToSolve.solution
        if (solution == Cell.EMPTYVAL) return false

        // Briefly highlight the cell (green frame), then fill the value
        val sl = context.sudokuLayout ?: return false
        val pos = sudoku.getPosition(cellToSolve.id) ?: return false
        val highlightView = de.sudoq.view.Hints.HighlightedCellView(
            context, sl, pos, android.graphics.Color.RED
        )
        // Add overlay to show which cell is being solved
        sl.addView(highlightView, sl.width, sl.height)

        val delayMs = 350L
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({
            // Remove highlight and perform the solve
            try { sl.removeView(highlightView) } catch (_: Exception) {}
            game.solveCell(cellToSolve)
            if (game.isFinished()) {
                updateStatistics()
                handleFinish(false)
            }
        }, delayMs)

        // Indicate the action was scheduled
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun onSolveCurrent(cell: Cell): Boolean {
        val res = game.solveCell(cell)
        if (game.isFinished()) {
            updateStatistics()
            handleFinish(false)
        }
        return res
    }

    /**
     * {@inheritDoc}
     */
    override fun onSolveAll(): Boolean {
        for (f in game.sudoku!!) {
            if (!f.isNotWrong) {
                game.addAndExecute(SolveActionFactory().createAction(Cell.EMPTYVAL, f))
            }
        }
        val res = game.solveAll()
        if (res) handleFinish(true)
        return res
    }

    /**
     * Zeigt einen Gewinndialog an, der fragt, ob das Spiel beendet werden soll.
     *
     * @param surrendered
     * TODO
     */
    private fun handleFinish(surrendered: Boolean) {
        context.setFinished(true, surrendered)
    }

    /**
     * Updatet die Spielerstatistik des aktuellen Profils in der App.
     */
    private fun updateStatistics() {
        when (game.sudoku!!.complexity) {
            Complexity.infernal -> incrementStatistic(Statistics.playedInfernalSudokus)
            Complexity.difficult -> incrementStatistic(Statistics.playedDifficultSudokus)
            Complexity.medium -> incrementStatistic(Statistics.playedMediumSudokus)
            Complexity.easy -> incrementStatistic(Statistics.playedEasySudokus)
            else -> {}
        }
        incrementStatistic(Statistics.playedSudokus)
        val profilesDir = context.getDir(
            context.getString(R.string.path_rel_profiles),
            Context.MODE_PRIVATE
        )
        val p = ProfileSingleton.getInstance(profilesDir, ProfileRepo(profilesDir),
                                             ProfilesListRepo(profilesDir))
        if (p.getStatistic(Statistics.fastestSolvingTime) > game.time) {
            p.setStatistic(Statistics.fastestSolvingTime, game.time)
        }
        if (p.getStatistic(Statistics.maximumPoints) < game.score) {
            p.setStatistic(Statistics.maximumPoints, game.score)
        }
    }

    private fun incrementStatistic(s: Statistics) { //TODO this should probably be in model...
        val profilesDir = context.getDir(
            context.getString(R.string.path_rel_profiles),
            Context.MODE_PRIVATE
        )
        val p = ProfileSingleton.getInstance(profilesDir, ProfileRepo(profilesDir),
                                             ProfilesListRepo(profilesDir))
        p.setStatistic(s, p.getStatistic(s) + 1)
    }

    /**
     * Fills all empty cells with valid candidates based on Sudoku rules.
     * For each empty cell, calculates which numbers are valid by checking
     * the row, column, and constraint group (block).
     */
    fun fillAllCandidates() {
        val sudoku = game.sudoku ?: return
        val sudokuType = sudoku.sudokuType ?: return
        val numberOfSymbols = sudokuType.numberOfSymbols

        // Collect all changes to be made
        val changes = mutableListOf<de.sudoq.model.actionTree.FillCandidatesAction.CellChange>()

        // Iterate through all cells
        for (cell in sudoku) {
            // Skip cells that are not editable or already have a value
            if (!cell.isEditable || cell.isSolved) {
                continue
            }

            // Get the position of this cell
            val position = sudoku.getPosition(cell.id) ?: continue

            // Create a set of all possible values (0 to numberOfSymbols-1)
            val validCandidates = mutableSetOf<Int>()
            for (i in 0 until numberOfSymbols) {
                validCandidates.add(i)
            }

            // Check all constraints (row, column, block) for this position
            for (constraint in sudokuType) {
                // Check if this constraint contains the current position
                if (constraint.includes(position)) {
                    // Remove values that are already present in this constraint
                    for (constraintPos in constraint) {
                        val constraintCell = sudoku.getCell(constraintPos)
                        if (constraintCell != null && constraintCell.isSolved) {
                            validCandidates.remove(constraintCell.currentValue)
                        }
                    }
                }
            }

            // Record changes for the valid candidates as notes in the cell
            for (candidate in 0 until numberOfSymbols) {
                val shouldBeSet = validCandidates.contains(candidate)
                val isCurrentlySet = cell.isNoteSet(candidate)

                // Only add to changes if the state needs to change
                if (shouldBeSet != isCurrentlySet) {
                    changes.add(
                        de.sudoq.model.actionTree.FillCandidatesAction.CellChange(
                            cell,
                            candidate,
                            shouldBeSet
                        )
                    )
                }
            }
        }

        // Execute all changes as a single undoable action
        if (changes.isNotEmpty()) {
            val fillAction = de.sudoq.model.actionTree.FillCandidatesAction(changes)
            game.addAndExecute(fillAction)
        }
    }
}
package de.sudoq.model.solverGenerator.solver.helper

import de.sudoq.model.solverGenerator.solution.DerivationCell
import de.sudoq.model.solverGenerator.solution.SwordfishDerivation
import de.sudoq.model.solverGenerator.solver.SolverSudoku
import de.sudoq.model.solvingAssistant.HintTypes
import de.sudoq.model.sudoku.*
import java.util.*

/**
 * Swordfish Helper - extension of X-Wing using 3 rows/columns instead of 2
 * 
 * Idea:
 * Similar to X-Wing but with 3 rows and 3 columns forming intersections.
 * If a candidate appears in exactly 2-3 positions in each of 3 rows,
 * and all these positions are aligned in the same 3 columns,
 * then the candidate can be eliminated from other cells in those 3 columns.
 * (Or vice versa with rows and columns swapped)
 */
class SwordfishHelper(sudoku: SolverSudoku, complexity: Int) : SolveHelper(sudoku, complexity) {

    init {
        hintType = HintTypes.Swordfish
    }

    private fun separateIntoRowColumn(
        pool: Iterable<Constraint>,
        rows: MutableList<Constraint>,
        cols: MutableList<Constraint>
    ) {
        for (c in pool) {
            when (getGroupShape(c.getPositions())) {
                Utils.ConstraintShape.Row -> rows.add(c)
                Utils.ConstraintShape.Column -> cols.add(c)
                else -> {}
            }
        }
    }

    override fun update(buildDerivation: Boolean): Boolean {
        val constraints: Iterable<Constraint> = sudoku.sudokuType!!

        /* collect rows / cols */
        val rows: MutableList<Constraint> = ArrayList()
        val cols: MutableList<Constraint> = ArrayList()
        separateIntoRowColumn(constraints, rows, cols)

        // Try rows locked, eliminate from columns
        if (findSwordfish(rows, cols, buildDerivation)) return true
        
        // Try columns locked, eliminate from rows
        if (findSwordfish(cols, rows, buildDerivation)) return true

        return false
    }

    /**
     * Find swordfish pattern where lockedConstraints have the candidate locked
     * and reducibleConstraints can have the candidate eliminated
     */
    private fun findSwordfish(
        lockedConstraints: List<Constraint>,
        reducibleConstraints: List<Constraint>,
        buildDerivation: Boolean
    ): Boolean {
        val size = lockedConstraints.size
        
        // Try all combinations of 3 locked constraints
        for (i1 in 0 until size - 2) {
            for (i2 in i1 + 1 until size - 1) {
                for (i3 in i2 + 1 until size) {
                    val locked = arrayOf(
                        lockedConstraints[i1],
                        lockedConstraints[i2],
                        lockedConstraints[i3]
                    )
                    
                    // Check if they are non-overlapping (important for Samurai sudokus)
                    if (hasOverlap(locked)) continue
                    
                    // For each candidate number, check if it forms a swordfish
                    for (note in 0 until sudoku.sudokuType!!.numberOfSymbols) {
                        if (checkSwordfishForNote(locked, reducibleConstraints, note, buildDerivation)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * Check if the three locked constraints have overlapping positions
     */
    private fun hasOverlap(constraints: Array<Constraint>): Boolean {
        for (i in 0 until constraints.size - 1) {
            for (j in i + 1 until constraints.size) {
                if (intersectionPoint(constraints[i].getPositions(), constraints[j].getPositions()) != null) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if a specific note forms a swordfish pattern
     */
    private fun checkSwordfishForNote(
        lockedConstraints: Array<Constraint>,
        reducibleConstraints: List<Constraint>,
        note: Int,
        buildDerivation: Boolean
    ): Boolean {
        // For each locked constraint, the candidate must appear 2-3 times
        val columnsUsed = mutableSetOf<Constraint>()
        
        for (locked in lockedConstraints) {
            val occurrences = countOccurrences(note, locked.getPositions())
            if (occurrences < 2 || occurrences > 3) {
                return false
            }
            
            // Track which reducible constraints (columns) contain this note in this locked constraint (row)
            for (reducible in reducibleConstraints) {
                val intersection = intersectionPoint(locked.getPositions(), reducible.getPositions())
                if (intersection != null && sudoku.getCurrentCandidates(intersection).isSet(note)) {
                    columnsUsed.add(reducible)
                }
            }
        }
        
        // Must use exactly 3 reducible constraints
        if (columnsUsed.size != 3) {
            return false
        }
        
        // Check if we can eliminate the candidate from these reducible constraints
        val canBeDeleted = mutableListOf<Position>()
        for (reducible in columnsUsed) {
            for (pos in reducible.getPositions()) {
                // If this position has the candidate but is not in any of the locked constraints' intersection
                if (sudoku.getCurrentCandidates(pos).isSet(note)) {
                    var inIntersection = false
                    for (locked in lockedConstraints) {
                        if (locked.getPositions().contains(pos)) {
                            inIntersection = true
                            break
                        }
                    }
                    if (!inIntersection) {
                        canBeDeleted.add(pos)
                    }
                }
            }
        }
        
        // If we can delete at least one candidate, we found a swordfish
        if (canBeDeleted.isNotEmpty()) {
            // Delete the candidates
            for (pos in canBeDeleted) {
                sudoku.getCurrentCandidates(pos).clear(note)
            }
            
            if (buildDerivation) {
                buildDerivation(lockedConstraints, columnsUsed.toList(), canBeDeleted, note)
            }
            return true
        }
        
        return false
    }

    private fun buildDerivation(
        lockedConstraints: Array<Constraint>,
        reducibleConstraints: List<Constraint>,
        canBeDeleted: List<Position>,
        note: Int
    ) {
        val internalDerivation = SwordfishDerivation()
        internalDerivation.setLockedConstraints(lockedConstraints[0], lockedConstraints[1], lockedConstraints[2])
        internalDerivation.setReducibleConstraints(
            reducibleConstraints[0],
            reducibleConstraints[1],
            reducibleConstraints[2]
        )
        
        for (pos in canBeDeleted) {
            val relevant = CandidateSet()
            relevant.set(note)
            val irrelevant = CandidateSet()
            internalDerivation.addDerivationCell(DerivationCell(pos, relevant, irrelevant))
        }
        internalDerivation.note = note
        derivation = internalDerivation
    }

    private fun countOccurrences(note: Int, positions: Iterable<Position>): Int {
        return positions.count { p -> sudoku.getCurrentCandidates(p).isSet(note) }
    }

    companion object {
        private fun <T> intersectionPoint(a: Iterable<T>, b: Iterable<T>): T? {
            for (t1 in a) {
                for (t2 in b) {
                    if (t1 == t2) return t1
                }
            }
            return null
        }
    }
}

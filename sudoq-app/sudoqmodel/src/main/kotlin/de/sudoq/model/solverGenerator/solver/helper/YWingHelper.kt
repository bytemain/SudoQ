package de.sudoq.model.solverGenerator.solver.helper

import de.sudoq.model.solverGenerator.solution.DerivationCell
import de.sudoq.model.solverGenerator.solution.YWingDerivation
import de.sudoq.model.solverGenerator.solver.SolverSudoku
import de.sudoq.model.solvingAssistant.HintTypes
import de.sudoq.model.sudoku.*
import java.util.*

/**
 * Y-Wing Helper
 * 
 * A Y-Wing consists of three cells:
 * 1. Pivot cell with exactly 2 candidates (AB)
 * 2. Pincer 1 with exactly 2 candidates (AC) that shares a constraint with pivot
 * 3. Pincer 2 with exactly 2 candidates (BC) that shares a constraint with pivot
 * 
 * If a cell can see both pincers (but not necessarily the pivot),
 * then candidate C can be eliminated from that cell.
 * 
 * The logic: Either pivot is A (then one pincer must be C) or pivot is B (then other pincer must be C).
 * So any cell seeing both pincers cannot be C.
 */
class YWingHelper(sudoku: SolverSudoku, complexity: Int) : SolveHelper(sudoku, complexity) {

    init {
        hintType = HintTypes.YWing
    }

    override fun update(buildDerivation: Boolean): Boolean {
        // Find all cells with exactly 2 candidates (potential pivots and pincers)
        val biValueCells = mutableListOf<Position>()
        
        for (pos in sudoku.sudokuType!!.validPositions) {
            if (sudoku.getCurrentCandidates(pos).cardinality() == 2) {
                biValueCells.add(pos)
            }
        }
        
        // Try each bi-value cell as a pivot
        for (pivot in biValueCells) {
            val pivotCandidates = sudoku.getCurrentCandidates(pivot)
            val pivotList = pivotCandidates.setBits.toList()
            if (pivotList.size != 2) continue
            
            val a = pivotList[0]
            val b = pivotList[1]
            
            // Find cells that share a constraint with the pivot
            val neighbors = getNeighbors(pivot)
            
            // Find potential pincers
            val pincersAC = mutableListOf<Position>()  // Cells with candidates A and C
            val pincersBC = mutableListOf<Position>()  // Cells with candidates B and C
            
            for (neighbor in neighbors) {
                if (neighbor !in biValueCells) continue
                
                val neighborCandidates = sudoku.getCurrentCandidates(neighbor)
                val neighborList = neighborCandidates.setBits.toList()
                
                if (neighborCandidates.isSet(a) && !neighborCandidates.isSet(b)) {
                    // This cell has A and some other candidate C
                    pincersAC.add(neighbor)
                } else if (neighborCandidates.isSet(b) && !neighborCandidates.isSet(a)) {
                    // This cell has B and some other candidate C
                    pincersBC.add(neighbor)
                }
            }
            
            // Try combinations of pincers
            for (pincerAC in pincersAC) {
                val candidatesAC = sudoku.getCurrentCandidates(pincerAC).setBits.toList()
                val c = candidatesAC.first { it != a }
                
                for (pincerBC in pincersBC) {
                    val candidatesBC = sudoku.getCurrentCandidates(pincerBC).setBits.toList()
                    
                    // Check if the second candidate is also C
                    if (!candidatesBC.contains(c)) continue
                    
                    // We found a Y-Wing! Now find cells that can see both pincers
                    val canBeDeleted = findDeletableCells(pincerAC, pincerBC, c)
                    
                    if (canBeDeleted.isNotEmpty()) {
                        // Delete the candidate C from those cells
                        for (pos in canBeDeleted) {
                            sudoku.getCurrentCandidates(pos).clear(c)
                        }
                        
                        if (buildDerivation) {
                            buildDerivation(pivot, pincerAC, pincerBC, canBeDeleted, a, b, c)
                        }
                        return true
                    }
                }
            }
        }
        
        return false
    }

    /**
     * Find all cells that can see both pincers and have candidate C
     */
    private fun findDeletableCells(pincer1: Position, pincer2: Position, c: Int): List<Position> {
        val neighbors1 = getNeighbors(pincer1)
        val neighbors2 = getNeighbors(pincer2)
        
        val canBeDeleted = mutableListOf<Position>()
        
        for (pos in sudoku.sudokuType!!.validPositions) {
            // Skip the pincers themselves
            if (pos == pincer1 || pos == pincer2) continue
            
            // Check if this cell can see both pincers and has candidate C
            if (pos in neighbors1 && pos in neighbors2) {
                if (sudoku.getCurrentCandidates(pos).isSet(c)) {
                    canBeDeleted.add(pos)
                }
            }
        }
        
        return canBeDeleted
    }

    /**
     * Get all positions that share at least one constraint with the given position
     */
    private fun getNeighbors(pos: Position): Set<Position> {
        val neighbors = mutableSetOf<Position>()
        
        for (constraint in sudoku.sudokuType!!) {
            if (constraint.hasUniqueBehavior() && pos in constraint.getPositions()) {
                neighbors.addAll(constraint.getPositions())
            }
        }
        
        neighbors.remove(pos)  // Don't include the position itself
        return neighbors
    }

    private fun buildDerivation(
        pivot: Position,
        pincer1: Position,
        pincer2: Position,
        canBeDeleted: List<Position>,
        a: Int,
        b: Int,
        c: Int
    ) {
        val internalDerivation = YWingDerivation()
        internalDerivation.setPivot(pivot)
        internalDerivation.setPincers(pincer1, pincer2)
        internalDerivation.candidateA = a
        internalDerivation.candidateB = b
        internalDerivation.candidateC = c
        
        for (pos in canBeDeleted) {
            val relevant = CandidateSet()
            relevant.set(c)
            val irrelevant = CandidateSet()
            internalDerivation.addDerivationCell(DerivationCell(pos, relevant, irrelevant))
        }
        
        derivation = internalDerivation
    }
}

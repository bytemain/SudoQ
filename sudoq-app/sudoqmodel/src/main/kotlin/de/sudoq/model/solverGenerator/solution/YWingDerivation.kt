package de.sudoq.model.solverGenerator.solution

import de.sudoq.model.actionTree.Action
import de.sudoq.model.actionTree.NoteActionFactory
import de.sudoq.model.solvingAssistant.HintTypes
import de.sudoq.model.sudoku.Position
import de.sudoq.model.sudoku.Sudoku
import de.sudoq.model.sudoku.CandidateSet.Companion.fromBitSet
import java.util.*

class YWingDerivation : SolveDerivation(HintTypes.YWing) {

    private var pivot: Position? = null
    private val pincers: MutableList<Position> = Stack()

    var candidateA = 0
    var candidateB = 0
    var candidateC = 0

    fun setPivot(p: Position) {
        pivot = p
    }

    fun setPincers(p1: Position, p2: Position) {
        pincers.add(p1)
        pincers.add(p2)
    }

    fun getPivot(): Position? {
        return pivot
    }

    fun getPincers(): List<Position> {
        return pincers
    }

    /* creates a list of actions in case the user wants the app to execute the hint */
    override fun getActionList(sudoku: Sudoku): List<Action> {
        val actionlist: MutableList<Action> = ArrayList()
        val af = NoteActionFactory()
        val it = cellIterator
        while (it.hasNext()) {
            val df = it.next()
            for (note in fromBitSet(df.relevantCandidates).setBits) {
                actionlist.add(af.createAction(note, sudoku.getCell(df.position)!!))
            }
        }
        return actionlist
    }

    init {
        setDescription("Y-Wing")
        hasActionListCapability = true
    }
}

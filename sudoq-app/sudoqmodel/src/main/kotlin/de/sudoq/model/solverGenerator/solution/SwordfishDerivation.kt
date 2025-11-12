package de.sudoq.model.solverGenerator.solution

import de.sudoq.model.actionTree.Action
import de.sudoq.model.actionTree.NoteActionFactory
import de.sudoq.model.solvingAssistant.HintTypes
import de.sudoq.model.sudoku.Constraint
import de.sudoq.model.sudoku.Sudoku
import de.sudoq.model.sudoku.CandidateSet.Companion.fromBitSet
import java.util.*

class SwordfishDerivation : SolveDerivation(HintTypes.Swordfish) {

    private val lockedConstraints: MutableList<Constraint> = Stack()
    private val reducibleConstraints: MutableList<Constraint> = Stack()

    var note = 0

    fun setLockedConstraints(c1: Constraint, c2: Constraint, c3: Constraint) {
        lockedConstraints.add(c1)
        lockedConstraints.add(c2)
        lockedConstraints.add(c3)
    }

    fun setReducibleConstraints(c1: Constraint, c2: Constraint, c3: Constraint) {
        reducibleConstraints.add(c1)
        reducibleConstraints.add(c2)
        reducibleConstraints.add(c3)
    }

    fun getReducibleConstraints(): List<Constraint> {
        return reducibleConstraints
    }

    fun getLockedConstraints(): List<Constraint> {
        return lockedConstraints
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
        setDescription("Swordfish")
        hasActionListCapability = true
    }
}

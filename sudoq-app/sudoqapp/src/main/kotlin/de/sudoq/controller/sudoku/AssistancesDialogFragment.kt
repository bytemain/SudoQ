package de.sudoq.controller.sudoku

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import de.sudoq.R
import de.sudoq.controller.sudoku.hints.HintFormulator.getText
import de.sudoq.model.game.Assistances
import de.sudoq.model.game.Game
import de.sudoq.model.profile.ProfileSingleton
import de.sudoq.model.solvingAssistant.SolvingAssistant.giveAHint
import de.sudoq.persistence.profile.ProfileRepo
import de.sudoq.persistence.profile.ProfilesListRepo
import de.sudoq.view.SudokuLayout
import java.util.*

/**
 * Created by timo on 29.10.16.
 */
class AssistancesDialogFragment : DialogFragment() {
    private var sl: SudokuLayout? = null
    private var game: Game? = null
    private var controller: SudokuController? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the Builder class for convenient dialog construction
        val activity = activity as SudokuActivity
        sl = activity.sudokuLayout
        game = activity.game
        controller = activity.sudokuController
        val itemStack = Stack<CharSequence?>()
        itemStack.addAll(
            listOf(
                getString(R.string.sf_sudoku_assistances_solve_surrender),
                getString(R.string.sf_sudoku_assistances_back_to_valid_state),
                getString(R.string.sf_sudoku_assistances_back_to_bookmark),
                getString(R.string.sf_sudoku_assistances_check),
                getString(R.string.sf_sudoku_assistances_solve_random)
            )
        )
        val v = (getActivity() as SudokuActivity).currentCellView
        if (v != null && v.cell.isNotSolved) itemStack.add(getString(R.string.sf_sudoku_assistances_solve_specific))
        val profilesDir = activity.getDir(
            getString(R.string.path_rel_profiles),
            Context.MODE_PRIVATE
        )
        val p = ProfileSingleton.getInstance(profilesDir, ProfileRepo(profilesDir),
                                             ProfilesListRepo(profilesDir))
        if (p.getAssistance(Assistances.provideHints)) itemStack.add(getString(R.string.sf_sudoku_assistances_give_hint))
        if (p.appSettings.isDebugSet) itemStack.add(getString(R.string.sf_sudoku_assistances_crash))

        // TODO why this no work? final CharSequence[] items = (CharSequence[]) itemStack.toArray();
        val items = itemStack.toTypedArray()
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(getString(R.string.sf_sudoku_assistances_title))
        builder.setItems(items) { dialog, item ->
            when (item) {
                0 -> if (!controller!!.onSolveAll()) Toast.makeText(
                    activity,
                    R.string.toast_solved_wrong,
                    Toast.LENGTH_SHORT
                ).show()
                1 -> game!!.goToLastCorrectState()
                2 -> game!!.goToLastBookmark()
                3 -> if (game!!.checkSudoku()) Toast.makeText(
                    activity,
                    R.string.toast_solved_correct,
                    Toast.LENGTH_SHORT
                ).show() else Toast.makeText(
                    activity,
                    R.string.toast_solved_wrong,
                    Toast.LENGTH_LONG
                ).show()
                4 -> if (!controller!!.onSolveOne()) Toast.makeText(
                    activity,
                    R.string.toast_solved_wrong,
                    Toast.LENGTH_SHORT
                ).show()
            }
            /* not inside switch, because they are at variable positions */
            if (items[item] === getString(R.string.sf_sudoku_assistances_solve_specific)) {
                if (!controller!!.onSolveCurrent(activity.currentCellView!!.cell)) {
                    Toast.makeText(activity, R.string.toast_solved_wrong, Toast.LENGTH_SHORT).show()
                }
            } else if (items[item] === getString(R.string.sf_sudoku_assistances_give_hint)) {
                hint(activity)
            } else if (items[item] === getString(R.string.sf_sudoku_assistances_crash)) {
                throw RuntimeException("This is a crash the user requested")
            }
            // Note: In Compose UI, button updates are handled automatically through state management
            // activity.panel!!.updateButtons() - No longer needed
        }
        return builder.create()
    }

    private fun hint(activity: SudokuActivity) {
        val sd = giveAHint(game!!.sudoku!!)
        activity.setModeHint()
        sl!!.hintPainter.realizeHint(sd)
        sl!!.hintPainter.invalidateAll()

        // Resolve ComposeView safely; fall back to creating one under hintPanel when ID is unavailable
        val composeViewId = activity.resources.getIdentifier("hintComposeView", "id", activity.packageName)
        val composeView = if (composeViewId != 0)
            activity.findViewById<ComposeView>(composeViewId)
        else null
        val safeComposeView = composeView ?: run {
            val panelId = activity.resources.getIdentifier("hintPanel", "id", activity.packageName)
            val parent = if (panelId != 0) activity.findViewById<View>(panelId) as? android.widget.LinearLayout else null
            val cv = ComposeView(activity)
            cv.layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
            parent?.addView(cv)
            cv
        }
        safeComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    HintPanelContent(
                        text = getText(activity, sd).toString(),
                        showExecute = sd.hasActionListCapability(),
                        onContinue = {
                            activity.setModeRegular()
                            sl!!.hintPainter.deleteAll()
                            sl!!.invalidate()
                            sl!!.hintPainter.invalidateAll()
                        },
                        onExecute = {
                            activity.setModeRegular()
                            sl!!.hintPainter.deleteAll()
                            sl!!.invalidate()
                            sl!!.hintPainter.invalidateAll()
                            previewAndExecuteHintActions(activity, sd)
                        }
                    )
                }
            }
        }
    }

    /**
     * Shows a red indicator preview on all target cells, then executes actions sequentially.
     * This improves clarity for bulk note deletions (e.g., X-Wing), similar to auto-fill.
     */
    private fun previewAndExecuteHintActions(activity: SudokuActivity, sd: de.sudoq.model.solverGenerator.solution.SolveDerivation) {
        val actions = sd.getActionList(game!!.sudoku!!)
        val layout = activity.sudokuLayout ?: return
        val painter = de.sudoq.controller.sudoku.board.CellViewPainter.instance ?: return

        // 1) Preview: small red indicator boxes on each affected cell, aligned to the note when possible
        for (a in actions) {
            try {
                val cell = a.cell
                val pos = game!!.sudoku!!.getPosition(cell.id) ?: continue
                val cellView = layout.getSudokuCellView(pos)
                // Compute preview symbol for alignment:
                // - NoteAction: use note index directly
                // - SolveAction: use final value = currentValue + diff
                val symbolStr = when (a) {
                    is de.sudoq.model.actionTree.NoteAction -> de.sudoq.controller.sudoku.Symbol.getInstance().getMapping(a.diff)
                    is de.sudoq.model.actionTree.SolveAction -> {
                        val absIdx = (a.cell.currentValue + a.diff).coerceAtLeast(0)
                        de.sudoq.controller.sudoku.Symbol.getInstance().getMapping(absIdx)
                    }
                    else -> null
                }
                painter.showPreFillIndicator(cellView, android.graphics.Color.RED, symbolStr)
            } catch (_: Exception) { /* best-effort preview */ }
        }

        // 2) Execute: one-by-one with short delay, removing indicator for each cell
        val delayPerAction = 160L
        var index = 0
        val execRunnable = object : Runnable {
            override fun run() {
                if (index >= actions.size) return
                val a = actions[index]
                try {
                    controller!!.onHintAction(a)
                    activity.onInputAction()
                    activity.mediator!!.restrictCandidates()
                    // Hide indicator for this cell after applying
                    val pos = game!!.sudoku!!.getPosition(a.cell.id)
                    if (pos != null) {
                        val cellView = layout.getSudokuCellView(pos)
                        painter.hidePreFillIndicator(cellView)
                    }
                } catch (_: Exception) { /* continue */ }

                index++
                if (index < actions.size) {
                    layout.postDelayed(this, delayPerAction)
                }
            }
        }
        layout.postDelayed(execRunnable, delayPerAction)
    }

    @Composable
    private fun HintPanelContent(
        text: String,
        showExecute: Boolean,
        onContinue: () -> Unit,
        onExecute: () -> Unit
    ) {
        val scrollState = rememberScrollState()
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF282828))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(4.dp)
                ) {
                    Text(text = text, color = Color.White)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                ) {
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(id = R.string.hint_panel_continue))
                    }
                    if (showExecute) {
                        Spacer(modifier = Modifier.size(12.dp))
                        Button(onClick = onExecute, modifier = Modifier.fillMaxWidth()) {
                            Text(text = stringResource(id = R.string.hint_panel_execute))
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF282828))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(4.dp)
                    ) {
                        Text(text = text, color = Color.White)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onContinue, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.hint_panel_continue))
                    }
                    if (showExecute) {
                        Button(onClick = onExecute, modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.hint_panel_execute))
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        //this is added to prevent dialog from disappearing on orientation change.
        // http://stackoverflow.com/a/12434038/3014199
        //it fixes a bug in the supportLibrary
        if (dialog != null && retainInstance) {
            dialog?.setDismissMessage(null)
        }
        super.onDestroyView()
    }
}

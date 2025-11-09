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
            activity.panel!!.updateButtons()
        }
        return builder.create()
    }

    private fun hint(activity: SudokuActivity) {
        val sd = giveAHint(game!!.sudoku!!)
        activity.setModeHint()
        sl!!.hintPainter.realizeHint(sd)
        sl!!.hintPainter.invalidateAll()

        val composeView = activity.findViewById<ComposeView>(R.id.hintComposeView)
        composeView.apply {
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
                            for (a in sd.getActionList(game!!.sudoku!!)) {
                                controller!!.onHintAction(a)
                                activity.onInputAction()
                                // in case we delete a note in the focussed cell
                                activity.mediator!!.restrictCandidates()
                            }
                        }
                    )
                }
            }
        }
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

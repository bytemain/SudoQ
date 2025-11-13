/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.sudoku

import android.app.AlertDialog
import android.content.Intent
import android.gesture.GestureOverlayView
import android.gesture.GestureStore
import android.graphics.Bitmap.CompressFormat
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.widget.Toolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.delay
import de.sudoq.R
import de.sudoq.controller.SudoqCompatActivity
import de.sudoq.controller.menus.Utility
import de.sudoq.controller.menus.preferences.PlayerPreferencesActivity
import de.sudoq.controller.sudoku.CellInteractionListener.SelectEvent
import de.sudoq.controller.sudoku.board.CellViewPainter.Companion.instance
import de.sudoq.controller.sudoku.board.CellViewStates
import de.sudoq.model.actionTree.ActionTree
import de.sudoq.model.actionTree.ActionTreeElement
import de.sudoq.model.actionTree.NoteAction
import de.sudoq.model.actionTree.SolveAction
import de.sudoq.model.game.Assistances
import de.sudoq.model.game.Game
import de.sudoq.model.game.GameManager
import de.sudoq.model.persistence.xml.game.IGamesListRepo
import de.sudoq.model.profile.ProfileSingleton
import de.sudoq.model.profile.ProfileManager
import de.sudoq.model.sudoku.Cell
import de.sudoq.model.sudoku.Position
import de.sudoq.persistence.game.GameRepo
import de.sudoq.persistence.game.GamesListRepo
import de.sudoq.persistence.profile.ProfileRepo
import de.sudoq.persistence.profile.ProfilesListRepo
import de.sudoq.persistence.sudokuType.SudokuTypeRepo
import de.sudoq.view.*
import java.io.*
import kotlin.math.abs

/**
 * Diese Klasse stellt die Activity des Sudokuspiels dar. Die Klasse hält das
 * Game und mehrere Controller um auf Interaktionen des Benutzers mit dem
 * Spielfeld zu reagieren. Die Klasse wird außerdem benutzt um zu verwalten,
 * welche Navigationselemente dem Nutzer angezeigt werden.
 */
class SudokuActivity : SudoqCompatActivity(), View.OnClickListener, ActionListener,
    ActionTreeNavListener {

    private lateinit var profilesFile: File
    private lateinit var sudokuFile: File

    private lateinit var sudokuTypeRepo: SudokuTypeRepo
    private lateinit var gameManager: GameManager

    /**
     * Eine Referenz auf einen ActionTreeController, der die Verwaltung der
     * ActionTree-Anzeige und Benutzerinteraktion übernimmt
     */
    var actionTreeController: ActionTreeController? = null
        private set

    /**
     * Eine Referenz auf einen SudokuController, der Nutzereingaben verwaltet
     * und mit dem Model interagiert
     */
    var sudokuController: SudokuController? = null
        private set
    /**
     * JUST FOR TESTING PURPOSE!
     *
     * @return Das SudokuLayout des aktuellen Spiels
     */
    /**
     * Die View des aktuellen Sudokus
     */
    var sudokuLayout: SudokuLayout? = null
        private set

    /**
     * Die ScrollView, welche die SudokuView beinhaltet
     */
    private var sudokuScrollView: FullScrollLayout? = null
    /**
     * Gibt das aktuelle Game zurück.
     *
     * @return Das Game
     */
    /**
     * Das Game, auf welchem gerade gespielt wird
     */
    var game: Game? = null
        private set

    /**
     * Fängt Gesteneingaben des Benutzers ab
     */
    private var gestureOverlay: GestureInputOverlay? = null

    /**
     * Hält die von der Activity unterstützten Gesten
     */
    private val gestureStore = GestureStore()
    /**
     * Gibt zurück, ob zurzeit der ActionTree angezeigt wird.
     *
     * @return true, falls der ActionTree gerade angezeigt wird, false falls
     * nicht
     */
    /**
     * Ein Flag welches aussagt, ob gerade der ActionTree angezeigt wird
     */
    var isActionTreeShown = false
        private set

    private enum class Mode {
        Regular, HintMode
    }

    private var mode =
        Mode.Regular //TODO see that this gets saved in oninstancesaved and restored so hint persitst orientation change

    /**
     * Hint state for Compose UI
     */
    data class HintState(
        val text: String,
        val hasExecute: Boolean,
        val onContinue: () -> Unit,
        val onExecute: () -> Unit
    )
    
    private var currentHintState: HintState? = null
    
    /**
     * Trigger to force keyboard update in Compose
     */
    private var keyboardUpdateTrigger = mutableStateOf(0)
    
    /**
     * Request keyboard update (call this when cell selection changes)
     */
    fun requestKeyboardUpdate() {
        keyboardUpdateTrigger.value++
    }

    /**
     * Der Handler für die Zeit
     */
    private val timeHandler = Handler()
    //TODO this.finished vs game.finished, which is what
    /**
     * Zeigt an, dass dieses Spiel beendet wurde
     */
    var finished = false
        private set

    /**
     * Der Vermittler zwischen Sudoku und Eingabemöglichkeiten
     */
    var mediator: UserInteractionMediator? = null
        private set

    private lateinit var currentSymbolSet: Array<String>

    /** for time. YES IT IS USED! */
    private var mMenu: Menu? = null

    /** Methods  */
    private fun initializeSymbolSet() {
        currentSymbolSet = when (game!!.sudoku!!.sudokuType!!.numberOfSymbols) {
            4 -> Symbol.MAPPING_NUMBERS_FOUR
            6 -> Symbol.MAPPING_NUMBERS_SIX
            9 -> Symbol.MAPPING_NUMBERS_NINE
            16 -> Symbol.MAPPING_NUMBERS_HEX_LETTERS
            else -> Symbol.MAPPING_NUMBERS_HEX_LETTERS
        }
        Symbol.createSymbol(currentSymbolSet)
    }

    var panel: ControlPanelFragment? = null
        private set

    /**
     * Wird beim ersten Aufruf der Activity aufgerufen. Setzt das Layout der
     * Activity und nimmt Initialisierungen vor.
     *
     * @param savedInstanceState
     * Gespeicherte Daten eines vorigen Aufrufs dieser Activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "Created")

        profilesFile = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        sudokuFile = getDir(getString(R.string.path_rel_sudokus), MODE_PRIVATE)
        sudokuTypeRepo = SudokuTypeRepo(sudokuFile)
        val pm = ProfileManager(profilesFile, ProfileRepo(profilesFile),
                                ProfilesListRepo(profilesFile))
        ///init params for game*repos
        pm.loadCurrentProfile()
        //todo: pass externally initialized object to constructor
        val gameRepo = GameRepo(
            pm.profilesDir!!,
            pm.currentProfileID,
            sudokuTypeRepo)
        val gamesFile = File(pm.currentProfileDir, "games.xml")
        val gamesDir = File(pm.currentProfileDir, "games")

        val gamesListRepo : IGamesListRepo = GamesListRepo(gamesDir, gamesFile)
        gameManager = GameManager(pm, gameRepo, gamesListRepo, sudokuTypeRepo)

        // Load the Game by using current game id
        if (savedInstanceState != null) {
            try {
                game = gameManager.load(savedInstanceState.getInt(SAVE_GAME_ID.toString() + ""))
            } catch (e: Exception) {
                finish()
            }
        } else {
            pm.loadCurrentProfile()
            game = gameManager.load(pm.currentGame)
        }

        if (game != null) {
            // Update game assistances with current profile settings
            // This ensures that when user changes settings and continues a game,
            // the new settings take effect
            pm.loadCurrentProfile()
            game!!.setAssistances(pm.assistances)
            Log.d(LOG_TAG, "Updated game assistances from current profile")

            /* Determine how many numbers are needed. 1-9 or 1-16 ? */
            initializeSymbolSet()
            
            // Initialize controllers
            sudokuController = SudokuController(game!!, this)
            // TODO: ActionTreeController needs to be reimplemented for Compose
            // actionTreeController = ActionTreeController(this)
            actionTreeController = null
            Log.d(LOG_TAG, "Initialized")
            
            // Initialize views with proper styling
            sudokuLayout = SudokuLayout(this).apply {
                gravity = Gravity.CENTER
            }
            // Apply CellViewPainter marking to sudokuLayout
            instance!!.setMarking(sudokuLayout!!, CellViewStates.SUDOKU)
            
            val keyboardView = VirtualKeyboardLayout(this, null).apply {
                // Apply keyboard layout mode from profile settings
                layoutMode = when (pm.appSettings.keyboardLayoutMode) {
                    "horizontal" -> VirtualKeyboardLayout.KeyboardLayoutMode.HORIZONTAL
                    else -> VirtualKeyboardLayout.KeyboardLayoutMode.GRID
                }
                Log.d(LOG_TAG, "Keyboard layout mode: $layoutMode")
                Log.d(LOG_TAG, "Refreshing keyboard with ${game!!.sudoku!!.sudokuType!!.numberOfSymbols} symbols")
                refresh(game!!.sudoku!!.sudokuType!!.numberOfSymbols)
                // Activate keyboard to make buttons visible
                Log.d(LOG_TAG, "Activating keyboard buttons")
                setActivated(true)
                Log.d(LOG_TAG, "Keyboard setup completed")
            }
            // Apply CellViewPainter marking to keyboard
            instance!!.setMarking(keyboardView, CellViewStates.KEYBOARD)
            
            inflateGestures(savedInstanceState == null)
            Log.d(LOG_TAG, "Inflated gestures")
            
            // Use Compose for UI
            setContent {
                MaterialTheme {
                    var hintState by remember { mutableStateOf<HintState?>(null) }
                    var keyboardButtons by remember { mutableStateOf<List<KeyboardButtonState>>(emptyList()) }
                    val keyboardTrigger by keyboardUpdateTrigger
                    
                    // Update hint state when currentHintState changes
                    LaunchedEffect(currentHintState) {
                        hintState = currentHintState
                    }
                    
                    // Initialize keyboard buttons
                    LaunchedEffect(Unit) {
                        keyboardButtons = getKeyboardButtonStates()
                    }
                    
                    // Update keyboard when trigger changes
                    LaunchedEffect(keyboardTrigger) {
                        if (keyboardTrigger > 0) {
                            keyboardButtons = getKeyboardButtonStates()
                        }
                    }
                    
                    val gameState = remember {
                        mutableStateOf(
                            SudokuGameState(
                                game = game!!,
                                isActionTreeShown = isActionTreeShown,
                                isFinished = finished,
                                elapsedTime = game!!.time.toLong() * 1000,
                                hintText = hintState?.text,
                                hintHasExecute = hintState?.hasExecute ?: false,
                                onHintContinue = hintState?.onContinue,
                                onHintExecute = hintState?.onExecute,
                                keyboardButtons = keyboardButtons
                            )
                        )
                    }
                    
                    // Update game state when hint state changes
                    LaunchedEffect(hintState) {
                        gameState.value = gameState.value.copy(
                            hintText = hintState?.text,
                            hintHasExecute = hintState?.hasExecute ?: false,
                            onHintContinue = hintState?.onContinue,
                            onHintExecute = hintState?.onExecute
                        )
                    }
                    
                    // Update game state when keyboard buttons change
                    LaunchedEffect(keyboardButtons) {
                        gameState.value = gameState.value.copy(
                            keyboardButtons = keyboardButtons
                        )
                    }
                    
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(1000)
                            if (!finished) {
                                gameState.value = gameState.value.copy(
                                    elapsedTime = game!!.time.toLong() * 1000
                                )
                            }
                        }
                    }
                    
                    SudokuScreen(
                        state = gameState.value,
                        sudokuLayout = sudokuLayout!!,
                        onBackClick = {
                            onBackPressed()
                        },
                        onMenuClick = { menuItem ->
                            when (menuItem) {
                                SudokuMenuItem.Settings -> {
                                    startActivity(Intent(this@SudokuActivity, PlayerPreferencesActivity::class.java))
                                }
                                SudokuMenuItem.NewGame -> {
                                    // Go back to main menu for new game
                                    finish()
                                }
                                SudokuMenuItem.Gestures -> {
                                    // Show gesture management
                                    Toast.makeText(this@SudokuActivity, "Gesture management coming soon", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onActionTreeToggle = {
                            toogleActionTree()
                            gameState.value = gameState.value.copy(isActionTreeShown = isActionTreeShown)
                        },
                        onUndoClick = {
                            sudokuController!!.onUndo()
                            keyboardButtons = getKeyboardButtonStates()
                            updateButtons()
                        },
                        onRedoClick = {
                            sudokuController!!.onRedo()
                            keyboardButtons = getKeyboardButtonStates()
                            updateButtons()
                        },
                        onHintClick = {
                            showAssistancesDialog()
                        },
                        onSolveClick = {
                            // Fill all candidates
                            sudokuController!!.fillAllCandidates()
                            keyboardButtons = getKeyboardButtonStates()
                            updateButtons()
                            Toast.makeText(this@SudokuActivity, R.string.sf_sudoku_fill_candidates_success, Toast.LENGTH_SHORT).show()
                        },
                        onNoteToggle = {
                            mediator?.toggleNoteMode()
                            keyboardButtons = getKeyboardButtonStates()
                        },
                        onKeyboardInput = { symbol ->
                            mediator?.onInput(symbol)
                            keyboardButtons = getKeyboardButtonStates()
                            updateButtons()
                        }
                    )
                }
            }
            
            Log.d(LOG_TAG, "Set up Compose UI")
            
            // Continue with mediator setup
            mediator = UserInteractionMediator(
                keyboardView,
                sudokuLayout,
                game,
                gestureOverlay,
                gestureStore
            )
            mediator!!.registerListener(sudokuController!!)
            mediator!!.registerListener(this)
            
            // Set up callback for keyboard updates in Compose
            mediator!!.onKeyboardUpdateNeeded = {
                requestKeyboardUpdate()
            }
            
            // Set up debug logger for auto-fill and model classes
            val debugLoggerFn: (String, String, Boolean) -> Unit = { tag, message, isError ->
                if (isError) {
                    Log.e(tag, message)
                } else {
                    Log.d(tag, message)
                }
            }
            
            game!!.setDebugLogger(debugLoggerFn)
            de.sudoq.model.sudoku.Cell.debugLogger = debugLoggerFn
            de.sudoq.model.actionTree.Action.debugLogger = debugLoggerFn
            
            // Configure animated auto-fill of unique candidates: schedule steps and highlight cells
            game!!.setAutoFillScheduler { delayMs, action ->
                // Use the view to post on UI thread with delay
                sudokuLayout!!.postDelayed(action, delayMs)
            }
            game!!.setAutoFillListener { cell ->
                runOnUiThread {
                    val pos = game!!.sudoku!!.getPosition(cell.id)!!
                    val cellView = sudokuLayout!!.getSudokuCellView(pos)
                    // Determine the symbol that will be filled to align the red box precisely
                    val abstractIdx = cell.getSingleNote()
                    val symbolStr = if (abstractIdx >= 0) Symbol.getInstance().getMapping(abstractIdx) else ""
                    // Show a small red indicator box without changing selection, aligned to glyph
                    instance!!.showPreFillIndicator(cellView, symbol = symbolStr)
                }
            }
            game!!.setAutoFillAfterListener { cell ->
                // Fade-in the filled number by animating text alpha from 0 to 255
                val pos = game!!.sudoku!!.getPosition(cell.id)!!
                val cellView = sudokuLayout!!.getSudokuCellView(pos)
                val painter = instance!!
                // Remove pre-fill indicator
                painter.hidePreFillIndicator(cellView)
                // start from transparent
                painter.setTextAlpha(cellView, 0)
                val totalSteps = 10
                val stepDelay = 24L
                var step = 0
                val fadeRunnable = object : Runnable {
                    override fun run() {
                        step++
                        val alpha = ((255L * step) / totalSteps).toInt()
                        painter.setTextAlpha(cellView, alpha)
                        if (step < totalSteps) {
                            cellView.postDelayed(this, stepDelay)
                        } else {
                            painter.clearTextAlpha(cellView)
                        }
                    }
                }
                cellView.post(fadeRunnable)
            }

            // After a non-recursive batch completes, bounce any fully-solved constraints in scope
            game!!.setAutoFillBatchCompleteListener { origin, scope, filledPositions ->
                runOnUiThread {
                    try {
                        val sudoku = game!!.sudoku!!
                        // Collect constraints to check: only those that include one of the newly filled positions
                        val constraintsToCheck = mutableSetOf<de.sudoq.model.sudoku.Constraint>()
                        val type = sudoku.sudokuType!!
                        val filledSet = filledPositions.toSet()
                        for (c in type) {
                            var intersects = false
                            for (p in c) {
                                if (filledSet.contains(p)) { intersects = true; break }
                            }
                            if (intersects) constraintsToCheck.add(c)
                        }

                        fun allSolved(c: de.sudoq.model.sudoku.Constraint): Boolean {
                            for (p in c) {
                                val cell = sudoku.getCell(p) ?: return false
                                if (cell.isNotSolved) return false
                            }
                            return true
                        }

                        for (c in constraintsToCheck) {
                            if (allSolved(c)) {
                                // Bounce all views in this constraint (stronger amplitude)
                                // Use dp-based amplitude for consistency across sizes
                                val density = resources.displayMetrics.density
                                val dy = -12f * density
                                val duration = 100L
                                for (p in c) {
                                    val v = sudokuLayout!!.getSudokuCellView(p)
                                    v.animate().translationY(dy).setDuration(duration).withEndAction {
                                        v.animate().translationY(0f).setDuration(duration).start()
                                    }.start()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("AUTO_FILL_ANIM", "Constraint bounce animation failed: $e")
                    }
                }
            }

            // Ensure finishing is triggered also when last steps are auto-filled
            game!!.setGameFinishedListener {
                runOnUiThread {
                    if (!finished) {
                        Log.d(LOG_TAG, "Game finished via GameFinishedListener, updating statistics")
                        
                        // Update statistics before showing dialog
                        sudokuController!!.updateStatisticsAndSave()
                        
                        finished = true
                        updateButtons()
                        // Keep current selection highlight consistent
                        sudokuLayout!!.currentCellView?.select(game!!.isAssistanceAvailable(Assistances.markRowColumn))
                        // Stop timer and show win dialog
                        timeHandler.removeCallbacks(timeUpdate)
                        showWinDialog(surrendered = false)
                    }
                }
            }
            if (game!!.isFinished()) {
                setFinished(showWinDialog = false, surrendered = false)
            } else {
                //find the current cell from when the game was saved and mark it selected
                val lastAction = game!!.currentState.action

                fun getCellView(cellId: Int): SudokuCellView {
                    val currentPosition = game!!.sudoku!!.getPosition(cellId)!!
                    return sudokuLayout!!.getSudokuCellView(currentPosition)
                }

                when (lastAction) {
                    // if no action
                    is ActionTree.MockAction -> { /* */}
                    is SolveAction -> {
                        val currentCellView = getCellView(lastAction.cell.id)
                        currentCellView.programmaticallySelectShort()}
                    is NoteAction -> {
                        val currentCellView = getCellView(lastAction.cell.id)
                        currentCellView.programmaticallySelectShort()}
                    else -> Log.e("GAME_RESTORE", "last action of unknown type")
                }

            }
            updateButtons()
        }
    }

    class MyGlobalLayoutListener(
        private val activity: SudokuActivity,
        private val savedInstanceState: Bundle?): OnGlobalLayoutListener {

        override fun onGlobalLayout() {
            Log.d(LOG_TAG, "SudokuView height: $activity.sudokuLayout!!.measuredHeight")
            Log.d(LOG_TAG, "SudokuScrollView height: $activity.sudokuScrollView!!.measuredHeight")
            activity.sudokuLayout!!.optiZoom(
                activity.sudokuScrollView!!.measuredWidth,
                activity.sudokuScrollView!!.measuredHeight
            )
            val obs = activity.sudokuLayout!!.viewTreeObserver
            if (savedInstanceState != null) {
                val zoomFactor =
                    savedInstanceState.getFloat(SAVE_ZOOM_FACTOR.toString() + "")
                if (zoomFactor != 0.0f) {
                    activity.sudokuLayout!!.zoom(zoomFactor)
                    activity.sudokuScrollView!!.zoomFactor = zoomFactor
                }
                val scrollX = savedInstanceState.getFloat(SAVE_SCROLL_X.toString()) +
                        activity.sudokuLayout!!.currentLeftMargin
                val scrollY = savedInstanceState.getFloat(SAVE_SCROLL_Y.toString()) +
                        activity.sudokuLayout!!.currentTopMargin
                activity.sudokuScrollView!!.scrollTo(scrollX.toInt(), scrollY.toInt())

            }
            obs.removeGlobalOnLayoutListener(this)
        }
    }

    /**
     * Speichert das markierte Feld und die Status des Aktionsbaumes, um bei
     * Wiederherstellung der Activity nach einem Orientierungswechsel oder
     * aufgrund einer temporären Verdrängung durch Speicherknappheit den alten
     * Status wiederherzustellen.
     *
     * @param outState
     * Der Status in den gespeichert wird
     */
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Note: In Compose UI, we don't use sudokuScrollView anymore
        // Zoom and scroll state management needs to be reimplemented for Compose if needed
        outState.putBoolean(SAVE_ACTION_TREE_SHOWN.toString(), isActionTreeShown)
        outState.putInt(SAVE_GAME_ID.toString(), game!!.id)
        outState.putBoolean(
            SAVE_GESTURE_ACTIVE.toString(),
            gestureOverlay != null && gestureOverlay!!.visibility == View.VISIBLE
        )
        if (sudokuLayout!!.currentCellView != null) {
            val position = game!!.sudoku!!.getPosition(sudokuLayout!!.currentCellView!!.cell.id)!!
            outState.putInt(SAVE_FIELD_X.toString(), position.x)
            outState.putInt(SAVE_FIELD_Y.toString(), position.y)
        } else {
            outState.putInt(SAVE_FIELD_X.toString(), -1)
        }
        Log.d(LOG_TAG, "Saved state")
    }

    /**
     * Stellt den Status der Activity wieder her, also insbesondere das
     * markierte Feld und den Status der Aktionsbaumes.
     *
     * @param state
     * Der wiederherzustellende Status
     */
    public override fun onRestoreInstanceState(state: Bundle) {
        if (state.getBoolean(SAVE_ACTION_TREE_SHOWN.toString())) {
            val vto = sudokuLayout!!.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    toogleActionTree()
                    val obs = sudokuLayout!!.viewTreeObserver
                    obs.removeGlobalOnLayoutListener(this)
                }
            })
        }
        if (state.getInt(SAVE_FIELD_X.toString()) != -1) {
            //save_field_x not being -1 means the last sudoku hat a cell selected
            //this cell shall be selected again
            val currentPosition = Position[
                    state.getInt(SAVE_FIELD_X.toString()),
                    state.getInt(SAVE_FIELD_Y.toString())]

            sudokuLayout!!.getSudokuCellView(currentPosition).programmaticallySelectShort()
            //todo kann man hier auch mit dem letztem Blatt im ActionTree arbeiten, so wie oben in GlobalLayoutListener?

        }
        if (state.getBoolean(SAVE_GESTURE_ACTIVE.toString())) {
            mediator!!.onCellSelected(sudokuLayout!!.currentCellView!!, SelectEvent.Short)
        }
        if (mode == Mode.HintMode) {
            // Note: In Compose UI, these views don't exist
            findViewById<View>(R.id.controlPanel)?.visibility = View.GONE
            findViewById<View>(R.id.hintPanel)?.visibility = View.VISIBLE
        }
        Log.d(LOG_TAG, "Restored state")
    }

    /**
     * Erzeugt die View für die Gesteneingabe
     *
     * @param firstStart
     * Gibt an, ob dies der erste Start der Activity ist und somit
     * Hinweise angezeigt werden sollen
     */
    private fun inflateGestures(firstStart: Boolean) {
        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val p = ProfileSingleton.getInstance(profilesDir, ProfileRepo(profilesDir),
                                             ProfilesListRepo(profilesDir))
        val gestureFile = p.getCurrentGestureFile()
        try {
            val fis = FileInputStream(gestureFile)
            gestureStore.load(fis)
            fis.close()
        } catch (e: FileNotFoundException) {
            try {
                val os: OutputStream = FileOutputStream(gestureFile)
                gestureStore.save(os)
            } catch (ioe: IOException) {
                Log.w(LOG_TAG, "Gesture file cannot be loaded!")
            }
        } catch (e: IOException) {
            p.isGestureActive = false
            Toast.makeText(this, R.string.error_gestures_no_library, Toast.LENGTH_SHORT).show()
        }
        if (firstStart && p.isGestureActive) {
            val allGesturesSet = checkGesture()
            if (!allGesturesSet) {
                p.isGestureActive = false
                Toast.makeText(
                    this,
                    getString(R.string.error_gestures_not_complete),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        // TODO: Gesture overlay needs to be integrated with Compose
        gestureOverlay = GestureInputOverlay(this)
        // Can't add to frame layout since we're using Compose
        // val frameLayout = findViewById<FrameLayout>(R.id.sudoku_frame_layout)
        // frameLayout.addView(gestureOverlay)
    }

    /**
     * Erstellt die Views und Buttons für diese Activity
     */
    private fun inflateViewAndButtons() {
        sudokuScrollView = findViewById(R.id.sudoku_cell)
        sudokuLayout = SudokuLayout(this)
        Log.d(LOG_TAG, "Inflated sudoku layout")
        sudokuLayout!!.gravity = Gravity.CENTER
        sudokuScrollView!!.addView(sudokuLayout!!)
        panel =
            supportFragmentManager.findFragmentById(R.id.controlPanelFragment) as ControlPanelFragment
        panel!!.initialize()
        panel!!.inflateButtons()
        var currentControlsView: LinearLayout? /* = (LinearLayout) findViewById(R.id.sudoku_time_border);
		FieldViewPainter.getInstance().setMarking(currentControlsView, FieldViewStates.CONTROLS);*/
        currentControlsView = findViewById(R.id.sudoku_border)
        instance!!.setMarking(currentControlsView, CellViewStates.SUDOKU)
        currentControlsView = findViewById(R.id.controls)
        instance!!.setMarking(currentControlsView, CellViewStates.KEYBOARD)
        val keyboardView = findViewById<VirtualKeyboardLayout>(R.id.virtual_keyboard)
        instance!!.setMarking(keyboardView, CellViewStates.KEYBOARD)
        keyboardView.refresh(game!!.sudoku!!.sudokuType!!.numberOfSymbols)
    }

    /**
     * Schaltet den ActionTree an bzw. aus.
     */
    fun toogleActionTree() {
        // TODO: ActionTree needs Compose implementation
        if (actionTreeController == null) {
            Toast.makeText(this, "Action Tree not available in Compose UI yet", Toast.LENGTH_SHORT).show()
            return
        }
        isActionTreeShown = !isActionTreeShown //toggle value
        actionTreeController?.setVisibility(isActionTreeShown) //update AT-Controller
        updateButtons()
    }

    /**
     * Behandelt die Klicks auf Buttons dieser Activity
     * Note: In Compose UI, button clicks are handled through lambda callbacks
     */
    override fun onClick(v: View) {
        // panel!!.onClick(v) - No longer used in Compose UI
        updateButtons()
    }

    /**
     * returns whether all Gestures are defined -> Gesture input possible
     */
    fun checkGesture(): Boolean {
        val symbolSet = Symbol.getInstance().symbolSet!!
        val gestures = gestureStore.gestureEntries

        return symbolSet.all { gestures.contains(it) }
    }

    /**
     * Wird aufgerufen, falls die Activity in den Vordergrund der App gelangt.
     */
    public override fun onResume() {
        super.onResume()
        
        // Reload profile settings to apply any changes made in preferences
        if (game != null) {
            val pm = ProfileManager(profilesFile, ProfileRepo(profilesFile), ProfilesListRepo(profilesFile))
            pm.loadCurrentProfile()
            game!!.setAssistances(pm.assistances)
            Log.d(LOG_TAG, "Reloaded game assistances from profile in onResume")
        }
        
        if (!finished) timeHandler.postDelayed(timeUpdate, 1000)
    }

    /**
     * Wird aufgerufen, falls eine andere Activity in den Vordergrund der App
     * gelangt.
     */
    public override fun onPause() {
        val p = ProfileSingleton.getInstance(profilesFile, ProfileRepo(profilesFile),
                                             ProfilesListRepo(profilesFile))
        timeHandler.removeCallbacks(timeUpdate)
        //gameid = 1
        gameManager.save(game!!)
        //gameid = -1
        
        // Create thumbnail for game list
        sudokuLayout!!.isDrawingCacheEnabled = true
        // Restoring measurements for thumbnail capture
        sudokuLayout!!.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        sudokuLayout!!.layout(0, 0, sudokuLayout!!.measuredWidth, sudokuLayout!!.measuredHeight)
        sudokuLayout!!.buildDrawingCache(true)
        val sudokuCapture = sudokuLayout!!.drawingCache
        try {
            if (sudokuCapture != null) {
                val gameRepo = GameRepo(p.profilesDir!!, p.currentProfileID, sudokuTypeRepo)
                val thumbnail = gameRepo.getGameThumbnailFile(p.currentGame)
                sudokuCapture.compress(CompressFormat.PNG, 100, FileOutputStream(thumbnail))
            } else {
                Log.d(LOG_TAG, getString(R.string.error_thumbnail_get))
            }
        } catch (e: FileNotFoundException) {
            Log.w(LOG_TAG, getString(R.string.error_thumbnail_saved))
        }
        
        if (finished) {
            p.currentGame = ProfileManager.NO_GAME
            p.saveChanges()
        }
        super.onPause()
    }

    /**
     * Wird aufgerufen, falls die "Zurück"-Taste gedrückt wird.
     */
    override fun onBackPressed() {
        if (isActionTreeShown) {
            toogleActionTree()
        } else if (gestureOverlay!!.visibility == GestureOverlayView.VISIBLE) {
            gestureOverlay!!.visibility = GestureOverlayView.INVISIBLE
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Wird aufgerufen, falls die Activity terminiert.
     */
    override fun finish() {
        if (game != null) {
            gameManager.save(game!!)
        }
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    fun setModeHint() {
        mode = Mode.HintMode
    }

    fun setModeRegular() {
        mode = Mode.Regular
        currentHintState = null
    }
    
    /**
     * Get keyboard button states for Compose UI
     */
    private fun getKeyboardButtonStates(): List<KeyboardButtonState> {
        val buttons = mutableListOf<KeyboardButtonState>()
        val currentField = sudokuLayout!!.currentCellView
        val noteMode = mediator?.isNoteMode() ?: false
        
        if (game == null || game!!.sudoku == null) {
            return buttons
        }
        
        // Get restricted symbol set if input assistance is enabled
        val restrictedSymbols = if (currentField != null && game!!.isAssistanceAvailable(Assistances.restrictCandidates)) {
            mediator?.getRestrictedSymbolSet(game!!.sudoku, currentField.cell, noteMode) ?: emptySet()
        } else {
            null  // null means no restriction, all symbols available
        }
        
        for (i in game!!.sudoku!!.sudokuType!!.symbolIterator) {
            // Check if button should be enabled based on restrictCandidates
            val isRestricted = if (restrictedSymbols != null) {
                !restrictedSymbols.contains(i)
            } else {
                false  // No restriction when assistance is disabled
            }
            
            val isSelected = if (currentField != null) {
                if (noteMode) {
                    currentField.cell.isNoteSet(i)
                } else {
                    i == currentField.cell.currentValue
                }
            } else {
                false
            }
            
            // Check if the symbol is completed and show checkmark if the assistance is enabled
            val showCheckmarksEnabled = game!!.isAssistanceAvailable(Assistances.showCompletedDigits)
            val isCompleted = showCheckmarksEnabled && game!!.sudoku!!.isSymbolCompleted(i)
            
            val displayText = Symbol.getInstance().getMapping(i)
            
            buttons.add(
                KeyboardButtonState(
                    symbol = i,
                    displayText = displayText,
                    isEnabled = !finished && !isRestricted,  // Disable if game finished or restricted
                    showCheckmark = isCompleted
                )
            )
        }
        
        return buttons
    }
    
    /**
     * Shows hint in the keyboard area
     */
    fun showHint(text: String, hasExecute: Boolean, onContinue: () -> Unit, onExecute: () -> Unit) {
        currentHintState = HintState(text, hasExecute, onContinue, onExecute)
        setModeHint()
    }
    
    /**
     * Hides the hint panel
     */
    fun hideHint() {
        setModeRegular()
    }

    private var fm: FragmentManager = supportFragmentManager

    /**
     * Zeigt einen Dialog mit den verfügbaren Hilfestellungen an.
     */
    fun showAssistancesDialog() {
        val ad: DialogFragment = AssistancesDialogFragment()
        ad.show(fm, "assistancesDialog")
    }

    /** Die aktuell ausgewählte FieldView */
    var currentCellView: SudokuCellView?
        get() = sudokuLayout!!.currentCellView
        set(cellView) {
            sudokuLayout!!.currentCellView = cellView
        }

    /**
     * Setzt dieses Spiel auf beendet.
     *
     * @param showWinDialog
     * Spezifiziert, ob ein Gewinn-Dialog angezeigt werden soll
     * @param surrendered
     * Gibt an, ob der Spieler aufgegeben hat
     */
    fun setFinished(showWinDialog: Boolean, surrendered: Boolean) {
        finished = true
        updateButtons()
        sudokuLayout!!.currentCellView?.select(game!!.isAssistanceAvailable(Assistances.markRowColumn))

        // Disable keyboard input when game is finished
        val keyView = findViewById<VirtualKeyboardLayout>(R.id.virtual_keyboard)
        keyView.isEnabled = false
        for (i in 0 until keyView.childCount) {
            keyView.getChildAt(i).isEnabled = false
        }
        
        if (showWinDialog) showWinDialog(surrendered)
        timeHandler.removeCallbacks(timeUpdate)
    }

    private val assistancesTimeString: String
        get() = getTimeString(game!!.assistancesTimeCost)

    /**
     * Gibt die vergangene Zeit als formatierten String zurück.
     *
     * @return Den String für die Zeitanzeige
     */
    private val gameTimeString: String
        get() = getTimeString(game!!.time)

    /**
     * Zeigt einen Gewinndialog an, der fragt, ob das Spiel beendet werden soll.
     *
     * @param surrendered
     * Gibt an, ob der Spieler aufgegeben hat
     */
    private fun showWinDialog(surrendered: Boolean) {
        // Create a dialog using Jetpack Compose
        val dialogFragment = androidx.compose.ui.platform.ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    var showDialog by remember { mutableStateOf(true) }
                    
                    if (showDialog) {
                        WinDialog(
                            surrendered = surrendered,
                            timeNeeded = gameTimeString,
                            score = game!!.score,
                            onDismiss = {
                                showDialog = false
                                // Stay in the game
                            },
                            onFinish = {
                                showDialog = false
                                finish()
                            }
                        )
                    }
                }
            }
        }
        
        // Add the compose view to the window
        val decorView = window.decorView as? FrameLayout
        decorView?.addView(dialogFragment)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.action_bar_sudoku, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        mMenu = menu
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val settingsIntent = Intent(this, PlayerPreferencesActivity::class.java)
                startActivity(settingsIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Das Update-Runnable für die Zeit
     * Note: In Compose UI, time updates are handled by LaunchedEffect in SudokuScreen
     */
    private val timeUpdate: Runnable = object : Runnable {
        private val offset = StringBuilder()
        override fun run() {
            game!!.addTime(1)

            // No longer needed in Compose - time display is managed by Compose state
            // Time updates are handled in SudokuScreen's LaunchedEffect
            timeHandler.postDelayed(this, 1000)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onHoverTreeElement(ate: ActionTreeElement) {
        updateButtons()
    }

    /**
     * {@inheritDoc}
     */
    override fun onLoadState(ate: ActionTreeElement) {
        updateButtons()
    }

    /**
     * {@inheritDoc}
     */
    override fun onRedo() {
        updateButtons()
    }

    /**
     * {@inheritDoc}
     */
    override fun onUndo() {
        updateButtons()
    }

    /**
     * {@inheritDoc}
     */
    override fun onNoteAdd(cell: Cell, value: Int) {
        onInputAction()
    }

    /**
     * {@inheritDoc}
     */
    override fun onNoteDelete(cell: Cell, value: Int) {
        onInputAction()
    }

    /**
     * {@inheritDoc}
     */
    override fun onAddEntry(cell: Cell, value: Int) {
        onInputAction()
    }

    /**
     * {@inheritDoc}
     */
    override fun onDeleteEntry(cell: Cell) {
        onInputAction()
    }

    fun onInputAction() {
        updateButtons()
        saveActionTree()
    }

    private fun updateButtons() {
        // No longer needed in Compose - button states are managed by Compose state
        // panel!!.updateButtons()
    }

    /** saves the whole game, purpose: save the action tree so a spontaneous crash doesn't lose us actions record  */
    private fun saveActionTree() {
        gameManager.save(game!!)
    }

    companion object {
        /** Attributes  */
        /**
         * Der Log-TAG
         */
        private val LOG_TAG = SudokuActivity::class.java.simpleName

        /**
         * Konstante für das Speichern der Game ID
         */
        private const val SAVE_GAME_ID = 0 //TODO make enum

        /**
         * Konstante für das Speichern der X-Koordinate der ausgewählten FieldView
         */
        private const val SAVE_FIELD_X = 1

        /**
         * Konstante für das Speichern der Y-Koordinate der ausgewählten FieldView
         */
        private const val SAVE_FIELD_Y = 2

        /**
         * Konstante für das Speichern des Aktionsbaum-Status
         */
        private const val SAVE_ACTION_TREE_SHOWN = 3

        /**
         * Konstante für das Speichern der Gesteneingabe
         */
        private const val SAVE_GESTURE_ACTIVE = 4

        /**
         * Konstante für das Speichern des aktuellen Zoomfaktors
         */
        private const val SAVE_ZOOM_FACTOR = 5

        /**
         * Konstante für das Speichern des Scrollwertes in X-Richtung
         */
        private const val SAVE_SCROLL_X = 6

        /**
         * Konstante für das Speichern des Scrollwertes in Y-Richtung
         */
        private const val SAVE_SCROLL_Y = 7

        /**
         * Returns a string in the format "HH:mm:ss" implied by the specified time in seconds.
         * There is no zero-padding for Hours, instead the string is just shorter if hours is zero.
         * @param time the time to format in seconds
         * @return a string representing the specified time in format "D..D HH:mm:ss"
         */
        @JvmStatic
        fun getTimeString(time: Int): String {
            var time = time
            val seconds = time % 60
            time /= 60
            val minutes = time % 60
            time /= 60
            val hours = time % 24
            time /= 24
            val days = time
            val pattern = StringBuilder()
            if (days > 0) pattern.append(days).append(" ")
            if (hours > 0) pattern.append(String.format("%02d:", hours))
            pattern.append(String.format("%02d:%02d", minutes, seconds))
            return pattern.toString()
        }
    }
}

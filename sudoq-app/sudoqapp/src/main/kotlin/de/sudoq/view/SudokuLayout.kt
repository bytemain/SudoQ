/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import de.sudoq.controller.sudoku.CellInteractionListener
import de.sudoq.controller.sudoku.ObservableCellInteraction
import de.sudoq.controller.sudoku.SudokuActivity
import de.sudoq.controller.sudoku.board.BoardPainter
import de.sudoq.controller.sudoku.board.CellViewPainter
import de.sudoq.controller.sudoku.hints.HintPainter
import de.sudoq.model.game.Assistances
import de.sudoq.model.game.Game
import de.sudoq.model.sudoku.Constraint
import de.sudoq.model.sudoku.ConstraintType
import de.sudoq.model.sudoku.Position
import java.util.*

/**
 * A View as a RelativeLayout that manages a Sudoku display.
 *
 * @param context The context in which this view is displayed

 */
class SudokuLayout(context: Context) : RelativeLayout(context), ObservableCellInteraction,
    ZoomableView {

    /**
     * Das Game, welches diese Anzeige verwaltet
     */
    private val game: Game = (context as SudokuActivity).game!!

    /**
     * Die Standardgröße eines Feldes
     */
    private var defaultCellViewSize: Int
    /**
     * Die aktuelle Größe eines Feldes
     */
    // private int currentCellViewSize;

    /**
     * The currently selected CellView (primary selection)
     */
    var currentCellView: SudokuCellView? = null

    /**
     * Set of all currently selected cells for multi-selection
     */
    private val selectedCellViews: MutableSet<SudokuCellView> = mutableSetOf()

    /**
     * Whether multi-selection mode is currently active
     */
    var isMultiSelectionMode: Boolean = false
        private set

    /**
     * Track which cells have been touched during current drag operation
     * to avoid repeatedly processing the same cell
     */
    private val touchedCellsDuringDrag: MutableSet<SudokuCellView> = mutableSetOf()

    /**
     * Track if we should start intercepting touch events for drag selection
     * Set to true when entering multi-selection mode
     */
    private var shouldInterceptForDrag: Boolean = false

    /**
     * Button to exit multi-selection mode
     */
    private var exitMultiSelectButton: android.widget.Button? = null

    private var zoomFactor: Float
        private set

    /**
     * Ein Array aller CellViews
     */
    private var sudokuCellViews: Array<Array<SudokuCellView?>>? = null

    /**
     * Der linke Rand, verursacht durch ein zu niedriges Layout
     */
    private var leftMargin = 0

    /**
     * Der linke Rand, verursacht durch ein zu schmales Layout
     */
    private var topMargin = 0
    private val boardPainter: BoardPainter
    val hintPainter: HintPainter
    private val paint: Paint

    /** Exposes whether a symbol (0-based) is fully filled in the current sudoku. */
    fun isSymbolFullyFilled(symbol: Int): Boolean {
        return try {
            game.sudoku!!.isSymbolFullyFilled(symbol)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Erstellt die Anzeige des Sudokus.
     * doesn't draw anything
     */
    private fun inflateSudoku() {
        Log.d(LOG_TAG, "SudokuLayout.inflateSudoku()")
        CellViewPainter.instance!!.flushMarkings()
        removeAllViews()
        val sudoku = game.sudoku
        val sudokuType = sudoku!!.sudokuType
        val isMarkWrongSymbolAvailable = game.isAssistanceAvailable(Assistances.markWrongSymbol)
        sudokuCellViews = Array(sudokuType!!.size!!.x + 1) { arrayOfNulls(sudokuType.size!!.y + 1) }
        for (p in sudokuType.validPositions) {
            val cell = sudoku.getCell(p)
            if (cell != null) {
                val x = p.x
                val y = p.y
                val params = LayoutParams(currentCellViewSize, defaultCellViewSize)
                params.topMargin = y * currentCellViewSize + y
                params.leftMargin = x * currentCellViewSize + x
                sudokuCellViews!![x][y] =
                    SudokuCellView(context, game, cell, isMarkWrongSymbolAvailable)
                cell.registerListener(sudokuCellViews!![x][y]!!)
                this.addView(sudokuCellViews!![x][y], params)
            }
        }
        val x = sudoku.sudokuType!!.size!!.x //why all this????
        val y = sudoku.sudokuType!!.size!!.y
        val params = LayoutParams(currentCellViewSize, defaultCellViewSize)
        params.topMargin = (y - 1) * currentCellViewSize + (y - 1) + currentTopMargin
        params.leftMargin = (x - 1) * currentCellViewSize + (x - 1) + currentLeftMargin
        sudokuCellViews!![x][y] = SudokuCellView(
            context,
            game,
            game.sudoku!!.getCell(Position[x - 1, y - 1])!!,
            isMarkWrongSymbolAvailable
        )
        this.addView(sudokuCellViews!![x][y], params)
        sudokuCellViews!![x][y]!!.visibility = INVISIBLE


        /* In case highlighting of current row and col is activated,
		   pass each pos its constraint-mates */if (game.isAssistanceAvailable(Assistances.markRowColumn)) {
            var positions: ArrayList<Position>
            val allConstraints: Iterable<Constraint>? = game.sudoku!!.sudokuType
            for (c in allConstraints!!) if (c.type == ConstraintType.LINE) {
                positions = c.getPositions()
                for (i in positions.indices) for (k in i + 1 until positions.size) {
                    val fvI = getSudokuCellView(positions[i])
                    val fvK = getSudokuCellView(positions[k])
                    fvI.addConnectedCell(fvK)
                    fvK.addConnectedCell(fvI)
                }
            }
        }
        hintPainter.updateLayout()
        //Log.d(LOG_TAG, "SudokuLayout.inflateSudoku()-end");
    }

    /**
     * Berechnet das aktuelle Spacing (gem. dem aktuellen ZoomFaktor) und gibt
     * es zurück.
     *
     * @return Das aktuelle Spacing
     */
    val currentSpacing: Int
        get() = (spacing * zoomFactor).toInt()

    /**
     * Berechnet das aktuelle obere Margin (gem. dem aktuellen ZoomFaktor) und
     * gibt es zurück.
     *
     * @return Das aktuelle obere Margin
     */
    val currentTopMargin: Int
        get() = (topMargin * zoomFactor).toInt()

    /**
     * Berechnet das aktuelle linke Margin (gem. dem aktuellen ZoomFaktor) und
     * gibt es zurück.
     *
     * @return Das aktuelle linke Margin
     */
    val currentLeftMargin: Int
        get() = (leftMargin * zoomFactor).toInt()

    /**
     * Aktualisiert die Sudoku-Anzeige bzw. der enthaltenen Felder.
     */
    private fun refresh() {
        Log.d(LOG_TAG, "SudokuLayout.refresh()")
        if (sudokuCellViews != null) {
            val type = game.sudoku!!.sudokuType
            val typeSize = type!!.size
            val cellPlusSpacing = currentCellViewSize + currentSpacing
            //Iterate over all positions within the size 
            for (p in type.validPositions) {
                val params = getSudokuCellView(p).layoutParams as LayoutParams
                params.width = currentCellViewSize
                params.height = currentCellViewSize
                params.topMargin = currentTopMargin + p.y * cellPlusSpacing
                params.leftMargin = currentLeftMargin + p.x * cellPlusSpacing
                getSudokuCellView(p).layoutParams = params
                getSudokuCellView(p).invalidate()
            }
            //still not sure why we are doing this...
            val x = typeSize!!.x
            val y = typeSize.y
            //both x and y are over the limit. Why do we go there? we could just do it outside the loop, why was it ever put it in there?!
            val params = LayoutParams(currentCellViewSize, defaultCellViewSize)
            params.width = currentCellViewSize
            params.height = currentCellViewSize
            params.topMargin = 2 * currentTopMargin + (y - 1) * cellPlusSpacing
            params.leftMargin = 2 * currentLeftMargin + (x - 1) * cellPlusSpacing
            sudokuCellViews!![x][y]!!.layoutParams = params
            sudokuCellViews!![x][y]!!.invalidate()
            //end strange thing
        }
        hintPainter.updateLayout()
        invalidate()
        //Log.d(LOG_TAG, "SudokuLayout.refresh()-end");
    }

    /**
     * Draws all black borders for the sudoku, nothing else
     * Cells have to be drawn after this method
     * No insight on the coordinate-wise workings, unsure about the 'i's.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(LOG_TAG, "SudokuLayout.onDraw()")
        val edgeRadius = currentCellViewSize / 20.0f
        paint.reset()
        paint.color = Color.BLACK
        boardPainter.paintBoard(paint, canvas, edgeRadius)
        hintPainter.invalidateAll()
    }

    var focusX = 0f
    var focusY = 0f

    /**
     * Zoom so heraus, dass ein diese View optimal in ein Layout der
     * spezifizierte Größe passt
     *
     * @param width
     * Die Breite auf die optimiert werden soll
     * @param height
     * Die Höhe auf die optimiert werden soll
     */
    fun optiZoom(width: Int, height: Int) {
        Log.d(LOG_TAG, "SudokuView height intern: " + this.measuredHeight)
        val sudokuType = game.sudoku!!.sudokuType
        val size = if (width < height) width else height
        val numberOfCells = if (width < height) sudokuType!!.size!!.x else sudokuType!!.size!!.y
        defaultCellViewSize = (size - (numberOfCells + 1) * spacing) / numberOfCells
        // this.currentCellViewSize = this.defaultCellViewSize;
        val cellSizeX =
            sudokuType.size!!.x * currentCellViewSize + (sudokuType.size!!.x - 1) * spacing
        val cellSizeY =
            sudokuType.size!!.y * currentCellViewSize + (sudokuType.size!!.y - 1) * spacing
        leftMargin = (width - cellSizeX) / 2
        topMargin = (height - cellSizeY) / 2
        Log.d(LOG_TAG, "Sudoku width: $width")
        Log.d(LOG_TAG, "Sudoku height: $height")
        refresh()
    }

    /**
     * Intercept touch events in multi-selection mode to handle drag selection
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        Log.d(LOG_TAG, "onInterceptTouchEvent ${MotionEvent.actionToString(event.actionMasked)} isMultiSelectionMode=${isMultiSelectionMode}")
        
        // Request parent not to intercept touch events so cells can handle long press
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        
        // In multi-selection mode, intercept MOVE events to handle drag selection
        if (isMultiSelectionMode) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // User touched down in multi-select mode, prepare to intercept drags
                    shouldInterceptForDrag = true
                    Log.d(LOG_TAG, "Multi-select mode ACTION_DOWN: shouldInterceptForDrag=true, returning false")
                    return false  // Don't intercept DOWN, let children handle it first
                }
                MotionEvent.ACTION_MOVE -> {
                    if (shouldInterceptForDrag) {
                        // Start intercepting to handle drag selection
                        Log.d(LOG_TAG, "Multi-select mode ACTION_MOVE: Intercepting! Returning true")
                        return true
                    } else {
                        Log.d(LOG_TAG, "Multi-select mode ACTION_MOVE: shouldInterceptForDrag=false, not intercepting")
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    shouldInterceptForDrag = false
                    Log.d(LOG_TAG, "Multi-select mode ${MotionEvent.actionToString(event.actionMasked)}: shouldInterceptForDrag=false")
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    /**
     * Handle touch events for multi-selection drag support
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d(LOG_TAG, "onTouchEvent ${MotionEvent.actionToString(event.actionMasked)} isMultiSelectionMode=${isMultiSelectionMode}")
        // Only handle touch events in multi-selection mode
        if (!isMultiSelectionMode) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Start of touch, clear the drag tracking set
                touchedCellsDuringDrag.clear()
                // Request parent not to intercept touch events during multi-selection
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Find which cell the user is touching during drag
                val x = event.x
                val y = event.y
                Log.d(LOG_TAG, "ACTION_MOVE: x=$x, y=$y, isMultiSelectionMode=$isMultiSelectionMode")
                
                // Find the cell at this position
                val cellView = findCellViewAt(x, y)
                Log.d(LOG_TAG, "findCellViewAt returned: ${cellView?.let { "Cell at (${it.cell.id})" } ?: "null"}")
                
                if (cellView != null && cellView.cell.isEditable) {
                    Log.d(LOG_TAG, "Cell is editable, isInTouchedSet=${touchedCellsDuringDrag.contains(cellView)}, isInSelectedSet=${selectedCellViews.contains(cellView)}")
                    
                    // Only process each cell once per drag
                    if (!touchedCellsDuringDrag.contains(cellView)) {
                        touchedCellsDuringDrag.add(cellView)
                        Log.d(LOG_TAG, "Added to touchedCellsDuringDrag, size=${touchedCellsDuringDrag.size}")
                        
                        // Add to selection if not already selected
                        if (!selectedCellViews.contains(cellView)) {
                            // Delegate to cell to handle multi-selection and notify listeners
                            cellView.addToMultiSelectionDrag()
                            Log.d(LOG_TAG, "Cell added to multi-selection during drag! Total selected: ${selectedCellViews.size}")
                        } else {
                            Log.d(LOG_TAG, "Cell already in selectedCellViews, skipping")
                        }
                    } else {
                        Log.d(LOG_TAG, "Cell already in touchedCellsDuringDrag, skipping")
                    }
                } else {
                    if (cellView == null) {
                        Log.d(LOG_TAG, "No cell found at coordinates")
                    } else if (!cellView.cell.isEditable) {
                        Log.d(LOG_TAG, "Cell is not editable")
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {
                // Finger lifted: finish drag selection but KEEP multi-selection state & all selected cells
                touchedCellsDuringDrag.clear()
                shouldInterceptForDrag = false
                Log.d(LOG_TAG, "Drag ended. Keeping multi-selection (selected=${selectedCellViews.size})")
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }

    /**
     * Find the cell view at the given screen coordinates
     */
    private fun findCellViewAt(x: Float, y: Float): SudokuCellView? {
        val sudokuType = game.sudoku!!.sudokuType
        for (p in sudokuType!!.validPositions) {
            val cellView = getSudokuCellView(p)
            val left = cellView.left
            val top = cellView.top
            val right = cellView.right
            val bottom = cellView.bottom

            if (x >= left && x <= right && y >= top && y <= bottom) {
                return cellView
            }
        }
        return null
    }

    /**
     * returns the CellView at Position p.
     */
    fun getSudokuCellView(p: Position): SudokuCellView {
        return sudokuCellViews!![p.x][p.y]!!
    }

    /**
     * Setzt den aktuellen Zoom-Faktor für diese View und refresh sie.
     *
     * @param factor
     * Der Zoom-Faktor
     */
    override fun zoom(factor: Float): Boolean {
        zoomFactor = factor
        //		//this.canvas.scale(factor,factor);
        refresh()
        //invalidate();
        return true
    }

    /**
     * Gibt die aktuelle Größe einer CellView zurück.
     *
     * @return die aktuelle Größe einer CellView
     */
    val currentCellViewSize: Int
        get() = (defaultCellViewSize * zoomFactor).toInt()

    /**
     * Unbenutzt.
     *
     * @throws UnsupportedOperationException
     * Wirft immer eine UnsupportedOperationException
     */
    fun notifyListener() {
        throw UnsupportedOperationException()
    }

    /**
     * {@inheritDoc}
     */
    override fun registerListener(listener: CellInteractionListener) {
        val sudokuType = game.sudoku!!.sudokuType
        for (p in sudokuType!!.validPositions) getSudokuCellView(p).registerListener(listener)
    }

    /**
     * {@inheritDoc}
     */
    override fun removeListener(listener: CellInteractionListener) {
        val sudokuType = game.sudoku!!.sudokuType
        for (p in sudokuType!!.validPositions) getSudokuCellView(p).removeListener(listener)
    }

    /**
     * {@inheritDoc}
     */
    override fun getMinZoomFactor(): Float {
        return 1.0f
    }

    /**
     * {@inheritDoc}
     */
    override fun getMaxZoomFactor(): Float {
        return 10f //this.game.getSudoku().getSudokuType().getSize().getX() / 2.0f;
    }

    /**
     * Adds a cell to the multi-selection
     */
    fun addToMultiSelection(cellView: SudokuCellView) {
        selectedCellViews.add(cellView)
        Log.d(LOG_TAG, "addToMultiSelection: Added cell, total=${selectedCellViews.size}, isMultiSelectionMode=$isMultiSelectionMode")
    }

    /**
     * Removes a cell from the multi-selection
     */
    fun removeFromMultiSelection(cellView: SudokuCellView) {
        selectedCellViews.remove(cellView)
        // If we have less than 1 cells left, exit multi-selection mode
        if (selectedCellViews.size < 1) {
            isMultiSelectionMode = false
            shouldInterceptForDrag = false
        }
        Log.d(LOG_TAG, "removeFromMultiSelection: Removed cell, total=${selectedCellViews.size}, isMultiSelectionMode=$isMultiSelectionMode")
    }

    /**
     * Clears all multi-selected cells
     */
    fun clearMultiSelection() {
        Log.d(LOG_TAG, "clearMultiSelection called")
        for (cellView in selectedCellViews) {
            cellView.setMultiSelected(false)
            if (cellView != currentCellView) {
                cellView.deselect(false)
            }
        }
        selectedCellViews.clear()
        touchedCellsDuringDrag.clear()
        isMultiSelectionMode = false
        shouldInterceptForDrag = false
        
        // Hide exit button
        Log.d(LOG_TAG, "Setting exit button GONE")
        exitMultiSelectButton?.visibility = android.view.View.GONE
        Log.d(LOG_TAG, "Exit button visibility after clear=${exitMultiSelectButton?.visibility}")
    }

    /**
     * Gets all currently selected cell views
     */
    fun getSelectedCellViews(): Set<SudokuCellView> {
        return if (isMultiSelectionMode) {
            selectedCellViews.toSet()
        } else if (currentCellView != null) {
            setOf(currentCellView!!)
        } else {
            emptySet()
        }
    }

    /**
     * Starts multi-selection mode with the current cell
     */
    fun startMultiSelectionMode() {
        Log.d(LOG_TAG, "startMultiSelectionMode called: currentCellView=$currentCellView, isMultiSelectionMode=$isMultiSelectionMode")
        Log.d(LOG_TAG, "Stack trace:\n${android.util.Log.getStackTraceString(Exception())}")
        
        if (currentCellView != null && !isMultiSelectionMode) {
            selectedCellViews.clear()
            selectedCellViews.add(currentCellView!!)
            isMultiSelectionMode = true
            shouldInterceptForDrag = true  // Enable drag interception immediately
            touchedCellsDuringDrag.clear()
            
            // Show exit button
            Log.d(LOG_TAG, "Setting exit button VISIBLE")
            exitMultiSelectButton?.visibility = android.view.View.VISIBLE
            
            Log.d(LOG_TAG, "Multi-selection mode STARTED! selectedCellViews.size=${selectedCellViews.size}, isMultiSelectionMode=$isMultiSelectionMode, button visibility=${exitMultiSelectButton?.visibility}")
        } else {
            Log.d(LOG_TAG, "Multi-selection mode NOT started: currentCellView=${currentCellView != null}, already in mode=$isMultiSelectionMode")
        }
    }

    companion object {
        /**
         * Das Log-Tag für den LogCat
         */
        private val LOG_TAG = SudokuLayout::class.java.simpleName

        /**
         * Der Platz zwischen 2 Blöcken
         */
        private const val spacing = 2
    }

    /**
     * Instanziiert eine neue SudokuView in dem spezifizierten Kontext.
     *
     */
    init {
        Log.d(LOG_TAG, "SudokuLayout init start, isMultiSelectionMode=$isMultiSelectionMode")
        defaultCellViewSize = 40
        zoomFactor = 1.0f
        // this.currentCellViewSize = this.defaultCellViewSize;
        setWillNotDraw(false)
        paint = Paint()
        boardPainter = BoardPainter(this, game.sudoku!!.sudokuType!!)
        CellViewPainter.instance!!.setSudokuLayout(this)
        hintPainter = HintPainter(this)
        inflateSudoku()
        
        // Create exit multi-select button
        createExitMultiSelectButton()
        
        Log.d(LOG_TAG, "End of Constructor. isMultiSelectionMode=$isMultiSelectionMode, exitButton.visibility=${exitMultiSelectButton?.visibility}")
    }
    
    /**
     * Creates and configures the exit multi-selection button
     */
    private fun createExitMultiSelectButton() {
        Log.d(LOG_TAG, "createExitMultiSelectButton called")
        
        // Remove old button if exists
        exitMultiSelectButton?.let {
            removeView(it)
        }
        
        exitMultiSelectButton = android.widget.Button(context).apply {
            text = context.getString(de.sudoq.R.string.button_exit_multi_select)
            visibility = android.view.View.GONE // Hidden by default
            
            // Style as a modern button
            setBackgroundColor(android.graphics.Color.parseColor("#2196F3")) // Material Blue
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            isAllCaps = false
            
            // Set padding
            val padding = (12 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, padding / 2)
            
            // Add elevation/shadow effect
            stateListAnimator = null
            elevation = 4 * resources.displayMetrics.density
            
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                addRule(RelativeLayout.ALIGN_PARENT_END)
                topMargin = (8 * resources.displayMetrics.density).toInt()
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
            
            // Click handler
            setOnClickListener {
                clearMultiSelection()
                android.widget.Toast.makeText(
                    context,
                    context.getString(de.sudoq.R.string.toast_multi_select_deactivated),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        Log.d(LOG_TAG, "Exit button created with visibility=${exitMultiSelectButton?.visibility}")
        addView(exitMultiSelectButton)
    }
}
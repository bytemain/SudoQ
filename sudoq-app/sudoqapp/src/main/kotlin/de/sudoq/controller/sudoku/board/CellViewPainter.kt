/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.sudoku.board

import android.graphics.*
import android.view.View
import de.sudoq.view.SudokuLayout
import de.sudoq.controller.sudoku.Symbol
import java.util.*
import kotlin.collections.set

/**
 * Data class holding theme-based colors for board rendering.
 * All colors are in android.graphics.Color integer format.
 */
data class BoardThemeColors(
    val selectionInputColor: Int = Color.rgb(255, 80, 80),
    val selectionNoteColor: Int = Color.YELLOW,
    val selectionColor: Int = Color.rgb(180, 180, 255),
    val connectedColor: Int = Color.rgb(220, 220, 255),
    val sameNumberColor: Int = Color.rgb(180, 230, 180),
    val defaultColor: Int = Color.rgb(250, 250, 250),
    val borderColor: Int = Color.DKGRAY,
    val fixedTextColor: Int = Color.rgb(0, 100, 0),
    val errorTextColor: Int = Color.RED,
    val normalTextColor: Int = Color.BLACK
)

/**
 * This class is responsible for Animationens and highlighting of cells.
 * TODO does it have to be singleton?
 */
class CellViewPainter private constructor() {
    /* Attributes */
    /**
     * Maps a cell onto an Animation value that describes how to draw the cell
     */
    private val markings: Hashtable<View, CellViewStates>
    /**
     * Optional per-cell text alpha (0-255). When not present, text renders fully opaque (255).
     */
    private val textAlphas: Hashtable<View, Int>
    /** Small red indicator box overlay for pre-fill highlighting. */
    private val preFillIndicators: Hashtable<View, Boolean>
    private val preFillIndicatorColors: Hashtable<View, Int>
    /** Optional symbol to align indicator to; if absent, falls back to current cell symbol. */
    private val preFillIndicatorSymbols: Hashtable<View, String>
    private var sl: SudokuLayout? = null
    
    /**
     * Theme colors for board rendering. Can be updated to follow user theme settings.
     */
    private var themeColors: BoardThemeColors = BoardThemeColors()
    
    fun setSudokuLayout(sl: SudokuLayout?) {
        this.sl = sl
    }
    
    /**
     * Updates the theme colors used for board rendering
     */
    fun setThemeColors(colors: BoardThemeColors) {
        this.themeColors = colors
    }
    /* Methods */
    /**
     * Bemalt das spezifizierte Canvas entsprechend der in der Hashtable für das
     * spezifizierte Feld eingetragenen Animation. Ist eines der beiden
     * Argumente null, so wird nichts getan.
     *
     * @param canvas
     * Das Canvas, welches bemalt werden soll
     * @param cell
     * Das Feld, anhand dessen Animation-Einstellung das Canvas
     * bemalt werden soll
     * @param symbol
     * Das Symbol das gezeichnet werden soll
     * @param justText
     * Definiert, dass nur Text geschrieben wird
     * @param darken
     * Verdunkelt das Feld
     */
    fun markCell(canvas: Canvas, cell: View, symbol: String, justText: Boolean, darken: Boolean) {
        val cellState = markings[cell]
        /*if(true){}else //to suppress celldrawing TODO remove again*/
        if (cellState != null && !justText) {
            when (cellState) {
                CellViewStates.SELECTED_INPUT_BORDER -> {
                    drawBackground(canvas, cell, themeColors.borderColor, true, darken)
                    drawInner(canvas, cell, themeColors.selectionInputColor, true, darken)
                    drawText(canvas, cell, themeColors.normalTextColor, false, symbol)
                }
                CellViewStates.SELECTED_INPUT -> {
                    drawBackground(canvas, cell, themeColors.selectionInputColor, true, darken)
                    drawText(canvas, cell, themeColors.normalTextColor, false, symbol)
                }
                CellViewStates.SELECTED_INPUT_WRONG -> {
                    drawBackground(canvas, cell, themeColors.selectionInputColor, true, darken)
                    drawText(canvas, cell, themeColors.errorTextColor, false, symbol)
                }
                CellViewStates.SELECTED_NOTE_BORDER -> {
                    drawBackground(canvas, cell, themeColors.borderColor, true, darken)
                    drawInner(canvas, cell, themeColors.selectionNoteColor, true, darken)
                    drawText(canvas, cell, themeColors.normalTextColor, false, symbol)
                }
                CellViewStates.SELECTED_NOTE -> {
                    drawBackground(canvas, cell, themeColors.selectionNoteColor, true, darken)
                    drawText(canvas, cell, themeColors.normalTextColor, false, symbol)
                }
                CellViewStates.SELECTED_NOTE_WRONG -> {
                    drawBackground(canvas, cell, themeColors.selectionNoteColor, true, darken)
                    drawText(canvas, cell, themeColors.errorTextColor, false, symbol)
                }
                CellViewStates.SELECTED -> {
                    drawBackground(canvas, cell, themeColors.selectionColor, true, darken)
                    drawText(canvas, cell, themeColors.fixedTextColor, true, symbol)
                }
                CellViewStates.SELECTED_FIXED -> {
                    drawBackground(canvas, cell, themeColors.connectedColor, true, darken)
                    drawText(canvas, cell, themeColors.fixedTextColor, true, symbol)
                }
                CellViewStates.CONNECTED -> {
                    drawBackground(canvas, cell, themeColors.connectedColor, true, darken)
                    drawText(canvas, cell, themeColors.normalTextColor, false, symbol)
                }
                CellViewStates.CONNECTED_WRONG -> {
                    drawBackground(canvas, cell, themeColors.connectedColor, true, darken)
                    drawText(canvas, cell, themeColors.errorTextColor, false, symbol)
                }
                CellViewStates.SAME_NUMBER -> {
                    drawBackground(canvas, cell, themeColors.sameNumberColor, true, darken)
                    drawText(canvas, cell, themeColors.normalTextColor, true, symbol)
                }
                CellViewStates.FIXED -> {
                    drawBackground(canvas, cell, themeColors.defaultColor, true, darken)
                    drawText(canvas, cell, themeColors.fixedTextColor, true, symbol)
                }
                CellViewStates.DEFAULT_BORDER -> {
                    drawBackground(canvas, cell, themeColors.borderColor, true, darken)
                    drawInner(canvas, cell, themeColors.defaultColor, true, darken)
                    drawText(canvas, cell, themeColors.normalTextColor, false, symbol)
                }
                CellViewStates.DEFAULT_WRONG -> {
                    drawBackground(canvas, cell, themeColors.defaultColor, true, darken)
                    drawText(canvas, cell, themeColors.errorTextColor, false, symbol)
                }
                CellViewStates.DEFAULT -> {
                    drawBackground(canvas, cell, themeColors.defaultColor, true, darken)
                    drawText(canvas, cell, themeColors.normalTextColor, false, symbol)
                }
                CellViewStates.CONTROLS -> drawBackground(
                    canvas,
                    cell,
                    Color.rgb(40, 40, 40),
                    false,
                    darken
                )
                CellViewStates.KEYBOARD -> {
                    drawBackground(canvas, cell, Color.rgb(230, 230, 230), false, darken)
                    drawInner(canvas, cell, Color.rgb(40, 40, 40), false, darken)
                }
                CellViewStates.SUDOKU -> drawBackground(
                    canvas,
                    cell,
                    Color.rgb(200, 200, 200),
                    false,
                    darken
                )
            }
        } else if (cellState != null) {
            when (cellState) {
                CellViewStates.SELECTED_INPUT_BORDER,
                CellViewStates.SELECTED_INPUT,
                CellViewStates.SELECTED_NOTE_BORDER,
                CellViewStates.SELECTED_NOTE,
                CellViewStates.CONNECTED,
                CellViewStates.SAME_NUMBER,
                CellViewStates.DEFAULT_BORDER,
                CellViewStates.DEFAULT -> drawText(canvas, cell, themeColors.normalTextColor, false, symbol)
                CellViewStates.SELECTED_INPUT_WRONG,
                CellViewStates.SELECTED_NOTE_WRONG,
                CellViewStates.DEFAULT_WRONG,
                CellViewStates.CONNECTED_WRONG -> drawText(canvas, cell, themeColors.errorTextColor, false, symbol)
                CellViewStates.SELECTED_FIXED,
                CellViewStates.SELECTED,
                CellViewStates.FIXED -> drawText(canvas, cell, themeColors.fixedTextColor, true, symbol)
                else -> {}
            }
        }
        //Log.d("FieldPainter", "Field drawn");
        // Draw small pre-fill indicator overlay if requested
        drawPreFillIndicatorIfAny(canvas, cell, symbol)

    try {
            sl!!.hintPainter.invalidateAll() //invalidate();
        } catch (e: NullPointerException) {
            /*
			I don't see how this happens but a nullpointer exception was reported, so I made a try-catch-block here:
			reported at version 20
			This happens when 'gesture' is clicked in profile without playing a game first. sl is then null
			java.lang.NullPointerException:
			at de.sudoq.controller.sudoku.board.FieldViewPainter.markField (FieldViewPainter.java:182)
			at de.sudoq.view.VirtualKeyboardButtonView.onDraw (VirtualKeyboardButtonView.java:104)
		        at android.view.View.draw (View.java:17469)
			at android.view.View.updateDisplayListIfDirty (View.java:16464)
			at android.view.View.draw (View.java:17238)
			at android.view.ViewGroup.drawChild (ViewGroup.java:3921)
			at android.view.ViewGroup.dispatchDraw (ViewGroup.java:3711)
			at android.view.View.updateDisplayListIfDirty (View.java:16459)
			at android.view.View.draw (View.java:17238)
			at android.view.ViewGroup.drawChild (ViewGroup.java:3921)
			at android.view.ViewGroup.dispatchDraw (ViewGroup.java:3711)
            ...
            */
        }
    }

    private fun drawPreFillIndicatorIfAny(canvas: Canvas, cell: View, currentSymbol: String) {
        val show = preFillIndicators[cell] ?: false
        if (!show) return
        val color = preFillIndicatorColors[cell] ?: Color.RED

        val density = cell.resources.displayMetrics.density
        val targetSymbol = preFillIndicatorSymbols[cell] ?: currentSymbol
        val abstractIdx = try { Symbol.getInstance().getAbstract(targetSymbol) } catch (_: Exception) { -1 }

        val rect: RectF = if (abstractIdx >= 0) {
            // Align to the note grid cell (small candidate square)
            val raster = try { Symbol.getInstance().getRasterSize() } catch (_: Exception) { 3 }
            val noteSize = cell.height / raster.toFloat() // match SudokuCellView.drawNotes
            val col = abstractIdx % raster
            val row = abstractIdx / raster
            val left = col * noteSize
            val top = row * noteSize
            val right = left + noteSize
            val bottom = top + noteSize
            // Inset slightly so the stroke sits nicely inside the mini-cell
            val inset = 1.25f * density
            RectF(left + inset, top + inset, right - inset, bottom - inset)
        } else {
            // Fallback: build a small square around the main glyph position (rare)
            val textPaint = Paint()
            textPaint.isAntiAlias = true
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = Math.min(cell.height * 3 / 4, cell.width * 3 / 4).toFloat()
            val symbolToMeasure = if (targetSymbol.isNotEmpty()) targetSymbol else "0"
            val bounds = Rect()
            textPaint.getTextBounds(symbolToMeasure, 0, symbolToMeasure.length, bounds)
            val xCenter = cell.width / 2f
            val yBaseline = (cell.height / 2 + Math.min(cell.height / 4, cell.width / 4)).toFloat()
            val left = xCenter + bounds.left
            val top = yBaseline + bounds.top
            val right = xCenter + bounds.right
            val bottom = yBaseline + bounds.bottom
            val pad = 2.0f * density
            val glyphWidth = (right - left)
            val glyphHeight = (bottom - top)
            val side = kotlin.math.max(glyphWidth, glyphHeight) + 2f * pad
            val half = side / 2f
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f
            RectF(cx - half, cy - half, cx + half, cy + half)
        }

        val paint = Paint()
        paint.style = Paint.Style.STROKE
        // Thin stroke for small note boxes
        paint.strokeWidth = 1.0f * density
        paint.color = color
        paint.isAntiAlias = true
        // Draw without rounded corners for square cells
        canvas.drawRect(rect, paint)
    }

    /**
     * Zeichnet den Hintergrund.
     *
     * @param canvas
     * Das Canvas
     * @param cell
     * Das Field, das gezeichnet wird
     * @param color
     * Die Hintergrundfarbe
     * @param round
     * Gibt an, ob die Ecken rund gezeichnet werden sollen
     * @param darken
     * Gibt an, ob das Feld verdunkelt werden soll
     */
    private fun drawBackground(
        canvas: Canvas,
        cell: View,
        color: Int,
        round: Boolean,
        darken: Boolean
    ) {
        val mainPaint = Paint()
        var darkenPaint: Paint? = null
        if (darken) {
            darkenPaint = Paint()
            darkenPaint.setARGB(60, 0, 0, 0)
        }
        mainPaint.color = color
        val rect = RectF(0f, 0f, cell.width.toFloat(), cell.height.toFloat())
        // Draw without rounded corners for square cells
        canvas.drawRect(rect, mainPaint)
        if (darken) {
            canvas.drawRect(rect, darkenPaint!!)
        }
    }

    /**
     * Malt den inneren Bereich (lässt einen Rahmen).
     *
     * @param canvas
     * Das Canvas
     * @param cell
     * The cell to draw
     * @param color
     * Die Farbe
     * @param round
     * Gibt an, ob die Ecken rund gezeichnet werden sollen
     * @param darken
     * determines whether the cell should be darkened
     */
    private fun drawInner(canvas: Canvas, cell: View, color: Int, round: Boolean, darken: Boolean) {
        val mainPaint = Paint()
        var darkenPaint: Paint? = null
        if (darken) {
            darkenPaint = Paint()
            darkenPaint.setARGB(60, 0, 0, 0)
        }
        mainPaint.color = color
        val rect = RectF(2f, 2f, (cell.width - 2).toFloat(), (cell.height - 2).toFloat())
        // Draw without rounded corners for square cells
        canvas.drawRect(rect, mainPaint)
        if (darken) {
            canvas.drawRect(rect, darkenPaint!!)
        }
    }

    /**
     * Schreibt den Text
     *
     * @param canvas
     * Das Canvas
     * @param cell
     * The cell on which to draw
     * @param color
     * Die Farbe des Textes
     * @param bold
     * Definiert, ob der Text fett ist
     * @param symbol
     * Das Symbol, welches geschrieben wird
     */
    private fun drawText(canvas: Canvas, cell: View, color: Int, bold: Boolean, symbol: String) {
        val paint = Paint()
        val alpha = textAlphas[cell] ?: 255
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        paint.color = Color.argb(alpha, r, g, b)
        // Make fixed numbers non-bold if their symbol is fully filled in the sudoku
        var effectiveBold = bold
        try {
            val idx = Symbol.getInstance().getAbstract(symbol)
            if (idx >= 0 && sl != null && sl!!.isSymbolFullyFilled(idx)) {
                effectiveBold = false
            }
        } catch (_: Exception) { /* ignore, fallback to requested bold */ }
        if (effectiveBold) {
            paint.typeface = Typeface.DEFAULT_BOLD
        }
        paint.isAntiAlias = true
        paint.textSize = Math.min(cell.height * 3 / 4, cell.width * 3 / 4).toFloat()
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            symbol + "",
            (cell.width / 2).toFloat(),
            (cell.height / 2 + Math.min(cell.height / 4, cell.width / 4)).toFloat(),
            paint
        )
    }

    /**
     * Sets the specified animation for the passed cell, so that it is drawn when markCell is
     * called. If either parameter is null, nothing happens.
     *
     *
     * @param cell
     * The cell for which the animation is to be stored
     * @param marking
     * Die Animation die eingetragen werden soll
     */
    fun setMarking(cell: View, marking: CellViewStates) {
        markings[cell] = marking
    }

    /**
     * Löscht alle hinzugefügten Markierungen auf Default.
     */
    fun flushMarkings() {
        markings.clear()
    }

    /** Sets the text alpha (0-255) for the given cell view and requests redraw. */
    fun setTextAlpha(cell: View, alpha: Int) {
        val a = alpha.coerceIn(0, 255)
        textAlphas[cell] = a
        cell.invalidate()
    }

    /** Clears any custom text alpha for the cell view (will render fully opaque). */
    fun clearTextAlpha(cell: View) {
        textAlphas.remove(cell)
        cell.invalidate()
    }

    /** Shows a small indicator box on the given cell. */
    fun showPreFillIndicator(cell: View, color: Int = Color.RED, symbol: String? = null) {
        preFillIndicators[cell] = true
        preFillIndicatorColors[cell] = color
        if (symbol != null) preFillIndicatorSymbols[cell] = symbol
        cell.invalidate()
    }

    /** Hides the small indicator box on the given cell. */
    fun hidePreFillIndicator(cell: View) {
        preFillIndicators.remove(cell)
        preFillIndicatorColors.remove(cell)
        preFillIndicatorSymbols.remove(cell)
        cell.invalidate()
    }

    companion object {
        /**
         * Gibt die Singleton-Instanz des Handlers zurück.
         *
         * @return Die Instanz dieses Handlers
         */
        /**
         * Die Singleton-Instanz des Handlers
         */
        @JvmStatic
        var instance: CellViewPainter? = null
            get() {
                if (field == null) {
                    field = CellViewPainter()
                }
                return field
            }
            private set
    }
    /* Constructors */ /**
     * Privater Konstruktor, da diese Klasse statisch ist.
     */
    init {
        markings = Hashtable()
        textAlphas = Hashtable()
        preFillIndicators = Hashtable()
        preFillIndicatorColors = Hashtable()
        preFillIndicatorSymbols = Hashtable()
    }
}

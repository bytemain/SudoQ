/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.view.Hints

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import de.sudoq.controller.sudoku.Symbol
import de.sudoq.model.sudoku.Cell
import de.sudoq.model.sudoku.Position
import de.sudoq.view.SudokuLayout

/**
 * Diese Subklasse des von der Android API bereitgestellten Views stellt ein
 * einzelnes Feld innerhalb eines Sudokus dar. Es erweitert den Android View um
 * Funktionalität zur Benutzerinteraktion und Färben.
 *
 * @property position Position of the cell represented by this View
 */
class HighlightedCellView(
    context: Context, sl: SudokuLayout,
    private val position: Position, color: Int
) : View(context) {
    /* Attributes */

    /**
     * Color of the margin
     */
    private val marginColor: Int = color
    private val sl: SudokuLayout = sl
    private val paint = Paint()
    private val oval = RectF()
    var style: Paint.Style
    /* Methods */
    /**
     * Draws the content of the cell on the canvas of this SudokuCellView.
     * Sollte den AnimationHandler nutzen um vorab Markierungen/Färbung an dem
     * Canvas Objekt vorzunehmen.
     *
     * @param canvas Das Canvas Objekt auf das gezeichnet wird
     * @throws IllegalArgumentException Wird geworfen, falls das übergebene Canvas null ist
     */
    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //Todo use canvas.drawRoundRect();
        drawNewMethod(position, canvas, marginColor) //red
    }

    private fun drawNewMethod(p: Position, canvas: Canvas, color: Int) {
        paint.reset()
        paint.color = color
        paint.style = Paint.Style.STROKE
        val thickness = 10
        paint.strokeWidth = (thickness * sl.currentSpacing).toFloat()
        val cellSizeAndSpacing = sl.currentCellViewSize + sl.currentSpacing
        val left =
            (sl.currentLeftMargin + p.x * cellSizeAndSpacing - sl.currentSpacing / 2).toFloat()
        val top = (sl.currentTopMargin + p.y * cellSizeAndSpacing - sl.currentSpacing / 2).toFloat()
        val right =
            (sl.currentLeftMargin + (p.x + 1) * cellSizeAndSpacing - sl.currentSpacing / 2).toFloat()
        val bottom =
            (sl.currentTopMargin + (p.y + 1) * cellSizeAndSpacing - sl.currentSpacing / 2).toFloat()
        // Draw without rounded corners for square cells
        canvas.drawRect(
            RectF(left, top, right, bottom),
            paint
        )
    }

    /** TODO may come in handy later for highlighting notes. or do that seperately
     * Zeichnet die Notizen in dieses Feld
     *
     * @param canvas
     * Das Canvas in das gezeichnet werde nsoll
     *
     * @param cell
     * Das Canvas in das gezeichnet werde nsoll
     */
    private fun drawNotes(canvas: Canvas, cell: Cell) {
        val notePaint = Paint()
        notePaint.isAntiAlias = true
        val noteTextSize = height / Symbol.getInstance().getRasterSize()
        notePaint.textSize = noteTextSize.toFloat()
        notePaint.textAlign = Paint.Align.CENTER
        notePaint.color = Color.BLACK
        for (i in 0 until Symbol.getInstance().getNumberOfSymbols()) {
            if (cell.isNoteSet(i)) {
                val note = Symbol.getInstance().getMapping(i)
                canvas.drawText(
                    note + "",
                    (i % Symbol.getInstance()
                        .getRasterSize() * noteTextSize + noteTextSize / 2).toFloat(),
                    (i / Symbol.getInstance()
                        .getRasterSize() * noteTextSize + noteTextSize).toFloat(), notePaint
                )
            }
        }
    }
    /* Constructors */ /**
     * Creates a SudokuCellView
     *
     * @param context    the application context
     * @param sl         a sudokuLayout
     * @param position   cell represented
     * @param color      Color of the margin
     */
    init {
        paint.color = marginColor
        val thickness = 10
        paint.strokeWidth = (thickness * sl.currentSpacing).toFloat()
        style = paint.style
    }
}
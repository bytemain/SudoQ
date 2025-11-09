package de.sudoq.controller.sudoku.board

import android.graphics.Canvas
import android.graphics.Paint
import de.sudoq.model.sudoku.Constraint
import de.sudoq.model.sudoku.ConstraintType
import de.sudoq.model.sudoku.Position
import de.sudoq.model.sudoku.sudokuTypes.SudokuType
import de.sudoq.view.SudokuLayout

/**
 * Created by timo on 13.10.16.
 */
class BoardPainter(var sl: SudokuLayout, var type: SudokuType) {
    fun paintBoard(paint: Paint, canvas: Canvas) {
        type.filter { it.type == ConstraintType.BLOCK } //for every constraint which is a Block
            .forEach { outlineConstraint(it, canvas, paint) } //paint the outline
    }

    /* highlighted constraint has a more intuitive version, update sometime */
    private fun outlineConstraint(c: Constraint, canvas: Canvas, paint: Paint) {
        val topMargin = sl.currentTopMargin
        val leftMargin = sl.currentLeftMargin
        val spacing = sl.currentSpacing
        for (p in c) {
            /* determine whether the position p is in the (right|left|top|bottom) border of its block constraint.
            * test for 0 to avoid illegalArgExc for neg. vals
            * careful when trying to optimize this definition: blocks can be squiggly (every additional compound to row/col but extra as in hypersudoku is s.th. different)
            */
            val isLeft = p.x == 0 || !c.includes(Position[p.x - 1, p.y])
            val isRight = !c.includes(Position[p.x + 1, p.y])
            val isTop = p.y == 0 || !c.includes(Position[p.x, p.y - 1])
            val isBottom = !c.includes(Position[p.x, p.y + 1])
            // (0,0) is in the top left
            for (i in 1..spacing) { //?
                //deklariert hier, weil wir es nicht früher brauchen, effizienter wäre weiter oben
                val cellSizeAndSpacing = sl.currentCellViewSize + spacing
                /* these first 4 seem similar. drawing the black line around?*/
                /* cells that touch the edge: Paint your edge but leave space at the corners*/
                //paint.setColor(Color.GREEN);
                if (isLeft) {
                    val x = (leftMargin + p.x * cellSizeAndSpacing - i).toFloat()
                    val startY = (topMargin + p.y * cellSizeAndSpacing - if (isTop) i else 0).toFloat()
                    val stopY = (topMargin + (p.y + 1) * cellSizeAndSpacing - spacing + if (isBottom) i else 0).toFloat()
                    canvas.drawLine(x, startY, x, stopY, paint)
                }
                if (isRight) {
                    val x =
                        (leftMargin + (p.x + 1) * cellSizeAndSpacing - spacing - 1 + i).toFloat()
                    val startY = (topMargin + p.y * cellSizeAndSpacing - if (isTop) i else 0).toFloat()
                    val stopY = (topMargin + (p.y + 1) * cellSizeAndSpacing - spacing + if (isBottom) i else 0).toFloat()
                    canvas.drawLine(x, startY, x, stopY, paint)
                }
                if (isTop) {
                    val startX = (leftMargin + p.x * cellSizeAndSpacing - if (isLeft) i else 0).toFloat()
                    val stopX = (leftMargin + (p.x + 1) * cellSizeAndSpacing - spacing + if (isRight) i else 0).toFloat()
                    val y = (topMargin + p.y * cellSizeAndSpacing - i).toFloat()
                    canvas.drawLine(startX, y, stopX, y, paint)
                }
                if (isBottom) {
                    val startX = (leftMargin + p.x * cellSizeAndSpacing - if (isLeft) i else 0).toFloat()
                    val stopX = (leftMargin + (p.x + 1) * cellSizeAndSpacing - spacing + if (isRight) i else 0).toFloat()
                    val y = (topMargin + (p.y + 1) * cellSizeAndSpacing - spacing - 1 + i).toFloat()
                    canvas.drawLine(startX, y, stopX, y, paint)
                }

                /* Corners are now square with complete intersections - no fill needed */
            }
        }
    }
}
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
            for (i in 1..spacing) {
                val cellSizeAndSpacing = sl.currentCellViewSize + spacing
                /* Draw block borders - extend to corners only when both edges meet at that corner */
                
                if (isLeft) {
                    val x = (leftMargin + p.x * cellSizeAndSpacing - i).toFloat()
                    // Extend to top corner only if this is also top edge
                    val startY = (topMargin + p.y * cellSizeAndSpacing + if (!isTop) 0 else -i).toFloat()
                    // Extend to bottom corner only if this is also bottom edge  
                    val stopY = (topMargin + (p.y + 1) * cellSizeAndSpacing - spacing + if (!isBottom) 0 else i).toFloat()
                    canvas.drawLine(x, startY, x, stopY, paint)
                }
                if (isRight) {
                    val x = (leftMargin + (p.x + 1) * cellSizeAndSpacing - spacing - 1 + i).toFloat()
                    val startY = (topMargin + p.y * cellSizeAndSpacing + if (!isTop) 0 else -i).toFloat()
                    val stopY = (topMargin + (p.y + 1) * cellSizeAndSpacing - spacing + if (!isBottom) 0 else i).toFloat()
                    canvas.drawLine(x, startY, x, stopY, paint)
                }
                if (isTop) {
                    val startX = (leftMargin + p.x * cellSizeAndSpacing + if (!isLeft) 0 else -i).toFloat()
                    val stopX = (leftMargin + (p.x + 1) * cellSizeAndSpacing - spacing + if (!isRight) 0 else i).toFloat()
                    val y = (topMargin + p.y * cellSizeAndSpacing - i).toFloat()
                    canvas.drawLine(startX, y, stopX, y, paint)
                }
                if (isBottom) {
                    val startX = (leftMargin + p.x * cellSizeAndSpacing + if (!isLeft) 0 else -i).toFloat()
                    val stopX = (leftMargin + (p.x + 1) * cellSizeAndSpacing - spacing + if (!isRight) 0 else i).toFloat()
                    val y = (topMargin + (p.y + 1) * cellSizeAndSpacing - spacing - 1 + i).toFloat()
                    canvas.drawLine(startX, y, stopX, y, paint)
                }
            }
        }
    }
}
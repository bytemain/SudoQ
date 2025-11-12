/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.view.Hints

import android.content.Context
import android.graphics.Color
import android.view.View
import de.sudoq.model.solverGenerator.solution.YWingDerivation
import de.sudoq.view.SudokuLayout

class YWingView(context: Context, sl: SudokuLayout, d: YWingDerivation) : HintView(context, sl, d) {

    init {
        // Highlight the pivot cell in blue
        val pivot = d.getPivot()
        if (pivot != null) {
            val pivotView: View = HighlightedCellView(context, sl, pivot, Color.BLUE)
            highlightedObjects.add(pivotView)
        }
        
        // Highlight the pincer cells in yellow
        for (pincer in d.getPincers()) {
            val pincerView: View = HighlightedCellView(context, sl, pincer, Color.YELLOW)
            highlightedObjects.add(pincerView)
        }
    }
}

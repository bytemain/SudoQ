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
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import de.sudoq.controller.sudoku.InputListener
import de.sudoq.controller.sudoku.ObservableInput
import de.sudoq.controller.sudoku.board.CellViewPainter
import de.sudoq.controller.sudoku.board.CellViewStates
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Dieses Layout stellt ein virtuelles Keyboard zur Verfügung, in dem sich die
 * Buttons möglichst quadratisch ausrichten.
 *
 * Instanziiert ein neues VirtualKeyboardLayout mit den gegebenen Parametern
 *
 * @param context der Applikationskontext
 * @param attrs das Android AttributeSet
 */
class VirtualKeyboardLayout(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs),
    ObservableInput, Iterable<View?> {
    
    /**
     * Keyboard layout modes
     */
    enum class KeyboardLayoutMode {
        GRID,       // Square grid layout (3x3 for 9 buttons)
        HORIZONTAL  // Single horizontal row
    }
    
    /**
     * Die Buttons des VirtualKeyboard
     */
    private var buttons: Array<Array<VirtualKeyboardButtonView>>? = null
    
    /**
     * Current keyboard layout mode
     */
    var layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.GRID
        set(value) {
            if (field != value) {
                field = value
                // Re-inflate if we have buttons already created
                buttons?.let {
                    val numButtons = it.sumOf { row -> row.size }
                    if (numButtons > 0) {
                        inflate(numButtons)
                    }
                }
            }
        }

    private val buttonIterator: Iterable<VirtualKeyboardButtonView> =
        object : Iterable<VirtualKeyboardButtonView> {
            override fun iterator(): Iterator<VirtualKeyboardButtonView> {
                return object : Iterator<VirtualKeyboardButtonView> {
                    var i = 0
                    var j = 0
                    override fun hasNext(): Boolean {
                        return i < buttons!!.size
                    }

                    override fun next(): VirtualKeyboardButtonView {
                        val current = buttons!![i][j++]
                        if (j == buttons!![i].size) {
                            j = 0
                            i++
                        }
                        return current
                    }
                }
            }
        }

    /**
     * Beschreibt, ob die Tastatur deaktiviert ist.
     */
    private var deactivated = false

    /**
     * Aktualisiert das Keyboard, sodass für das angegebene Game die korrekten
     * Buttons dargestellt werden.
     *
     * @param numberOfButtons
     * Die Anzahl der Buttons für dieses Keyboard
     */
    fun refresh(numberOfButtons: Int) {
        if (numberOfButtons < 0) return
        deactivated = false
        inflate(numberOfButtons)
    }

    /**
     * Inflatet das Keyboard.
     *
     * @param numberOfButtons
     * Anzahl der Buttons dieser Tastatur
     */
    private fun inflate(numberOfButtons: Int) {
        removeAllViews()
        
        Log.d("VirtualKeyboardLayout", "inflate: numberOfButtons=$numberOfButtons, layoutMode=$layoutMode")
        
        val buttonsPerRow: Int
        val buttonsPerColumn: Int
        
        when (layoutMode) {
            KeyboardLayoutMode.GRID -> {
                // Square grid layout (e.g., 3x3 for 9 buttons)
                // buttonsPerColumn is number of rows, buttonsPerRow is number of columns
                buttonsPerColumn = floor(sqrt(numberOfButtons.toDouble())).toInt()
                buttonsPerRow = ceil(sqrt(numberOfButtons.toDouble())).toInt()
            }
            KeyboardLayoutMode.HORIZONTAL -> {
                // Single horizontal row
                buttonsPerRow = numberOfButtons
                buttonsPerColumn = 1
            }
        }
        
        Log.d("VirtualKeyboardLayout", "Layout: ${buttonsPerRow}x${buttonsPerColumn} (columns x rows)")

        // buttons[x][y] where x is column index, y is row index
        buttons = Array(buttonsPerRow) { r ->
            Array(buttonsPerColumn) { c ->
                VirtualKeyboardButtonView(context, r + c * buttonsPerRow)
            }
        }

        // Build layout: y iterates over rows, x iterates over columns
        for (y in 0 until buttonsPerColumn) {
            val la = LinearLayout(context)
            for (x in 0 until buttonsPerRow) {
                val button = buttons!![x][y]
                button.visibility = INVISIBLE
                Log.d("VirtualKeyboardLayout", "Creating button at [$x][$y] with symbol=${button.symbol}, visibility=INVISIBLE, isEnabled=${button.isEnabled}")
                val params =
                    LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.0f)
                params.leftMargin = 2
                params.bottomMargin = 2
                params.topMargin = 2
                params.rightMargin = 2
                la.addView(button, params)
            }
            addView(la, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.0f))
        }
        
        Log.d("VirtualKeyboardLayout", "inflate completed: created ${buttonsPerRow * buttonsPerColumn} buttons")
    }

    /**
     * {@inheritDoc}
     */
    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //		FieldViewPainter.getInstance().markField(canvas, this, ' ', false);
    }

    /**
     * Aktiviert bzw. deaktiviert dieses Keyboard.
     *
     * @param activated
     * Spezifiziert, ob das Keyboard aktiviert oder deaktiviert sein
     * soll
     */
    override fun setActivated(activated: Boolean) {
        Log.d("VirtualKeyboardLayout", "setActivated: $activated")
        var count = 0
        for (b in buttonIterator) {
            b.visibility = if (activated) VISIBLE else INVISIBLE
            Log.d("VirtualKeyboardLayout", "Button ${b.symbol}: visibility=${if (activated) "VISIBLE" else "INVISIBLE"}, isEnabled=${b.isEnabled}")
            count++
        }
        Log.d("VirtualKeyboardLayout", "Total buttons activated/deactivated: $count")
    }

    /**
     * Unbenutzt.
     *
     * @throws UnsupportedOperationException
     * Wirft immer eine UnsupportedOperationException
     */
    override fun notifyListeners() {
        throw UnsupportedOperationException()
    }

    /**
     * {@inheritDoc}
     */
    override fun registerListener(listener: InputListener) {
        Log.d("VirtualKeyboardLayout", "registerListener: ${listener::class.java.simpleName}")
        var count = 0
        for (b in buttonIterator) {
            b.registerListener(listener)
            count++
        }
        Log.d("VirtualKeyboardLayout", "Registered listener on $count buttons")
    }

    /**
     * {@inheritDoc}
     */
    override fun removeListener(listener: InputListener) {
        for (b in buttonIterator) b.removeListener(listener)
    }

    /**
     * Markiert das spezifizierte Feld mit dem übergebenen Status, um von dem
     * FieldViewPainter entsprechend gezeichnet zu werden.
     *
     * @param symbol
     * Das Symbol des Feldes
     * @param state
     * Der zu setzende Status
     */
    fun markCell(symbol: Int, state: CellViewStates?) {
        val buttonsPerRow = buttons!!.size
        CellViewPainter.instance!!.setMarking(
            buttons!![symbol % buttonsPerRow][symbol / buttonsPerRow],
            state!!
        )
        buttons!![symbol % buttonsPerRow][symbol / buttonsPerRow].invalidate()
    }

    /**
     * Enable all buttons of this keyboard.
     */
    fun enableAllButtons() {
        Log.d("VirtualKeyboardLayout", "enableAllButtons called")
        var count = 0
        for (b in buttonIterator) {
            b.isEnabled = true
            count++
        }
        Log.d("VirtualKeyboardLayout", "Enabled $count buttons")
    }

    fun disableAllButtons() {
        Log.d("VirtualKeyboardLayout", "disableAllButtons called")
        for (b in buttonIterator) b.isEnabled = false
    }

    fun reset() {
        for (b in buttonIterator) {
            CellViewPainter.instance!!.setMarking(
                b,
                CellViewStates.DEFAULT_BORDER
            )
        }

        this.enableAllButtons()
        this.isActivated = true
        this.invalidate()
        this.requestLayout()
    }

    /**
     * Deaktiviert den spezifizierten Button.
     *
     * @param symbol
     * Das Symbol des zu deaktivierenden Button
     */
    fun disableButton(symbol: Int) {
        val buttonsPerRow = buttons!!.size
        buttons!![symbol % buttonsPerRow][symbol / buttonsPerRow].isEnabled = false
    }

    /**
     * Sets whether to display a checkmark for the specified symbol button.
     *
     * @param symbol
     * The symbol of the button to update
     * @param showCheckmark
     * true to show checkmark, false to show the original symbol
     */
    fun setButtonCheckmark(symbol: Int, showCheckmark: Boolean) {
        val buttonsPerRow = buttons!!.size
        buttons!![symbol % buttonsPerRow][symbol / buttonsPerRow].setShowCheckmark(showCheckmark)
    }

    /**
     * {@inheritDoc}
     */
    override fun invalidate() {
        if (buttons == null) {
            return
        }
        for (b in buttonIterator) b.invalidate()
    }

    /**
     * Gibt zurueck ob die view angezeigt wird
     *
     * @return true falls aktive andernfalls false
     */
    override fun isActivated(): Boolean {
        return !deactivated
    }

    override fun iterator(): Iterator<View> {
        return object : Iterator<View> {
            var i = 0
            override fun hasNext(): Boolean {
                return i < childCount
            }

            override fun next(): View {
                return getChildAt(i++)
            }
        }
    }

    init {
        setWillNotDraw(false)
        orientation = VERTICAL
    }
}

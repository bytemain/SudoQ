package de.sudoq.persistence.game

import de.sudoq.model.game.Assistances
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import de.sudoq.persistence.sudoku.sudokuTypes.SudokuTypesListBE
import de.sudoq.persistence.XmlAttribute
import de.sudoq.persistence.XmlTree
import de.sudoq.persistence.Xmlable
import java.util.*
import kotlin.math.pow

class GameSettingsBE(val assistances: BitSet = BitSet(),
                     var isLefthandModeSet: Boolean = false,
                     var isGesturesSet: Boolean = false,
                     val wantedTypesList: SudokuTypesListBE = SudokuTypesListBE(listOf(*SudokuTypes.values()))
) : Xmlable {

    /**
     * Sets an assistance to true
     *
     * @param assistance The assistance to set
     */
    fun setAssistance(assistance: Assistances) {
        assistances.set(
            2.0.pow((assistance.ordinal + 1).toDouble()).toInt()
        ) //TODO that looks wrong...
    }


    /* to and from string */
    override fun toXmlTree(): XmlTree {
        val representation = XmlTree("gameSettings")
        representation.addAttribute(
            XmlAttribute(
                "assistances",
                convertAssistancesToString()
            )
        ) //TODO scrap that, representation as 0,1 is ugly -> save all with name, then make all of the boolean assistances enums
        representation.addAttribute(XmlAttribute("gestures", isGesturesSet))
        representation.addAttribute(XmlAttribute("left", isLefthandModeSet))
        representation.addChild(wantedTypesList.toXmlTree())
        return representation
    }

    @Throws(IllegalArgumentException::class)
    override fun fillFromXml(xmlTreeRepresentation: XmlTree) {
        assistancesfromString(xmlTreeRepresentation.getAttributeValue("assistances")!!)
        isGesturesSet =
            java.lang.Boolean.parseBoolean(xmlTreeRepresentation.getAttributeValue("gestures"))
        isLefthandModeSet =
            java.lang.Boolean.parseBoolean(xmlTreeRepresentation.getAttributeValue("left"))
        for (xt in xmlTreeRepresentation) if (xt.name == SudokuTypesListBE.ROOT_NAME) wantedTypesList.fillFromXml(
            xt
        )
    }

    /**
     * Generates a String of "0" and "1" from the AssistanceSet.
     * The String car be parsed again with [assistancesfromString].
     *
     * @return String representation of the AssistanceSet
     */
    private fun convertAssistancesToString(): String {
        val bitstring = StringBuilder()
        for (assist in Assistances.values()) bitstring.append(if (getAssistance(assist)) "1" else "0")
        return bitstring.toString()
    }
    /**
     * Checks if an assistance is set
     *
     * @param assistance [Assistances] to check
     * @return true, if assistance is set, false otherwise
     */
    open fun getAssistance(assistance: Assistances): Boolean {
        return assistances[2.0.pow((assistance.ordinal + 1).toDouble()).toInt()]
    }

    /**
     * Reads the Assistance set from a String of "0" and "1"s
     *
     * @param representation String representation of the assistances
     *
     * @throws IllegalArgumentException on parse error
     */
    @Throws(IllegalArgumentException::class)
    private fun assistancesfromString(representation: String) {
        val expectedLength = Assistances.values().size
        
        // Handle legacy profiles that may have fewer assistances saved
        if (representation.length < expectedLength) {
            // For old profiles, only read what's available
            for (i in representation.indices) {
                if (representation[i] == '1') {
                    setAssistance(Assistances.values()[i])
                }
            }
            // New assistances default to false (not set), which is already the default
        } else {
            // Normal case: read all assistances
            for ((i, assist) in Assistances.values().withIndex()) {
                try {
                    if (representation[i] == '1') {
                        setAssistance(assist)
                    }
                } catch (exc: Exception) {
                    throw IllegalArgumentException("Invalid assistances string representation: expected $expectedLength characters, but got ${representation.length}. String: '$representation'", exc)
                }
            }
        }
    }

}

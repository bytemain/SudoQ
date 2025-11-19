package de.sudoq.model.actionTree

import de.sudoq.model.sudoku.Cell
import org.amshove.kluent.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ActionTreeElementTest {
    private val cell = Cell(0, 9)
    private val action = SolveAction(5, cell)
    
    @Nested
    inner class Marking {
        @Test
        fun `should not be marked by default`() {
            val element = ActionTreeElement(1, action, null)
            
            element.isMarked `should be equal to` false
        }
        
        @Test
        fun `should be marked after calling mark`() {
            val element = ActionTreeElement(1, action, null)
            
            element.mark()
            
            element.isMarked `should be equal to` true
        }
        
        @Test
        fun `should be unmarked after calling unmark`() {
            val element = ActionTreeElement(1, action, null)
            
            element.mark()
            element.unmark()
            
            element.isMarked `should be equal to` false
        }
        
        @Test
        fun `should toggle correctly`() {
            val element = ActionTreeElement(1, action, null)
            
            // Start unmarked
            element.isMarked `should be equal to` false
            
            // Mark it
            element.mark()
            element.isMarked `should be equal to` true
            
            // Unmark it
            element.unmark()
            element.isMarked `should be equal to` false
            
            // Mark again
            element.mark()
            element.isMarked `should be equal to` true
        }
        
        @Test
        fun `should be able to unmark without marking first`() {
            val element = ActionTreeElement(1, action, null)
            
            element.unmark()
            
            element.isMarked `should be equal to` false
        }
        
        @Test
        fun `should be able to mark multiple times`() {
            val element = ActionTreeElement(1, action, null)
            
            element.mark()
            element.mark()
            element.mark()
            
            element.isMarked `should be equal to` true
        }
    }
    
    @Nested
    inner class MarkingStates {
        @Test
        fun `should not be mistake by default`() {
            val element = ActionTreeElement(1, action, null)
            
            element.isMistake `should be equal to` false
        }
        
        @Test
        fun `should be mistake after calling markWrong`() {
            val element = ActionTreeElement(1, action, null)
            
            element.markWrong()
            
            element.isMistake `should be equal to` true
        }
        
        @Test
        fun `should not be correct by default`() {
            val element = ActionTreeElement(1, action, null)
            
            element.isCorrect `should be equal to` false
        }
        
        @Test
        fun `should be correct after calling markCorrect`() {
            val element = ActionTreeElement(1, action, null)
            
            element.markCorrect()
            
            element.isCorrect `should be equal to` true
        }
    }
}

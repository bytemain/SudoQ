package de.sudoq.controller.sudoku

import android.content.Context
import android.content.SharedPreferences
import de.sudoq.model.sudoku.NoteStyle

/**
 * Manages gesture mapping preferences for the keyboard
 */
object GesturePreferences {
    private const val PREFS_NAME = "gesture_prefs"
    private const val KEY_GESTURE_UP = "gesture_up"
    private const val KEY_GESTURE_DOWN = "gesture_down"
    private const val KEY_GESTURE_LEFT = "gesture_left"
    private const val KEY_GESTURE_RIGHT = "gesture_right"
    
    // Default gesture actions
    private const val DEFAULT_UP = "STRIKETHROUGH"
    private const val DEFAULT_DOWN = "CANCEL"
    private const val DEFAULT_LEFT = "DELETE"
    private const val DEFAULT_RIGHT = "NORMAL"
    
    enum class GestureAction {
        NORMAL,         // Add normal note
        STRIKETHROUGH,  // Add strikethrough note
        DELETE,         // Delete note
        CANCEL;         // Do nothing
        
        fun toNoteStyle(): NoteStyle? = when (this) {
            NORMAL -> NoteStyle.NORMAL
            STRIKETHROUGH -> NoteStyle.STRIKETHROUGH
            DELETE -> null
            CANCEL -> null
        }
        
        fun getDisplayName(): String = when (this) {
            NORMAL -> "Normal"
            STRIKETHROUGH -> "Strike"
            DELETE -> "Delete"
            CANCEL -> "Cancel"
        }
    }
    
    fun saveGestureUp(context: Context, action: GestureAction) {
        getPrefs(context).edit()
            .putString(KEY_GESTURE_UP, action.name)
            .apply()
    }
    
    fun loadGestureUp(context: Context): GestureAction {
        val name = getPrefs(context).getString(KEY_GESTURE_UP, DEFAULT_UP)
        return try {
            GestureAction.valueOf(name ?: DEFAULT_UP)
        } catch (e: IllegalArgumentException) {
            GestureAction.valueOf(DEFAULT_UP)
        }
    }
    
    fun saveGestureDown(context: Context, action: GestureAction) {
        getPrefs(context).edit()
            .putString(KEY_GESTURE_DOWN, action.name)
            .apply()
    }
    
    fun loadGestureDown(context: Context): GestureAction {
        val name = getPrefs(context).getString(KEY_GESTURE_DOWN, DEFAULT_DOWN)
        return try {
            GestureAction.valueOf(name ?: DEFAULT_DOWN)
        } catch (e: IllegalArgumentException) {
            GestureAction.valueOf(DEFAULT_DOWN)
        }
    }
    
    fun saveGestureLeft(context: Context, action: GestureAction) {
        getPrefs(context).edit()
            .putString(KEY_GESTURE_LEFT, action.name)
            .apply()
    }
    
    fun loadGestureLeft(context: Context): GestureAction {
        val name = getPrefs(context).getString(KEY_GESTURE_LEFT, DEFAULT_LEFT)
        return try {
            GestureAction.valueOf(name ?: DEFAULT_LEFT)
        } catch (e: IllegalArgumentException) {
            GestureAction.valueOf(DEFAULT_LEFT)
        }
    }
    
    fun saveGestureRight(context: Context, action: GestureAction) {
        getPrefs(context).edit()
            .putString(KEY_GESTURE_RIGHT, action.name)
            .apply()
    }
    
    fun loadGestureRight(context: Context): GestureAction {
        val name = getPrefs(context).getString(KEY_GESTURE_RIGHT, DEFAULT_RIGHT)
        return try {
            GestureAction.valueOf(name ?: DEFAULT_RIGHT)
        } catch (e: IllegalArgumentException) {
            GestureAction.valueOf(DEFAULT_RIGHT)
        }
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

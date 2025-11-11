package de.sudoq.controller.menus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import de.sudoq.R
import de.sudoq.controller.SudoqCompatActivity
import de.sudoq.controller.menus.preferences.PlayerPreferencesActivity
import de.sudoq.controller.sudoku.SudokuActivity
import de.sudoq.model.game.GameManager
import de.sudoq.model.game.GameSettings
import de.sudoq.model.profile.ProfileManager
import de.sudoq.model.persistence.xml.game.IGamesListRepo
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import de.sudoq.persistence.game.GameRepo
import de.sudoq.persistence.game.GameSettingsBE
import de.sudoq.persistence.game.GameSettingsMapper
import de.sudoq.persistence.game.GamesListRepo
import de.sudoq.persistence.profile.ProfileRepo
import de.sudoq.persistence.profile.ProfilesListRepo
import de.sudoq.persistence.sudoku.SudokuRepoProvider
import de.sudoq.persistence.sudoku.sudokuTypes.SudokuTypesListBE
import de.sudoq.persistence.sudokuType.SudokuTypeRepo
import java.io.File

/**
 * Activity for creating a new Sudoku game with selected type and complexity
 */
class NewSudokuActivity : SudoqCompatActivity() {

    private val prefsName = "new_sudoku_prefs"
    private val keyLastType = "last_sudoku_type"
    private val keyLastComplexity = "last_complexity"
    
    private lateinit var gameSettings: GameSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load game settings from profile
        val profileDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val pm = ProfileManager(profileDir, ProfileRepo(profileDir), ProfilesListRepo(profileDir))
        check(!pm.noProfiles()) { 
            "there are no profiles. this is unexpected. they should be initialized in splashActivity" 
        }
        pm.loadCurrentProfile()
        
        val xt = GameSettingsMapper.toBE(pm.assistances).toXmlTree()
        val gameSettingsBE = GameSettingsBE()
        gameSettingsBE.fillFromXml(xt)
        gameSettings = GameSettingsMapper.fromBE(gameSettingsBE)
        
        setContent {
            MaterialTheme {
                val state = remember { mutableStateOf(createInitialState()) }
                
                NewSudokuScreen(
                    state = state.value,
                    onTypeSelected = { type ->
                        state.value = state.value.copy(selectedType = type)
                        persistType(type)
                    },
                    onComplexitySelected = { complexity ->
                        state.value = state.value.copy(selectedComplexity = complexity)
                        persistComplexity(complexity)
                    },
                    onStartGame = {
                        startGame(state.value.selectedType, state.value.selectedComplexity)
                    },
                    onNavigateToSettings = {
                        startActivity(Intent(this, PlayerPreferencesActivity::class.java))
                    },
                    onBackClick = {
                        finish()
                    }
                )
            }
        }
    }
    
    private fun createInitialState(): NewSudokuState {
        val availableTypes = gameSettings.wantedTypesList.sortedBy { type ->
            SudokuTypeOrder.getKey(type)
        }
        
        // Load persisted selections
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        
        val savedType = prefs.getString(keyLastType, null)?.let { typeName ->
            try {
                SudokuTypes.valueOf(typeName).takeIf { it in availableTypes }
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        
        val savedComplexity = prefs.getString(keyLastComplexity, null)?.let { complexityName ->
            try {
                Complexity.valueOf(complexityName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        
        return NewSudokuState(
            selectedType = savedType,
            selectedComplexity = savedComplexity,
            availableTypes = availableTypes
        )
    }
    
    private fun persistType(type: SudokuTypes) {
        getSharedPreferences(prefsName, MODE_PRIVATE)
            .edit()
            .putString(keyLastType, type.name)
            .apply()
    }
    
    private fun persistComplexity(complexity: Complexity) {
        getSharedPreferences(prefsName, MODE_PRIVATE)
            .edit()
            .putString(keyLastComplexity, complexity.name)
            .apply()
    }
    
    private fun startGame(type: SudokuTypes?, complexity: Complexity?) {
        if (type == null || complexity == null) {
            Toast.makeText(
                this,
                getString(R.string.error_sudoku_preference_incomplete),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        try {
            val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
            val pm = ProfileManager(
                profilesDir,
                ProfileRepo(profilesDir),
                ProfilesListRepo(profilesDir)
            )
            val sudokuDir = getDir(getString(R.string.path_rel_sudokus), MODE_PRIVATE)

            // Initialize parameters for game repos
            pm.loadCurrentProfile()
            val sudokuTypeRepo = SudokuTypeRepo(sudokuDir)
            val gameRepo = GameRepo(
                pm.profilesDir!!,
                pm.currentProfileID,
                sudokuTypeRepo
            )
            val gamesFile = File(pm.currentProfileDir, "games.xml")
            val gamesDir = File(pm.currentProfileDir, "games")
            val gamesListRepo: IGamesListRepo = GamesListRepo(gamesDir, gamesFile)

            // Create game
            val gm = GameManager(pm, gameRepo, gamesListRepo, sudokuTypeRepo)
            val sudokuRepoProvider = SudokuRepoProvider(sudokuDir, sudokuTypeRepo)
            val game = gm.newGame(
                type,
                complexity,
                gameSettings,
                sudokuDir,
                sudokuRepoProvider
            )
            
            check(!pm.noProfiles()) { 
                "there are no profiles. this is unexpected. they should be initialized in splashActivity" 
            }
            pm.loadCurrentProfile()
            pm.currentGame = game.id
            pm.saveChanges()
            
            startActivity(Intent(this, SudokuActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } catch (e: IllegalArgumentException) {
            Log.e(LOG_TAG, "exception: $e")
            Toast.makeText(
                this,
                getString(R.string.sf_sudokupreferences_copying),
                Toast.LENGTH_SHORT
            ).show()
            Log.d(LOG_TAG, "no template found - 'wait please'")
        }
    }

    companion object {
        private val LOG_TAG = NewSudokuActivity::class.java.simpleName
    }
}

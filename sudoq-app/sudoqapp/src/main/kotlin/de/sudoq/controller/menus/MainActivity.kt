package de.sudoq.controller.menus

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import de.sudoq.R
import de.sudoq.controller.SudoqCompatActivity
import de.sudoq.controller.menus.preferences.PlayerPreferencesActivity
import de.sudoq.controller.sudoku.SudokuActivity
import de.sudoq.model.profile.ProfileManager
import de.sudoq.persistence.profile.ProfileRepo
import de.sudoq.persistence.profile.ProfilesListRepo
import de.sudoq.view.theme.SudoQTheme
import de.sudoq.view.theme.ThemeManager
import java.io.File

/**
 * Main menu activity of the app
 */
class MainActivity : SudoqCompatActivity() {
    private lateinit var profilesFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profilesFile = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        
        setContent {
            val themeColor = ThemeManager.loadThemeColor(this)
            val darkMode = ThemeManager.loadDarkMode(this)
            
            SudoQTheme(
                themeColor = themeColor,
                darkTheme = darkMode
            ) {
                MainScreen(
                    state = getMainScreenState(),
                    onNewGameClick = {
                        startActivity(Intent(this, NewSudokuActivity::class.java))
                    },
                    onContinueGameClick = {
                        startActivity(Intent(this, SudokuActivity::class.java))
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    },
                    onLoadGameClick = {
                        startActivity(Intent(this, SudokuLoadingActivity::class.java))
                    },
                    onProfileClick = {
                        startActivity(Intent(this, PlayerPreferencesActivity::class.java))
                    },
                    onStatisticsClick = {
                        startActivity(Intent(this, StatisticsActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the screen when returning
        setContent {
            val themeColor = ThemeManager.loadThemeColor(this)
            val darkMode = ThemeManager.loadDarkMode(this)
            
            SudoQTheme(
                themeColor = themeColor,
                darkTheme = darkMode
            ) {
                MainScreen(
                    state = getMainScreenState(),
                    onNewGameClick = {
                        startActivity(Intent(this, NewSudokuActivity::class.java))
                    },
                    onContinueGameClick = {
                        startActivity(Intent(this, SudokuActivity::class.java))
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    },
                    onLoadGameClick = {
                        startActivity(Intent(this, SudokuLoadingActivity::class.java))
                    },
                    onProfileClick = {
                        startActivity(Intent(this, PlayerPreferencesActivity::class.java))
                    },
                    onStatisticsClick = {
                        startActivity(Intent(this, StatisticsActivity::class.java))
                    }
                )
            }
        }
    }

    private fun getMainScreenState(): MainScreenState {
        val pm = ProfileManager(
            profilesFile,
            ProfileRepo(profilesFile),
            ProfilesListRepo(profilesFile)
        )
        
        check(!pm.noProfiles()) {
            "there are no profiles. this is unexpected. they should be initialized in splashActivity"
        }
        
        pm.loadCurrentProfile()
        
        return MainScreenState(
            profileName = pm.name ?: getString(R.string.default_user_name),
            hasCurrentGame = pm.currentGame > ProfileManager.NO_GAME,
            currentGameProgress = 0, // TODO: Calculate actual progress
            gamesPlayed = pm.getStatistic(de.sudoq.model.profile.Statistics.playedSudokus),
            gamesWon = pm.getStatistic(de.sudoq.model.profile.Statistics.playedSudokus), // TODO: Get actual won count
            currentScore = pm.getStatistic(de.sudoq.model.profile.Statistics.maximumPoints)
        )
    }
}

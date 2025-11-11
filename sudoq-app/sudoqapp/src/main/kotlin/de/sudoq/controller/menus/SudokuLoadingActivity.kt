/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.menus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import de.sudoq.BuildConfig
import de.sudoq.R
import de.sudoq.controller.sudoku.SudokuActivity
import de.sudoq.model.game.GameManager
import de.sudoq.model.persistence.xml.game.IGamesListRepo
import de.sudoq.model.profile.ProfileManager
import de.sudoq.persistence.game.GameRepo
import de.sudoq.persistence.game.GamesListRepo
import de.sudoq.persistence.profile.ProfileRepo
import de.sudoq.persistence.profile.ProfilesListRepo
import de.sudoq.persistence.sudokuType.SudokuTypeRepo
import de.sudoq.view.theme.SudoQTheme
import de.sudoq.view.theme.ThemeManager
import java.io.*
import java.nio.charset.Charset

class SudokuLoadingActivity : ComponentActivity() {
    private lateinit var profileManager: ProfileManager
    private lateinit var gameManager: GameManager
    private lateinit var sudokuTypeRepo: SudokuTypeRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val sudokuDir = getDir(getString(R.string.path_rel_sudokus), MODE_PRIVATE)
        
        sudokuTypeRepo = SudokuTypeRepo(sudokuDir)
        profileManager = ProfileManager(
            profilesDir,
            ProfileRepo(profilesDir),
            ProfilesListRepo(profilesDir)
        )

        check(!profileManager.noProfiles()) {
            "there are no profiles. this is unexpected. they should be initialized in splashActivity"
        }
        
        profileManager.loadCurrentProfile()
        
        val gameRepo = GameRepo(
            profileManager.profilesDir!!,
            profileManager.currentProfileID,
            sudokuTypeRepo
        )
        val gamesFile = File(profileManager.currentProfileDir, "games.xml")
        val gamesDir = File(profileManager.currentProfileDir, "games")
        val gamesListRepo: IGamesListRepo = GamesListRepo(gamesDir, gamesFile)

        gameManager = GameManager(profileManager, gameRepo, gamesListRepo, sudokuTypeRepo)

        setContent {
            val themeColor = ThemeManager.loadThemeColor(this)
            val darkMode = ThemeManager.loadDarkMode(this)
            
            var games by remember { mutableStateOf(gameManager.gameList) }
            
            SudoQTheme(
                themeColor = themeColor,
                darkTheme = darkMode
            ) {
                SudokuLoadingScreen(
                    games = games,
                    gameRepo = gameRepo,
                    onBackPressed = { finish() },
                    onGameClick = { gameId ->
                        profileManager.currentGame = gameId
                        startActivity(Intent(this, SudokuActivity::class.java))
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    },
                    onDeleteGame = { gameId ->
                        gameManager.deleteGame(gameId)
                        games = gameManager.gameList
                    },
                    onDeleteFinished = {
                        gameManager.deleteFinishedGames()
                        games = gameManager.gameList
                    },
                    onDeleteAll = {
                        gameManager.gameList.forEach { gameManager.deleteGame(it.id) }
                        games = gameManager.gameList
                    },
                    onExportGameAsText = if (profileManager.appSettings.isDebugSet) {
                        { gameId -> exportGameAsText(gameId) }
                    } else null,
                    onExportGameAsFile = if (profileManager.appSettings.isDebugSet) {
                        { gameId -> exportGameAsFile(gameId) }
                    } else null
                )
            }
        }
    }

    private fun exportGameAsText(gameId: Int) {
        try {
            val gameRepo = GameRepo(
                profileManager.profilesDir!!,
                profileManager.currentProfileID,
                sudokuTypeRepo
            )
            val gameFile = gameRepo.getGameFile(gameId)
            val str = gameFile.readText(Charset.forName("UTF-8"))
            
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, str)
                type = "text/plain"
            }
            startActivity(sendIntent)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error exporting game as text", e)
        }
    }

    private fun exportGameAsFile(gameId: Int) {
        try {
            val gameRepo = GameRepo(
                profileManager.profilesDir!!,
                profileManager.currentProfileID,
                sudokuTypeRepo
            )
            val gameFile = gameRepo.getGameFile(gameId)
            
            // Copy to files directory for FileProvider
            val tmpFile = File(filesDir, gameFile.name)
            gameFile.inputStream().use { input ->
                tmpFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            val fileUri = FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                tmpFile
            )
            
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(sendIntent)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error exporting game as file", e)
        }
    }

    companion object {
        private val LOG_TAG = SudokuLoadingActivity::class.java.simpleName
    }
}

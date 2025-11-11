/*
 * SudoQ is a Sudoku-App for Android Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.test

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.sudoq.controller.sudoku.WinDialog

/**
 * Test Activity for Compose components
 * Only available in debug builds
 */
class ComposeTestActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                ComposeTestScreen()
            }
        }
    }
    
    @Composable
    fun ComposeTestScreen() {
        var showWinDialog by remember { mutableStateOf(false) }
        var showSurrenderDialog by remember { mutableStateOf(false) }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Compose Dialog Test",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                Button(
                    onClick = { showWinDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("测试胜利弹窗 (Win Dialog)")
                }
                
                Button(
                    onClick = { showSurrenderDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("测试投降弹窗 (Surrender Dialog)")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedButton(
                    onClick = { finish() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("返回 (Back)")
                }
            }
        }
        
        // Show win dialog
        if (showWinDialog) {
            WinDialog(
                surrendered = false,
                timeNeeded = "12:34",
                score = 8765,
                onDismiss = {
                    showWinDialog = false
                    Toast.makeText(this, "继续游戏 (Continue)", Toast.LENGTH_SHORT).show()
                },
                onFinish = {
                    showWinDialog = false
                    Toast.makeText(this, "结束游戏 (Finish)", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        // Show surrender dialog
        if (showSurrenderDialog) {
            WinDialog(
                surrendered = true,
                timeNeeded = "08:45",
                score = 4321,
                onDismiss = {
                    showSurrenderDialog = false
                    Toast.makeText(this, "继续游戏 (Continue)", Toast.LENGTH_SHORT).show()
                },
                onFinish = {
                    showSurrenderDialog = false
                    Toast.makeText(this, "结束游戏 (Finish)", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

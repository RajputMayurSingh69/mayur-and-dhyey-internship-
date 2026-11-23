package com.example.mayuranddhyeyinternshipproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.mayuranddhyeyinternshipproject.data.repository.SecurityPreferences
import com.example.mayuranddhyeyinternshipproject.navigation.MedMateNavGraph
import com.example.mayuranddhyeyinternshipproject.ui.theme.MedMateTheme

/**
 * Main Activity for MedMate: Medicine Reminder App.
 *
 * Health Data Notice:
 * This application handles personal health schedules and medicine adherence tracking.
 * All data access is governed under strict user isolation and Firestore security rules.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val securityPreferences = SecurityPreferences(this)

        setContent {
            var isDarkMode by remember {
                mutableStateOf(securityPreferences.isDarkMode)
            }

            MedMateTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    MedMateNavGraph(
                        navController = navController,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { enabled ->
                            isDarkMode = enabled
                            securityPreferences.isDarkMode = enabled
                        },
                        onExitApp = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

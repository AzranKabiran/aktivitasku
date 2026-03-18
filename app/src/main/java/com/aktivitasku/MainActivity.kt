package com.aktivitasku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.aktivitasku.presentation.navigation.AktivitasKuNavGraph
import com.aktivitasku.presentation.settings.PrefKeys
import com.aktivitasku.presentation.settings.dataStore
import com.aktivitasku.presentation.theme.AktivitasKuTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Read dark mode preference from DataStore reactively
            val darkMode by applicationContext.dataStore.data
                .map { it[PrefKeys.DARK_MODE] ?: false }
                .collectAsState(initial = false)

            AktivitasKuTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    AktivitasKuNavGraph()
                }
            }
        }
    }
}

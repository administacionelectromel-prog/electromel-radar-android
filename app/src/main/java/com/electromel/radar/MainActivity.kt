package com.electromel.radar

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.electromel.radar.ui.*

/**
 * Fase 2: los leads se persisten en Room. Al abrir la app, se cargan solos.
 * El import guarda en disco; ya no hay que reimportar en cada sesión.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = RadarColors.bg,
                primary    = RadarColors.accent,
                secondary  = RadarColors.orange
            )) {
                val vm: TerrenoViewModel = viewModel()
                val state by vm.state.collectAsState()

                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        val texto = contentResolver.openInputStream(it)
                            ?.bufferedReader()?.use { r -> r.readText() } ?: ""
                        if (texto.isNotBlank()) vm.importarBackup(texto)
                    }
                }

                Surface(color = RadarColors.bg) {
                    TerrenoScreen(
                        state = state,
                        onImportarClick = { picker.launch("application/json") }
                    )
                }
            }
        }
    }
}

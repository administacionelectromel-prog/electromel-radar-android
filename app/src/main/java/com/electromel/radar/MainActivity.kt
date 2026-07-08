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
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.electromel.radar.ui.*

/**
 * Fase 1: pantalla Terreno con datos reales importados del backup JSON de la PWA.
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

                // Selector de archivo del sistema (SAF) — devuelve un Uri
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        val texto = contentResolver.openInputStream(it)
                            ?.bufferedReader()?.use { r -> r.readText() } ?: ""
                        if (texto.isNotBlank()) vm.importarBackup(texto)
                    }
                }

                Surface(color = Color(0xFF04080F)) {
                    TerrenoScreen(
                        state = state,
                        onImportarClick = { picker.launch("application/json") }
                    )
                }
            }
        }
    }
}

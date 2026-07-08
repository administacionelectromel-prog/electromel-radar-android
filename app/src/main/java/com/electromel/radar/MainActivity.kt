package com.electromel.radar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.electromel.radar.domain.*

/**
 * Fase 0 de la migración: verifica que el motor de dominio portado
 * funciona en Android. Muestra el IUT de un lead de prueba.
 * Las pantallas reales (Terreno/Buscar/Leads) vienen en la fase 1.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF04080F),
                primary    = Color(0xFF00E8A0),
                secondary  = Color(0xFFFF6B1A)
            )) {
                val demo = remember {
                    Lead(id = "demo", nombre = "Metalúrgica San Jorge",
                         rubro = "industrial", fuente = "terreno",
                         equipos = listOf("soldadora", "tablero"))
                }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp),
                           verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⚡ ELECTROMEL RADAR", style = MaterialTheme.typography.headlineMedium)
                        Text("Motor de dominio nativo — Fase 0")
                        Spacer(Modifier.height(16.dp))
                        Text("Lead demo: ${demo.nombre}")
                        Text("IUT: ${IutEngine.calcular(demo)}",
                             style = MaterialTheme.typography.headlineLarge,
                             color = MaterialTheme.colorScheme.secondary)
                        Text("Dist Nqn→Cipolletti: %.1f km".format(
                            GeoUtils.distKm(-38.9516, -68.0591, -38.9440, -68.0070)))
                    }
                }
            }
        }
    }
}

package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Contenedor principal: alterna entre la LISTA de terreno y el MAPA.
 * Botón flotante en el mapa para centrar en tu ubicación.
 */
@Composable
fun RadarApp(
    state: TerrenoState,
    onImportarClick: () -> Unit,
    onLeadClick: (String) -> Unit,
    onCerrarLead: () -> Unit,
    onCambiarEstado: (String, String) -> Unit
) {
    var tab by remember { mutableStateOf(1) }   // arranca en MAPA (1); lista = 0
    var centrarUser by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).statusBarsPadding().navigationBarsPadding()) {

        // Selector de pestañas
        Row(
            Modifier.fillMaxWidth().background(RadarColors.bgCard).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabBtn("📋 LISTA", tab == 0) { tab = 0 }
            TabBtn("🗺️ MAPA", tab == 1) { tab = 1 }
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> TerrenoScreen(state = state, onImportarClick = onImportarClick, onLeadClick = onLeadClick)
                1 -> {
                    MapaView(
                        leads = state.leads,
                        userLat = state.userLat,
                        userLon = state.userLon,
                        centrarEnUser = centrarUser,
                        onLeadClick = onLeadClick,
                        modifier = Modifier.fillMaxSize()
                    )
                    // FAB centrar en usuario
                    FloatingActionButton(
                        onClick = { centrarUser++ },
                        containerColor = RadarColors.orange,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) { Text("◎", fontSize = 22.sp) }

                    // Aviso si no hay GPS
                    if (state.userLat == null) {
                        Surface(
                            color = RadarColors.bgPanel,
                            modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                        ) {
                            Text("Esperando GPS...", color = RadarColors.textDim,
                                 fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Ficha del lead seleccionado (overlay)
    state.leadSeleccionado?.let { sel ->
        LeadFicha(
            item = sel,
            onCerrar = onCerrarLead,
            onCambiarEstado = { nuevo -> onCambiarEstado(sel.lead.id, nuevo) }
        )
    }
}

@Composable
private fun RowScope.TabBtn(label: String, activo: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (activo) RadarColors.accent else RadarColors.bgPanel,
            contentColor = if (activo) RadarColors.bg else RadarColors.textDim
        )
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
}

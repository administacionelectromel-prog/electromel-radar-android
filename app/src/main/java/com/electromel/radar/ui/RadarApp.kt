package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.zIndex

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
    onCambiarEstado: (String, String) -> Unit,
    onGenerarRuta: () -> Unit,
    onGuardarConfig: (String, String, String, String) -> Unit,
    onCapturar: (String, String, List<String>, List<String>, String) -> Unit,
    onExportar: (String, com.electromel.radar.domain.ExportEngine.Filtro) -> Unit,
    onBuscar: (String, String, Boolean) -> Unit,
    onGuardarResultado: (com.electromel.radar.domain.BuscarEngine.Resultado) -> Unit
) {
    var tab by remember { mutableStateOf(1) }   // arranca en MAPA (1); lista = 0
    var centrarUser by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).statusBarsPadding().navigationBarsPadding()) {

        // Selector de pestañas — zIndex alto para quedar SOBRE el MapView
        // nativo (osmdroid captura toques de su área via AndroidView)
        Row(
            Modifier.fillMaxWidth().zIndex(10f).background(RadarColors.bgCard)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabBtn("📋 LISTA", tab == 0) { tab = 0 }
            TabBtn("🗺️ MAPA", tab == 1) { tab = 1 }
            TabBtn("⚡ HOY", tab == 2) { tab = 2 }
            TabBtn("🧭 RUTA", tab == 3) { tab = 3 }
            TabBtn("📊 STATS", tab == 4) { tab = 4 }
            TabBtn("➕ CAPT", tab == 5) { tab = 5 }
            TabBtn("⬇ EXP", tab == 6) { tab = 6 }
            TabBtn("🔍 BUSCAR", tab == 8) { tab = 8 }
            TabBtn("⚙️ CFG", tab == 7) { tab = 7 }
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> TerrenoScreen(state = state, onImportarClick = onImportarClick, onLeadClick = onLeadClick)
                2 -> HoyScreen(state = state, onLeadClick = onLeadClick)
                3 -> RutaScreen(state = state, onGenerarRuta = onGenerarRuta, onLeadClick = onLeadClick)
                4 -> StatsScreen(state = state, onLeadClick = onLeadClick)
                5 -> CapturaScreen(tieneGps = state.userLat != null, onGuardar = onCapturar)
                6 -> ExportarScreen(state = state, onExportar = onExportar, onImportar = onImportarClick)
                8 -> BuscarScreen(state = state, onBuscar = onBuscar, onGuardarResultado = onGuardarResultado)
                7 -> ConfigScreen(
                        googleKey = state.googleKey,
                        msgPrimero = state.msgPrimero,
                        msgSeguimiento = state.msgSeguimiento,
                        msgCierre = state.msgCierre,
                        onGuardar = onGuardarConfig
                    )
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
private fun TabBtn(label: String, activo: Boolean, onClick: () -> Unit) {
    // Estilo píldora como la PWA (.mode-tab): chico, redondeado, con borde
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (activo) RadarColors.accent else RadarColors.bgCard)
            .border(1.dp, if (activo) RadarColors.accent else RadarColors.border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 5.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp,
             color = if (activo) RadarColors.bg else RadarColors.textDim)
    }
}

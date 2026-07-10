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
    onGuardarResultado: (com.electromel.radar.domain.BuscarEngine.Resultado) -> Unit,
    onGuardarTodos: () -> Unit
) {
    var tab by remember { mutableStateOf(1) }   // arranca en MAPA (1); lista = 0
    var centrarUser by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(RadarColors.bg).statusBarsPadding().navigationBarsPadding()) {

        // Selector de pestañas — zIndex alto para quedar SOBRE el MapView
        // nativo (osmdroid captura toques de su área via AndroidView)
        Row(
            Modifier.fillMaxWidth().zIndex(10f).background(RadarColors.bg)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabBtn("TERRENO", tab == 0) { tab = 0 }
            TabBtn("BUSCAR", tab == 1) { tab = 1 }
            TabBtn("LEADS", tab == 2) { tab = 2 }
            TabBtn("HOY", tab == 3) { tab = 3 }
            TabBtn("RUTA", tab == 4) { tab = 4 }
            TabBtn("ZONAS", tab == 5) { tab = 5 }
            TabBtn("STATS", tab == 6) { tab = 6 }
            TabBtn("CONFIG", tab == 7) { tab = 7 }
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> TerrenoConMapa(state, centrarUser, { centrarUser++ }, onImportarClick, onLeadClick)
                1 -> BuscarScreen(state = state, onBuscar = onBuscar, onGuardarResultado = onGuardarResultado, onGuardarTodos = onGuardarTodos)
                2 -> TerrenoScreen(state = state, onImportarClick = onImportarClick, onLeadClick = onLeadClick)
                3 -> HoyScreen(state = state, onLeadClick = onLeadClick)
                4 -> RutaScreen(state = state, onGenerarRuta = onGenerarRuta, onLeadClick = onLeadClick)
                5 -> ZonasScreenPlaceholder()
                6 -> StatsScreen(state = state, onLeadClick = onLeadClick)
                7 -> ConfigScreen(
                        googleKey = state.googleKey,
                        msgPrimero = state.msgPrimero,
                        msgSeguimiento = state.msgSeguimiento,
                        msgCierre = state.msgCierre,
                        onGuardar = onGuardarConfig
                    )
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

/* TERRENO como la PWA: mapa arriba (55%) + lista abajo (45%). */
@Composable
private fun TerrenoConMapa(
    state: TerrenoState,
    centrarUser: Int,
    onCentrar: () -> Unit,
    onImportarClick: () -> Unit,
    onLeadClick: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(0.55f)) {
            MapaView(
                leads = state.leads,
                userLat = state.userLat,
                userLon = state.userLon,
                centrarEnUser = centrarUser,
                onLeadClick = onLeadClick,
                modifier = Modifier.fillMaxSize()
            )
            FloatingActionButton(
                onClick = onCentrar,
                containerColor = RadarColors.orange,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Text("\u25ce", fontSize = 22.sp) }
        }
        Box(Modifier.fillMaxWidth().weight(0.45f)) {
            TerrenoScreen(state = state, onImportarClick = onImportarClick, onLeadClick = onLeadClick)
        }
    }
}

@Composable
private fun ZonasScreenPlaceholder() {
    Box(Modifier.fillMaxSize().background(RadarColors.bg), contentAlignment = Alignment.Center) {
        Text("ZONAS \u2014 en construcci\u00f3n", color = RadarColors.textDim, fontSize = 14.sp)
    }
}

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
    onGuardarFicha: (com.electromel.radar.domain.Lead) -> Unit,
    onEliminarLead: (String) -> Unit,
    onAgregarFoto: (String, String) -> Unit,
    onQuitarFoto: (String, Int) -> Unit,
    onGenerarRuta: () -> Unit,   // lo consumirá el panel ARRANCAR DÍA (loop TERRENO)
    onAgregarManual: (String) -> Unit,
    onMoverParada: (Int, Int) -> Unit,
    onQuitarParada: (Int) -> Unit,
    onLimpiarRuta: () -> Unit,
    onIniciarRecorrido: () -> Unit,
    onGuardarApiKey: (String) -> Unit,
    onAgregarZona: (String) -> Unit,
    onQuitarZona: (String) -> Unit,
    onGuardarPlantillas: (String, String, String, String) -> Unit,
    onRestaurarPlantillas: () -> Unit,
    onRecalcularZonas: (com.electromel.radar.domain.ZonasEngine.Modo, Int) -> Unit,
    onZonaARuta: (com.electromel.radar.domain.ZonasEngine.Zona) -> Unit,
    onWhatsAppSeguimiento: (com.electromel.radar.domain.Lead) -> Unit,
    onPostergar: (String) -> Unit,
    onWhatsAppPrimero: (com.electromel.radar.domain.Lead) -> Unit,
    onMaps: (com.electromel.radar.domain.Lead) -> Unit,
    onAgregarARuta: (String) -> Unit,
    onLlamar: (com.electromel.radar.domain.Lead) -> Unit,
    onVisitado: (String) -> Unit,
    onUrgente: (String) -> Unit,
    onGuardarNota: (String, String) -> Unit,
    onIniciarDia: () -> Unit,
    onRegenerarDia: () -> Unit,
    onCapturar: (String, String, List<String>, List<String>, String) -> Unit,
    onExportar: (String, com.electromel.radar.domain.ExportEngine.Filtro) -> Unit,
    onBuscar: (String, String, Boolean) -> Unit,
    onGuardarResultado: (com.electromel.radar.domain.BuscarEngine.Resultado) -> Unit,
    onGuardarTodos: () -> Unit,
    onCampanaWhatsapp: () -> Unit,
    onBorrarTodo: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }   // arranca en TERRENO (mapa + lista)
    var centrarUser by remember { mutableStateOf(0) }
    var centrarPunto by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var centrarPuntoTick by remember { mutableStateOf(0) }
    var mostrarCaptura by remember { mutableStateOf(false) }
    var mostrarExportar by remember { mutableStateOf(false) }
    var mapLeadsVisible by remember { mutableStateOf(true) }
    var busquedaRapida by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(RadarColors.bg)) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {

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
                0 -> TerrenoConMapa(state, centrarUser, centrarPunto, centrarPuntoTick,
                        mapLeadsVisible, { centrarUser++ }, onLeadClick,
                        onMaps, onWhatsAppPrimero, onLlamar, onVisitado, onUrgente,
                        onGuardarNota, onAgregarARuta,
                        onIniciarDiaNav = { onIniciarDia(); tab = 4 },
                        onRegenerarDia = onRegenerarDia)
                1 -> BuscarScreen(state = state, onBuscar = onBuscar, onGuardarResultado = onGuardarResultado, onGuardarTodos = onGuardarTodos)
                2 -> LeadsScreen(state = state, onLeadClick = onLeadClick,
                        onWhatsAppPrimero = onWhatsAppPrimero,
                        onMaps = onMaps,
                        onAgregarARuta = onAgregarARuta)
                3 -> HoyScreen(state = state, onLeadClick = onLeadClick,
                        onWhatsAppSeguimiento = onWhatsAppSeguimiento,
                        onPostergar = onPostergar)
                4 -> RutaScreen(state = state,
                        onAgregarManual = onAgregarManual,
                        onMover = onMoverParada,
                        onQuitar = onQuitarParada,
                        onLimpiar = onLimpiarRuta,
                        onIniciar = onIniciarRecorrido)
                5 -> ZonasScreen(
                        state = state,
                        onRecalcular = onRecalcularZonas,
                        onLeadClick = onLeadClick,
                        onZonaARuta = { z -> onZonaARuta(z); tab = 4 },
                        onVerEnMapa = { lat, lon ->
                            centrarPunto = lat to lon; centrarPuntoTick++; tab = 0
                        }
                    )
                6 -> StatsScreen(
                        state = state,
                        onLeadClick = onLeadClick,
                        onCampanaWhatsapp = onCampanaWhatsapp,
                        onExportarAvanzado = { mostrarExportar = true },
                        onExportarJson = { onExportar("json", com.electromel.radar.domain.ExportEngine.Filtro.TODOS) },
                        onImportar = onImportarClick,
                        onBorrarTodo = onBorrarTodo
                    )
                7 -> ConfigScreen(
                        googleKey = state.googleKey,
                        zonasExtra = state.zonasExtra,
                        mensajes = state.mensajes,
                        onGuardarApiKey = onGuardarApiKey,
                        onAgregarZona = onAgregarZona,
                        onQuitarZona = onQuitarZona,
                        onGuardarPlantillas = onGuardarPlantillas,
                        onRestaurarPlantillas = onRestaurarPlantillas
                    )
            }
        }
    }

    // FAB "+" naranja global — captura rápida desde cualquier pestaña (como la PWA)
    FloatingActionButton(
        onClick = { mostrarCaptura = true },
        containerColor = RadarColors.orange,
        modifier = Modifier.align(Alignment.BottomEnd)
            .navigationBarsPadding().padding(16.dp).zIndex(20f)
    ) { Text("+", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = RadarColors.bg) }

    // Overlay de EXPORTAR / SINCRONIZAR (modal como la PWA)
    if (mostrarExportar) {
        Box(Modifier.fillMaxSize().zIndex(30f).background(RadarColors.bg)
            .statusBarsPadding().navigationBarsPadding()) {
            ExportarScreen(
                state = state,
                onExportar = { f, filtro -> onExportar(f, filtro); mostrarExportar = false },
                onImportar = { onImportarClick(); mostrarExportar = false }
            )
            Text("✕", color = RadarColors.textDim, fontSize = 22.sp,
                 fontWeight = FontWeight.Bold,
                 modifier = Modifier.align(Alignment.TopEnd)
                     .clickable { mostrarExportar = false }
                     .padding(16.dp))
        }
    }

    // Overlay de CAPTURA RÁPIDA (modal como la PWA)
    if (mostrarCaptura) {
        Box(Modifier.fillMaxSize().zIndex(30f).background(RadarColors.bg)
            .statusBarsPadding().navigationBarsPadding()) {
            CapturaScreen(
                tieneGps = state.userLat != null,
                onGuardar = { nom, tipo, eq, tags, tel ->
                    onCapturar(nom, tipo, eq, tags, tel)
                    mostrarCaptura = false
                }
            )
            Text("✕", color = RadarColors.textDim, fontSize = 22.sp,
                 fontWeight = FontWeight.Bold,
                 modifier = Modifier.align(Alignment.TopEnd)
                     .clickable { mostrarCaptura = false }
                     .padding(16.dp))
        }
    }

    // Ficha del lead seleccionado (overlay)
    state.leadSeleccionado?.let { sel ->
        LeadFicha(
            item = sel,
            mensajes = state.mensajes,
            onCerrar = onCerrarLead,
            onGuardar = onGuardarFicha,
            onEliminar = onEliminarLead,
            onAgregarFoto = onAgregarFoto,
            onQuitarFoto = onQuitarFoto,
            onMaps = onMaps,
            onAgregarARuta = onAgregarARuta,
            onWhatsAppPrimero = onWhatsAppPrimero,
            onWhatsAppSeguimiento = onWhatsAppSeguimiento
        )
    }
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
    centrarPunto: Pair<Double, Double>?,
    centrarPuntoTick: Int,
    mostrarLeads: Boolean,
    onCentrar: () -> Unit,
    onLeadClick: (String) -> Unit,
    onMaps: (com.electromel.radar.domain.Lead) -> Unit,
    onWhatsAppPrimero: (com.electromel.radar.domain.Lead) -> Unit,
    onLlamar: (com.electromel.radar.domain.Lead) -> Unit,
    onVisitado: (String) -> Unit,
    onUrgente: (String) -> Unit,
    onGuardarNota: (String, String) -> Unit,
    onAgregarARuta: (String) -> Unit,
    onIniciarDiaNav: () -> Unit,
    onRegenerarDia: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(0.55f)) {
            MapaView(
                leads = state.leads,
                userLat = state.userLat,
                userLon = state.userLon,
                centrarEnUser = centrarUser,
                centrarPunto = centrarPunto,
                centrarPuntoTick = centrarPuntoTick,
                mostrarLeads = mostrarLeads,
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
            TerrenoScreen(state = state, onLeadClick = onLeadClick,
                onMaps = onMaps, onWhatsAppPrimero = onWhatsAppPrimero,
                onLlamar = onLlamar, onVisitado = onVisitado, onUrgente = onUrgente,
                onGuardarNota = onGuardarNota, onAgregarARuta = onAgregarARuta,
                onIniciarDia = onIniciarDiaNav, onRegenerarDia = onRegenerarDia)
        }
    }
}


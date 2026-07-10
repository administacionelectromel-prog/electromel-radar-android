package com.electromel.radar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.electromel.radar.data.LeadStore
import com.electromel.radar.data.RadarDatabase
import com.electromel.radar.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

data class LeadUi(
    val lead: Lead,
    val iut: Int,
    val prioridad: PrioridadEngine.Prioridad
)

data class TerrenoState(
    val leads: List<LeadUi> = emptyList(),
    val cargando: Boolean = true,
    val mensaje: String = "",
    val userLat: Double? = null,
    val userLon: Double? = null,
    val leadSeleccionado: LeadUi? = null,
    val objetivosDia: List<LeadUi> = emptyList(),
    val diaSeguimientos: Int = 0,
    val diaUrgentes: Int = 0,
    val diaSinContacto: Int = 0,
    val ruta: List<LeadUi> = emptyList(),
    val rutaDistanciaKm: Double = 0.0,
    val statsResumen: StatsEngine.Resumen = StatsEngine.Resumen(0,0,0,0,0,0,0,0),
    val statsConversion: List<StatsEngine.ConversionRubro> = emptyList(),
    val statsRevisitas: List<StatsEngine.Revisita> = emptyList(),
    val statsEquipos: List<StatsEngine.EquipoTop> = emptyList(),
    val googleKey: String = "",
    val mensajes: Map<String, Map<String, String>> = Mensajes.DEFAULT,
    val zonas: List<ZonasEngine.Zona> = emptyList(),
    val zonasModo: ZonasEngine.Modo = ZonasEngine.Modo.AUTO,
    val zonasRadio: Int = 800,
    val buscando: Boolean = false,
    val buscarError: String = "",
    val resultadosBusqueda: List<BuscarEngine.Resultado> = emptyList(),
    val zonasExtra: List<String> = emptyList()
)

/**
 * AndroidViewModel para tener Context (necesario para Room).
 * Carga los leads persistidos al abrir; el import los guarda en disco.
 */
class TerrenoViewModel(app: Application) : AndroidViewModel(app) {

    private val jsonCfg = Json { ignoreUnknownKeys = true }
    private val db = RadarDatabase.get(app)
    private val store = LeadStore(db.leadDao(), db.configDao())
    private val _state = MutableStateFlow(TerrenoState())
    val state: StateFlow<TerrenoState> = _state

    init {
        viewModelScope.launch {
            store.cargar()
            cargarConfig()
            recomputar()
        }
    }

    /** Import con modo reemplazar (default). Persiste en Room. */
    fun importarBackup(contenido: String, merge: Boolean = false) {
        BackupParser.parsearLeads(contenido)
            .onSuccess { leads ->
                viewModelScope.launch {
                    val n = if (merge) store.merge(leads) else store.replaceAll(leads)
                    recomputar(
                        if (merge) "Merge: +$n leads nuevos"
                        else "Importados $n leads"
                    )
                }
            }
            .onFailure {
                _state.value = _state.value.copy(
                    cargando = false,
                    mensaje = "No se pudo leer el archivo. ¿Es un backup de ELECTROMEL RADAR?"
                )
            }
    }

    fun abrirLead(id: String) {
        val ui = _state.value.leads.find { it.lead.id == id }
        _state.value = _state.value.copy(leadSeleccionado = ui)
    }

    fun cerrarLead() {
        _state.value = _state.value.copy(leadSeleccionado = null)
    }

    /** Cambia el estado del lead, persiste, y recalcula prioridad/orden. */
    fun cambiarEstado(id: String, nuevoEstado: String) {
        val lead = store.byId(id) ?: return
        val actualizado = lead.copy(
            estado = nuevoEstado,
            historial = lead.historial + EventoHistorial(
                fecha = java.time.Instant.now().toString(),
                accion = "Estado → $nuevoEstado"
            )
        )
        viewModelScope.launch {
            store.upsert(actualizado)
            recomputar()
        }
    }

    private suspend fun cargarConfig() {
        val cfg = store.getConfig()
        val msgs = cfg["mensajes"]?.let {
            try { jsonCfg.decodeFromString<Map<String, Map<String, String>>>(it) }
            catch (e: Exception) { null }
        } ?: Mensajes.DEFAULT
        _state.value = _state.value.copy(
            googleKey = cfg["googleKey"] ?: "",
            mensajes = msgs,
            zonasExtra = cfg["zonasExtra"]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    fun guardarApiKey(key: String) {
        viewModelScope.launch {
            store.setConfig("googleKey", key)
            _state.value = _state.value.copy(googleKey = key, mensaje = "API Key guardada")
        }
    }

    /** Guarda las 3 plantillas del rubro seleccionado (port de btn-save-msg). */
    fun guardarPlantillas(rubro: String, primero: String, seguimiento: String, cierre: String) {
        val nuevos = _state.value.mensajes.toMutableMap()
        nuevos[rubro] = mapOf("primero" to primero, "seguimiento" to seguimiento, "cierre" to cierre)
        viewModelScope.launch {
            store.setConfig("mensajes", jsonCfg.encodeToString<Map<String, Map<String, String>>>(nuevos))
            _state.value = _state.value.copy(mensajes = nuevos, mensaje = "Plantillas guardadas")
        }
    }

    /** Restaura TODAS las plantillas a los textos por defecto (btn-reset-msg). */
    fun restaurarPlantillas() {
        viewModelScope.launch {
            store.setConfig("mensajes", jsonCfg.encodeToString<Map<String, Map<String, String>>>(Mensajes.DEFAULT))
            _state.value = _state.value.copy(mensajes = Mensajes.DEFAULT, mensaje = "Plantillas restauradas")
        }
    }

    /** MAPA CALOR: recalcula zonas con el modo y radio elegidos. */
    fun recalcularZonas(modo: ZonasEngine.Modo, radioM: Int) {
        val zs = ZonasEngine.calcular(store.all(), modo, radioM)
        _state.value = _state.value.copy(zonas = zs, zonasModo = modo, zonasRadio = radioM)
    }

    /** +RUTA desde una zona: optimiza recorrido con los leads de esa zona. */
    fun rutaDesdeZona(zona: ZonasEngine.Zona) {
        val s = _state.value
        val optimizada = RutaEngine.optimizar(zona.leads, s.userLat, s.userLon)
        val dist = RutaEngine.distanciaTotal(optimizada, s.userLat, s.userLon)
        val rutaUi = optimizada.mapNotNull { lead -> s.leads.find { it.lead.id == lead.id } }
        _state.value = s.copy(ruta = rutaUi, rutaDistanciaKm = dist,
            mensaje = "Ruta con ${rutaUi.size} paradas de ${zona.nombre}")
    }

    /** Borra TODOS los leads (con confirmación en la UI). */
    fun borrarTodo() {
        viewModelScope.launch {
            store.clear()
            recomputar("Todo borrado")
        }
    }

    /** Busca negocios (OSM + opcional Google) en IO, sin bloquear la UI. */
    fun buscar(ciudad: String, rubro: String, usarGoogle: Boolean) {
        _state.value = _state.value.copy(buscando = true, buscarError = "", resultadosBusqueda = emptyList())
        viewModelScope.launch {
            try {
                val resultados = withContext(Dispatchers.IO) {
                    val geo = BuscarEngine.geocodar(ciudad)
                    val osm = BuscarEngine.buscarOsm(rubro, geo)
                    val google = if (usarGoogle && _state.value.googleKey.isNotBlank())
                        BuscarEngine.buscarGooglePorZonas(rubro, ciudad, _state.value.googleKey, _state.value.zonasExtra)
                    else emptyList()
                    val fusion = BuscarEngine.fusionar(osm, google)
                    BuscarEngine.filtrarPorRadio(fusion, geo)  // descarta lejanos
                }
                // Filtrar los que ya son leads (por osmId/googleId/nombre)
                val nuevos = resultados.filter {
                    store.encontrarExistente(it.googleId, it.osmId, it.nombre, it.direccion) == null
                }
                _state.value = _state.value.copy(buscando = false, resultadosBusqueda = nuevos,
                    buscarError = if (nuevos.isEmpty()) "Sin resultados nuevos para \"$rubro\" en $ciudad" else "")
            } catch (e: Exception) {
                _state.value = _state.value.copy(buscando = false,
                    buscarError = "Error: ${e.message ?: "sin conexión"}")
            }
        }
    }

    /** Guarda un resultado de búsqueda como lead. */
    fun guardarResultado(r: BuscarEngine.Resultado) {
        val lead = BuscarEngine.aLead(r, java.util.UUID.randomUUID().toString(),
            java.time.Instant.now().toString())
        viewModelScope.launch {
            store.upsert(lead)
            // Quitar de resultados (ya guardado) y recomputar
            _state.value = _state.value.copy(
                resultadosBusqueda = _state.value.resultadosBusqueda.filter { it !== r }
            )
            recomputar("Guardado: " + lead.nombre)
        }
    }

    /** Guarda TODOS los resultados de búsqueda de una (menos toques). */
    fun guardarTodosResultados() {
        val resultados = _state.value.resultadosBusqueda
        if (resultados.isEmpty()) return
        viewModelScope.launch {
            val ahora = java.time.Instant.now().toString()
            resultados.forEach { r ->
                store.upsert(BuscarEngine.aLead(r, java.util.UUID.randomUUID().toString(), ahora))
            }
            _state.value = _state.value.copy(resultadosBusqueda = emptyList())
            recomputar("Guardados ${resultados.size} leads")
        }
    }

    /** Agrega una zona de búsqueda personalizada. */
    fun agregarZona(zona: String) {
        val z = zona.trim()
        if (z.isBlank() || z in _state.value.zonasExtra) return
        val nuevas = _state.value.zonasExtra + z
        viewModelScope.launch {
            store.setConfig("zonasExtra", nuevas.joinToString("|"))
            _state.value = _state.value.copy(zonasExtra = nuevas)
        }
    }

    fun quitarZona(zona: String) {
        val nuevas = _state.value.zonasExtra - zona
        viewModelScope.launch {
            store.setConfig("zonasExtra", nuevas.joinToString("|"))
            _state.value = _state.value.copy(zonasExtra = nuevas)
        }
    }

    /** Captura un lead en terreno usando el GPS actual. */
    fun capturarLead(nombre: String, tipo: String, equipos: List<String>, tags: List<String>, tel: String) {
        val s = _state.value
        val lead = CapturaEngine.construir(
            nombre = nombre, tipo = tipo, equipos = equipos, tags = tags, telefono = tel,
            lat = s.userLat, lon = s.userLon,
            idGenerado = java.util.UUID.randomUUID().toString(),
            ahoraIso = java.time.Instant.now().toString()
        )
        viewModelScope.launch {
            store.upsert(lead)
            recomputar("Capturado: " + lead.nombre)
        }
    }

    /** Genera la ruta óptima con los objetivos del día (RutaEngine). */
    fun generarRuta() {
        val s = _state.value
        val leadsObjetivo = s.objetivosDia.map { it.lead }
        val optimizada = RutaEngine.optimizar(leadsObjetivo, s.userLat, s.userLon)
        val dist = RutaEngine.distanciaTotal(optimizada, s.userLat, s.userLon)
        val rutaUi = optimizada.mapNotNull { lead -> s.leads.find { it.lead.id == lead.id } }
        _state.value = s.copy(ruta = rutaUi, rutaDistanciaKm = dist)
    }

    fun setUbicacion(lat: Double, lon: Double) {
        _state.value = _state.value.copy(userLat = lat, userLon = lon)
    }

    private fun parseIsoMs(iso: String): Long? = try {
        java.time.Instant.parse(if (iso.endsWith("Z") || iso.contains("+")) iso else iso + "Z").toEpochMilli()
    } catch (e: Exception) { null }

    private fun recomputar(msg: String = "") {
        val ahora = System.currentTimeMillis()
        val ui = store.all()
            .filter { it.estado != "descartado" }
            .map { LeadUi(it, IutEngine.calcular(it), PrioridadEngine.calcular(it, ahora)) }
            .sortedWith(compareBy<LeadUi> { it.prioridad.nivel.ordinal }.thenByDescending { it.iut })
        val prev = _state.value

        // Objetivos del día (DiaEngine) + resumen
        val objetivos = DiaEngine.generarObjetivos(
            store.all(), prev.userLat, prev.userLon, 8, ahora
        ).mapNotNull { (lead, _) -> ui.find { it.lead.id == lead.id } }

        val seguHoy = store.all().count {
            it.seguimientoFecha?.let { s -> parseIsoMs(s)?.let { m -> m <= ahora } } == true &&
            it.estado != "descartado"
        }
        val urgentes = store.all().count { it.estado == "urgente" }
        val stResumen = StatsEngine.resumen(store.all(), ahora)
        val stConv = StatsEngine.conversionPorRubro(store.all())
        val stRev = StatsEngine.revisitasPendientes(store.all(), ahora)
        val stEquipos = StatsEngine.equiposTop(store.all())
        val sinContacto = store.all().count {
            it.estado == "no-contactado" && it.telefono.filter { c -> c.isDigit() }.length >= 6
        }

        val selRefrescado = prev.leadSeleccionado?.let { sel ->
            ui.find { it.lead.id == sel.lead.id }
        }
        _state.value = TerrenoState(
            leads = ui,
            cargando = false,
            userLat = prev.userLat,
            userLon = prev.userLon,
            leadSeleccionado = selRefrescado,
            objetivosDia = objetivos,
            diaSeguimientos = seguHoy,
            diaUrgentes = urgentes,
            diaSinContacto = sinContacto,
            ruta = prev.ruta.mapNotNull { r -> ui.find { it.lead.id == r.lead.id } },
            rutaDistanciaKm = prev.rutaDistanciaKm,
            statsResumen = stResumen,
            statsConversion = stConv,
            statsRevisitas = stRev,
            statsEquipos = stEquipos,
            googleKey = prev.googleKey,
            mensajes = prev.mensajes,
            zonas = prev.zonas,
            zonasModo = prev.zonasModo,
            zonasRadio = prev.zonasRadio,
            buscando = prev.buscando,
            buscarError = prev.buscarError,
            resultadosBusqueda = prev.resultadosBusqueda,
            zonasExtra = prev.zonasExtra,
            mensaje = when {
                ui.isNotEmpty() && msg.isNotEmpty() -> msg
                ui.isEmpty() && store.count() == 0  -> "Importá el JSON exportado desde la PWA para ver tus leads."
                ui.isEmpty()                        -> "El backup no tenía leads activos."
                else                                -> ""
            }
        )
    }
}

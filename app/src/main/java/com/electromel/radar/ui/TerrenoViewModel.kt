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
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement

data class LeadUi(
    val lead: Lead,
    val iut: Int,
    val prioridad: PrioridadEngine.Prioridad
)

data class TerrenoState(
    val leads: List<LeadUi> = emptyList(),
    val leadsTodos: List<LeadUi> = emptyList(),   // incluye descartados (LEADS filtro 'todos')
    val cargando: Boolean = true,
    val mensaje: String = "",
    val userLat: Double? = null,
    val userLon: Double? = null,
    val leadSeleccionado: LeadUi? = null,
    val objetivosDia: List<LeadUi> = emptyList(),
    val diaSeguimientos: Int = 0,
    val diaUrgentes: Int = 0,
    val diaSinContacto: Int = 0,
    val ruta: List<RutaEngine.Parada> = emptyList(),
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
    val buscarProgreso: String = "",
    val backups: List<com.electromel.radar.data.BackupEntity> = emptyList(),
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
    private val store = LeadStore(db.leadDao(), db.configDao(), db.backupDao())
    private val _state = MutableStateFlow(TerrenoState())
    val state: StateFlow<TerrenoState> = _state

    init {
        viewModelScope.launch {
            store.cargar()
            cargarConfig()
            refrescarBackups()
            recomputar()
            backupAutoDiario()   // 1 por día, port del init de la PWA
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
        val rutaGuardada = cfg["ruta"]?.let {
            try { jsonCfg.decodeFromString<List<RutaEngine.Parada>>(it) }
            catch (e: Exception) { null }
        } ?: emptyList()
        _state.value = _state.value.copy(
            googleKey = cfg["googleKey"] ?: "",
            ruta = rutaGuardada,
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

    /* ── BACKUPS (port crearBackup / restaurar / auto diario) ── */

    private var backupsCache: List<com.electromel.radar.data.BackupEntity> = emptyList()

    private suspend fun refrescarBackups() { backupsCache = store.backups(5) }

    /** Port 1:1 de crearBackup(tipo): datos = JSON {version:6, leads, ruta, mensajes}. */
    private suspend fun crearBackup(tipo: String) {
        val s = _state.value
        val datos = kotlinx.serialization.json.buildJsonObject {
            put("version", kotlinx.serialization.json.JsonPrimitive(6))
            put("fecha", kotlinx.serialization.json.JsonPrimitive(java.time.Instant.now().toString()))
            put("tipo", kotlinx.serialization.json.JsonPrimitive(tipo))
            put("leads", jsonCfg.encodeToJsonElement(
                kotlinx.serialization.builtins.ListSerializer(Lead.serializer()),
                store.all()))
            put("ruta", jsonCfg.encodeToJsonElement(
                kotlinx.serialization.builtins.ListSerializer(RutaEngine.Parada.serializer()),
                s.ruta))
            put("mensajes", jsonCfg.encodeToJsonElement(s.mensajes))
        }.toString()
        store.guardarBackup(java.time.Instant.now().toString(), tipo, datos, store.all().size)
        refrescarBackups()
    }

    /** Backup automático — 1 por día al iniciar (port del init de la PWA). */
    private suspend fun backupAutoDiario() {
        val hoy = java.time.Instant.now().toString().take(10)
        val ultima = store.getConfig()["ultimoBackupFecha"]
        if (ultima != hoy && store.all().isNotEmpty()) {
            crearBackup("auto")
            store.setConfig("ultimoBackupFecha", hoy)
        }
    }

    /** RESTAURAR — port del handler data-restore: reemplaza leads y mensajes. */
    fun restaurarBackup(id: Long) {
        viewModelScope.launch {
            val b = store.backupPorId(id) ?: return@launch
            try {
                val root = kotlinx.serialization.json.Json.parseToJsonElement(b.datos)
                    .let { it as kotlinx.serialization.json.JsonObject }
                val leads = root["leads"]?.let {
                    jsonCfg.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(Lead.serializer()), it)
                } ?: emptyList()
                store.clear()
                leads.forEach { store.upsert(it) }
                root["mensajes"]?.let { msj ->
                    val m = jsonCfg.decodeFromJsonElement<Map<String, Map<String, String>>>(msj)
                    _state.value = _state.value.copy(mensajes = m)
                }
                recomputar("Backup restaurado ✓")
            } catch (e: Exception) {
                _state.value = _state.value.copy(mensaje = "Error restaurando: " + e.message)
            }
        }
    }

    /** CAMPAÑA WA — port de aplicarEnvioCampana: historial '(campaña)'. */
    fun registrarEnvioCampana(id: String, tipo: String) {
        val l = store.all().find { it.id == id } ?: return
        val upd = l.copy(
            estado = if (l.estado == "no-contactado") "contactado" else l.estado,
            intentosContacto = l.intentosContacto + 1,
            historial = l.historial + EventoHistorial(
                java.time.Instant.now().toString(), "WhatsApp $tipo (campaña)"))
        viewModelScope.launch { store.upsert(upd); recomputar() }
    }

    /** TERRENO ↺ REGENERAR — recalcula objetivos del día (renderArrancarDia). */
    fun refrescar() { viewModelScope.launch { recomputar() } }

    /** TERRENO ✅ — port de data-ta=visitado. */
    fun marcarVisitado(id: String) {
        val l = store.all().find { it.id == id } ?: return
        val upd = l.copy(
            estado = "visitado",
            intentosContacto = l.intentosContacto + 1,
            historial = l.historial + EventoHistorial(
                java.time.Instant.now().toString(), "Visitado en terreno"))
        viewModelScope.launch { store.upsert(upd); recomputar("✓ Marcado como visitado") }
    }

    /** TERRENO 🚨 — port de data-ta=urgente (estado + tag). */
    fun marcarUrgente(id: String) {
        val l = store.all().find { it.id == id } ?: return
        val upd = l.copy(estado = "urgente", tags = l.tags + "urgente")
        viewModelScope.launch { store.upsert(upd); recomputar("🚨 Marcado como URGENTE") }
    }

    /** TERRENO 📝 — port de nq-guardar: notas + historial 'Nota: …40'. */
    fun guardarNotaRapida(id: String, nota: String) {
        val l = store.all().find { it.id == id } ?: return
        val upd = l.copy(
            notas = nota.trim(),
            historial = l.historial + EventoHistorial(
                java.time.Instant.now().toString(), "Nota: " + nota.trim().take(40)))
        viewModelScope.launch { store.upsert(upd); recomputar("Nota guardada ✓") }
    }

    /** FICHA GUARDAR — port de construirLeadActualizado + m-guardar:
     *  historial += 'Lead actualizado', persiste, recomputa. */
    fun guardarFicha(actualizado: Lead) {
        val upd = actualizado.copy(
            historial = actualizado.historial + EventoHistorial(
                java.time.Instant.now().toString(), "Lead actualizado"))
        viewModelScope.launch { store.upsert(upd); recomputar("Lead actualizado ✓") }
    }

    /** FICHA ELIMINAR — port de m-eliminar: borrado real. */
    fun eliminarLead(id: String) {
        viewModelScope.launch { store.remove(id); recomputar("Lead eliminado") }
    }

    /** FICHA FOTO — guarda inmediato (port del handler m-foto-input). */
    fun agregarFotoLead(id: String, dataUrl: String) {
        val l = store.all().find { it.id == id } ?: return
        viewModelScope.launch {
            store.upsert(l.copy(fotos = l.fotos + dataUrl))
            recomputar("Foto guardada ✓")
        }
    }

    /** FICHA FOTO ✕ — port del handler foto-del. */
    fun quitarFotoLead(id: String, idx: Int) {
        val l = store.all().find { it.id == id } ?: return
        if (idx !in l.fotos.indices) return
        viewModelScope.launch {
            store.upsert(l.copy(fotos = l.fotos.filterIndexed { i, _ -> i != idx }))
            recomputar("Foto eliminada")
        }
    }

    /** HOY: posterga el seguimiento N días (port del botón +2 DÍAS). */
    fun postergarSeguimiento(id: String, dias: Int = 2) {
        val l = store.all().find { it.id == id } ?: return
        val nueva = java.time.Instant.now()
            .plus(dias.toLong(), java.time.temporal.ChronoUnit.DAYS).toString()
        viewModelScope.launch {
            store.upsert(l.copy(seguimientoFecha = nueva))
            recomputar("Postergado +$dias días")
        }
    }

    /** HOY/ficha: registra un envío de WhatsApp — port 1:1 de abrirWhatsApp():
     *  no-contactado → contactado · intentos+1 · historial "WhatsApp {tipo}". */
    fun registrarWhatsApp(id: String, tipo: String) {
        val l = store.all().find { it.id == id } ?: return
        val upd = l.copy(
            estado = if (l.estado == "no-contactado") "contactado" else l.estado,
            intentosContacto = l.intentosContacto + 1,
            historial = l.historial + EventoHistorial(
                java.time.Instant.now().toString(), "WhatsApp $tipo")
        )
        viewModelScope.launch { store.upsert(upd); recomputar() }
    }

    /** MAPA CALOR: recalcula zonas con el modo y radio elegidos. */
    fun recalcularZonas(modo: ZonasEngine.Modo, radioM: Int) {
        val zs = ZonasEngine.calcular(store.all(), modo, radioM)
        _state.value = _state.value.copy(zonas = zs, zonasModo = modo, zonasRadio = radioM)
    }

    /** +RUTA desde una zona — port 1:1: AGREGA con dedup por leadId. */
    fun rutaDesdeZona(zona: ZonasEngine.Zona): Int {
        var n = 0
        var ruta = _state.value.ruta
        for (l in zona.leads) {
            if (ruta.none { it.leadId == l.id }) {
                ruta = ruta + RutaEngine.Parada(
                    id = java.util.UUID.randomUUID().toString(), leadId = l.id,
                    nombre = l.nombre, direccion = l.direccion, lat = l.lat, lon = l.lon)
                n++
            }
        }
        _state.value = _state.value.copy(ruta = ruta)
        guardarRuta(ruta)
        return n
    }

    /** Agrega un lead a la ruta — port 1:1 de agregarLeadARuta (dedup+toast). */
    fun agregarLeadARuta(id: String) {
        val l = store.all().find { it.id == id } ?: return
        if (_state.value.ruta.any { it.leadId == id }) {
            _state.value = _state.value.copy(mensaje = "Ya está en la ruta"); return
        }
        val ruta = _state.value.ruta + RutaEngine.Parada(
            id = java.util.UUID.randomUUID().toString(), leadId = id,
            nombre = l.nombre, direccion = l.direccion, lat = l.lat, lon = l.lon)
        _state.value = _state.value.copy(ruta = ruta)
        guardarRuta(ruta)
    }

    /** Parada manual — port 1:1: {nombre:v, direccion:v, sin coords}. */
    fun agregarParadaManual(texto: String) {
        val v = texto.trim(); if (v.isEmpty()) return
        val ruta = _state.value.ruta + RutaEngine.Parada(
            id = java.util.UUID.randomUUID().toString(),
            nombre = v, direccion = v, lat = null, lon = null)
        _state.value = _state.value.copy(ruta = ruta)
        guardarRuta(ruta)
    }

    /** Mover parada ↑/↓ — port del control data-mov. */
    fun moverParada(idx: Int, dir: Int) {
        val r = _state.value.ruta.toMutableList()
        val j = idx + dir
        if (idx !in r.indices || j !in r.indices) return
        val tmp = r[idx]; r[idx] = r[j]; r[j] = tmp
        _state.value = _state.value.copy(ruta = r)
        guardarRuta(r)
    }

    /** Quitar parada ✕ — port del control data-del. */
    fun quitarParada(idx: Int) {
        val r = _state.value.ruta.toMutableList()
        if (idx !in r.indices) return
        r.removeAt(idx)
        _state.value = _state.value.copy(ruta = r)
        guardarRuta(r)
    }

    /** LIMPIAR — port de btn-clear-ruta (la confirmación la hace la UI). */
    fun limpiarRuta() {
        _state.value = _state.value.copy(ruta = emptyList())
        guardarRuta(emptyList())
    }

    /** INICIAR RECORRIDO — port 1:1: optimiza (sin persistir, como la PWA)
     *  y devuelve la URL de Maps, o null si la ruta está vacía. */
    fun iniciarRecorrido(): String? {
        val s = _state.value
        if (s.ruta.isEmpty()) return null
        val opt = RutaEngine.optimizarParadas(s.ruta, s.userLat, s.userLon)
        _state.value = s.copy(ruta = opt)
        return RutaEngine.urlRecorrido(opt)
    }

    private fun guardarRuta(ruta: List<RutaEngine.Parada>) {
        viewModelScope.launch {
            store.setConfig("ruta", jsonCfg.encodeToString<List<RutaEngine.Parada>>(ruta))
        }
    }

    /** Borra TODOS los leads (con confirmación en la UI). */
    fun borrarTodo() {
        viewModelScope.launch {
            crearBackup("pre-borrado")   // port: la PWA respalda antes de borrar
            store.clear()
            _state.value = _state.value.copy(ruta = emptyList())
            recomputar("Todo borrado")
        }
    }

    /** Busca negocios (OSM + opcional Google) en IO, sin bloquear la UI. */
    @Volatile private var buscarCancelado = false

    /** ⏹ FRENAR — port de btn-frenar. */
    fun frenarBusqueda() { buscarCancelado = true }

    /** BUSCAR OBJETIVOS — port 1:1 de btn-buscar + buscarMultiZona:
     *  multi-rubro × ciudad, dedup global (googleId/osmId/nombre+dir20),
     *  progreso por zona (solo Google es multi-zona; OSM 1 llamada),
     *  orden iut desc → rating desc, corte por errores > total/2. */
    fun buscar(ciudad: String, rubros: List<String>, fuente: String) {
        if (rubros.isEmpty() || ciudad.isBlank()) return
        buscarCancelado = false
        val tsInicio = System.currentTimeMillis()
        _state.value = _state.value.copy(
            buscando = true, buscarError = "", resultadosBusqueda = emptyList(),
            buscarProgreso = "Iniciando radar · ${rubros.size} rubro(s) × 1 ciudad(es) = " +
                "${rubros.size} campañas...")

        viewModelScope.launch {
            val idsGlobales = mutableSetOf<String>()
            var acumulados = listOf<BuscarEngine.Resultado>()
            var rubroActual = 0

            for (rubro in rubros) {
                if (buscarCancelado) break
                rubroActual++
                try {
                    val parciales = withContext(Dispatchers.IO) {
                        if (fuente == "google" && _state.value.googleKey.isNotBlank()) {
                            val queries = BuscarEngine.generarConsultas(
                                ciudad, rubro, _state.value.zonasExtra)
                            val acc = mutableListOf<BuscarEngine.Resultado>()
                            var errores = 0
                            for ((i, q) in queries.withIndex()) {
                                if (buscarCancelado) break
                                val elapsed = (System.currentTimeMillis() - tsInicio) / 1000
                                val restante = if (i > 0)
                                    (elapsed.toDouble() / i * (queries.size - i)).toInt() else 0
                                _state.value = _state.value.copy(buscarProgreso =
                                    "$rubro ($rubroActual/${rubros.size}) · $ciudad" +
                                    " · Zona ${i + 1}/${queries.size}" +
                                    " · ${acumulados.size + acc.size} únicos" +
                                    (if (restante > 0) " · ~${restante}s" else "") +
                                    "\n→ $q")
                                try {
                                    acc += BuscarEngine.buscarGoogle(q, _state.value.googleKey)
                                } catch (e: Exception) {
                                    errores++
                                    if (errores > queries.size / 2) break
                                }
                                if (i < queries.size - 1) kotlinx.coroutines.delay(800)
                            }
                            acc
                        } else {
                            _state.value = _state.value.copy(buscarProgreso =
                                "$rubro ($rubroActual/${rubros.size}) · $ciudad · Zona 1/1" +
                                " · ${acumulados.size} únicos")
                            val geo = BuscarEngine.geocodar(ciudad)
                            BuscarEngine.buscarOsm(rubro, geo)
                        }
                    }

                    // IUT + rubro detectado + dedup global (port exacto)
                    val conIut = parciales.map { r ->
                        r.copy(iut = IutEngine.calcular(BuscarEngine.aLead(
                            r, "tmp", java.time.Instant.now().toString())))
                    }
                    val nuevos = conIut.filter { r ->
                        val id = r.googleId ?: r.osmId?.toString()
                        if (id != null && id in idsGlobales) return@filter false
                        val k = IutEngine.normalizar(r.nombre) + "|" +
                            IutEngine.normalizar(r.direccion).take(20)
                        if (k in idsGlobales) return@filter false
                        if (id != null) idsGlobales.add(id)
                        idsGlobales.add(k)
                        true
                    }
                    acumulados = (acumulados + nuevos).sortedWith(
                        compareByDescending<BuscarEngine.Resultado> { it.iut }
                            .thenByDescending { it.rating ?: 0.0 })
                    _state.value = _state.value.copy(resultadosBusqueda = acumulados)
                } catch (e: Exception) {
                    // port: console.warn y sigue con el próximo rubro
                }
            }

            // Resumen final (textos exactos)
            val elapsed = (System.currentTimeMillis() - tsInicio) / 1000
            val fin = if (acumulados.isNotEmpty())
                "✓ ${acumulados.size} objetivos únicos · ${rubros.size} rubro(s)" +
                " · ${elapsed}s · " + (if (fuente == "google") "Google" else "OSM")
            else "Sin resultados. Probá con otros rubros o ciudad."
            buscarCancelado = false
            _state.value = _state.value.copy(buscando = false, buscarProgreso = fin)

            // Enriquecer teléfonos en background (port lanzarEnriquecimiento)
            if (fuente == "google" && acumulados.any { it.googleId != null }) {
                launch(Dispatchers.IO) {
                    for (r in acumulados) {
                        val gid = r.googleId ?: continue
                        if (r.telefono.isNotBlank()) continue
                        val tel = BuscarEngine.detallesTelefono(gid, _state.value.googleKey)
                            ?: continue
                        _state.value = _state.value.copy(
                            resultadosBusqueda = _state.value.resultadosBusqueda.map {
                                if (it.googleId == gid) it.copy(telefono = tel) else it
                            })
                        kotlinx.coroutines.delay(250)
                    }
                }
            }
        }
    }

    /** WA desde resultado — port data-rc-wa: guarda si no existe y devuelve el lead. */
    fun waDesdeResultado(r: BuscarEngine.Resultado): Lead {
        val existente = encontrarLeadDe(r)
        if (existente != null) return existente
        val lead = BuscarEngine.aLead(r, java.util.UUID.randomUUID().toString(),
            java.time.Instant.now().toString())
        viewModelScope.launch { store.upsert(lead); recomputar() }
        return lead
    }

    /** Port de encontrarLeadExistente: googleId → osmId → nombre+dir20. */
    fun encontrarLeadDe(r: BuscarEngine.Resultado): Lead? {
        val todos = store.all()
        r.googleId?.let { g -> todos.find { it.googleId == g }?.let { return it } }
        r.osmId?.let { o -> todos.find { it.osmId == o }?.let { return it } }
        val k = IutEngine.normalizar(r.nombre) + "|" + IutEngine.normalizar(r.direccion).take(20)
        return todos.find {
            IutEngine.normalizar(it.nombre) + "|" +
                IutEngine.normalizar(it.direccion).take(20) == k
        }
    }

    /** Guarda un resultado de búsqueda como lead. */
    fun guardarResultado(r: BuscarEngine.Resultado) {
        val lead = BuscarEngine.aLead(r, java.util.UUID.randomUUID().toString(),
            java.time.Instant.now().toString())
        viewModelScope.launch {
            store.upsert(lead)
            recomputar("✓ Lead guardado")   // la card queda visible en ✓ GUARDADO
        }
    }

    /** Guarda TODOS los resultados de búsqueda de una (menos toques). */
    fun guardarTodosResultados() {
        val nuevos = _state.value.resultadosBusqueda.filter { encontrarLeadDe(it) == null }
        if (nuevos.isEmpty()) return
        viewModelScope.launch {
            val ahora = java.time.Instant.now().toString()
            nuevos.forEach { r ->
                store.upsert(BuscarEngine.aLead(r, java.util.UUID.randomUUID().toString(), ahora))
            }
            recomputar("✓ ${'$'}{nuevos.size} leads guardados")
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
    /** ARRANCAR DÍA → ruta (port de btn-dia-iniciar; lo consumirá TERRENO). */
    fun generarRuta() {
        val s = _state.value
        val paradas = s.objetivosDia.map { ui ->
            RutaEngine.Parada(
                id = java.util.UUID.randomUUID().toString(), leadId = ui.lead.id,
                nombre = ui.lead.nombre, direccion = ui.lead.direccion,
                lat = ui.lead.lat, lon = ui.lead.lon)
        }
        val opt = RutaEngine.optimizarParadas(paradas, s.userLat, s.userLon)
        _state.value = s.copy(ruta = opt)
        guardarRuta(opt)
    }

    fun setUbicacion(lat: Double, lon: Double) {
        _state.value = _state.value.copy(userLat = lat, userLon = lon)
    }

    private fun parseIsoMs(iso: String): Long? = try {
        java.time.Instant.parse(if (iso.endsWith("Z") || iso.contains("+")) iso else iso + "Z").toEpochMilli()
    } catch (e: Exception) { null }

    private fun recomputar(msg: String = "") {
        val ahora = System.currentTimeMillis()
        val uiTodos = store.all()
            .map { LeadUi(it, IutEngine.calcular(it), PrioridadEngine.calcular(it, ahora)) }
            .sortedWith(compareBy<LeadUi> { it.prioridad.nivel.ordinal }.thenByDescending { it.iut })
        val ui = uiTodos.filter { it.lead.estado != "descartado" }
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
            leadsTodos = uiTodos,
            cargando = false,
            userLat = prev.userLat,
            userLon = prev.userLon,
            leadSeleccionado = selRefrescado,
            backups = backupsCache,
            objetivosDia = objetivos,
            diaSeguimientos = seguHoy,
            diaUrgentes = urgentes,
            diaSinContacto = sinContacto,
            ruta = prev.ruta,
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

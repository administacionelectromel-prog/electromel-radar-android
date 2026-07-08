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

data class LeadUi(
    val lead: Lead,
    val iut: Int,
    val prioridad: PrioridadEngine.Prioridad
)

data class TerrenoState(
    val leads: List<LeadUi> = emptyList(),
    val cargando: Boolean = true,
    val mensaje: String = ""
)

/**
 * AndroidViewModel para tener Context (necesario para Room).
 * Carga los leads persistidos al abrir; el import los guarda en disco.
 */
class TerrenoViewModel(app: Application) : AndroidViewModel(app) {

    private val store = LeadStore(RadarDatabase.get(app).leadDao())
    private val _state = MutableStateFlow(TerrenoState())
    val state: StateFlow<TerrenoState> = _state

    init {
        viewModelScope.launch {
            store.cargar()
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

    private fun recomputar(msg: String = "") {
        val ahora = System.currentTimeMillis()
        val ui = store.all()
            .filter { it.estado != "descartado" }
            .map { LeadUi(it, IutEngine.calcular(it), PrioridadEngine.calcular(it, ahora)) }
            .sortedWith(compareBy<LeadUi> { it.prioridad.nivel.ordinal }.thenByDescending { it.iut })
        _state.value = TerrenoState(
            leads = ui,
            cargando = false,
            mensaje = when {
                ui.isNotEmpty() && msg.isNotEmpty() -> msg
                ui.isEmpty() && store.count() == 0  -> "Importá el JSON exportado desde la PWA para ver tus leads."
                ui.isEmpty()                        -> "El backup no tenía leads activos."
                else                                -> ""
            }
        )
    }
}

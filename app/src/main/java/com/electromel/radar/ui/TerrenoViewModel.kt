package com.electromel.radar.ui

import androidx.lifecycle.ViewModel
import com.electromel.radar.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LeadUi(
    val lead: Lead,
    val iut: Int,
    val prioridad: PrioridadEngine.Prioridad
)

data class TerrenoState(
    val leads: List<LeadUi> = emptyList(),
    val mensaje: String = "Importá el JSON exportado desde la PWA para ver tus leads."
)

class TerrenoViewModel : ViewModel() {
    private val repo = LeadRepository()
    private val _state = MutableStateFlow(TerrenoState())
    val state: StateFlow<TerrenoState> = _state

    /** Importa el contenido de un backup JSON de la PWA. */
    fun importarBackup(contenido: String) {
        BackupParser.parsearLeads(contenido)
            .onSuccess { leads ->
                repo.replaceAll(leads)
                recomputar()
            }
            .onFailure {
                _state.value = _state.value.copy(
                    mensaje = "No se pudo leer el archivo. ¿Es un backup de ELECTROMEL RADAR?"
                )
            }
    }

    /** Recalcula IUT + prioridad y ordena: HOY → SEMANA → REVISITA → BAJA, luego IUT desc. */
    private fun recomputar() {
        val ahora = System.currentTimeMillis()
        val ui = repo.all()
            .filter { it.estado != "descartado" }
            .map { l ->
                LeadUi(l, IutEngine.calcular(l), PrioridadEngine.calcular(l, ahora))
            }
            .sortedWith(
                compareBy<LeadUi> { it.prioridad.nivel.ordinal }.thenByDescending { it.iut }
            )
        _state.value = TerrenoState(
            leads = ui,
            mensaje = if (ui.isEmpty()) "El backup no tenía leads activos." else ""
        )
    }
}

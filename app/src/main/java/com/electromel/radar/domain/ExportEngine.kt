package com.electromel.radar.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Export — port de exportarCSV/exportarTXT/JSON de la PWA. Lógica pura:
 * devuelve el contenido como String; el guardado/compartir lo hace la UI.
 */
object ExportEngine {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    private fun limpiarTel(t: String) = t.filter { it.isDigit() || it == '+' }

    enum class Filtro(val label: String) {
        TODOS("Todos"), CON_TEL("Con teléfono"), CLIENTES("Clientes"),
        SIN_CONTACTAR("Sin contactar"), ALTA("Prioridad alta"),
        CON_COORDS("Con coordenadas")
    }

    fun filtrar(leads: List<Lead>, f: Filtro): List<Lead> = when (f) {
        Filtro.TODOS         -> leads.filter { it.estado != "descartado" }
        Filtro.CON_TEL       -> leads.filter { limpiarTel(it.telefono).length >= 6 }
        Filtro.CLIENTES      -> leads.filter { it.estado in listOf("cliente","recurrente","mantenimiento") }
        Filtro.SIN_CONTACTAR -> leads.filter { it.estado == "no-contactado" }
        Filtro.ALTA          -> leads.filter { IutEngine.calcular(it) >= 45 }
        Filtro.CON_COORDS    -> leads.filter { it.lat != null && it.lon != null }
    }

    fun toJson(leads: List<Lead>): String {
        val backup = BackupPwa(version = 6, fecha = java.time.Instant.now().toString(), leads = leads)
        return json.encodeToString(backup)
    }

    fun toCsv(leads: List<Lead>): String {
        val cols = listOf("nombre","telefono","direccion","zona","rubro","estado","nivel","notas","fuente","lat","lon")
        fun esc(v: String): String =
            if (v.contains(",") || v.contains("\"") || v.contains("\n"))
                "\"" + v.replace("\"", "\"\"") + "\"" else v
        val header = cols.joinToString(",")
        val rows = leads.joinToString("\n") { l ->
            listOf(l.nombre, l.telefono, l.direccion, l.zona, l.rubro, l.estado, l.nivel,
                   l.notas, l.fuente, l.lat?.toString() ?: "", l.lon?.toString() ?: "")
                .joinToString(",") { esc(it) }
        }
        return "$header\n$rows"
    }

    fun toTxt(leads: List<Lead>): String =
        leads.filter { limpiarTel(it.telefono).length >= 6 }
            .joinToString("\n") { l ->
                l.nombre + "\t" + l.telefono + if (l.direccion.isNotEmpty()) "\t" + l.direccion else ""
            }
}

package com.electromel.radar.domain

/**
 * MAPA CALOR INDUSTRIAL — port fiel de calcularZonas()/construirZona().
 * Score = clientes×3 + respondieron×2 + contactados.
 * Modo AUTO: clustering greedy por radio. Modo BARRIO: agrupa por l.zona.
 */
object ZonasEngine {

    enum class Modo { AUTO, BARRIO }

    data class Zona(
        val nombre: String, val leads: List<Lead>,
        val contactados: Int, val respondieron: Int, val clientes: Int,
        val score: Int, val iutTotal: Int,
        val latCentro: Double, val lonCentro: Double,
        val temp: String,                       // hot / warm / cold (score>=5 / >=2 / else)
        val topEquipo: Pair<String, Int>? = null // equipo más frecuente en la zona
    )

    fun calcular(todos: List<Lead>, modo: Modo, radioM: Int): List<Zona> {
        val leads = todos.filter { it.lat != null && it.lon != null && it.estado != "descartado" }
        if (leads.isEmpty()) return emptyList()

        val grupos: List<Pair<String?, List<Lead>>> = if (modo == Modo.AUTO) {
            val r = radioM / 1000.0
            val usados = HashSet<String>()
            val gs = mutableListOf<List<Lead>>()
            for (l in leads) {
                if (l.id in usados) continue
                val grupo = mutableListOf(l); usados.add(l.id)
                val lLat = l.lat!!; val lLon = l.lon!!
                for (m in leads) {
                    if (m.id !in usados &&
                        GeoUtils.distKm(lLat, lLon, m.lat!!, m.lon!!) <= r) {
                        grupo.add(m); usados.add(m.id)
                    }
                }
                gs.add(grupo)
            }
            gs.map { null to it }
        } else {
            leads.groupBy { l ->
                l.zona.ifBlank {
                    l.direccion.split(",").getOrNull(1)?.trim() ?: "Sin zona"
                }.ifBlank { "Sin zona" }
            }.map { (k, g) -> k to g }
        }

        return grupos.mapIndexed { i, (nombre, g) -> construir(g, i, nombre) }
            .sortedByDescending { it.score }
    }

    private fun construir(g: List<Lead>, idx: Int, nombre: String?): Zona {
        val contactados = g.count { it.estado != "no-contactado" }
        val respondieron = g.count { it.estado in listOf("respondio","cliente","recurrente","mantenimiento") }
        val clientes = g.count { it.estado in listOf("cliente","recurrente","mantenimiento") }
        val score = clientes * 3 + respondieron * 2 + contactados
        // Nombre fiel al original: zona del 1er lead || parte 2 de dirección || Zona N
        val n = nombre
            ?: g[0].zona.ifBlank { g[0].direccion.split(",").getOrNull(1)?.trim() ?: "" }
                .ifBlank { "Zona ${idx + 1}" }
        // Equipo más frecuente
        val freq = HashMap<String, Int>()
        g.forEach { l -> l.equipos.forEach { e -> freq[e] = (freq[e] ?: 0) + 1 } }
        val top = freq.entries.maxByOrNull { it.value }?.let { it.key to it.value }
        return Zona(
            nombre = n,
            leads = g, contactados = contactados, respondieron = respondieron, clientes = clientes,
            score = score,
            iutTotal = g.sumOf { IutEngine.calcular(it) },
            latCentro = g.mapNotNull { it.lat }.average(),
            lonCentro = g.mapNotNull { it.lon }.average(),
            temp = if (score >= 5) "hot" else if (score >= 2) "warm" else "cold",
            topEquipo = top
        )
    }
}

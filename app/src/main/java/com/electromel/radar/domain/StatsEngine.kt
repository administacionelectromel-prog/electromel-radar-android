package com.electromel.radar.domain

/**
 * Analytics — port de calcularConversionPorRubro() y
 * calcularRevisitasPendientes() de la PWA. Lógica pura.
 */
object StatsEngine {

    data class Resumen(
        val total: Int, val contactados: Int, val respondio: Int,
        val clientes: Int, val urgentes: Int, val pendSeg: Int
    )

    data class ConversionRubro(
        val rubro: String, val total: Int, val contactados: Int, val clientes: Int,
        val tasaContacto: Int, val tasaCliente: Int
    )

    data class Revisita(
        val lead: Lead, val tipo: String, val diasHasta: Int, val motivo: String
    )

    fun resumen(leads: List<Lead>, ahoraMs: Long = System.currentTimeMillis()): Resumen {
        val activos = leads.filter { it.estado != "descartado" }
        return Resumen(
            total = activos.size,
            contactados = activos.count { it.estado != "no-contactado" },
            respondio = activos.count { it.estado in listOf("respondio", "presupuesto", "esperando") },
            clientes = activos.count { it.estado in listOf("cliente", "recurrente", "mantenimiento") },
            urgentes = activos.count { it.estado == "urgente" },
            pendSeg = activos.count {
                it.seguimientoFecha?.let { s -> parseIsoMs(s)?.let { m -> m <= ahoraMs } } == true
            }
        )
    }

    fun conversionPorRubro(leads: List<Lead>): List<ConversionRubro> {
        data class Acc(var total: Int = 0, var contactados: Int = 0, var clientes: Int = 0)
        val rubros = HashMap<String, Acc>()
        for (l in leads) {
            if (l.estado == "descartado") continue
            val r = l.rubro.ifEmpty { "comercio" }
            val a = rubros.getOrPut(r) { Acc() }
            a.total++
            if (l.estado != "no-contactado") a.contactados++
            if (l.estado in listOf("cliente", "recurrente", "mantenimiento")) a.clientes++
        }
        return rubros.map { (rubro, d) ->
            ConversionRubro(
                rubro = rubro, total = d.total, contactados = d.contactados, clientes = d.clientes,
                tasaContacto = if (d.total > 0) Math.round(d.contactados * 100.0 / d.total).toInt() else 0,
                tasaCliente = if (d.contactados > 0) Math.round(d.clientes * 100.0 / d.contactados).toInt() else 0
            )
        }.sortedWith(compareByDescending<ConversionRubro> { it.clientes }.thenByDescending { it.total })
    }

    fun revisitasPendientes(leads: List<Lead>, ahoraMs: Long = System.currentTimeMillis()): List<Revisita> {
        val pendientes = mutableListOf<Revisita>()
        for (l in leads) {
            if (l.estado == "descartado") continue

            // Ciclo de mantenimiento
            val ciclo = l.cicloMantenimiento
            if (ciclo != null && l.historial.isNotEmpty()) {
                val ultimo = l.historial
                    .filter { it.accion.contains("WhatsApp") || it.accion.contains("visitado") || it.accion.contains("cliente") }
                    .maxByOrNull { parseIsoMs(it.fecha) ?: 0L }
                if (ultimo != null) {
                    val base = parseIsoMs(ultimo.fecha)
                    if (base != null) {
                        val proxima = base + ciclo * 30L * 86_400_000L
                        val diasHasta = Math.round((proxima - ahoraMs) / 86_400_000.0).toInt()
                        if (diasHasta <= 30)
                            pendientes.add(Revisita(l, "mantenimiento", diasHasta,
                                "Ciclo de mantenimiento cada $ciclo mes(es)"))
                        continue
                    }
                }
            }

            // Seguimiento programado
            l.seguimientoFecha?.let { seg ->
                parseIsoMs(seg)?.let { m ->
                    val diasHasta = Math.round((m - ahoraMs) / 86_400_000.0).toInt()
                    if (diasHasta in 0..14)
                        pendientes.add(Revisita(l, "seguimiento", diasHasta, "Seguimiento programado"))
                }
            }

            // Cliente inactivo (>45 días sin seguimiento)
            if (l.estado in listOf("cliente", "recurrente") && l.historial.isNotEmpty() && l.seguimientoFecha == null) {
                val ultimo = parseIsoMs(l.historial.last().fecha)
                if (ultimo != null) {
                    val diasSin = Math.round((ahoraMs - ultimo) / 86_400_000.0).toInt()
                    if (diasSin > 45)
                        pendientes.add(Revisita(l, "cliente-inactivo", -diasSin,
                            "Cliente sin contacto hace $diasSin días"))
                }
            }
        }
        return pendientes.sortedBy { it.diasHasta }
    }

    private fun parseIsoMs(iso: String): Long? = try {
        java.time.Instant.parse(if (iso.endsWith("Z") || iso.contains("+")) iso else iso + "Z").toEpochMilli()
    } catch (e: Exception) { null }
}

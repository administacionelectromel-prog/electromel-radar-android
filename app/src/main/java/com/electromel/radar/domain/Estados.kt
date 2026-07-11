package com.electromel.radar.domain

/** Catálogo de estados — port 1:1 de const ESTADOS (id → label, en orden). */
object Estados {
    val LISTA = listOf(
        "no-contactado" to "Sin contactar",
        "visitado" to "Visitado",
        "contactado" to "Contactado",
        "respondio" to "Respondió",
        "presupuesto" to "Presupuesto enviado",
        "esperando" to "Esperando resp.",
        "revisita" to "Revisita pend.",
        "cliente" to "Cliente",
        "recurrente" to "Cliente recurrente",
        "mantenimiento" to "Mant. periódico",
        "urgente" to "URGENTE",
        "descartado" to "Descartado"
    )
    val LABEL: Map<String, String> = LISTA.toMap()
}

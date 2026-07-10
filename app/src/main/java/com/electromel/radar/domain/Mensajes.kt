package com.electromel.radar.domain

/**
 * Plantillas WhatsApp POR RUBRO — port 1:1 de MENSAJES_DEFAULT y
 * buildMensajeCampana() de la PWA. {nombre} se reemplaza.
 */
object Mensajes {

    /** rubro → (tipo → plantilla). Tipos: primero / seguimiento / cierre. */
    val DEFAULT: Map<String, Map<String, String>> = mapOf(
        "gimnasio" to mapOf(
            "primero" to "Hola {nombre}, ¿cómo estás?\nSoy técnico de ELECTROMEL, trabajamos con mantenimiento y reparación de cintas de correr y equipos de gimnasio.\nEstoy en la zona y quería consultar si tienen alguna máquina para revisar.\nPuedo pasar sin compromiso.",
            "seguimiento" to "Hola {nombre}, ¿cómo va?\nTe escribí hace unos días por mantenimiento de equipos.\nSi necesitan revisar cintas o máquinas puedo acercarme esta semana.",
            "cierre" to "Hola {nombre}, ¿cómo estás?\nTengo disponibilidad esta semana. ¿Coordinamos? Paso a dar diagnóstico en el momento."
        ),
        "hotel" to mapOf(
            "primero" to "Hola {nombre}, ¿cómo estás?\nSoy técnico de ELECTROMEL, hacemos mantenimiento de equipos eléctricos y electrónicos.\nEstoy en la zona y quería saber si necesitan revisar algo.",
            "seguimiento" to "Hola {nombre}, ¿cómo va?\nTe había contactado por mantenimiento de equipos. Si necesitan revisar algo puedo acercarme.",
            "cierre" to "Hola {nombre}, tengo disponibilidad esta semana. ¿Coordinamos una visita?"
        ),
        "constructora" to mapOf(
            "primero" to "Hola {nombre}, ¿cómo estás?\nSoy técnico de ELECTROMEL, reparamos soldadoras inverter y equipos de obra.\nEstoy en la zona — ¿tienen alguna máquina para revisar?",
            "seguimiento" to "Hola {nombre}, ¿cómo va?\nTe escribí por equipos de obra. Si tienen algo para revisar puedo pasar.",
            "cierre" to "Hola {nombre}, tengo disponibilidad esta semana. ¿Coordinamos?"
        ),
        "industrial" to mapOf(
            "primero" to "Hola {nombre}, ¿cómo estás?\nSoy técnico de ELECTROMEL, trabajamos con reparación y mantenimiento de equipos industriales — soldadoras, variadores, tableros, motores.\nEstoy en la zona, ¿tienen algo para revisar o mantener?",
            "seguimiento" to "Hola {nombre}, ¿cómo va?\nTe contacté por mantenimiento industrial. Si tienen equipos para revisar avísame.",
            "cierre" to "Hola {nombre}, tengo disponibilidad esta semana para diagnóstico de equipos. ¿Coordinamos?"
        ),
        "comercio" to mapOf(
            "primero" to "Hola {nombre}, ¿cómo estás?\nSoy técnico de ELECTROMEL, reparamos equipos eléctricos y electrónicos. Estoy en la zona.",
            "seguimiento" to "Hola {nombre}, ¿cómo va?\nSi necesitan revisar algún equipo puedo acercarme.",
            "cierre" to "Hola {nombre}, tengo disponibilidad esta semana. ¿Coordinamos una visita?"
        )
    )

    /** Rubros editables en Config (orden del select original). */
    val RUBROS_EDIT = listOf(
        "gimnasio" to "Gimnasios / Fitness",
        "hotel" to "Hoteles / Alojamiento",
        "constructora" to "Constructoras / Obras",
        "industrial" to "Industrial / Taller / Metalúrgica",
        "comercio" to "Comercio General"
    )

    /** Port de buildMensajeCampana: rubro del lead → plantilla, fallback comercio. */
    fun build(lead: Lead, tipo: String, mensajes: Map<String, Map<String, String>>): String {
        val rubro = lead.rubro.ifBlank { "comercio" }
        val tpl = mensajes[rubro]?.get(tipo)
            ?: mensajes["comercio"]?.get(tipo)
            ?: DEFAULT["comercio"]?.get(tipo) ?: ""
        return tpl.replace(Regex("\\{nombre\\}", RegexOption.IGNORE_CASE), lead.nombre)
    }
}

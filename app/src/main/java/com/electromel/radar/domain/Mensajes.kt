package com.electromel.radar.domain

/** Plantillas de mensaje WhatsApp por tipo. {nombre} se reemplaza. */
object Mensajes {
    fun primerContacto(lead: Lead): String {
        val n = lead.nombre
        return "Hola $n! Te contacto de ELECTROMEL, servicio técnico de equipos " +
               "eléctricos e industriales en la zona. ¿Tienen algún equipo con fallas " +
               "o que necesite mantenimiento? Trabajamos con soldadoras, tableros, " +
               "motores, variadores y más. Saludos!"
    }
    fun seguimiento(lead: Lead): String =
        "Hola ${lead.nombre}! Te escribo para hacer un seguimiento. " +
        "¿Pudiste evaluar lo que conversamos? Quedo atento. Saludos!"
    fun cierre(lead: Lead): String =
        "Hola ${lead.nombre}! Te paso la propuesta que preparamos. " +
        "Cualquier consulta quedo a disposición. Saludos!"
}

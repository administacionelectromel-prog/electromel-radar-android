package com.electromel.radar.domain

/**
 * Optimización de ruta — port fiel de optimizarRuta() de la PWA.
 * Algoritmo: vecino más cercano (greedy) desde el origen (GPS o primera parada).
 * Los leads sin coordenadas van al final sin ordenar.
 *
 * Lógica pura: recibe leads + origen, devuelve orden optimizado. Sin estado.
 */
object RutaEngine {

    /** Optimiza el orden de visita. [origen] = ubicación GPS o null. */
    fun optimizar(paradas: List<Lead>, origenLat: Double?, origenLon: Double?): List<Lead> {
        if (paradas.size <= 2) return paradas

        // Par (lead, lat, lon) ya garantizado no-null — evita !! y smart-cast sobre var
        data class ConCoord(val lead: Lead, val lat: Double, val lon: Double)
        val conCoords = paradas.mapNotNull { l ->
            val la = l.lat; val lo = l.lon
            if (la != null && lo != null) ConCoord(l, la, lo) else null
        }
        val sinCoords = paradas.filter { it.lat == null || it.lon == null }
        if (conCoords.size <= 1) return paradas

        var startLat = origenLat ?: conCoords[0].lat
        var startLon = origenLon ?: conCoords[0].lon
        val disponibles = conCoords.toMutableList()
        val ordenado = mutableListOf<Lead>()

        while (disponibles.isNotEmpty()) {
            var mejorIdx = 0
            var mejorDist = Double.MAX_VALUE
            disponibles.forEachIndexed { i, p ->
                val d = GeoUtils.distKm(startLat, startLon, p.lat, p.lon)
                if (d < mejorDist) { mejorDist = d; mejorIdx = i }
            }
            val siguiente = disponibles.removeAt(mejorIdx)
            ordenado.add(siguiente.lead)
            startLat = siguiente.lat
            startLon = siguiente.lon
        }
        return ordenado + sinCoords
    }

    /** Distancia total del recorrido en km (para mostrar al usuario). */
    fun distanciaTotal(ruta: List<Lead>, origenLat: Double?, origenLon: Double?): Double {
        if (ruta.isEmpty()) return 0.0
        var total = 0.0
        var lat = origenLat
        var lon = origenLon
        for (p in ruta) {
            val pLat = p.lat; val pLon = p.lon
            if (pLat != null && pLon != null) {
                if (lat != null && lon != null) total += GeoUtils.distKm(lat, lon, pLat, pLon)
                lat = pLat; lon = pLon
            }
        }
        return total
    }

    /** Parada de ruta — shape 1:1 de la PWA: lead o parada manual. */
    @kotlinx.serialization.Serializable
    data class Parada(
        val id: String,
        val leadId: String? = null,
        val nombre: String,
        val direccion: String = "",
        val lat: Double? = null,
        val lon: Double? = null
    )

    /** Port 1:1 de optimizarRuta(): nearest-neighbor; sinCoords al final.
     *  Replica el quirk JS `origen || con[0]`: origen 0.0 cuenta como ausente. */
    fun optimizarParadas(paradas: List<Parada>, origenLat: Double?, origenLon: Double?): List<Parada> {
        if (paradas.size <= 2) return paradas
        val con = paradas.filter { it.lat != null && it.lon != null }
        val sin = paradas.filter { it.lat == null || it.lon == null }
        if (con.size <= 1) return paradas
        var sLat = origenLat?.takeIf { it != 0.0 } ?: con[0].lat!!
        var sLon = origenLon?.takeIf { it != 0.0 } ?: con[0].lon!!
        val disp = con.toMutableList()
        val orden = mutableListOf<Parada>()
        while (disp.isNotEmpty()) {
            var bi = 0; var bd = Double.MAX_VALUE
            disp.forEachIndexed { i, pd ->
                val d = GeoUtils.distKm(sLat, sLon, pd.lat!!, pd.lon!!)
                if (d < bd) { bd = d; bi = i }
            }
            val s = disp.removeAt(bi)
            orden.add(s); sLat = s.lat!!; sLon = s.lon!!
        }
        return orden + sin
    }

    /** Port 1:1 de iniciarRecorrido()/abrirMaps(): URL de Google Maps.
     *  1 parada → maps?q= o search; N → dir con origin/waypoints/destination. */
    fun urlRecorrido(ruta: List<Parada>): String? {
        if (ruta.isEmpty()) return null
        fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
        if (ruta.size == 1) {
            val p = ruta[0]
            return if (p.lat != null && p.lon != null)
                "https://www.google.com/maps?q=${p.lat},${p.lon}"
            else
                "https://www.google.com/maps/search/?api=1&query=" +
                    enc((p.nombre + " " + p.direccion).trim())
        }
        fun fmt(p: Parada) = if (p.lat != null && p.lon != null) "${p.lat},${p.lon}"
                             else enc(p.direccion.ifBlank { p.nombre })
        var url = "https://www.google.com/maps/dir/?api=1&origin=${fmt(ruta.first())}" +
                  "&destination=${fmt(ruta.last())}&travelmode=driving"
        val wpts = ruta.drop(1).dropLast(1).joinToString("|") { fmt(it) }
        if (wpts.isNotEmpty()) url += "&waypoints=$wpts"
        return url
    }
}

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
}

package com.electromel.radar.domain

import kotlin.math.*

/** Port fiel de distKm, calcularBBoxZona, latLonToTile y generarUrlsTiles. */
object GeoUtils {

    data class BBox(val latMin: Double, val lonMin: Double,
                    val latMax: Double, val lonMax: Double)
    data class Tile(val x: Int, val y: Int)

    /** Haversine — misma fórmula que la PWA. */
    fun distKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Centro + radio(km) → bounding box (compensa coseno de latitud). */
    fun bboxDesdeRadio(lat: Double, lon: Double, radioKm: Double): BBox {
        val cosLat = cos(Math.toRadians(lat))
        val dLat = radioKm / 111.0
        val dLon = radioKm / (111.0 * cosLat)
        return BBox(lat - dLat, lon - dLon, lat + dLat, lon + dLon)
    }

    /** lat/lon/zoom → coordenadas de tile OSM (slippy map). */
    fun latLonToTile(lat: Double, lon: Double, zoom: Int): Tile {
        val n = 2.0.pow(zoom)
        val x = floor((lon + 180.0) / 360.0 * n).toInt()
        val rad = Math.toRadians(lat)
        val y = floor((1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * n).toInt()
        return Tile(x, y)
    }

    /** URLs de tiles de un bbox para un rango de zooms (pre-descarga offline). */
    fun urlsTiles(b: BBox, zoomMin: Int, zoomMax: Int): List<String> {
        val subs = listOf("a", "b", "c")
        val urls = mutableListOf<String>()
        for (z in zoomMin..zoomMax) {
            val t1 = latLonToTile(b.latMax, b.lonMin, z)
            val t2 = latLonToTile(b.latMin, b.lonMax, z)
            for (x in minOf(t1.x, t2.x)..maxOf(t1.x, t2.x))
                for (y in minOf(t1.y, t2.y)..maxOf(t1.y, t2.y))
                    urls += "https://${subs[(x + y) % 3]}.tile.openstreetmap.org/$z/$x/$y.png"
        }
        return urls
    }
}

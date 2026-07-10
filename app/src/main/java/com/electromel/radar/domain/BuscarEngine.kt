package com.electromel.radar.domain

import com.electromel.radar.data.HttpClient
import org.json.JSONArray
import org.json.JSONObject

/**
 * Motor de búsqueda de negocios — port de la lógica OSM/Google de la PWA.
 * - Geocodifica la ciudad con Nominatim (bbox)
 * - Busca POIs con Overpass (3 servidores en fallback)
 * - Opcionalmente Google Places (si hay API key)
 * - Fusiona sin duplicar
 *
 * Todas las funciones son bloqueantes → correr en Dispatchers.IO.
 */
object BuscarEngine {

    private val OVERPASS_SERVERS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.private.coffee/api/interpreter"
    )
    private var serverIdx = 0

    data class Resultado(
        val nombre: String, val lat: Double, val lon: Double,
        val tipo: String = "", val telefono: String = "", val direccion: String = "",
        val fuente: String = "osm", val osmId: Long? = null, val googleId: String? = null
    )

    data class Geo(val lat: Double, val lon: Double,
                   val south: Double, val north: Double, val west: Double, val east: Double)

    // Cache de geocodificación por ciudad (evita repetir Nominatim)
    private val geoCache = HashMap<String, Geo>()

    /** Radio máximo útil: diagonal del bbox / 2, +20% margen, mínimo 15km. */
    fun radioMaxKm(g: Geo): Double {
        val diag = GeoUtils.distKm(g.south, g.west, g.north, g.east)
        return maxOf(diag / 2 * 1.2, 15.0)
    }

    /** Geocodifica una ciudad → centro + bounding box. Con cache. */
    fun geocodar(ciudad: String): Geo {
        val key = IutEngine.normalizar(ciudad)
        geoCache[key]?.let { return it }
        val url = "https://nominatim.openstreetmap.org/search?q=" +
                HttpClient.encode(ciudad) + "&format=json&limit=1&addressdetails=0"
        val body = HttpClient.get(url)
        val arr = JSONArray(body)
        if (arr.length() == 0) throw RuntimeException("Ciudad no encontrada: \"$ciudad\"")
        val r = arr.getJSONObject(0)
        // Nominatim boundingbox = [S, N, W, E] como strings
        val bb = r.getJSONArray("boundingbox")
        val geo = Geo(
            lat = r.getString("lat").toDouble(),
            lon = r.getString("lon").toDouble(),
            south = bb.getString(0).toDouble(), north = bb.getString(1).toDouble(),
            west = bb.getString(2).toDouble(), east = bb.getString(3).toDouble()
        )
        geoCache[key] = geo
        return geo
    }

    /** Construye la query Overpass (bbox en orden S,W,N,E que espera Overpass). */
    private fun queryOverpass(rubro: String, g: Geo): String {
        val r = rubro.trim()
        val bbox = "${g.south},${g.west},${g.north},${g.east}"
        return "[out:json][timeout:25];\n(\n" +
            "  node[\"name\"~\"$r\",i]($bbox);\n" +
            "  way[\"name\"~\"$r\",i]($bbox);\n" +
            "  node[\"shop\"~\"$r\",i]($bbox);\n" +
            "  node[\"amenity\"~\"$r\",i]($bbox);\n" +
            "  node[\"leisure\"~\"$r\",i]($bbox);\n" +
            "  node[\"tourism\"~\"$r\",i]($bbox);\n" +
            "  node[\"craft\"~\"$r\",i]($bbox);\n" +
            "  node[\"industrial\"~\"$r\",i]($bbox);\n" +
            ");\nout center tags 30;"
    }

    /** Busca en OSM vía Overpass, con fallback entre servidores. */
    fun buscarOsm(rubro: String, g: Geo): List<Resultado> {
        val query = queryOverpass(rubro, g)
        var ultimoError: Exception? = null
        for (i in OVERPASS_SERVERS.indices) {
            val server = OVERPASS_SERVERS[(serverIdx + i) % OVERPASS_SERVERS.size]
            try {
                val body = HttpClient.post(server, "data=" + HttpClient.encode(query))
                serverIdx = (serverIdx + i) % OVERPASS_SERVERS.size
                return parsearOverpass(body)
            } catch (e: Exception) { ultimoError = e }
        }
        throw RuntimeException("Overpass sin respuesta: ${ultimoError?.message}")
    }

    private fun parsearOverpass(body: String): List<Resultado> {
        val json = JSONObject(body)
        val elements = json.optJSONArray("elements") ?: return emptyList()
        val out = mutableListOf<Resultado>()
        for (i in 0 until elements.length()) {
            val e = elements.getJSONObject(i)
            val tags = e.optJSONObject("tags") ?: continue
            val nombre = tags.optString("name", "")
            if (nombre.isBlank()) continue

            // Coordenadas: node tiene lat/lon; way tiene center
            val lat: Double; val lon: Double
            if (e.has("lat")) { lat = e.getDouble("lat"); lon = e.getDouble("lon") }
            else {
                val center = e.optJSONObject("center") ?: continue
                lat = center.getDouble("lat"); lon = center.getDouble("lon")
            }

            val dir = listOf(
                tags.optString("addr:street", ""), tags.optString("addr:housenumber", "")
            ).filter { it.isNotBlank() }.joinToString(" ")

            out.add(Resultado(
                nombre = nombre, lat = lat, lon = lon,
                tipo = tags.optString("shop", tags.optString("amenity",
                        tags.optString("craft", tags.optString("leisure", "")))),
                telefono = tags.optString("phone", tags.optString("contact:phone", "")),
                direccion = dir, fuente = "osm", osmId = e.optLong("id")
            ))
        }
        return out
    }

    /** Busca en Google Places (Text Search). Requiere API key. */
    fun buscarGoogle(rubro: String, ciudad: String, apiKey: String): List<Resultado> {
        if (apiKey.isBlank()) return emptyList()
        val query = "$rubro en $ciudad"
        val url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" +
                HttpClient.encode(query) + "&key=" + apiKey
        val body = HttpClient.get(url)
        val json = JSONObject(body)
        val status = json.optString("status")
        if (status != "OK" && status != "ZERO_RESULTS")
            throw RuntimeException("Google Places: $status")
        val results = json.optJSONArray("results") ?: return emptyList()
        val out = mutableListOf<Resultado>()
        for (i in 0 until results.length()) {
            val r = results.getJSONObject(i)
            val loc = r.optJSONObject("geometry")?.optJSONObject("location") ?: continue
            out.add(Resultado(
                nombre = r.optString("name", ""),
                lat = loc.getDouble("lat"), lon = loc.getDouble("lng"),
                tipo = r.optJSONArray("types")?.optString(0) ?: "",
                direccion = r.optString("formatted_address", ""),
                fuente = "google", googleId = r.optString("place_id").ifBlank { null }
            ))
        }
        return out
    }

    /** Descarta resultados fuera del radio útil de la ciudad (evita lejanos). */
    fun filtrarPorRadio(resultados: List<Resultado>, g: Geo): List<Resultado> {
        val rMax = radioMaxKm(g)
        return resultados.filter { GeoUtils.distKm(g.lat, g.lon, it.lat, it.lon) <= rMax }
    }

    /** Fusiona OSM + Google sin duplicar (por proximidad + nombre). */
    fun fusionar(osm: List<Resultado>, google: List<Resultado>): List<Resultado> {
        val fusion = osm.toMutableList()
        for (g in google) {
            val dup = fusion.any { o ->
                IutEngine.normalizar(o.nombre) == IutEngine.normalizar(g.nombre) &&
                GeoUtils.distKm(o.lat, o.lon, g.lat, g.lon) < 0.15
            }
            if (!dup) fusion.add(g)
        }
        return fusion
    }

    /** Convierte un Resultado en Lead para guardar. */
    fun aLead(r: Resultado, idGenerado: String, ahoraIso: String): Lead = Lead(
        id = idGenerado, nombre = r.nombre, rubro = r.tipo.ifBlank { "comercio" },
        tipo = r.tipo, telefono = r.telefono, direccion = r.direccion,
        lat = r.lat, lon = r.lon, fuente = r.fuente,
        estado = "no-contactado", osmId = r.osmId, googleId = r.googleId,
        creado = ahoraIso, historial = listOf(EventoHistorial(ahoraIso, "Encontrado en búsqueda"))
    )
}

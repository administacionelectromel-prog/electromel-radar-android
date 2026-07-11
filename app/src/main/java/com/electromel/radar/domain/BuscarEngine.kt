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

    /** Port 1:1 de const CIUDADES_ZONA (autosuggest de ciudad). */
    val CIUDADES_ZONA = listOf(
        "Neuquén", "Cipolletti", "Plottier", "Centenario", "Senillosa",
        "Vista Alegre", "General Roca", "Allen", "Fernández Oro",
        "San Martín de los Andes", "Junín de los Andes",
        "Villa La Angostura", "Bariloche", "Dina Huapi")


    private val OVERPASS_SERVERS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.private.coffee/api/interpreter"
    )
    private var serverIdx = 0

    // ── Zonas de búsqueda (port de la PWA) ──
    val ZONAS_INDUSTRIALES = listOf(
        "", "parque industrial", "zona industrial", "zona talleres", "zona oeste",
        "zona norte", "ruta 22", "corredor productivo", "barrio industrial",
        "área logística", "polo industrial", "zona sur"
    )
    val ZONAS_GENERICAS = listOf(
        "", "centro", "zona norte", "zona sur", "zona este", "zona oeste",
        "zona comercial", "barrio centro", "avenida principal"
    )
    private val KEYWORDS_INDUSTRIALES = listOf(
        "metalurg", "taller", "soldad", "torner", "herrer", "industrial", "industria",
        "fabrica", "motor", "variador", "tablero", "electric", "mecanic", "corralon",
        "logistic", "deposito", "galpon", "construcc", "plastic", "caucho",
        "hidraulic", "neumatic", "generador", "compresor", "bomba", "refriger", "frigorif"
    )

    fun esRubroIndustrial(rubro: String): Boolean {
        val r = IutEngine.normalizar(rubro)
        return KEYWORDS_INDUSTRIALES.any { r.contains(it) }
    }

    /** Genera las consultas ciudad × zonas (port de generarConsultas). */
    fun generarZonas(rubro: String, zonasExtra: List<String>): List<String> {
        val base = when {
            zonasExtra.isNotEmpty() -> listOf("") + zonasExtra
            esRubroIndustrial(rubro) -> ZONAS_INDUSTRIALES
            else -> ZONAS_GENERICAS
        }
        return base.take(12)  // MAX_CONSULTAS
    }

    /** Port 1:1 de generarConsultas: zona vacía → 'rubro ciudad Argentina';
     *  con zona → 'rubro ciudad zona'. Máx 12. */
    fun generarConsultas(ciudad: String, rubro: String,
                         zonasExtra: List<String>): List<String> =
        generarZonas(rubro, zonasExtra).map { zona ->
            (if (zona.isBlank()) "$rubro $ciudad Argentina"
             else "$rubro $ciudad $zona").trim()
        }

    data class Resultado(
        val iut: Int = 0,
        val nombre: String, val lat: Double, val lon: Double,
        val tipo: String = "", val telefono: String = "", val direccion: String = "",
        val fuente: String = "osm", val osmId: Long? = null, val googleId: String? = null,
        val rating: Double? = null
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

    /** Busca en Google Places por CADA zona (ciudad × zonas). Requiere API key. */
    fun buscarGooglePorZonas(rubro: String, ciudad: String, apiKey: String,
                             zonasExtra: List<String>): List<Resultado> {
        if (apiKey.isBlank()) return emptyList()
        val zonas = generarZonas(rubro, zonasExtra)
        val todos = mutableListOf<Resultado>()
        for (zona in zonas) {
            val q = if (zona.isBlank()) "$rubro $ciudad Argentina" else "$rubro $ciudad $zona"
            try { todos.addAll(buscarGoogle(q, apiKey)) } catch (e: Exception) { /* seguir */ }
        }
        return todos
    }

    /** Busca en Google Places (Text Search) con una query armada. */
    fun buscarGoogle(query: String, apiKey: String): List<Resultado> {
        if (apiKey.isBlank()) return emptyList()
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
                fuente = "google", googleId = r.optString("place_id").ifBlank { null },
                rating = if (r.has("rating")) r.optDouble("rating") else null
            ))
        }
        return out
    }

    // Cache de Place Details (port de _detailsCache)
    private val detailsCache = HashMap<String, String>()

    /** Port de enriquecerConTelefonos: Place Details → teléfono (con cache). */
    fun detallesTelefono(placeId: String, apiKey: String): String? {
        detailsCache[placeId]?.let { return it.ifBlank { null } }
        return try {
            val url = "https://maps.googleapis.com/maps/api/place/details/json?place_id=" +
                HttpClient.encode(placeId) + "&fields=formatted_phone_number&key=" + apiKey
            val json = JSONObject(HttpClient.get(url))
            val tel = json.optJSONObject("result")
                ?.optString("formatted_phone_number", "") ?: ""
            detailsCache[placeId] = tel
            tel.ifBlank { null }
        } catch (e: Exception) { null }
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

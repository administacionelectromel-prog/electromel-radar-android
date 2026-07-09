package com.electromel.radar.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * HTTP nativo con HttpURLConnection — cero dependencias externas.
 * Todas las llamadas son bloqueantes; el llamador las corre en Dispatchers.IO.
 */
object HttpClient {

    fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** GET con User-Agent (requerido por Nominatim/Overpass). Devuelve el body o lanza. */
    fun get(url: String, timeoutMs: Int = 15000): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "ElectromelRadar/1.0 (contacto@electromel.com.ar)")
            setRequestProperty("Accept", "application/json")
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
        }
        return leerRespuesta(conn)
    }

    /** POST con body (para Overpass, que acepta la query como data). */
    fun post(url: String, body: String, timeoutMs: Int = 30000): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("User-Agent", "ElectromelRadar/1.0 (contacto@electromel.com.ar)")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return leerRespuesta(conn)
    }

    private fun leerRespuesta(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val texto = stream?.let {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
        } ?: ""
        conn.disconnect()
        if (code !in 200..299) throw RuntimeException("HTTP $code: ${texto.take(200)}")
        return texto
    }
}

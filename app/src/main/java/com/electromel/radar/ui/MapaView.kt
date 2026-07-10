package com.electromel.radar.ui

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.preference.PreferenceManager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.electromel.radar.domain.PrioridadEngine
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * MapView de osmdroid embebido en Compose. osmdroid cachea tiles en disco
 * automáticamente → offline real a nivel de sistema, sin Service Worker.
 */
@Composable
fun MapaView(
    leads: List<LeadUi>,
    userLat: Double?,
    userLon: Double?,
    centrarEnUser: Int,          // se incrementa para pedir "centrar en mi ubicación"
    centrarPunto: Pair<Double, Double>? = null,  // punto arbitrario (MAPA de zonas)
    centrarPuntoTick: Int = 0,   // se incrementa para pedir el centrado
    onLeadClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Configuración global de osmdroid (una vez)
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context, PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue = "com.electromel.radar"
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
            )
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(-38.9516, -68.0591)) // Neuquén
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier) { map ->
        map.overlays.clear()

        // Marcadores de leads
        for (item in leads) {
            val lat = item.lead.lat ?: continue
            val lon = item.lead.lon ?: continue
            val marker = Marker(map).apply {
                position = GeoPoint(lat, lon)
                title = item.lead.nombre
                snippet = "IUT ${item.iut} · ${item.prioridad.label}"
                icon = puntoColor(context, prioridadColorInt(item.prioridad.nivel))
                setOnMarkerClickListener { _, _ ->
                    onLeadClick(item.lead.id); true
                }
            }
            map.overlays.add(marker)
        }

        // Marcador del usuario (punto azul)
        if (userLat != null && userLon != null) {
            val yo = Marker(map).apply {
                position = GeoPoint(userLat, userLon)
                title = "Tu ubicación"
                icon = puntoColor(context, AndroidColor.parseColor("#2D8FFF"), 44)
            }
            map.overlays.add(yo)
        }

        map.invalidate()
    }

    // Centrar en el usuario cuando se pide
    LaunchedEffect(centrarEnUser) {
        if (centrarEnUser > 0 && userLat != null && userLon != null) {
            mapView.controller.animateTo(GeoPoint(userLat, userLon))
            mapView.controller.setZoom(15.0)
        }
    }

    LaunchedEffect(centrarPuntoTick) {
        if (centrarPuntoTick > 0 && centrarPunto != null) {
            mapView.controller.animateTo(GeoPoint(centrarPunto.first, centrarPunto.second))
            mapView.controller.setZoom(15.0)
        }
    }
}

private fun prioridadColorInt(nivel: PrioridadEngine.Nivel): Int = when (nivel) {
    PrioridadEngine.Nivel.HOY      -> AndroidColor.parseColor("#FF3355")
    PrioridadEngine.Nivel.SEMANA   -> AndroidColor.parseColor("#F5C400")
    PrioridadEngine.Nivel.REVISITA -> AndroidColor.parseColor("#00E8A0")
    PrioridadEngine.Nivel.BAJA     -> AndroidColor.parseColor("#4A6888")
}

/** Genera un pin circular del color dado como BitmapDrawable con bounds reales.
 *  osmdroid usa intrinsicWidth/Height para posicionar el marcador; un
 *  GradientDrawable con setSize() deja esos valores en -1 y el pin no se dibuja.
 *  Rasterizarlo a bitmap garantiza dimensiones intrínsecas válidas. */
private fun puntoColor(ctx: android.content.Context, color: Int, size: Int = 36): android.graphics.drawable.Drawable {
    val gd = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(4, AndroidColor.WHITE)
    }
    val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    gd.setBounds(0, 0, size, size)
    gd.draw(canvas)
    return android.graphics.drawable.BitmapDrawable(ctx.resources, bmp)
}

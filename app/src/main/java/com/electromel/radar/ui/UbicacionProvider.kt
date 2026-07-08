package com.electromel.radar.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

/**
 * Envuelve LocationManager (API del sistema, sin Google Play Services,
 * para que funcione en cualquier Android incluso sin servicios de Google).
 */
class UbicacionProvider(private val context: Context) {

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var listener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun iniciar(onUbicacion: (Double, Double) -> Unit) {
        // Última conocida primero (respuesta inmediata)
        val last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        last?.let { onUbicacion(it.latitude, it.longitude) }

        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onUbicacion(location.latitude, location.longitude)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        listener = l

        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, l)
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, l)
        } catch (e: SecurityException) { /* sin permiso — lo pide la Activity */ }
    }

    fun detener() { listener?.let { lm.removeUpdates(it) } }
}

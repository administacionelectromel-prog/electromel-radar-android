package com.electromel.radar.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.electromel.radar.domain.Lead

/**
 * Acciones nativas via Intents. A diferencia de los enlaces wa.me/tel:
 * de la web, estos abren directo la app correspondiente del sistema.
 */
object AccionesNativas {

    private fun limpiarTel(t: String) = t.filter { it.isDigit() || it == '+' }

    /** Abre WhatsApp con el mensaje precargado. */
    fun whatsapp(ctx: Context, lead: Lead, mensaje: String) {
        val tel = limpiarTel(lead.telefono)
        if (tel.length < 6) { Toast.makeText(ctx, "Sin teléfono válido", Toast.LENGTH_SHORT).show(); return }
        val url = "https://wa.me/$tel?text=" + Uri.encode(mensaje)
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(ctx, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
        }
    }

    /** Abre el marcador telefónico con el número cargado. */
    fun llamar(ctx: Context, lead: Lead) {
        val tel = limpiarTel(lead.telefono)
        if (tel.length < 6) { Toast.makeText(ctx, "Sin teléfono válido", Toast.LENGTH_SHORT).show(); return }
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /** Abre la navegación (Google Maps u otra app de mapas) hacia el lead. */
    fun navegar(ctx: Context, lead: Lead) {
        val lat = lead.lat; val lon = lead.lon
        val uri = if (lat != null && lon != null)
            Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(lead.nombre)})")
        else
            Uri.parse("geo:0,0?q=" + Uri.encode(lead.direccion.ifEmpty { lead.nombre }))
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

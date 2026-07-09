package com.electromel.radar.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Escribe el contenido a un archivo temporal y lanza el share sheet nativo. */
object ExportShare {
    fun compartir(ctx: Context, contenido: String, nombreArchivo: String, mime: String) {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val archivo = File(dir, nombreArchivo)
        archivo.writeText(contenido)

        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Compartir $nombreArchivo").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

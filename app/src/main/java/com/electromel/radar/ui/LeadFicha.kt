package com.electromel.radar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.electromel.radar.domain.Mensajes

private val ESTADOS = listOf(
    "no-contactado", "contactado", "presupuesto", "esperando",
    "cliente", "recurrente", "mantenimiento", "urgente", "descartado"
)

@Composable
fun LeadFicha(
    item: LeadUi,
    mensajes: Map<String, Map<String, String>> = Mensajes.DEFAULT,
    onCerrar: () -> Unit,
    onCambiarEstado: (String) -> Unit
) {
    val ctx = LocalContext.current
    val lead = item.lead

    Dialog(onDismissRequest = onCerrar) {
        Column(
            Modifier.fillMaxWidth()
                .background(RadarColors.bgCard, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(lead.nombre, color = RadarColors.text,
                     fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                     modifier = Modifier.weight(1f))
                TextButton(onClick = onCerrar) {
                    Text("✕", color = RadarColors.textDim, fontSize = 18.sp)
                }
            }

            // Prioridad + IUT
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val pc = RadarColors.prioridadColor(item.prioridad.nivel)
                Text("${item.prioridad.ico} ${item.prioridad.label}",
                     color = pc, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("IUT ${item.iut}", color = RadarColors.orange,
                     fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Datos
            Spacer(Modifier.height(10.dp))
            if (lead.direccion.isNotEmpty()) Dato("📍", lead.direccion)
            if (lead.telefono.isNotEmpty())  Dato("📞", lead.telefono)
            if (lead.rubro.isNotEmpty())     Dato("🏷️", lead.rubro)
            if (lead.notas.isNotEmpty())     Dato("📝", lead.notas)

            // Acciones nativas
            Spacer(Modifier.height(14.dp))
            Text("ACCIONES", color = RadarColors.textDim, fontSize = 10.sp,
                 fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AccBtn("💬 WhatsApp", RadarColors.accent, Modifier.weight(1f)) {
                    // Tipo de plantilla según etapa del lead (como la PWA)
                    val tipo = when (lead.estado) {
                        "no-contactado" -> "primero"
                        "contactado", "respondio" -> "seguimiento"
                        else -> "cierre"
                    }
                    AccionesNativas.whatsapp(ctx, lead, Mensajes.build(lead, tipo, mensajes))
                }
                AccBtn("📞 Llamar", RadarColors.blue, Modifier.weight(1f)) {
                    AccionesNativas.llamar(ctx, lead)
                }
            }
            Spacer(Modifier.height(6.dp))
            AccBtn("🧭 Navegar hasta acá", RadarColors.orange, Modifier.fillMaxWidth()) {
                AccionesNativas.navegar(ctx, lead)
            }

            // Cambiar estado
            Spacer(Modifier.height(14.dp))
            Text("ESTADO", color = RadarColors.textDim, fontSize = 10.sp,
                 fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            var expandido by remember { mutableStateOf(false) }
            Box {
                AccBtn("${lead.estado} ▾", RadarColors.bgPanel, Modifier.fillMaxWidth()) {
                    expandido = true
                }
                DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                    ESTADOS.forEach { est ->
                        DropdownMenuItem(
                            text = { Text(est) },
                            onClick = { expandido = false; onCambiarEstado(est) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Dato(ico: String, valor: String) {
    Text("$ico  $valor", color = RadarColors.text, fontSize = 13.sp,
         modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun AccBtn(label: String, color: androidx.compose.ui.graphics.Color,
                   modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if (color == RadarColors.accent || color == RadarColors.orange)
                RadarColors.bg else RadarColors.text
        )
    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
}


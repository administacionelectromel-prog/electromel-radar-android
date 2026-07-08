# ⚡ ELECTROMEL RADAR — Android nativo (Fase 4)

Kotlin + Jetpack Compose. Puerto del motor de dominio de la PWA.

## Qué incluye esta fase
- `domain/Lead.kt` — modelo idéntico al JSON de la PWA (import/export directo)
- `domain/IutEngine.kt` — calcularIUT portado 1:1 (keywords, pesos, tope 99)
- `domain/GeoUtils.kt` — distKm, bbox, tiles OSM (pre-descarga offline)
- `domain/LeadRepository.kt` — el Store con índices O(1)
- `domain/RevisitaEngine.kt` — clasificación de revisitas
- `MainActivity.kt` — pantalla de verificación del motor

## Cómo usar SIN Android Studio (tu flujo)
1. Creá un repo nuevo en GitHub: `electromel-radar-android`
2. Descomprimí este ZIP y pusheá:
   `git remote add origin https://github.com/mauroelectromel/electromel-radar-android.git`
   `git push -u origin main`
3. Esperá ~5 minutos (pestaña Actions muestra el progreso)
4. Bajá la APK desde la pestaña **Releases** del repo → `electromel-radar.apk`
5. Instalala en el celular (permitir "orígenes desconocidos" la primera vez)

La firma está fijada en el repo (`app/debug.keystore`), así que cada APK
nueva se instala ARRIBA de la anterior sin desinstalar ni perder datos.

## Novedad Fase 4 — ficha del lead + acciones nativas
Tocá un lead (en la lista o en el mapa) y se abre su ficha con:
- 💬 WhatsApp — abre la app con el mensaje precargado (Intent nativo)
- 📞 Llamar — abre el marcador con el número cargado
- 🧭 Navegar — abre Google Maps guiándote hasta el negocio
- Cambiar estado — el lead se reclasifica y se reordena solo, persistido

Los Intents nativos abren directo la app correcta, más fluido que los
enlaces web wa.me/tel: de la PWA.

## Novedad Fase 3 — mapa nativo + GPS
- Pestañas LISTA / MAPA arriba
- Mapa osmdroid con tus leads como pines coloreados por prioridad
  (rojo=hoy, amarillo=semana, verde=revisita, azul=sin urgencia)
- Punto azul = tu ubicación (GPS del sistema, sin Google Play Services)
- Botón ◎ para centrar el mapa en donde estás
- osmdroid cachea los tiles en disco solo → offline real de sistema
- La primera vez pide permiso de ubicación

## Novedad Fase 2 — persistencia
Los leads importados se guardan en el celular (Room). Cerrás la app,
la volvés a abrir, y siguen ahí. Ya no hay que reimportar cada vez.

## Cómo usar
1. En la PWA: pestaña STATS → EXPORTAR / SINCRONIZAR → JSON → se baja `electromel_xxx.json`
2. Pasá ese archivo al celular (o exportalo desde el mismo celular)
3. En la APK: tocá **⬇ IMPORTAR** → elegí el JSON
4. Aparecen tus leads reales, ordenados por prioridad táctica:
   CONTACTAR HOY (rojo) → ESTA SEMANA (amarillo) → REVISITA (verde) → SIN URGENCIA (azul)
   Cada card muestra el IUT calculado por el motor nativo.

## Roadmap de fases
1. ✅ **Fase 1**: pantalla Terreno (lista + prioridad táctica)
2. ✅ **Fase 2**: Room (persistencia) + import del JSON de la PWA
3. ✅ **Fase 3**: mapa osmdroid + GPS
4. ✅ **Fase 4**: ficha del lead + acciones nativas (WhatsApp/llamar/navegar)

### Próximo (Fase 5)
- Búsqueda de negocios OSM/Google desde la app nativa
- Captura rápida de leads en terreno con el GPS
- Exportar de vuelta a JSON para sincronizar con la PWA

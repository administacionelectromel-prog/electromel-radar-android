package com.electromel.radar.domain

/**
 * Port del Store de la PWA: única barrera de mutación de la lista de leads,
 * con índices O(1) por id / googleId / osmId / nombre+dirección.
 * La persistencia (Room) se inyecta después — este repo es el contrato.
 */
class LeadRepository {

    private val leads = mutableListOf<Lead>()
    private val idxById = HashMap<String, Lead>()
    private val idxByGoogle = HashMap<String, Lead>()
    private val idxByOsm = HashMap<Long, Lead>()
    private val idxByNombre = HashMap<String, Lead>()

    private fun claveNombre(l: Lead) =
        IutEngine.normalizar(l.nombre) + "|" + IutEngine.normalizar(l.direccion).take(20)

    private fun indexar(l: Lead) {
        idxById[l.id] = l
        l.googleId?.let { idxByGoogle[it] = l }
        l.osmId?.let { idxByOsm[it] = l }
        idxByNombre[claveNombre(l)] = l
    }

    private fun desindexar(l: Lead) {
        idxById.remove(l.id)
        l.googleId?.let { idxByGoogle.remove(it) }
        l.osmId?.let { idxByOsm.remove(it) }
        idxByNombre.remove(claveNombre(l))
    }

    fun all(): List<Lead> = leads
    fun byId(id: String): Lead? = idxById[id]
    fun count(): Int = leads.size

    /** Detección de duplicados O(1) — port de encontrarLeadExistente. */
    fun encontrarExistente(googleId: String?, osmId: Long?, nombre: String, direccion: String): Lead? {
        googleId?.let { idxByGoogle[it]?.let { l -> return l } }
        osmId?.let { idxByOsm[it]?.let { l -> return l } }
        val clave = IutEngine.normalizar(nombre) + "|" + IutEngine.normalizar(direccion).take(20)
        return idxByNombre[clave]
    }

    fun upsert(lead: Lead): Lead {
        val i = leads.indexOfFirst { it.id == lead.id }
        if (i >= 0) { desindexar(leads[i]); leads[i] = lead } else leads.add(lead)
        indexar(lead)
        return lead
    }

    fun remove(id: String): Boolean {
        val lead = idxById[id] ?: return false
        desindexar(lead)
        leads.removeAll { it.id == id }
        return true
    }

    fun appendMany(nuevos: List<Lead>): Int {
        nuevos.forEach { leads.add(it); indexar(it) }
        return nuevos.size
    }

    fun replaceAll(nuevos: List<Lead>): Int {
        leads.clear(); idxById.clear(); idxByGoogle.clear()
        idxByOsm.clear(); idxByNombre.clear()
        nuevos.forEach { leads.add(it); indexar(it) }
        return leads.size
    }

    fun clear() = replaceAll(emptyList())
}

package com.electromel.radar.data

import com.electromel.radar.domain.Lead
import com.electromel.radar.domain.LeadRepository

/**
 * Store persistente: mantiene el índice O(1) en memoria (LeadRepository)
 * y sincroniza con Room. La UI habla con este Store, nunca con el DAO
 * directo — misma barrera que el Store de la PWA.
 */
class LeadStore(private val dao: LeadDao, private val configDao: ConfigDao) {

    private val repo = LeadRepository()

    /** Carga inicial desde disco al abrir la app. */
    suspend fun cargar() {
        val leads = dao.getAll().map { it.toDomain() }
        repo.replaceAll(leads)
    }

    fun all(): List<Lead> = repo.all()
    fun count(): Int = repo.count()
    fun byId(id: String): Lead? = repo.byId(id)

    fun encontrarExistente(googleId: String?, osmId: Long?, nombre: String, direccion: String): Lead? =
        repo.encontrarExistente(googleId, osmId, nombre, direccion)

    suspend fun upsert(lead: Lead): Lead {
        repo.upsert(lead)
        dao.upsert(LeadEntity.from(lead))
        return lead
    }

    suspend fun remove(id: String): Boolean {
        val ok = repo.remove(id)
        if (ok) dao.deleteById(id)
        return ok
    }

    /** Import "reemplazar": pisa todo (memoria + disco). */
    suspend fun replaceAll(leads: List<Lead>): Int {
        repo.replaceAll(leads)
        dao.clear()
        dao.upsertAll(leads.map { LeadEntity.from(it) })
        return leads.size
    }

    /** Import "merge": agrega solo los que no existen (por id/googleId/osmId). */
    suspend fun merge(importados: List<Lead>): Int {
        val nuevos = importados.filter {
            encontrarExistente(it.googleId, it.osmId, it.nombre, it.direccion) == null
        }
        repo.appendMany(nuevos)
        dao.upsertAll(nuevos.map { LeadEntity.from(it) })
        return nuevos.size
    }

    suspend fun clear() {
        repo.clear()
        dao.clear()
    }

    // ── Configuración key-value ──
    suspend fun getConfig(): Map<String, String> =
        configDao.getAll().associate { it.clave to it.valor }

    suspend fun setConfig(clave: String, valor: String) {
        configDao.set(ConfigEntity(clave, valor))
    }
}

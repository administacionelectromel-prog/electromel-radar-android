package com.electromel.radar.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/** Backup — port 1:1 del objeto de crearBackup(): fecha ISO, tipo
 *  (auto/manual/pre-borrado), datos = JSON {leads, ruta, mensajes}. */
@Entity(tableName = "backups")
data class BackupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: String,
    val tipo: String,
    val datos: String,
    val cantLeads: Int
)

@Dao
interface BackupDao {
    @Insert
    suspend fun insertar(b: BackupEntity): Long

    /** Port de DB.loadBackups(5): últimos N, más reciente primero. */
    @Query("SELECT id, fecha, tipo, '' AS datos, cantLeads FROM backups ORDER BY id DESC LIMIT :n")
    suspend fun ultimos(n: Int): List<BackupEntity>

    /** Port de DB.getBackupById. */
    @Query("SELECT * FROM backups WHERE id = :id")
    suspend fun porId(id: Long): BackupEntity?
}

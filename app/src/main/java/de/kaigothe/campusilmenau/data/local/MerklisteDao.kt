package de.kaigothe.campusilmenau.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Ein gemerkter Titel des Bibliothekskatalogs. Nur lokal auf dem Gerät, ohne Verfügbarkeitsstatus. */
@Entity(tableName = "merkliste_titel")
data class MerklisteTitelEntity(
    @PrimaryKey val id: String,
    val titel: String,
    val personen: String,
    val jahr: String?,
    val medientyp: String,
    val coverUrl: String?,
    val gemerktAmMillis: Long,
    val foundAtMillis: Long?,
    val changedAtMillis: Long,
)

@Dao
interface MerklisteDao {
    @Query("SELECT * FROM merkliste_titel")
    fun observeAll(): Flow<List<MerklisteTitelEntity>>

    @Query("SELECT * FROM merkliste_titel WHERE id = :id")
    suspend fun find(id: String): MerklisteTitelEntity?

    /** Eine ID gibt es nur einmal: Ist sie schon da, bleibt der Eintrag, wie er ist. Gibt die neue Zeile oder -1 zurück. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: MerklisteTitelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(entity: MerklisteTitelEntity)

    @Query("DELETE FROM merkliste_titel WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM merkliste_titel")
    suspend fun deleteAll()
}

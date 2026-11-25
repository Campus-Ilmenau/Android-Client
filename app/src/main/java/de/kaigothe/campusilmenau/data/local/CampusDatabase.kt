package de.kaigothe.campusilmenau.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Ein gespeicherter Guthaben-Datenpunkt einer Thoska-Karte für den Verlauf. Nur lokal auf dem Gerät. */
@Entity(tableName = "thoska_balance")
data class ThoskaBalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    /** Tausendstel Euro, so wie die Karte sie liefert. */
    val balanceMilli: Long,
    val lastTransactionMilli: Long,
    val scannedAtMillis: Long,
)

/** Der vom Nutzer vergebene Name einer Karte. Hängt an der Karten-ID. */
@Entity(tableName = "thoska_card_name")
data class ThoskaCardNameEntity(
    @PrimaryKey val cardId: String,
    val name: String,
)

@Dao
interface ThoskaDao {
    @Insert
    suspend fun insert(record: ThoskaBalanceEntity)

    @Query("SELECT * FROM thoska_balance ORDER BY scannedAtMillis")
    fun observeBalances(): Flow<List<ThoskaBalanceEntity>>

    @Query("SELECT * FROM thoska_card_name")
    fun observeNames(): Flow<List<ThoskaCardNameEntity>>

    @Upsert
    suspend fun upsertName(name: ThoskaCardNameEntity)

    @Query("DELETE FROM thoska_card_name WHERE cardId = :cardId")
    suspend fun deleteName(cardId: String)
}

/** Die Datenbank der App. Bisher nur die Thoska; weitere Tabellen kommen mit Bibliothek (M7). */
@Database(
    entities = [ThoskaBalanceEntity::class, ThoskaCardNameEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun thoskaDao(): ThoskaDao

    companion object {
        const val NAME = "campus.db"
    }
}

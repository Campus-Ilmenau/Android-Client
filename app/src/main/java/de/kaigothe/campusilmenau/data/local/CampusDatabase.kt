package de.kaigothe.campusilmenau.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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

    @Query("DELETE FROM thoska_balance")
    suspend fun deleteAllBalances()

    @Query("DELETE FROM thoska_card_name")
    suspend fun deleteAllNames()
}

/** Die Datenbank der App: Thoska-Verlauf und Merkliste der Bibliothek. */
@Database(
    entities = [ThoskaBalanceEntity::class, ThoskaCardNameEntity::class, MerklisteTitelEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun thoskaDao(): ThoskaDao
    abstract fun merklisteDao(): MerklisteDao

    companion object {
        const val NAME = "campus.db"

        /** Version 2: Tabelle der Merkliste (Bibliothek, M7). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `merkliste_titel` (`id` TEXT NOT NULL, `titel` TEXT NOT NULL, " +
                        "`personen` TEXT NOT NULL, `jahr` TEXT, `medientyp` TEXT NOT NULL, `coverUrl` TEXT, " +
                        "`gemerktAmMillis` INTEGER NOT NULL, `foundAtMillis` INTEGER, `changedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
            }
        }
    }
}

package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.data.local.MerklisteDao
import de.kaigothe.campusilmenau.data.local.MerklisteTitelEntity
import de.kaigothe.campusilmenau.domain.model.library.merkliste.GemerkterTitel
import de.kaigothe.campusilmenau.domain.model.library.merkliste.MerklisteListe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Die Merkliste der Titel aus dem Bibliothekskatalog. Liegt nur auf diesem Gerät (Room), hat nichts mit PAIA oder der
 * Anmeldung zu tun, funktioniert also abgemeldet und bleibt über App-Neustarts erhalten. Grabsteine gibt es nicht, bis
 * ein Abgleich mit Wear OS kommt; `foundAt` und `changedAt` werden aber schon geführt.
 */
interface MerklisteRepository {
    /** Offene und gefundene Titel, sortiert für die Anzeige. */
    val liste: Flow<MerklisteListe>

    /** IDs aller gemerkten Titel, für das Lesezeichen in der Titelansicht. */
    val ids: Flow<Set<String>>

    /** Dieselbe ID gibt es nur einmal: Ist sie schon da, bleibt der Eintrag, wie er ist. */
    suspend fun merken(titel: GemerkterTitel)

    /** Nimmt den Titel von der Liste und gibt ihn zurück, damit sich [wiederherstellen] anwenden lässt. */
    suspend fun entfernen(id: String): GemerkterTitel?

    /** Setzt einen entfernten Titel zurück (mit seinem „Gefunden“). Sortiert wird nach `gemerktAm`, er steht also wieder an seiner Stelle. */
    suspend fun wiederherstellen(titel: GemerkterTitel)

    suspend fun alleEntfernen()

    /** Hakt ab ([gefunden]) oder nimmt zurück. Gibt den Eintrag vorher zurück, `null`, wenn es die ID nicht gibt oder sich nichts ändert. */
    suspend fun setzeGefunden(id: String, gefunden: Boolean): GemerkterTitel?

    /** Widerruft das Abhaken: stellt `foundAt` des früheren Eintrags wieder her (als neue Aktion mit `changedAt` = jetzt). */
    suspend fun widerrufeGefunden(vorher: GemerkterTitel)
}

@Singleton
class RoomMerklisteRepository @Inject constructor(
    private val dao: MerklisteDao,
    private val clock: Clock,
) : MerklisteRepository {

    override val liste: Flow<MerklisteListe> = dao.observeAll().map { rows -> MerklisteListe.of(rows.map { it.toDomain() }) }

    override val ids: Flow<Set<String>> = dao.observeAll().map { rows -> rows.mapTo(mutableSetOf()) { it.id } }

    override suspend fun merken(titel: GemerkterTitel) {
        dao.insertIfAbsent(titel.toEntity())
    }

    override suspend fun entfernen(id: String): GemerkterTitel? {
        val entry = dao.find(id) ?: return null
        dao.delete(id)
        return entry.toDomain()
    }

    override suspend fun wiederherstellen(titel: GemerkterTitel) {
        // Gibt es die ID inzwischen wieder, passiert nichts.
        dao.insertIfAbsent(titel.copy(changedAt = clock.instant()).toEntity())
    }

    override suspend fun alleEntfernen() = dao.deleteAll()

    override suspend fun setzeGefunden(id: String, gefunden: Boolean): GemerkterTitel? {
        val entry = dao.find(id)?.toDomain() ?: return null
        if (entry.istGefunden == gefunden) return null
        val now = clock.instant()
        dao.replace(entry.copy(foundAt = if (gefunden) now else null, changedAt = now).toEntity())
        return entry
    }

    override suspend fun widerrufeGefunden(vorher: GemerkterTitel) {
        val entry = dao.find(vorher.id)?.toDomain() ?: return
        dao.replace(entry.copy(foundAt = vorher.foundAt, changedAt = clock.instant()).toEntity())
    }
}

private fun MerklisteTitelEntity.toDomain() = GemerkterTitel(
    id = id,
    titel = titel,
    personen = personen,
    jahr = jahr,
    medientyp = medientyp,
    coverUrl = coverUrl,
    gemerktAm = Instant.ofEpochMilli(gemerktAmMillis),
    foundAt = foundAtMillis?.let(Instant::ofEpochMilli),
    changedAt = Instant.ofEpochMilli(changedAtMillis),
)

private fun GemerkterTitel.toEntity() = MerklisteTitelEntity(
    id = id,
    titel = titel,
    personen = personen,
    jahr = jahr,
    medientyp = medientyp,
    coverUrl = coverUrl,
    gemerktAmMillis = gemerktAm.toEpochMilli(),
    foundAtMillis = foundAt?.toEpochMilli(),
    changedAtMillis = changedAt.toEpochMilli(),
)

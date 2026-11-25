package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.data.local.ThoskaBalanceEntity
import de.kaigothe.campusilmenau.data.local.ThoskaCardNameEntity
import de.kaigothe.campusilmenau.data.local.ThoskaDao
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaBalance
import de.kaigothe.campusilmenau.domain.model.thoska.ThoskaCardNaming
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Gespeicherte Auslesungen und Kartennamen. Alles bleibt auf diesem Gerät. */
interface ThoskaRepository {
    /** Alle Auslesungen aller Karten, älteste zuerst. */
    val balances: Flow<List<ThoskaBalance>>

    /** Die vergebenen Namen je Karten-ID (auch leere und solche, die der ID gleichen: Die Namens-Logik wertet sie aus). */
    val names: Flow<Map<String, String>>

    /** Jedes erfolgreiche Auslesen landet im Verlauf. */
    suspend fun save(balance: ThoskaBalance)

    /** Legt den Namen an oder ändert ihn. Ein leerer Name setzt die Karte zurück. */
    suspend fun rename(cardId: String, input: String)
}

@Singleton
class RoomThoskaRepository @Inject constructor(private val dao: ThoskaDao) : ThoskaRepository {

    override val balances: Flow<List<ThoskaBalance>> = dao.observeBalances().map { list ->
        list.map {
            ThoskaBalance(it.cardId, it.balanceMilli, it.lastTransactionMilli, Instant.ofEpochMilli(it.scannedAtMillis))
        }
    }

    override val names: Flow<Map<String, String>> = dao.observeNames().map { list -> list.associate { it.cardId to it.name } }

    override suspend fun save(balance: ThoskaBalance) {
        dao.insert(
            ThoskaBalanceEntity(
                cardId = balance.cardId,
                balanceMilli = balance.balanceMilli,
                lastTransactionMilli = balance.lastTransactionMilli,
                scannedAtMillis = balance.scannedAt.toEpochMilli(),
            ),
        )
    }

    override suspend fun rename(cardId: String, input: String) {
        val name = ThoskaCardNaming.normalized(input)
        if (name == null) dao.deleteName(cardId) else dao.upsertName(ThoskaCardNameEntity(cardId, name))
    }
}

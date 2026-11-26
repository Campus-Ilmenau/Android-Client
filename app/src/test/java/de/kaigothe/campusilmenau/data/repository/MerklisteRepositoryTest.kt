package de.kaigothe.campusilmenau.data.repository

import de.kaigothe.campusilmenau.data.local.MerklisteDao
import de.kaigothe.campusilmenau.data.local.MerklisteTitelEntity
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailability
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryAvailabilityStatus
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryItem
import de.kaigothe.campusilmenau.domain.model.library.catalog.LibraryLocation
import de.kaigothe.campusilmenau.domain.model.library.merkliste.GemerkterTitel
import de.kaigothe.campusilmenau.domain.model.library.merkliste.MerklisteExemplar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MerklisteRepositoryTest {
    private class FakeDao : MerklisteDao {
        val rows = MutableStateFlow<List<MerklisteTitelEntity>>(emptyList())
        override fun observeAll() = rows
        override suspend fun find(id: String) = rows.value.firstOrNull { it.id == id }
        override suspend fun insertIfAbsent(entity: MerklisteTitelEntity): Long {
            if (find(entity.id) != null) return -1
            rows.value += entity
            return 1
        }
        override suspend fun replace(entity: MerklisteTitelEntity) {
            rows.value = rows.value.filter { it.id != entity.id } + entity
        }
        override suspend fun delete(id: String) { rows.value = rows.value.filter { it.id != id } }
        override suspend fun deleteAll() { rows.value = emptyList() }
    }

    private val now = Instant.parse("2026-03-12T10:00:00Z")
    private val repository = RoomMerklisteRepository(FakeDao(), Clock.fixed(now, ZoneOffset.UTC))

    private fun titel(id: String, minutes: Long) = GemerkterTitel(id = id, titel = "T $id", gemerktAm = Instant.parse("2026-03-01T10:00:00Z").plusSeconds(minutes * 60))

    @Test
    fun `lists newest first and keeps one entry per id`() = runTest {
        repository.merken(titel("a", 1))
        repository.merken(titel("b", 2))
        repository.merken(titel("a", 5).copy(titel = "anders"))
        val liste = repository.liste.first()
        assertEquals(listOf("b", "a"), liste.offen.map { it.id })
        assertEquals("T a", liste.offen.last().titel)
    }

    @Test
    fun `found titles move to their own list, newest found first`() = runTest {
        repository.merken(titel("a", 1))
        repository.merken(titel("b", 2))
        assertNotNull(repository.setzeGefunden("a", true))
        val liste = repository.liste.first()
        assertEquals(listOf("b"), liste.offen.map { it.id })
        assertEquals(listOf("a"), liste.gefunden.map { it.id })
        assertEquals(now, liste.gefunden.single().foundAt)
        assertEquals(now, liste.gefunden.single().changedAt)
    }

    @Test
    fun `setting the same state again changes nothing`() = runTest {
        repository.merken(titel("a", 1))
        assertNull(repository.setzeGefunden("a", false))
        assertNull(repository.setzeGefunden("missing", true))
    }

    @Test
    fun `undoing found restores foundAt as a new action`() = runTest {
        repository.merken(titel("a", 1))
        val before = repository.setzeGefunden("a", true)!!
        repository.widerrufeGefunden(before)
        val entry = repository.liste.first().offen.single()
        assertNull(entry.foundAt)
        assertEquals(now, entry.changedAt)
    }

    @Test
    fun `remove and restore puts the entry back in its place with its found state`() = runTest {
        repository.merken(titel("a", 1))
        repository.merken(titel("b", 2))
        repository.merken(titel("c", 3))
        repository.setzeGefunden("b", true)
        val removed = repository.entfernen("b")!!
        assertEquals(listOf("c", "a"), repository.liste.first().offen.map { it.id })
        repository.wiederherstellen(removed)
        val liste = repository.liste.first()
        assertEquals(listOf("b"), liste.gefunden.map { it.id })
        assertEquals(2, liste.offen.size)
        assertNull(repository.entfernen("missing"))
    }

    @Test
    fun `restoring an id that exists again does nothing`() = runTest {
        repository.merken(titel("a", 1))
        repository.wiederherstellen(titel("a", 1).copy(titel = "alt"))
        assertEquals("T a", repository.liste.first().offen.single().titel)
    }

    @Test
    fun `clear removes everything and ids reflect the list`() = runTest {
        repository.merken(titel("a", 1))
        repository.merken(titel("b", 2))
        assertEquals(setOf("a", "b"), repository.ids.first())
        repository.alleEntfernen()
        assertTrue(repository.liste.first().isEmpty)
    }

    private fun item(status: LibraryAvailabilityStatus, label: String = "ST 1", place: String? = "Lehrbuchsammlung", plan: String? = "https://plan") = LibraryItem(
        label = label,
        location = place?.let { LibraryLocation(null, null, it, plan) },
        status = status,
        dueDate = null,
        reserveUrl = null,
        queue = null,
    )

    @Test
    fun `exemplar prefers a copy on the shelf, then reading room, then on loan`() {
        val availability = LibraryAvailability(
            "m",
            LibraryAvailabilityStatus.Available,
            listOf(item(LibraryAvailabilityStatus.OnLoan, "A"), item(LibraryAvailabilityStatus.ReferenceOnly, "B"), item(LibraryAvailabilityStatus.Available, "C")),
        )
        val exemplar = MerklisteExemplar.of(availability)!!
        assertEquals("C", exemplar.signatur)
        assertEquals(2, exemplar.weitere)
        assertEquals("B", MerklisteExemplar.of(availability.copy(items = availability.items.take(2)))!!.signatur)
    }

    @Test
    fun `exemplar is null without call number and location and drops non web plans`() {
        assertNull(MerklisteExemplar.of(LibraryAvailability("m", LibraryAvailabilityStatus.Online, emptyList())))
        assertNull(MerklisteExemplar.of(LibraryAvailability("m", LibraryAvailabilityStatus.Online, listOf(item(LibraryAvailabilityStatus.Online, label = " ", place = null)))))
        val ftp = MerklisteExemplar.of(LibraryAvailability("m", LibraryAvailabilityStatus.Available, listOf(item(LibraryAvailabilityStatus.Available, plan = "ftp://x"))))!!
        assertNull(ftp.lageplan)
    }
}

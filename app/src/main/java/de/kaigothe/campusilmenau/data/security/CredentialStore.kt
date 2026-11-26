package de.kaigothe.campusilmenau.data.security

/** Benutzernummer und Passwort der Bibliothek, wenn der Nutzer „Angemeldet bleiben“ gewählt hat. */
data class StoredCredentials(val username: String, val password: String)

/**
 * Speicher für die Zugangsdaten der Bibliothek. Nur auf ausdrücklichen Wunsch (Schalter „Angemeldet bleiben“), verschlüsselt
 * mit einem Schlüssel im Android Keystore, nie im Backup. Das Token wird nie gespeichert. Beim Abmelden und in „Gespeicherte
 * Daten löschen“ wird alles entfernt.
 */
interface CredentialStore {
    fun save(credentials: StoredCredentials)

    /** Die gespeicherten Daten, oder `null`, wenn es keine gibt oder sie sich nicht entschlüsseln lassen (dann sind sie weg). */
    fun load(): StoredCredentials?

    fun clear()
}

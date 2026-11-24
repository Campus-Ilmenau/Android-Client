package de.kaigothe.campusilmenau.domain.model.mensa

/**
 * Die vier Mensen, die als Seiten erscheinen. Sie sind fest und verschwinden nie, auch wenn sie keine Gerichte haben.
 * Die Zuordnung läuft über den Namen, den die Mensa-API liefert (`canteen.name`).
 *
 * Der [key] wird gespeichert und steht in Deeplinks. Er darf nicht umbenannt werden (wie `rawValue` in der iOS-App).
 * Die Namen sind Eigennamen und werden nicht übersetzt.
 */
enum class MensaVenue(
    val key: String,
    /** Name in der API. */
    val apiName: String,
    /** Name auf der Seite und für TalkBack. */
    val displayName: String,
    /** Kurzname in der Namensleiste. */
    val shortName: String,
    /** Nur die Cafeteria Ehrenberg bietet Mittag und Abendessen an. */
    val hasMealtimes: Boolean = false,
) {
    MensaEhrenberg("mensaEhrenberg", "Mensa Ehrenberg", "Mensa Ehrenberg", "Ehrenberg"),
    CafeteriaEhrenberg(
        "cafeteriaEhrenberg", "Cafeteria Mensa Ehrenberg", "Cafeteria Ehrenberg", "Cafeteria Ehrenberg",
        hasMealtimes = true,
    ),
    Nanoteria("nanoteria", "Cafeteria NANOteria", "Cafeteria Nanoteria", "Nanoteria"),
    Roentgenbau("roentgenbau", "Cafeteria Röntgenbau", "Cafeteria Röntgenbau", "Röntgenbau"),
    ;

    /** Die nächste Mensa in der Reihenfolge der App, nach der letzten wieder die erste. */
    val next: MensaVenue get() = entries[(ordinal + 1) % entries.size]

    /** Der Name der API, getrimmt und ohne Beachtung der Groß- und Kleinschreibung. */
    fun matches(canteenName: String): Boolean = canteenName.trim().equals(apiName, ignoreCase = true)

    companion object {
        /** Die Mensa zum gespeicherten Schlüssel, oder `null`. */
        fun fromKey(key: String?): MensaVenue? = entries.firstOrNull { it.key == key }
    }
}

enum class MensaMealtime {
    Lunch,
    Dinner,
    ;

    companion object {
        /** Das Gericht gehört zum Abendessen, wenn die Gruppe der API „Abendessen“ heißt. */
        fun isDinner(meal: MensaMeal): Boolean = meal.category.contains("abend", ignoreCase = true)
    }
}

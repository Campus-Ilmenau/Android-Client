package de.kaigothe.campusilmenau.ui.bibliothek

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.library.LoanUrgency
import de.kaigothe.campusilmenau.domain.model.library.HoldTone
import de.kaigothe.campusilmenau.domain.model.library.PaiaError
import de.kaigothe.campusilmenau.ui.components.ChipVariant

/** Der Text zu einem Fehler der Bibliothek. Die Meldung des Servers (Login, Verlängern) steht unverändert da. */
@Composable
fun PaiaError.message(): String = when (this) {
    is PaiaError.AuthenticationFailed -> serverMessage
    is PaiaError.RenewalFailed -> stringResource(R.string.paia_fehler_verlaengerung, serverMessage)
    is PaiaError.Http -> stringResource(R.string.paia_fehler_http, status)
    is PaiaError.InvalidResponse -> stringResource(R.string.paia_fehler_antwort)
    is PaiaError.NotAuthenticated -> stringResource(R.string.paia_fehler_nicht_angemeldet)
    is PaiaError.SessionExpired -> stringResource(R.string.paia_fehler_sitzung)
    is PaiaError.MissingItemIdentifier -> stringResource(R.string.paia_fehler_ohne_kennung)
    is PaiaError.Network -> stringResource(R.string.paia_fehler_netz)
}

fun LoanUrgency.chipVariant(): ChipVariant = when (this) {
    LoanUrgency.Normal -> ChipVariant.Neutral
    LoanUrgency.Soon -> ChipVariant.Warning
    LoanUrgency.Urgent -> ChipVariant.DangerSoft
}

fun HoldTone.chipVariant(): ChipVariant = when (this) {
    HoldTone.Green -> ChipVariant.Green
    HoldTone.Blue -> ChipVariant.Blue
    HoldTone.Neutral -> ChipVariant.Neutral
    HoldTone.DangerSoft -> ChipVariant.DangerSoft
}

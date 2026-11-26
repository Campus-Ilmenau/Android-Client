package de.kaigothe.campusilmenau.ui.share.card

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kaigothe.campusilmenau.domain.model.share.EventShare
import de.kaigothe.campusilmenau.domain.model.share.MensaMealShare
import de.kaigothe.campusilmenau.domain.model.share.MensaPlanShare
import de.kaigothe.campusilmenau.domain.model.share.ShareStrings

// Die drei fertigen Bilder: Rahmen mit Verlauf, darin die Karte. Immer in `ShareCardTheme`.

@Composable
fun SharePlanImage(share: MensaPlanShare, strings: ShareStrings) {
    ShareCardFrame(ShareCardStyle.Light) { SharePlanCard(share, strings) }
}

@Composable
fun ShareMealImage(share: MensaMealShare, strings: ShareStrings) {
    ShareCardFrame(ShareCardStyle.Light) { ShareMealCard(share, strings) }
}

/** Immer nachtblau; die Marke steht als Fußzeile auf dem Nachtblau unter der Karte, ganz unten im Bild. */
@Composable
fun ShareEventImage(share: EventShare, strings: ShareStrings) {
    ShareCardFrame(ShareCardStyle.Night) {
        ShareEventTicket(share, strings)
        Spacer(Modifier.weight(1f))
        ShareBrandFooter(placement = ShareBrandPlacement.OnBackground, modifier = Modifier.padding(top = 12.dp))
    }
}

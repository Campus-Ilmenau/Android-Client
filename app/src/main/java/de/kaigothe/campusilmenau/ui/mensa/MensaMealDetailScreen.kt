package de.kaigothe.campusilmenau.ui.mensa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.core.time.BerlinTime
import de.kaigothe.campusilmenau.domain.model.mensa.MealBadge
import de.kaigothe.campusilmenau.domain.model.mensa.MensaFormatting
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMeal
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealComponent
import de.kaigothe.campusilmenau.domain.model.mensa.MensaMealPrice
import de.kaigothe.campusilmenau.domain.model.mensa.MensaPriceGroup
import de.kaigothe.campusilmenau.domain.model.mensa.MensaVenue
import de.kaigothe.campusilmenau.domain.model.mensa.componentInfos
import de.kaigothe.campusilmenau.domain.model.mensa.detailChips
import de.kaigothe.campusilmenau.domain.model.mensa.uniqueAdditives
import de.kaigothe.campusilmenau.domain.model.mensa.uniqueAllergies
import de.kaigothe.campusilmenau.ui.components.BackBar
import de.kaigothe.campusilmenau.ui.components.BandsLayer
import de.kaigothe.campusilmenau.ui.components.CampusCard
import de.kaigothe.campusilmenau.ui.components.CampusChip
import de.kaigothe.campusilmenau.ui.components.ChipVariant
import de.kaigothe.campusilmenau.ui.components.DiagonalBands
import de.kaigothe.campusilmenau.ui.components.GoatLoader
import de.kaigothe.campusilmenau.ui.components.MessageCard
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.components.SecondaryButton
import de.kaigothe.campusilmenau.ui.navigation.MensaGerichtRoute
import de.kaigothe.campusilmenau.ui.theme.CampusIlmenauTheme
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import de.kaigothe.campusilmenau.ui.theme.Spacing
import java.time.LocalDate

private val HeaderHeight = 250.dp
private val Overlap = 36.dp

/** Gericht-Detail, per Push geöffnet. Liest die Preisgruppe und merkt Favoriten lokal auf dem Gerät. */
@Composable
fun MensaMealDetailScreen(
    route: MensaGerichtRoute,
    onBack: () -> Unit,
    onOpenHours: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MensaMealDetailViewModel = hiltViewModel<MensaMealDetailViewModel, MensaMealDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(route) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = remember { BerlinTime.date(java.time.Instant.now()) }

    when (val current = state) {
        is MensaMealDetailState.Loaded -> MensaMealDetailContent(
            meal = current.meal,
            priceGroup = current.priceGroup,
            isFavorite = current.isFavorite,
            today = today,
            onBack = onBack,
            onToggleFavorite = viewModel::toggleFavorite,
            onOpenHours = onOpenHours,
            modifier = modifier,
        )

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
        ) {
            BackBar(onBack = onBack, description = stringResource(R.string.gericht_zurueck))
            when (current) {
                MensaMealDetailState.Loading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    GoatLoader(size = 96.dp)
                    Text(
                        text = stringResource(R.string.gericht_laedt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CampusTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }

                is MensaMealDetailState.Failed -> MessageCard(
                    title = stringResource(R.string.mensa_fehler_titel),
                    text = current.error.mensaMessage(),
                    buttonText = stringResource(R.string.erneut_versuchen),
                    onButtonClick = viewModel::retry,
                    modifier = Modifier.padding(horizontal = Spacing.ScreenEdge),
                )

                MensaMealDetailState.NotFound -> MessageCard(
                    title = stringResource(R.string.gericht_nicht_gefunden_titel),
                    text = stringResource(R.string.gericht_nicht_gefunden_text),
                    buttonText = stringResource(R.string.gericht_zum_speiseplan),
                    onButtonClick = onBack,
                    modifier = Modifier.padding(horizontal = Spacing.ScreenEdge),
                )

            }
        }
    }
}

/**
 * Oberfläche des Details ohne Datenhaltung, damit sie sich in Vorschauen in jeder Variante zeigen lässt.
 * Drei Ebenen übereinander: hinten Hintergrund und grünes Band, darüber der Inhalt, vorn das blaue Band. So liegt der
 * Inhalt über dem Grünen und wird genau an der blauen Schräge abgeschnitten.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MensaMealDetailContent(
    meal: MensaMeal,
    priceGroup: MensaPriceGroup,
    isFavorite: Boolean,
    today: LocalDate,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenHours: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CampusTheme.colors
    val venueName = MensaVenue.entries.firstOrNull { it.matches(meal.canteenName) }?.displayName ?: meal.canteenName
    val allergies = meal.uniqueAllergies
    val additives = meal.uniqueAdditives

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DiagonalBands(layer = BandsLayer.Behind, height = HeaderHeight)

        // Der Inhalt scrollt über die ganze Höhe bis zum Bildschirmrand. Die Karte ragt 36 dp in den Kopf, der Kopf
        // misst 250 dp vom Bildschirmrand.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.ScreenEdge, end = Spacing.ScreenEdge, top = HeaderHeight - Overlap, bottom = Spacing.Xl),
        ) {
            TitleCard(meal = meal, venueName = venueName, today = today)
            PriceSection(meal = meal, priceGroup = priceGroup)
            if (allergies.isNotEmpty()) {
                ChipSection(stringResource(R.string.gericht_abschnitt_allergene), Icons.Outlined.WarningAmber, allergies)
            }
            if (additives.isNotEmpty()) {
                ChipSection(stringResource(R.string.gericht_abschnitt_zusatzstoffe), Icons.Outlined.Science, additives)
            }
            if (meal.components.isNotEmpty()) ComponentsSection(meal)
            meal.co2GramPerPortion?.let { co2 ->
                SectionHead(stringResource(R.string.gericht_abschnitt_umwelt), Icons.Outlined.Eco)
                CampusCard {
                    Text(
                        text = stringResource(R.string.gericht_co2, "%.0f".format(LocalConfiguration.current.locales[0], co2)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary,
                    )
                }
            }

            Column(modifier = Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = stringResource(if (isFavorite) R.string.favorit_gemerkt else R.string.favorit_merken),
                    onClick = onToggleFavorite,
                )
                SecondaryButton(text = stringResource(R.string.mensa_zeiten_oeffnen), onClick = onOpenHours)
            }

            Column(modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 14.dp)) {
                val style = MaterialTheme.typography.bodySmall
                Text(
                    text = stringResource(R.string.gericht_fuss_ort, venueName, MensaFormatting.fullDate(meal.date)),
                    style = style,
                    color = colors.textTertiary,
                )
                Text(text = stringResource(R.string.gericht_fuss_hinweis), style = style, color = colors.textTertiary)
            }
        }

        // Das Blau liegt vorn, fängt keine Berührungen ab und wirft seinen Schatten auf den Inhalt.
        DiagonalBands(layer = BandsLayer.Front, height = HeaderHeight)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                description = stringResource(R.string.gericht_zurueck),
                onClick = onBack,
            )
            Box(modifier = Modifier.weight(1f))
            // Das Teilen des Gerichts kommt mit M8 hierher, links vom Herz.
            RoundButton(
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                description = stringResource(if (isFavorite) R.string.favorit_entfernen else R.string.favorit_merken),
                tint = if (isFavorite) colors.danger else colors.brandNight,
                onClick = onToggleFavorite,
            )
        }
    }
}

// MARK: - Bausteine

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = CampusTheme.colors.brandNight,
) {
    Box(
        modifier = Modifier
            .size(Spacing.MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(CampusTheme.colors.onBlue.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SectionHead(title: String, icon: ImageVector) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .padding(start = 2.dp, end = 2.dp, top = 22.dp, bottom = 8.dp)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleCard(meal: MensaMeal, venueName: String, today: LocalDate) {
    val colors = CampusTheme.colors
    val category = meal.category.ifEmpty { stringResource(R.string.gericht) }
    val kicker = stringResource(R.string.gericht_kicker, venueName, category, dayLabel(meal.date, today))
    val chips = meal.detailChips

    CampusCard(verticalPadding = 18.dp) {
        // Im Mockup steht das Label weiß im Kopf, dort liegt es aber teils auf Grün und teils auf dem Hintergrund und ist
        // nicht lesbar. Deshalb steht es hier als Kategorie-Zeile wie in der Liste.
        Text(
            text = kicker.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.72.sp,
            ),
            color = colors.textTertiary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = meal.name,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp, lineHeight = 32.sp),
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        meal.description?.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (chips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chips.forEach { CampusChip(text = it.title(), variant = it.tone.variant()) }
            }
        }
    }
}

@Composable
private fun PriceSection(meal: MensaMeal, priceGroup: MensaPriceGroup) {
    val colors = CampusTheme.colors
    val others = MensaPriceGroup.entries.filter { it != priceGroup }
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        SectionHead(stringResource(R.string.gericht_abschnitt_preis), Icons.Outlined.LocalOffer)
        CampusCard {
            Text(text = priceGroup.title(), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
            Text(
                text = meal.price.formatted(priceGroup),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp, lineHeight = 46.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            others.forEach { group ->
                HorizontalDivider(color = colors.separator)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = group.title(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = meal.price.formatted(group),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSection(title: String, icon: ImageVector, items: List<String>) {
    SectionHead(title, icon)
    CampusCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { CampusChip(text = it, variant = ChipVariant.Neutral) }
        }
    }
}

@Composable
private fun ComponentsSection(meal: MensaMeal) {
    val colors = CampusTheme.colors
    val infos = meal.componentInfos
    var expanded by rememberSaveable { mutableStateOf(setOf<Int>()) }

    SectionHead(stringResource(R.string.gericht_abschnitt_bestandteile), Icons.AutoMirrored.Outlined.FormatListBulleted)
    CampusCard(verticalPadding = 0.dp) {
        infos.forEachIndexed { index, info ->
            val isExpanded = index in expanded
            if (index > 0) HorizontalDivider(color = colors.separator)
            val stateText = stringResource(if (isExpanded) R.string.aufgeklappt else R.string.zugeklappt)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { expanded = if (isExpanded) expanded - index else expanded + index }
                    .semantics { stateDescription = stateText },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                val rotation by animateFloatAsState(if (isExpanded) 90f else 0f, label = "chevron")
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation },
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = info.infoText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }
        }
    }
}

// MARK: - Vorschauen

private val PreviewFull = MensaMeal(
    id = "detail-full",
    category = "Essen 1",
    name = "Linsen-Dal",
    description = "Basmatireis, Mango-Chutney",
    price = MensaMealPrice(2.9, 4.4, 5.8, "€"),
    allergies = listOf("Sellerie", "Senf"),
    badges = setOf(MealBadge.Vegan, MealBadge.Vegetarian),
    additives = listOf("Antioxidationsmittel"),
    categories = listOf("Regional", "Kinderessen"),
    co2GramPerPortion = 420.0,
    components = listOf(
        MensaMealComponent("Linsen-Dal", listOf("Sellerie", "Vegan", "Vegetarisch")),
        MensaMealComponent("Basmatireis", listOf("Ohne Zusatzstoffe", "Vegan")),
        MensaMealComponent("Mango-Chutney", listOf("Senf", "Antioxidationsmittel", "Vegan")),
    ),
    canteenName = "Mensa Ehrenberg",
    date = LocalDate.of(2026, 10, 29),
)

private val PreviewShort = MensaMeal(
    id = "detail-short",
    category = "Aktionstheke",
    name = "Pasta Arrabbiata",
    description = null,
    price = MensaMealPrice(2.6, 3.9, null, "€"),
    allergies = emptyList(),
    badges = setOf(MealBadge.Vegan),
    additives = emptyList(),
    categories = emptyList(),
    co2GramPerPortion = null,
    components = emptyList(),
    canteenName = "Mensa Ehrenberg",
    date = LocalDate.of(2026, 10, 29),
)

@Composable
private fun DetailPreview(darkTheme: Boolean, meal: MensaMeal, group: MensaPriceGroup = MensaPriceGroup.Studierende) {
    CampusIlmenauTheme(darkTheme = darkTheme) {
        MensaMealDetailContent(
            meal = meal, priceGroup = group, isFavorite = false, today = LocalDate.of(2026, 10, 29),
            onBack = {}, onToggleFavorite = {}, onOpenHours = {},
        )
    }
}

@Preview(name = "Gericht vollständig Light", showBackground = true, heightDp = 1100)
@Composable
private fun DetailFullLightPreview() = DetailPreview(darkTheme = false, meal = PreviewFull)

@Preview(name = "Gericht vollständig Dark", showBackground = true, heightDp = 1100, backgroundColor = 0xFF0E1A2E)
@Composable
private fun DetailFullDarkPreview() = DetailPreview(darkTheme = true, meal = PreviewFull)

@Preview(name = "Gericht knapp Light", showBackground = true, heightDp = 780)
@Composable
private fun DetailShortLightPreview() = DetailPreview(darkTheme = false, meal = PreviewShort)

@Preview(name = "Preisgruppe Bedienstete Dark", showBackground = true, heightDp = 780, backgroundColor = 0xFF0E1A2E)
@Composable
private fun DetailStaffDarkPreview() = DetailPreview(darkTheme = true, meal = PreviewFull, group = MensaPriceGroup.Bedienstete)

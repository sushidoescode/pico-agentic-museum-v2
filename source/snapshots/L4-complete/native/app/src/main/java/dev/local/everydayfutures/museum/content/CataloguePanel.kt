package dev.local.everydayfutures.museum.content

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.HorizontalDivider
import com.pico.spatial.ui.design.Text
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.catalogue.CatalogueQuery
import dev.local.everydayfutures.museum.catalogue.EmptyReason
import dev.local.everydayfutures.museum.catalogue.Section
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.MuseumState
import dev.local.everydayfutures.museum.state.Surface
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishField
import dev.local.picopolish.PolishSegment
import dev.local.picopolish.PolishSegmented
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishSwitchRow
import dev.local.picopolish.PolishTokens
import dev.local.picopolish.PolishTypography
import kotlinx.coroutines.delay

/** Three cells per row: eighteen objects in six rows, so the whole collection fits without scrolling. */
private const val GRID_COLUMNS = 3

/** Pause in typing (or focus without typing) after which the search keyboard is put away. */
private const val SEARCH_IDLE_HIDE_MS = 8_000L

/** One grid cell: a 44 dp thumbnail beside the number and up to two lines of the name. */
private val CellHeight = 64.dp
private val ThumbnailSize = 44.dp

/**
 * The catalogue region: the title search, the section filter, the All / Saved view and the data-derived counts over a
 * thumbnail grid of every object the filter shows, with explicit zero-result and empty-collection states, then the
 * comfort switches. Sized so the whole collection and every control fit at the window's default size without
 * scrolling (the emulator's verified input route is host clicks only; there is no scroll route). A selection the
 * filter hides stays selected, and the view row says so and offers Show. Text never sits on the window root: this is
 * one Panel surface, and each cell is a Card.
 */
@Composable
internal fun CataloguePanel(modifier: Modifier, state: MuseumState, controller: MuseumController) {
    val filter = state.filter
    val artifacts = Catalogue.artifacts
    val visible = CatalogueQuery.visible(artifacts, filter, state.savedIds)
    val count = CatalogueQuery.count(artifacts, filter, state.savedIds)
    PolishSurface(
        modifier = modifier,
        kind = PolishSurfaceKind.Panel,
        contentPadding = PaddingValues(PolishTokens.SpaceMedium),
        verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSnug),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.catalogue_heading),
                modifier = Modifier.weight(1f),
                style = PolishTypography.overline,
                color = supportingColor(),
            )
            Text(
                text = if (count.filtered) {
                    stringResource(R.string.catalogue_count_filtered, count.visible, count.total, count.saved)
                } else {
                    stringResource(R.string.catalogue_count_all, count.total, count.saved)
                },
                style = PolishTypography.caption,
                color = supportingColor(),
            )
        }
        val keyboard = LocalSoftwareKeyboardController.current
        val focus = LocalFocusManager.current
        var searchFocused by remember { mutableStateOf(false) }
        val putKeyboardAway: (String) -> Unit = { how ->
            keyboard?.hide()
            focus.clearFocus()
            Log.i(MuseumController.TAG, "search-$how keyboard=hide")
        }
        // While the emulator's system keyboard is open it takes every touch (build-003), and build-005 showed that
        // neither Enter route reaches the field there, so the keyboard also goes away after a pause in typing; tapping
        // the field brings it back. Restarted by every keystroke and by focus changes.
        LaunchedEffect(searchFocused, filter.query) {
            if (!searchFocused) return@LaunchedEffect
            delay(SEARCH_IDLE_HIDE_MS)
            putKeyboardAway("idle")
        }
        PolishField(
            label = stringResource(R.string.catalogue_search_label),
            value = filter.query,
            onValueChange = { typed ->
                if (CatalogueQuery.isSubmit(typed)) {
                    // Enter = done: keep the text, put the keyboard away so the catalogue receives touches again.
                    controller.setQuery(CatalogueQuery.withoutLineBreaks(typed), Surface.PRIMARY)
                    putKeyboardAway("submit")
                } else {
                    controller.setQuery(typed, Surface.PRIMARY)
                }
            },
            // A keyboard that sends Enter as a key rather than a line break is caught here, before the field sees it.
            modifier = Modifier
                .onFocusChanged { searchFocused = it.hasFocus }
                .onPreviewKeyEvent { event ->
                    if (event.key != Key.Enter && event.key != Key.NumPadEnter) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyDown) putKeyboardAway("submit")
                    true
                },
            placeholder = stringResource(R.string.catalogue_search_placeholder),
            maxLines = 1,
        )
        PolishSegmented(
            options = listOf(
                PolishSegment<Section?>(null, stringResource(R.string.filter_all)),
                PolishSegment<Section?>(Section.DAILY_RITUALS, stringResource(R.string.filter_daily)),
                PolishSegment<Section?>(Section.CONNECTED_LIVES, stringResource(R.string.filter_connected)),
                PolishSegment<Section?>(Section.DOMESTIC_SYSTEMS, stringResource(R.string.filter_domestic)),
            ),
            selected = filter.section,
            onSelect = { controller.setSection(it, Surface.PRIMARY) },
            modifier = Modifier.fillMaxWidth(),
        )
        ViewRow(state = state, controller = controller)
        // Measured after the rows below it (weight), so if the window is ever shorter than its default the grid gives
        // way — clipped at its bottom — rather than the comfort switches.
        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val empty = CatalogueQuery.emptyReason(artifacts, filter, state.savedIds)
            if (empty != null) {
                EmptyState(reason = empty, query = filter.query, controller = controller)
            } else {
                CatalogueGrid(items = visible, selectedId = state.selectedId, controller = controller)
            }
        }
        HorizontalDivider()
        ComfortSection(state = state, controller = controller)
    }
}

/** All / Saved, then either the hidden-selection note with Show, or nothing (the counts are in the heading row). */
@Composable
private fun ViewRow(state: MuseumState, controller: MuseumController) {
    val filter = state.filter
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSnug),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PolishSegmented(
            options = listOf(
                PolishSegment(false, stringResource(R.string.view_all)),
                PolishSegment(true, stringResource(R.string.view_saved)),
            ),
            selected = filter.savedOnly,
            onSelect = { controller.setSavedOnly(it, Surface.PRIMARY) },
            modifier = Modifier.width(208.dp),
        )
        val selected = Catalogue.byId(state.selectedId)
        if (selected != null && CatalogueQuery.selectionHidden(Catalogue.artifacts, filter, state.savedIds, selected.id)) {
            Text(
                text = stringResource(R.string.catalogue_hidden_selection, selected.number, selected.name),
                modifier = Modifier.weight(1f),
                style = PolishTypography.caption,
                color = supportingColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            PolishButton(onClick = { controller.showAll(Surface.PRIMARY) }, tone = PolishButtonTone.Quiet, size = ButtonDefaults.Small) {
                Text(text = stringResource(R.string.action_show))
            }
        }
    }
}

@Composable
private fun CatalogueGrid(items: List<Artifact>, selectedId: String, controller: MuseumController) {
    Column(verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceTiny)) {
        items.chunked(GRID_COLUMNS).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                row.forEach { item ->
                    CatalogueCell(
                        item = item,
                        selected = item.id == selectedId,
                        onClick = { controller.select(item.id, Surface.PRIMARY) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(GRID_COLUMNS - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * One selectable cell. The kit marks selection with its thicker accent edge (and `selected` semantics); the number and
 * name are always text, so a missing or undecodable thumbnail loses nothing but the picture.
 */
@Composable
private fun CatalogueCell(item: Artifact, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    PolishSurface(
        modifier = modifier.height(CellHeight),
        kind = PolishSurfaceKind.Card,
        onClick = onClick,
        selected = selected,
        contentPadding = PaddingValues(PolishTokens.SpaceSmall),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(item = item)
            Column {
                Text(text = item.number, style = PolishTypography.overline, color = supportingColor())
                Text(text = item.name, style = PolishTypography.caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The thumbnail, or — while it loads, and when it is missing or corrupt — the number as a badge. */
@Composable
private fun Thumbnail(item: Artifact) {
    val image = rememberThumbnail(item.thumbnail)
    Box(modifier = Modifier.size(ThumbnailSize), contentAlignment = Alignment.Center) {
        if (image != null) {
            // Decorative: the number and name beside it carry the identity for accessibility.
            Image(bitmap = image, contentDescription = null, modifier = Modifier.size(ThumbnailSize), contentScale = ContentScale.Fit)
        } else {
            Text(text = item.number, style = PolishTypography.sectionTitle, color = supportingColor())
        }
    }
}

/** Why the grid is empty, and the one action that helps: Clear search, or Show all. */
@Composable
private fun EmptyState(reason: EmptyReason, query: String, controller: MuseumController) {
    Column(verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
        Text(
            text = when (reason) {
                EmptyReason.NO_MATCH -> stringResource(R.string.catalogue_no_match, query.trim())
                EmptyReason.NOTHING_SAVED -> stringResource(R.string.catalogue_nothing_saved)
                EmptyReason.NONE_IN_VIEW -> stringResource(R.string.catalogue_none_in_view)
            },
            style = PolishTypography.objectTitle,
        )
        if (reason == EmptyReason.NO_MATCH) {
            PolishButton(onClick = { controller.clearQuery(Surface.PRIMARY) }, tone = PolishButtonTone.Standard) {
                Text(text = stringResource(R.string.action_clear_search))
            }
        } else {
            PolishButton(onClick = { controller.showAll(Surface.PRIMARY) }, tone = PolishButtonTone.Standard) {
                Text(text = stringResource(R.string.action_show_all))
            }
        }
    }
}

/** The three comfort switches, moved under the grid; titles only, so they stay reachable at the default size. */
@Composable
private fun ComfortSection(state: MuseumState, controller: MuseumController) {
    Column(verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
        Text(text = stringResource(R.string.comfort_heading), style = PolishTypography.sectionTitle)
        PolishSwitchRow(
            title = stringResource(R.string.comfort_motion),
            checked = state.options.reduceMotion,
            onCheckedChange = { controller.setReduceMotion(it, Surface.PRIMARY) },
        )
        PolishSwitchRow(
            title = stringResource(R.string.comfort_transparency),
            checked = state.options.reduceTransparency,
            onCheckedChange = { controller.setReduceTransparency(it, Surface.PRIMARY) },
        )
        PolishSwitchRow(
            title = stringResource(R.string.comfort_contrast),
            checked = state.options.increaseContrast,
            onCheckedChange = { controller.setIncreaseContrast(it, Surface.PRIMARY) },
        )
    }
}

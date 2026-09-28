package dev.local.everydayfutures.museum.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.HorizontalDivider
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.AlertDialog
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.MuseumState
import dev.local.everydayfutures.museum.state.StoryReducer
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.Turn
import dev.local.picopolish.LocalPolishOptions
import dev.local.picopolish.LocalPolishSurfaceKind
import dev.local.picopolish.PolishActionBar
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishFactRow
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceDefaults
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishSwitchRow
import dev.local.picopolish.PolishTokens
import dev.local.picopolish.PolishTypography
import dev.local.picopolish.PolishWindowContent

/** Supporting-text colour for the enclosing polish surface; never copies the kit's tone arithmetic. */
@Composable
internal fun supportingColor(): Color =
    PolishSurfaceDefaults.secondaryForegroundFor(LocalPolishSurfaceKind.current, LocalPolishOptions.current)

/**
 * One window, three regions: catalogue (left), study (centre, the 3D object on the window's own glass) and
 * details (right). Text never sits directly on the window root; every reading surface is a polish surface.
 */
@Composable
fun MuseumWindow(controller: MuseumController, modifier: Modifier = Modifier) {
    val state = controller.state
    val artifact = Catalogue.byId(state.selectedId) ?: Catalogue.artifacts.first()
    PolishWindowContent(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.app_name)) },
        titleTrailingActions = { Text(text = stringResource(R.string.museum_eyebrow), style = PolishTypography.caption) },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceLarge),
        ) {
            CatalogueColumn(
                modifier = Modifier.width(280.dp).fillMaxHeight(),
                state = state,
                controller = controller,
            )
            ObjectStudy(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                artifact = artifact,
                state = state,
                controller = controller,
            )
            DetailsColumn(
                modifier = Modifier.width(380.dp).fillMaxHeight(),
                artifact = artifact,
                state = state,
                controller = controller,
            )
        }
    }
}

@Composable
private fun CatalogueColumn(modifier: Modifier, state: MuseumState, controller: MuseumController) {
    PolishSurface(modifier = modifier, kind = PolishSurfaceKind.Panel) {
        Text(text = stringResource(R.string.catalogue_heading), style = PolishTypography.overline, color = supportingColor())
        Text(text = stringResource(R.string.catalogue_subheading), style = PolishTypography.sectionTitle)
        Catalogue.artifacts.forEach { item ->
            // Interactive card: the kit draws a hoverable fill (no material) and marks selection with an edge.
            PolishSurface(
                modifier = Modifier.fillMaxWidth(),
                kind = PolishSurfaceKind.Card,
                onClick = { controller.select(item.id, Surface.PRIMARY) },
                selected = item.id == state.selectedId,
            ) {
                Text(text = item.number, style = PolishTypography.overline, color = supportingColor())
                Text(text = item.shortName, style = PolishTypography.objectTitle)
                Text(text = item.selectLabel, style = PolishTypography.caption, color = supportingColor())
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()
        Text(text = stringResource(R.string.comfort_heading), style = PolishTypography.sectionTitle)
        PolishSwitchRow(
            title = stringResource(R.string.comfort_motion),
            checked = state.options.reduceMotion,
            onCheckedChange = { controller.setReduceMotion(it, Surface.PRIMARY) },
            supportingText = stringResource(R.string.comfort_motion_hint),
        )
        PolishSwitchRow(
            title = stringResource(R.string.comfort_transparency),
            checked = state.options.reduceTransparency,
            onCheckedChange = { controller.setReduceTransparency(it, Surface.PRIMARY) },
            supportingText = stringResource(R.string.comfort_transparency_hint),
        )
        PolishSwitchRow(
            title = stringResource(R.string.comfort_contrast),
            checked = state.options.increaseContrast,
            onCheckedChange = { controller.setIncreaseContrast(it, Surface.PRIMARY) },
            supportingText = stringResource(R.string.comfort_contrast_hint),
        )
    }
}

@Composable
private fun ObjectStudy(modifier: Modifier, artifact: Artifact, state: MuseumState, controller: MuseumController) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium)) {
        PolishSurface(modifier = Modifier.fillMaxWidth(), kind = PolishSurfaceKind.Control) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val story = state.story
                Text(
                    text = when {
                        story != null -> stringResource(
                            R.string.study_story_open,
                            story.chapterNumber,
                            StoryReducer.chapterCount(story.objectId),
                            Catalogue.byId(story.objectId)?.chapters?.getOrNull(story.chapter)?.title.orEmpty(),
                        )
                        state.isInspecting -> stringResource(R.string.study_inspecting)
                        else -> stringResource(R.string.study_number, artifact.number)
                    },
                    style = PolishTypography.overline,
                    color = supportingColor(),
                )
                Text(
                    text = if (state.isFrontView) stringResource(R.string.study_front) else stringResource(R.string.study_turned, state.displayDegrees),
                    style = PolishTypography.caption,
                )
            }
        }
        // The study scene sits on the window's own glass; no surface wraps it, so the volume keeps its full depth.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ObjectPresenter(
                selectedId = state.selectedId,
                turnDegrees = state.turnDegrees,
                modifier = Modifier.fillMaxSize(),
            )
        }
        PolishActionBar(modifier = Modifier.fillMaxWidth()) {
            PolishButton(onClick = { controller.turn(Turn.LEFT, Surface.PRIMARY) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_turn_left))
            }
            PolishButton(onClick = { controller.turn(Turn.RIGHT, Surface.PRIMARY) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_turn_right))
            }
            Spacer(modifier = Modifier.weight(1f))
            // The story is revealed from the study (L3); while it is open the same action brings the companion forward.
            val navigator = rememberWindowNavigator()
            PolishButton(onClick = { controller.revealStory(navigator, Surface.PRIMARY) }, tone = PolishButtonTone.Emphasis) {
                Text(text = if (state.story != null) stringResource(R.string.action_show_story) else stringResource(R.string.action_reveal_story))
            }
            PolishButton(onClick = { controller.reset(Surface.PRIMARY) }, enabled = state.canReset) {
                Text(text = stringResource(R.string.action_reset))
            }
        }
    }
}

@Composable
private fun DetailsColumn(modifier: Modifier, artifact: Artifact, state: MuseumState, controller: MuseumController) {
    PolishSurface(modifier = modifier, kind = PolishSurfaceKind.Panel) {
        Text(text = stringResource(R.string.details_object, artifact.number), style = PolishTypography.overline, color = supportingColor())
        Text(text = artifact.title, style = PolishTypography.display)
        Text(text = artifact.subtitle, style = PolishTypography.objectTitle)
        Text(text = artifact.description, style = PolishTypography.bodyLead(multiline = true))
        Text(text = artifact.question, style = PolishTypography.body(multiline = true), color = supportingColor())
        PolishFactRow(label = stringResource(R.string.details_collection), value = stringResource(R.string.details_collection_value))
        PolishFactRow(label = stringResource(R.string.details_focus), value = artifact.category)
        PolishFactRow(label = stringResource(R.string.details_era), value = artifact.era)
        Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
            PolishButton(onClick = { controller.toggleInspect(Surface.PRIMARY) }, tone = PolishButtonTone.Emphasis) {
                Text(text = if (state.isInspecting) stringResource(R.string.action_return) else stringResource(R.string.action_inspect))
            }
            PolishButton(onClick = { controller.keep(Surface.PRIMARY) }, enabled = !state.isSaved()) {
                Text(text = if (state.isSaved()) stringResource(R.string.action_kept) else stringResource(R.string.action_keep))
            }
        }
        CollectionSection(state = state, controller = controller)
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()
        CompanionSection(state = state, controller = controller)
    }
}

/**
 * The saved collection and its confirmed reset (design-doc "Reset saved collection"). The trigger is quiet, sits with
 * Keep rather than with the study's Reset or the story's Restart, and is labelled "Clear saved collection…" so it cannot
 * be mistaken for either; it asks first in the system alert dialog. Keep collection (or dismissing) changes nothing;
 * Start fresh clears only the saved objects — selection, study, story, windows and comfort preferences stay.
 */
@Composable
private fun CollectionSection(state: MuseumState, controller: MuseumController) {
    // Saveable, so a configuration change keeps an open confirmation open.
    var confirming by rememberSaveable { mutableStateOf(false) }
    val saved = state.savedIds.size
    Text(
        text = if (saved > 0) stringResource(R.string.collection_count, saved, Catalogue.artifacts.size) else stringResource(R.string.collection_empty),
        style = PolishTypography.caption,
        color = supportingColor(),
    )
    PolishButton(
        onClick = { confirming = true },
        enabled = saved > 0,
        tone = PolishButtonTone.Quiet,
    ) {
        Text(text = stringResource(R.string.action_clear_collection))
    }
    if (confirming) {
        // Logged whenever the confirmation is shown, including after the window is recreated with it open.
        LaunchedEffect(Unit) { controller.collectionClearAsked(Surface.PRIMARY) }
        val keep = {
            confirming = false
            controller.collectionClearCancelled(Surface.PRIMARY)
        }
        AlertDialog(
            onDismissRequest = keep,
            title = { Text(text = stringResource(R.string.collection_dialog_title)) },
            content = { Text(text = stringResource(R.string.collection_dialog_text), style = PolishTypography.body(multiline = true)) },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                    PolishButton(onClick = keep, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.action_keep_collection))
                    }
                    PolishButton(
                        onClick = {
                            confirming = false
                            controller.clearCollection(Surface.PRIMARY)
                        },
                        modifier = Modifier.weight(1f),
                        tone = PolishButtonTone.Destructive,
                    ) {
                        Text(text = stringResource(R.string.action_start_fresh))
                    }
                }
            },
        )
    }
}

/**
 * The window-level companion actions: an explicit open (or, when it is already open, a request for the same instance
 * again) and a close. The catalogue, details and study above keep working whether the companion is open or not.
 */
@Composable
private fun CompanionSection(state: MuseumState, controller: MuseumController) {
    val navigator = rememberWindowNavigator()
    val open = state.workspace.companionOpen
    Text(text = stringResource(R.string.details_companion_heading), style = PolishTypography.sectionTitle)
    Text(
        text = if (open) stringResource(R.string.details_companion_open) else stringResource(R.string.details_companion_closed),
        style = PolishTypography.caption,
        color = supportingColor(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
        PolishButton(onClick = { controller.openCompanion(navigator, Surface.PRIMARY) }, tone = PolishButtonTone.Standard) {
            Text(text = if (open) stringResource(R.string.action_focus_companion) else stringResource(R.string.action_open_companion))
        }
        if (open) {
            // Short label: the section heading names the window, and two long labels wrapped in the 380 dp column (build-001/-003 frames).
            PolishButton(onClick = { controller.closeCompanion(navigator, Surface.PRIMARY) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_close_short))
            }
        }
    }
}

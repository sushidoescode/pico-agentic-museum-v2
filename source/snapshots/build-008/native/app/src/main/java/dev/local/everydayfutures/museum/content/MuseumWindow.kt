package dev.local.everydayfutures.museum.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.HorizontalDivider
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.AlertDialog
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.catalogue.Size3
import dev.local.everydayfutures.museum.catalogue.SizeText
import dev.local.everydayfutures.museum.state.ArchivePhase
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.MuseumState
import dev.local.everydayfutures.museum.state.StoryReducer
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.Turn
import dev.local.picopolish.LocalPolishOptions
import dev.local.picopolish.LocalPolishSurfaceKind
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishFactRow
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceDefaults
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishTokens
import dev.local.picopolish.PolishTypography
import dev.local.picopolish.PolishWindowContent
import kotlinx.coroutines.launch

/** Supporting-text colour for the enclosing polish surface; never copies the kit's tone arithmetic. */
@Composable
internal fun supportingColor(): Color =
    PolishSurfaceDefaults.secondaryForegroundFor(LocalPolishSurfaceKind.current, LocalPolishOptions.current)

/**
 * Column weights at the default 1440 dp width (content 1376 dp after the 16 dp padding and gaps): catalogue ≈ 550 dp
 * (a three-across grid of ≈ 167 dp cells), study ≈ 454 dp, details ≈ 372 dp. Weights rather than fixed widths so the
 * 1120 dp minimum still shows all three regions.
 */
private const val CATALOGUE_WEIGHT = 0.40f
private const val STUDY_WEIGHT = 0.33f
private const val DETAILS_WEIGHT = 0.27f

/**
 * One window, three regions: the catalogue (a thumbnail grid of the whole collection with search, section filter and
 * All / Saved, then the comfort switches), the study (the selected object's model on the window's own glass) and the
 * details (identification, the curators' note, facts and the collection and window actions). The header's trailing
 * action enters the immersive archive. Text never sits directly on the window root; every reading surface is a polish
 * surface.
 */
@Composable
fun MuseumWindow(controller: MuseumController, modifier: Modifier = Modifier) {
    val state = controller.state
    val artifact = Catalogue.byId(state.selectedId) ?: Catalogue.artifacts.first()
    PolishWindowContent(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.app_name)) },
        titleTrailingActions = {
            Text(text = stringResource(R.string.museum_eyebrow), style = PolishTypography.caption)
            ArchiveAction(state = state, controller = controller)
        },
        contentPadding = PaddingValues(PolishTokens.SpaceMedium),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium),
        ) {
            CataloguePanel(
                modifier = Modifier.weight(CATALOGUE_WEIGHT).fillMaxHeight(),
                state = state,
                controller = controller,
            )
            ObjectStudy(
                modifier = Modifier.weight(STUDY_WEIGHT).fillMaxHeight(),
                artifact = artifact,
                state = state,
                controller = controller,
            )
            DetailsColumn(
                modifier = Modifier.weight(DETAILS_WEIGHT).fillMaxHeight(),
                artifact = artifact,
                state = state,
                controller = controller,
            )
        }
    }
}

/**
 * Enter archive (header trailing action): records the request, closes an open auxiliary window, then opens the Full
 * Space Stage in this window's coroutine scope and records the result. While the archive is open the same place
 * offers Return from archive. A refused open is said beside it.
 */
@Composable
private fun ArchiveAction(state: MuseumState, controller: MuseumController) {
    val windows = rememberWindowNavigator()
    val stage = rememberStageNavigator()
    val scope = rememberCoroutineScope()
    val phase = state.archive.phase
    if (phase == ArchivePhase.CLOSED && state.archive.lastRefusal != null) {
        Text(text = stringResource(R.string.archive_unavailable), style = PolishTypography.caption)
    }
    PolishButton(
        onClick = {
            when (phase) {
                ArchivePhase.CLOSED -> if (controller.enterArchive(windows, Surface.PRIMARY)) {
                    scope.launch { controller.completeArchiveOpen(stage, Surface.PRIMARY) }
                }
                ArchivePhase.OPEN -> scope.launch { controller.leaveArchive(stage, Surface.PRIMARY) }
                ArchivePhase.OPENING, ArchivePhase.CLOSING -> Unit
            }
        },
        loading = phase == ArchivePhase.OPENING || phase == ArchivePhase.CLOSING,
        tone = PolishButtonTone.Emphasis,
    ) {
        Text(text = if (phase == ArchivePhase.OPEN) stringResource(R.string.action_return_archive) else stringResource(R.string.action_enter_archive))
    }
}

@Composable
private fun ObjectStudy(modifier: Modifier, artifact: Artifact, state: MuseumState, controller: MuseumController) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSnug)) {
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
            val studyNavigator = rememberWindowNavigator()
            ObjectPresenter(
                artifact = artifact,
                turnDegrees = state.turnDegrees,
                controller = controller,
                modifier = Modifier.fillMaxSize(),
                // While the inspector shows this object the study hides its copy (the platform does not always fade
                // this window behind it); Bring it forward is Open in 3D again: the same window, focused.
                shownInInspector = state.studyShownInInspector,
                onBringForward = { controller.openInspector(studyNavigator, Surface.PRIMARY) },
            )
        }
        // Two rows, because the study is about 450 dp wide: the study's own controls, then the two ways out of it.
        PolishSurface(modifier = Modifier.fillMaxWidth(), kind = PolishSurfaceKind.Control) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                PolishButton(onClick = { controller.turn(Turn.LEFT, Surface.PRIMARY) }, tone = PolishButtonTone.Quiet) {
                    Text(text = stringResource(R.string.action_turn_left))
                }
                PolishButton(onClick = { controller.turn(Turn.RIGHT, Surface.PRIMARY) }, tone = PolishButtonTone.Quiet) {
                    Text(text = stringResource(R.string.action_turn_right))
                }
                Spacer(modifier = Modifier.weight(1f))
                PolishButton(onClick = { controller.reset(Surface.PRIMARY) }, enabled = state.canReset) {
                    Text(text = stringResource(R.string.action_reset))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                val navigator = rememberWindowNavigator()
                PolishButton(onClick = { controller.openInspector(navigator, Surface.PRIMARY) }, tone = PolishButtonTone.Standard) {
                    Text(text = stringResource(R.string.action_open_3d))
                }
                Spacer(modifier = Modifier.weight(1f))
                // Only objects with a story offer it (no dead-end story buttons). While it is open the same action
                // brings the companion forward (L3).
                if (artifact.hasStory) {
                    PolishButton(onClick = { controller.revealStory(navigator, Surface.PRIMARY) }, tone = PolishButtonTone.Emphasis) {
                        Text(
                            text = if (state.story != null) stringResource(R.string.action_show_story) else stringResource(R.string.action_reveal_story),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The selected object's label: number and section, name, subtitle, the factual identification, the curators' note
 * (visibly labelled as their interpretation), the question and the facts; then Inspect, Keep with the saved
 * collection, and the companion window. The reading text is measured last and clipped if it ever runs long, so the actions below
 * it always stay on screen.
 */
@Composable
private fun DetailsColumn(modifier: Modifier, artifact: Artifact, state: MuseumState, controller: MuseumController) {
    PolishSurface(
        modifier = modifier,
        kind = PolishSurfaceKind.Panel,
        contentPadding = PaddingValues(PolishTokens.SpaceMedium),
        verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSnug),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSnug),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceTiny)) {
                Text(
                    text = stringResource(R.string.details_overline, artifact.number, artifact.section.label),
                    style = PolishTypography.overline,
                    color = supportingColor(),
                )
                Text(text = artifact.name, style = PolishTypography.display)
                Text(text = artifact.subtitle, style = PolishTypography.objectTitle)
            }
            LabelledText(label = stringResource(R.string.details_identification), text = artifact.identification)
            LabelledText(label = stringResource(R.string.details_curators_note), text = artifact.interpretation)
            Text(text = artifact.question, style = PolishTypography.body(multiline = true), color = supportingColor())
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSnug)) {
            PolishFactRow(label = stringResource(R.string.details_section), value = artifact.section.label, modifier = Modifier.weight(1.4f))
            PolishFactRow(label = stringResource(R.string.details_real_size), value = realSizeText(artifact), modifier = Modifier.weight(1f))
        }
        PolishFactRow(label = stringResource(R.string.details_period), value = artifact.era)
        // Open in 3D lives once, under the study beside the model it opens (build-005 showed it here as well).
        PolishButton(onClick = { controller.toggleInspect(Surface.PRIMARY) }, tone = PolishButtonTone.Emphasis) {
            Text(text = if (state.isInspecting) stringResource(R.string.action_return) else stringResource(R.string.action_inspect))
        }
        CollectionSection(state = state, controller = controller)
        HorizontalDivider()
        CompanionSection(state = state, controller = controller)
    }
}

@Composable
private fun LabelledText(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceTiny)) {
        Text(text = label, style = PolishTypography.overline, color = supportingColor())
        Text(text = text, style = PolishTypography.body(multiline = true))
    }
}

@Composable
private fun realSizeText(artifact: Artifact): String = realSizePhrase(artifact.model.realSize)

/** "30 cm tall", "15 cm long", "7.5 cm wide": the object's largest real dimension and its direction. */
@Composable
fun realSizePhrase(size: Size3): String {
    val (extent, metres) = SizeText.largest(size)
    val length = SizeText.length(metres)
    return when (extent) {
        SizeText.Extent.TALL -> stringResource(R.string.real_size_tall, length)
        SizeText.Extent.WIDE -> stringResource(R.string.real_size_wide, length)
        SizeText.Extent.LONG -> stringResource(R.string.real_size_long, length)
    }
}

/**
 * Keep and the saved collection with its confirmed reset (design-doc "Reset saved collection"). The reset trigger is
 * quiet, sits with Keep rather than with the study's Reset or the story's Restart, and is labelled "Clear saved
 * collection…" so it cannot be mistaken for either; it asks first in the system alert dialog. Keep collection (or
 * dismissing) changes nothing; Start fresh clears only the saved objects — selection, study, story, filter, windows and
 * comfort preferences stay. The count uses the registry's size.
 */
@Composable
private fun CollectionSection(state: MuseumState, controller: MuseumController) {
    // Saveable, so a configuration change keeps an open confirmation open.
    var confirming by rememberSaveable { mutableStateOf(false) }
    val saved = state.savedIds.size
    // Keep beside the count it changes; the reset on its own row below (the two labels do not fit one ~340 dp row).
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSnug),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PolishButton(onClick = { controller.keep(Surface.PRIMARY) }, enabled = !state.isSaved()) {
            Text(text = if (state.isSaved()) stringResource(R.string.action_kept) else stringResource(R.string.action_keep))
        }
        Text(
            text = if (saved > 0) stringResource(R.string.collection_count, saved, Catalogue.artifacts.size) else stringResource(R.string.collection_empty),
            modifier = Modifier.weight(1f),
            style = PolishTypography.caption,
            color = supportingColor(),
        )
    }
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
 * again) and a close. Only one auxiliary window is shown at a time: opening the companion while the 3D inspector is
 * open closes the inspector first. The catalogue, details and study keep working whether the companion is open or not.
 */
@Composable
private fun CompanionSection(state: MuseumState, controller: MuseumController) {
    val navigator = rememberWindowNavigator()
    val open = state.workspace.companionOpen
    Text(
        text = when {
            open -> stringResource(R.string.details_companion_open)
            state.workspace.inspectorOpen -> stringResource(R.string.details_companion_inspector_open)
            else -> stringResource(R.string.details_companion_closed)
        },
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

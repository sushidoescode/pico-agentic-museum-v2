package dev.local.everydayfutures.museum.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Text
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.MuseumState
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.Turn
import dev.local.everydayfutures.museum.state.WindowNavigator
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishTokens
import dev.local.picopolish.PolishTypography
import dev.local.picopolish.PolishWindowContent

/**
 * The optional companion window, shown to the visitor as the "Object guide", in one of two modes over the same
 * app-owned state:
 *  - details (L2): the selected object's identity, the study's turn/reset controls, "Reveal its story" (only for the
 *    three objects that have a story) and a compact selection of the current bay's six objects (the selection's
 *    section; the whole collection is in the catalogue window); no 3D scene of its own, the study object stays in the
 *    primary window;
 *  - story (L3): the object's miniature in this window's own scene and its chapter controls ([CompanionStory]).
 * Closing it returns the visitor to the catalogue window and changes no logical state. [compact] is the 360 dp
 * Subwindow prototype ([CompanionSubwindow]); without it the layout is the accepted window's.
 */
@Composable
fun CompanionWindow(controller: MuseumController, modifier: Modifier = Modifier, compact: Boolean = false) {
    val state = controller.state
    val artifact = Catalogue.byId(state.selectedId) ?: Catalogue.artifacts.first()
    val navigator = rememberWindowNavigator()
    val story = state.story
    if (compact) {
        CompactGuide(controller = controller, artifact = artifact, state = state, navigator = navigator, modifier = modifier)
        return
    }
    if (story != null) {
        PolishWindowContent(modifier = modifier) {
            CompanionStory(
                controller = controller,
                artifact = Catalogue.byId(story.objectId) ?: artifact,
                story = story,
                reduceMotion = state.options.reduceMotion,
                navigator = navigator,
            )
        }
        return
    }
    PolishWindowContent(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium),
        ) {
            IdentityPanel(modifier = Modifier.weight(1f).fillMaxHeight(), artifact = artifact, state = state)
            InspectPanel(modifier = Modifier.width(320.dp).fillMaxHeight(), artifact = artifact, state = state, controller = controller, navigator = navigator)
        }
        // The current bay: the selection's section, six objects, in accession order. Everything else is one click away
        // in the catalogue window, which the hint says, so no selection here is a dead end. The hint sits on the window's
        // own glass, which Reduce transparency does not change, so it takes the glass colour, not the opaque ground's.
        val bay = Catalogue.bySection(artifact.section)
        Text(
            text = stringResource(R.string.companion_bay_hint, artifact.section.label, Catalogue.artifacts.size),
            style = PolishTypography.caption,
            color = rootSupportingColor(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall),
            verticalAlignment = Alignment.Bottom,
        ) {
            bay.forEach { item ->
                BayCard(modifier = Modifier.weight(1f), item = item, state = state, controller = controller)
            }
            PolishButton(onClick = { controller.closeCompanion(navigator, Surface.COMPANION) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_close_short))
            }
        }
    }
}

/**
 * The guide in the 360 dp Subwindow prototype ([CompanionSubwindow], CompanionHost.SUBWINDOW only): one column, with
 * the guide's name and Close at the top in both modes, because the system offers no way to dismiss a Subwindow. Details
 * mode stacks the identity, the study's controls, the bay hint and the bay as a 3 × 2 grid; story mode puts the
 * miniature above its chapter panel. The controller calls are the window's, from [Surface.COMPANION].
 */
@Composable
private fun CompactGuide(controller: MuseumController, artifact: Artifact, state: MuseumState, navigator: WindowNavigator, modifier: Modifier) {
    val story = state.story
    val shown = story?.let { Catalogue.byId(it.objectId) } ?: artifact
    PolishWindowContent(modifier = modifier, contentPadding = PaddingValues(PolishTokens.SpaceMedium)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // On the Subwindow's own glass, outside any surface (as the bay hint).
            Text(
                text = stringResource(R.string.companion_now_showing, shown.number),
                modifier = Modifier.weight(1f),
                style = PolishTypography.overline,
                color = rootSupportingColor(),
            )
            PolishButton(onClick = { controller.closeCompanion(navigator, Surface.COMPANION) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_close_short))
            }
        }
        if (story != null) {
            CompanionStory(
                controller = controller,
                artifact = shown,
                story = story,
                reduceMotion = state.options.reduceMotion,
                navigator = navigator,
                modifier = Modifier.weight(1f),
                compact = true,
            )
        } else {
            // The header names the object, so the identity panel does not repeat it.
            IdentityPanel(modifier = Modifier.fillMaxWidth().weight(1f), artifact = artifact, state = state, showOverline = false)
            InspectPanel(modifier = Modifier.fillMaxWidth(), artifact = artifact, state = state, controller = controller, navigator = navigator, compact = true)
            Text(
                text = stringResource(R.string.companion_bay_hint, artifact.section.label, Catalogue.artifacts.size),
                style = PolishTypography.caption,
                color = rootSupportingColor(),
            )
            Catalogue.bySection(artifact.section).chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                    row.forEach { item ->
                        BayCard(modifier = Modifier.weight(1f), item = item, state = state, controller = controller)
                    }
                }
            }
        }
    }
}

/** Compact selectable card: the kit marks selection with its thicker edge, the number repeats it in text. */
@Composable
private fun BayCard(modifier: Modifier, item: Artifact, state: MuseumState, controller: MuseumController) {
    PolishSurface(
        modifier = modifier,
        kind = PolishSurfaceKind.Card,
        onClick = { controller.select(item.id, Surface.COMPANION) },
        selected = item.id == state.selectedId,
        contentPadding = PaddingValues(PolishTokens.SpaceSmall),
        verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceTiny),
    ) {
        Text(text = item.number, style = PolishTypography.overline, color = supportingColor())
        Text(text = item.name, style = PolishTypography.caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun IdentityPanel(modifier: Modifier, artifact: Artifact, state: MuseumState, showOverline: Boolean = true) {
    PolishSurface(modifier = modifier, kind = PolishSurfaceKind.Panel) {
        if (showOverline) {
            Text(text = stringResource(R.string.companion_now_showing, artifact.number), style = PolishTypography.overline, color = supportingColor())
        }
        Text(text = artifact.title, style = PolishTypography.display)
        Text(text = "${artifact.shortName} · ${artifact.name}", style = PolishTypography.objectTitle)
        // The catalogue's one-line hook for why the object is here (existing copy; the details carry the curators' note).
        Text(text = artifact.subtitle, style = PolishTypography.body(multiline = true))
        Text(text = artifact.question, style = PolishTypography.body(multiline = true), color = supportingColor())
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (state.isSaved()) stringResource(R.string.companion_saved) else stringResource(R.string.companion_not_saved),
            style = PolishTypography.caption,
            color = supportingColor(),
        )
    }
}

/** The study's controls, acting on the primary's model. [compact]: sized to its content, so no weighted spacer above the buttons. */
@Composable
private fun InspectPanel(
    modifier: Modifier,
    artifact: Artifact,
    state: MuseumState,
    controller: MuseumController,
    navigator: WindowNavigator,
    compact: Boolean = false,
) {
    PolishSurface(modifier = modifier, kind = PolishSurfaceKind.Panel) {
        Text(text = stringResource(R.string.companion_inspect_heading), style = PolishTypography.overline, color = supportingColor())
        Text(
            text = if (state.isFrontView) stringResource(R.string.study_front) else stringResource(R.string.study_turned, state.displayDegrees),
            style = PolishTypography.sectionTitle,
        )
        // With no primary attached (closed by the system), the study these controls act on is not on screen.
        Text(
            text = if (state.workspace.primaryInstances == 0) {
                stringResource(R.string.companion_primary_closed)
            } else {
                stringResource(R.string.companion_turn_hint)
            },
            style = PolishTypography.caption,
            color = supportingColor(),
        )
        if (!compact) Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
            PolishButton(onClick = { controller.turn(Turn.LEFT, Surface.COMPANION) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_turn_left))
            }
            PolishButton(onClick = { controller.turn(Turn.RIGHT, Surface.COMPANION) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_turn_right))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
            PolishButton(onClick = { controller.reset(Surface.COMPANION) }, enabled = state.canReset) {
                Text(text = stringResource(R.string.action_reset))
            }
            // Only the three objects with a story offer it; the others have no story to reveal.
            if (artifact.hasStory) {
                PolishButton(onClick = { controller.revealStory(navigator, Surface.COMPANION) }, tone = PolishButtonTone.Emphasis) {
                    Text(text = stringResource(R.string.action_reveal_story_short))
                }
            }
        }
    }
}

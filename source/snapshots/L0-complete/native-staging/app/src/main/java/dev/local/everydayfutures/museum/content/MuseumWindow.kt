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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.HorizontalDivider
import com.pico.spatial.ui.design.Text
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.MuseumState
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
private fun supportingColor(): Color =
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
                onClick = { controller.select(item.id) },
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
            onCheckedChange = controller::setReduceMotion,
            supportingText = stringResource(R.string.comfort_motion_hint),
        )
        PolishSwitchRow(
            title = stringResource(R.string.comfort_transparency),
            checked = state.options.reduceTransparency,
            onCheckedChange = controller::setReduceTransparency,
            supportingText = stringResource(R.string.comfort_transparency_hint),
        )
        PolishSwitchRow(
            title = stringResource(R.string.comfort_contrast),
            checked = state.options.increaseContrast,
            onCheckedChange = controller::setIncreaseContrast,
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
                Text(
                    text = if (state.isInspecting) stringResource(R.string.study_inspecting) else stringResource(R.string.study_number, artifact.number),
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
            PolishButton(onClick = { controller.turn(Turn.LEFT) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_turn_left))
            }
            PolishButton(onClick = { controller.turn(Turn.RIGHT) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_turn_right))
            }
            Spacer(modifier = Modifier.weight(1f))
            PolishButton(onClick = controller::reset, enabled = state.canReset) {
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
            PolishButton(onClick = controller::toggleInspect, tone = PolishButtonTone.Emphasis) {
                Text(text = if (state.isInspecting) stringResource(R.string.action_return) else stringResource(R.string.action_inspect))
            }
            PolishButton(onClick = controller::keep, enabled = !state.isSaved()) {
                Text(text = if (state.isSaved()) stringResource(R.string.action_kept) else stringResource(R.string.action_keep))
            }
        }
    }
}

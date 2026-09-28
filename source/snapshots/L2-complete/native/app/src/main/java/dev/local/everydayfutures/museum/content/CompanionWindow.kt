package dev.local.everydayfutures.museum.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Text
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.MuseumState
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.Turn
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishTokens
import dev.local.picopolish.PolishTypography
import dev.local.picopolish.PolishWindowContent

/**
 * The optional companion window: the selected object's identity, the study's turn/reset controls and a compact
 * selection, all reading and writing the app-owned state the catalogue window uses. In details mode it has no 3D scene
 * of its own; the study object stays in the primary window. Closing it returns the visitor to the catalogue window and
 * changes no logical state.
 */
@Composable
fun CompanionWindow(controller: MuseumController, modifier: Modifier = Modifier) {
    val state = controller.state
    val artifact = Catalogue.byId(state.selectedId) ?: Catalogue.artifacts.first()
    val navigator = rememberWindowNavigator()
    PolishWindowContent(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium),
        ) {
            IdentityPanel(modifier = Modifier.weight(1f).fillMaxHeight(), artifact = artifact, state = state)
            InspectPanel(modifier = Modifier.width(320.dp).fillMaxHeight(), state = state, controller = controller)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall),
            verticalAlignment = Alignment.Bottom,
        ) {
            Catalogue.artifacts.forEach { item ->
                // Compact selectable card: the kit marks selection with its thicker edge, the number repeats it in text.
                PolishSurface(
                    modifier = Modifier.weight(1f),
                    kind = PolishSurfaceKind.Card,
                    onClick = { controller.select(item.id, Surface.COMPANION) },
                    selected = item.id == state.selectedId,
                ) {
                    Text(text = item.number, style = PolishTypography.overline, color = supportingColor())
                    Text(text = item.shortName, style = PolishTypography.caption)
                }
            }
            PolishButton(onClick = { controller.closeCompanion(navigator, Surface.COMPANION) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_close_short))
            }
        }
    }
}

@Composable
private fun IdentityPanel(modifier: Modifier, artifact: Artifact, state: MuseumState) {
    PolishSurface(modifier = modifier, kind = PolishSurfaceKind.Panel) {
        Text(text = stringResource(R.string.companion_now_showing, artifact.number), style = PolishTypography.overline, color = supportingColor())
        Text(text = artifact.title, style = PolishTypography.display)
        Text(text = "${artifact.shortName} · ${artifact.selectLabel}", style = PolishTypography.objectTitle)
        Text(text = artifact.question, style = PolishTypography.body(multiline = true), color = supportingColor())
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (state.isSaved()) stringResource(R.string.companion_saved) else stringResource(R.string.companion_not_saved),
            style = PolishTypography.caption,
            color = supportingColor(),
        )
    }
}

@Composable
private fun InspectPanel(modifier: Modifier, state: MuseumState, controller: MuseumController) {
    PolishSurface(modifier = modifier, kind = PolishSurfaceKind.Panel) {
        Text(text = stringResource(R.string.companion_inspect_heading), style = PolishTypography.overline, color = supportingColor())
        Text(
            text = if (state.isFrontView) stringResource(R.string.study_front) else stringResource(R.string.study_turned, state.displayDegrees),
            style = PolishTypography.sectionTitle,
        )
        Text(
            text = if (state.isInspecting) stringResource(R.string.companion_view_inspect) else stringResource(R.string.companion_view_catalogue),
            style = PolishTypography.caption,
            color = supportingColor(),
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
            PolishButton(onClick = { controller.turn(Turn.LEFT, Surface.COMPANION) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_turn_left))
            }
            PolishButton(onClick = { controller.turn(Turn.RIGHT, Surface.COMPANION) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_turn_right))
            }
        }
        PolishButton(onClick = { controller.reset(Surface.COMPANION) }, enabled = state.canReset) {
            Text(text = stringResource(R.string.action_reset))
        }
    }
}

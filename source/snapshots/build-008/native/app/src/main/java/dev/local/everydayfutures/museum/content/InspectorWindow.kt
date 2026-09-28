package dev.local.everydayfutures.museum.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.Text
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.catalogue.Catalogue
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.Turn
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishToolbar
import dev.local.picopolish.PolishTokens
import dev.local.picopolish.PolishTypography

/**
 * The Open in 3D inspector: an independent volumetric window with its material background off (the root paints
 * nothing; `PolishWindowSurface.AppOpaque`). It shows the object in [MuseumController]'s inspector presentation — the
 * loaded model standing free in the volume, with no backdrop — a compact title chip, and the window's system toolbar
 * with Turn left / Turn right (30°), Spin / Pause, Reset view and Close. Its rotation is its own state: the study, the
 * story and the saved collection never change from here.
 */
@Composable
fun InspectorWindow(controller: MuseumController, modifier: Modifier = Modifier) {
    val state = controller.state
    val presentation = state.inspector
    val artifact = Catalogue.byId(presentation?.objectId ?: state.selectedId) ?: Catalogue.artifacts.first()
    val navigator = rememberWindowNavigator()
    // One live pose per object: a new selection starts from a fresh accumulator, so no turn of the previous object can
    // be folded into the new one's state.
    val pose = remember(artifact.id) { InspectorPose() }
    val reduceMotion = state.options.reduceMotion
    val spinning = presentation?.spinning == true && !reduceMotion
    val committedYaw = { controller.state.inspector?.yawDegrees ?: 0f }

    // Every stop this window makes (Pause, Turn, Reset, Close, a drag) folds the live turn in before the state changes,
    // so nothing is left over. A stop from elsewhere — Reduce motion switched on in another window — leaves the turn
    // spin accumulated here; fold it in now, so the committed yaw is the angle on screen.
    LaunchedEffect(spinning, pose) {
        if (!spinning && pose.pending != 0f) {
            controller.inspectorSettle(
                liveYaw = pose.commit(committedYaw()),
                reason = if (reduceMotion) "reduce-motion" else "stopped",
                source = Surface.INSPECTOR,
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        InspectorPresenter(
            artifact = artifact,
            controller = controller,
            pose = pose,
            modifier = Modifier.fillMaxSize(),
        )
        TitleChip(artifact = artifact, modifier = Modifier.align(Alignment.TopCenter).padding(top = PolishTokens.SpaceMedium))
    }

    // The window's one system toolbar (a companion window the platform anchors below this one). Every action folds the
    // live turn into app state first, so what is committed is what is on screen.
    PolishToolbar {
        PolishButton(
            onClick = { controller.inspectorTurn(Turn.LEFT, pose.commit(committedYaw()), Surface.INSPECTOR) },
            tone = PolishButtonTone.Quiet,
            size = ButtonDefaults.Small,
        ) {
            Text(text = stringResource(R.string.action_turn_left))
        }
        PolishButton(
            onClick = { controller.inspectorTurn(Turn.RIGHT, pose.commit(committedYaw()), Surface.INSPECTOR) },
            tone = PolishButtonTone.Quiet,
            size = ButtonDefaults.Small,
        ) {
            Text(text = stringResource(R.string.action_turn_right))
        }
        // Spin is opt-in and never runs under Reduce motion, so the button is disabled there.
        PolishButton(
            onClick = { controller.inspectorToggleSpin(pose.commit(committedYaw()), Surface.INSPECTOR) },
            enabled = spinning || !reduceMotion,
            tone = PolishButtonTone.Standard,
            size = ButtonDefaults.Small,
        ) {
            Text(text = if (spinning) stringResource(R.string.action_pause) else stringResource(R.string.action_spin))
        }
        PolishButton(
            onClick = {
                val stoppedAt = pose.commit(committedYaw())
                pose.clear()
                controller.inspectorResetView(Surface.INSPECTOR, liveYaw = stoppedAt)
            },
            tone = PolishButtonTone.Quiet,
            size = ButtonDefaults.Small,
        ) {
            Text(text = stringResource(R.string.action_reset_view))
        }
        PolishButton(
            onClick = { controller.closeInspector(navigator, pose.commit(committedYaw()), Surface.INSPECTOR) },
            tone = PolishButtonTone.Quiet,
            size = ButtonDefaults.Small,
        ) {
            Text(text = stringResource(R.string.action_close_short))
        }
    }
}

/** The compact identity over the volume: number and name, then the real size (the model is fitted, not to scale). */
@Composable
private fun TitleChip(artifact: Artifact, modifier: Modifier = Modifier) {
    PolishSurface(
        modifier = modifier,
        kind = PolishSurfaceKind.Card,
        contentPadding = PaddingValues(horizontal = PolishTokens.SpaceMedium, vertical = PolishTokens.SpaceSmall),
        verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceTiny),
    ) {
        Text(text = stringResource(R.string.inspector_title, artifact.number, artifact.name), style = PolishTypography.objectTitle)
        Text(
            text = stringResource(R.string.inspector_real_size, realSizePhrase(artifact.model.realSize)),
            style = PolishTypography.caption,
            color = supportingColor(),
        )
    }
}

package dev.local.everydayfutures.museum.content

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Text
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishTypography

/** Widest a model card gets, so it reads as a card over the scene rather than a sheet across it. */
private val CardMaxWidth = 320.dp

/**
 * Shown over a model region while the selected object's model loads ("Loading 004 Coffee pod…"). Text only, so it is
 * the same with Reduce motion on.
 */
@Composable
internal fun ModelLoadingCard(artifact: Artifact, modifier: Modifier = Modifier) {
    PolishSurface(modifier = Modifier.widthIn(max = CardMaxWidth).then(modifier), kind = PolishSurfaceKind.Card) {
        Text(text = stringResource(R.string.model_loading, artifact.number, artifact.name), style = PolishTypography.objectTitle)
    }
}

/**
 * Shown over a model region when nothing can be shown for the selected object: it names the object (never shows
 * another one) and offers Try again while this selection's retries last.
 */
@Composable
internal fun ModelErrorCard(artifact: Artifact, canRetry: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    PolishSurface(modifier = Modifier.widthIn(max = CardMaxWidth).then(modifier), kind = PolishSurfaceKind.Card) {
        Text(text = stringResource(R.string.model_failed, artifact.number, artifact.name), style = PolishTypography.objectTitle)
        Text(
            text = if (canRetry) stringResource(R.string.model_failed_hint) else stringResource(R.string.model_failed_final),
            style = PolishTypography.body(multiline = true),
        )
        if (canRetry) {
            PolishButton(onClick = onRetry, tone = PolishButtonTone.Emphasis) {
                Text(text = stringResource(R.string.action_try_again))
            }
        }
    }
}

/**
 * Shown over the study while the Open in 3D inspector shows the same object (the study's model is hidden meanwhile,
 * so two copies never overlap): says where the object is, and brings that window forward (the same id and tag: a
 * focus, never a second inspector).
 */
@Composable
internal fun ModelElsewhereCard(onBringForward: () -> Unit, modifier: Modifier = Modifier) {
    PolishSurface(modifier = Modifier.widthIn(max = CardMaxWidth).then(modifier), kind = PolishSurfaceKind.Card) {
        Text(text = stringResource(R.string.study_in_3d_view), style = PolishTypography.objectTitle)
        PolishButton(onClick = onBringForward, tone = PolishButtonTone.Quiet) {
            Text(text = stringResource(R.string.action_bring_forward))
        }
    }
}

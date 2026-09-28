package dev.local.picopolish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pico.spatial.ui.design.windows.Toolbar

/**
 * The window's system toolbar: a real bottom-anchored companion window the platform positions,
 * materialises and follows the viewpoint with.
 *
 * Call it from inside a window container's content. It is deliberately a thin pass-through — the
 * kit adds a consistent gap and nothing else, because the toolbar's material, corner radius,
 * anchor and layer are system decisions. Put [PolishIconButton]s in it.
 *
 * Every toolbar is a real system window, so one per container, not one per list item.
 */
@Composable
fun PolishToolbar(content: @Composable RowScope.() -> Unit) {
    Toolbar(
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    )
}

/**
 * An in-window action bar for screens that should not spend the single system toolbar — a row of
 * actions that scrolls with the content instead of following the viewpoint.
 *
 * It is a [PolishSurface] of kind [PolishSurfaceKind.Control], so it obeys the same single-surface,
 * reduced-transparency and contrast rules as every other surface in the kit.
 */
@Composable
fun PolishActionBar(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal =
        Arrangement.spacedBy(PolishTokens.SpaceSmall),
    content: @Composable RowScope.() -> Unit,
) {
    PolishSurface(
        modifier = modifier,
        kind = PolishSurfaceKind.Control,
    ) {
        Row(
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

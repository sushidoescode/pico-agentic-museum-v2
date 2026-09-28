package dev.local.picopolish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pico.spatial.ui.design.HorizontalDivider
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.TitleBar

/**
 * The content layout for one window: an optional title bar, a body, and an optional action row.
 *
 * This composes **content only**. It never creates, opens or closes a container, and it never
 * touches the container's background: window and subwindow lifecycle belongs to the app's
 * `SpatialAppScope` declarations, and the root material belongs to the system unless the app took
 * it over deliberately (see [Modifier.polishWindowRoot]).
 *
 * Put it directly inside a `DefaultWindowContainer { PolishTheme { ... } }` or a
 * `WindowContainer(...) { PolishTheme { ... } }` body. Apply `windowConstraints(...)` yourself at
 * that call site — it is a member of the container scope, so it cannot be applied from in here.
 *
 * @param title the window title slot; `TitleBar` takes a composable, not a string.
 * @param actions a trailing action row pinned under the body, separated by a divider.
 */
@Composable
fun PolishWindowContent(
    modifier: Modifier = Modifier,
    title: @Composable (BoxScope.() -> Unit)? = null,
    titleLeadingActions: @Composable (RowScope.() -> Unit)? = null,
    titleTrailingActions: @Composable (RowScope.() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(PolishTokens.SpaceLarge),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(PolishTokens.SpaceMedium),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (title != null) {
            TitleBar(
                title = title,
                leadingActions = titleLeadingActions,
                trailingActions = titleTrailingActions,
            )
        }
        Column(
            modifier =
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
        if (actions != null) {
            HorizontalDivider(color = PicoTheme.colorScheme.dividerLine)
            Row(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/**
 * A two-region window body: a fixed-width leading region (a list, a navigation column) beside a
 * flexible detail region, with the kit's gap between them.
 *
 * Both regions are plain content. Give each one a [PolishSurface] if it needs its own surface; do
 * not wrap the whole row in one, or the two regions share a single material and stop reading as
 * two objects.
 */
@Composable
fun PolishSplitLayout(
    leading: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    leadingWeight: Float = 0.38f,
    detail: @Composable BoxScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceLarge),
    ) {
        Box(modifier = Modifier.weight(leadingWeight).fillMaxSize(), content = leading)
        Box(modifier = Modifier.weight(1f - leadingWeight).fillMaxSize(), content = detail)
    }
}

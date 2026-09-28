package dev.local.picopolish

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Slider
import com.pico.spatial.ui.design.Switch
import com.pico.spatial.ui.design.Text

/**
 * A labelled row that puts a control on the right of a title and an optional description.
 *
 * The row is layout only: it paints no background, so it composes inside a [PolishSurface] without
 * creating a second surface. Text colours come from the surface's provided foreground, with the
 * supporting line stepped down through [PolishSurfaceDefaults.secondaryForegroundFor] — which
 * strengthens automatically under `increaseContrast` instead of staying a light grey.
 */
@Composable
fun PolishControlRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    kind: PolishSurfaceKind = LocalPolishSurfaceKind.current,
    control: @Composable RowScope.() -> Unit,
) {
    val options = LocalPolishOptions.current
    val supportingColor = PolishSurfaceDefaults.secondaryForegroundFor(kind, options)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceTiny),
        ) {
            Text(text = title, style = PolishTypography.objectTitle)
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = PolishTypography.caption,
                    color = supportingColor,
                )
            }
        }
        control()
    }
}

/**
 * A titled on/off row built on the built-in `Switch`.
 *
 * The switch owns its own hover, press audio, haptics and track colours — the accent role for the
 * checked track — so nothing is layered on top of it. The whole row reports the on/off state to
 * accessibility, so the state is not conveyed by the thumb position alone.
 *
 * @param onDescription/[offDescription] localized state announcements.
 */
@Composable
fun PolishSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    kind: PolishSurfaceKind = LocalPolishSurfaceKind.current,
    onDescription: String = "On",
    offDescription: String = "Off",
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = if (checked) onDescription else offDescription
    PolishControlRow(
        title = title,
        modifier = modifier.semantics { this.stateDescription = state },
        supportingText = supportingText,
        kind = kind,
    ) {
        // The callback is passed unconditionally. A null callback makes the built-in skip its whole
        // `toggleable` modifier, which takes Role.Switch, the ToggleableState and the localized
        // on/off state description out of the accessibility tree with it — so a disabled switch
        // became an unlabelled shape rather than a switch that happens to be off. `enabled` already
        // refuses the interaction and dims the control.
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            interactionSource = interactionSource,
        )
    }
}

/**
 * A titled slider row built on the built-in `Slider`, with the current value shown as text.
 *
 * Showing the value is the point: a slider whose position is its only feedback is unreadable at
 * arm's length in a headset, and unusable with reduced vision. [valueLabel] formats it.
 */
@Composable
fun PolishSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    supportingText: String? = null,
    kind: PolishSurfaceKind = LocalPolishSurfaceKind.current,
    valueLabel: (Float) -> String = { formatPercent(it, valueRange) },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val label = valueLabel(value)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall),
    ) {
        PolishControlRow(title = title, supportingText = supportingText, kind = kind) {
            Text(text = label, style = PolishTypography.control)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().semantics { this.stateDescription = label },
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            interactionSource = interactionSource,
        )
    }
}

/** One option in a [PolishSegmented] group. */
data class PolishSegment<T>(val value: T, val label: String)

/**
 * A compact exclusive choice built on the built-in `SegmentControl` / `SegmentItem`.
 *
 * Use this for two to four peer options that fit on one line — a view mode, a density, a quality
 * setting. Beyond that, a list of `PolishSurface(onClick = ...)` rows reads better at headset
 * distance than a crowded segment bar.
 */
@Composable
fun <T> PolishSegmented(
    options: List<PolishSegment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentControl(modifier = modifier) {
        options.forEach { option ->
            val isSelected = option.value == selected
            SegmentItem(
                selected = isSelected,
                onClick = { onSelect(option.value) },
                modifier = Modifier.widthIn(min = 96.dp),
                title = { Text(text = option.label) },
            )
        }
    }
}

/**
 * A short read-only value line, for a details pane: a dimmed label above the value in the surface's
 * own foreground.
 */
@Composable
fun PolishFactRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    kind: PolishSurfaceKind = LocalPolishSurfaceKind.current,
) {
    val options = LocalPolishOptions.current
    val labelColor = PolishSurfaceDefaults.secondaryForegroundFor(kind, options)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceTiny),
    ) {
        Text(text = label, style = PolishTypography.overline, color = labelColor)
        // The value inherits the enclosing surface's foreground. Re-providing one here would let a
        // row placed inside an Emphasis surface paint the dark label over a dark fill.
        Text(text = value, style = PolishTypography.objectTitle)
    }
}

/** Percentage formatting without a locale dependency, for the default slider label. */
internal fun formatPercent(value: Float, range: ClosedFloatingPointRange<Float>): String {
    val span = range.endInclusive - range.start
    val fraction = if (span <= 0f) 0f else (value - range.start) / span
    val percent = (fraction.coerceIn(0f, 1f) * 100f).toInt()
    return "$percent%"
}

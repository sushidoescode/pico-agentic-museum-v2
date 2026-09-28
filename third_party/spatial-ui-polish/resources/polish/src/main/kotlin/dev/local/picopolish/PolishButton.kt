package dev.local.picopolish

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonColors
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.ButtonSize
import com.pico.spatial.ui.design.CircularProgressIndicator
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.IconButtonDefaults
import com.pico.spatial.ui.design.LocalContentColor
import androidx.compose.ui.graphics.Color
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.ToggleIconButton
import com.pico.spatial.ui.design.ToggleIconButtonDefaults

/**
 * What a button means, expressed through the design system's colour roles.
 *
 * The built-in `Button` has no public role enum; semantic weight is carried by the container and
 * content colours, so the kit names the four combinations an app actually needs instead of letting
 * every call site pick a pair.
 */
enum class PolishButtonTone {
    /** The default filled action: `fillPrimary` with the light label. */
    Standard,

    /** The one action the screen wants first: the accent (`interaction`) role. */
    Emphasis,

    /** A secondary action that should not compete: a quiet fill with the dark label. */
    Quiet,

    /** A destructive action. Gate it behind a confirmation surface. */
    Destructive,
}

/**
 * A text (optionally iconed) button that wraps the built-in `Button` and adds the two states apps
 * keep re-implementing: a semantic tone and a loading state.
 *
 * It is a wrapper, not a re-implementation. Shape, minimum size, typography, disabled alpha, hover,
 * the native press indication, click audio, controller haptics and the `Button` accessibility role
 * all come from the built-in — which already applies `spatialHoverEffect` internally, so nothing
 * here adds hover on top of it.
 *
 * Loading is a real state, not a caption swap: the click is suppressed for the duration, and the
 * button reports [loadingDescription] to accessibility so the state is not visual-only.
 *
 * @param loading suppresses activation and draws a progress indicator over the label, which stays
 *   laid out and invisible so the button keeps its width and its neighbours do not move.
 * @param loadingDescription the state announced while [loading]; supply a localized string.
 */
@Composable
fun PolishButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    tone: PolishButtonTone = PolishButtonTone.Standard,
    size: ButtonSize = ButtonDefaults.Regular,
    loadingDescription: String = "Loading",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val colors = PolishButtonDefaults.colorsFor(tone)
    val loadingState = loadingDescription
    val stateModifier =
        if (loading) modifier.semantics { this.stateDescription = loadingState } else modifier

    Button(
        // `enabled = false` is the SDK's DISABLED PRESENTATION, not just click suppression:
        // BasicButton multiplies the whole node by LocalDisableAlpha inside its graphicsLayer, and
        // the indicator is drawn inside that layer. Routing `loading` through it rendered a busy
        // button — spinner included — at 30 % alpha, indistinguishable from a disabled one.
        // Suppress the activation instead and leave the presentation alone.
        onClick = { if (!loading) onClick() },
        modifier = stateModifier,
        enabled = enabled,
        size = size,
        colors = colors,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
    ) {
        if (loading) {
            // The label stays laid out and invisible so the button keeps its width. Host-rendered
            // frame 09 caught the alternative: swapping the content for the indicator collapsed the
            // button to the spinner's size and made every button beside it jump.
            Box(contentAlignment = Alignment.Center) {
                Row(modifier = Modifier.alpha(0f)) { content() }
                CircularProgressIndicator(
                    modifier = Modifier.size(PolishButtonDefaults.LoadingIndicatorSize),
                    color = PolishButtonDefaults.loadingIndicatorColor(tone),
                )
            }
        } else {
            content()
        }
    }
}

/**
 * An icon-only button for a **stateless** action, wrapping the built-in `IconButton`.
 *
 * It deliberately has no `selected` parameter. An earlier version had one that only swapped the
 * container colour: the state was then carried by hue alone, reported nothing to a screen reader,
 * and silently discarded whatever `tone` the caller passed — the kit breaking its own rule in the
 * one wrapper where breaking it is easiest. A control that has an on state is a toggle, so it uses
 * [PolishToggleIconButton], which delegates to the design system's own `ToggleIconButton`.
 *
 * @param contentDescription what the icon means; an icon-only control is unusable without it.
 */
@Composable
fun PolishIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: PolishButtonTone = PolishButtonTone.Quiet,
    size: ButtonSize = IconButtonDefaults.Regular,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    // Captured: inside `semantics { }` the bare name would resolve to the receiver's write-only
    // property rather than to this parameter.
    val description = contentDescription
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = description },
        colors = PolishButtonDefaults.iconColorsFor(tone),
        size = size,
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * An icon-only control with an on state, wrapping the design system's `ToggleIconButton`.
 *
 * The built-in reports `toggleableState` to accessibility, so the state is never colour-only. The
 * caller still owes a non-colour visual cue — swap the icon, or place an indicator beside it — the
 * way the showcase's library list pairs its selection edge with a check glyph.
 *
 * @param contentDescription what the control does; it does not change with the state, because the
 *   state is already reported through `toggleableState`.
 */
@Composable
fun PolishToggleIconButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ToggleIconButtonDefaults.Regular,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val description = contentDescription
    ToggleIconButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { this.contentDescription = description },
        colors =
            ToggleIconButtonDefaults.toggleIconButtonColors(
                checkedContainerColor = PicoTheme.colorScheme.interaction,
                checkedContentColor = PicoTheme.colorScheme.labelPrimaryLight,
                uncheckedContainerColor = PolishButtonDefaults.quietContainerColor(),
                uncheckedContentColor = PolishButtonDefaults.quietContentColor(),
            ),
        size = size,
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )
}

/** Colour pairings for [PolishButton] and [PolishIconButton]. */
object PolishButtonDefaults {

    /**
     * The container/content pair for a tone.
     *
     * `Standard` keeps the built-in default (`fillPrimary` + `labelPrimaryLight`). `Emphasis` uses
     * the accent role. `Quiet` sits on `fillTertiary` with the dark label so it recedes against a
     * glass panel. `Destructive` uses the semantic `error` role.
     */
    @Composable
    fun colorsFor(tone: PolishButtonTone): ButtonColors =
        when (tone) {
            PolishButtonTone.Standard -> ButtonDefaults.buttonColors()
            PolishButtonTone.Emphasis ->
                ButtonDefaults.buttonColors(
                    containerColor = PicoTheme.colorScheme.interaction,
                    contentColor = PicoTheme.colorScheme.labelPrimaryLight,
                )
            PolishButtonTone.Quiet ->
                ButtonDefaults.buttonColors(
                    containerColor = quietContainerColor(),
                    contentColor = quietContentColor(),
                )
            PolishButtonTone.Destructive ->
                ButtonDefaults.buttonColors(
                    containerColor = PicoTheme.colorScheme.error,
                    contentColor = PicoTheme.colorScheme.labelPrimaryLight,
                )
        }

    /** As [colorsFor], for the icon-button defaults. */
    @Composable
    fun iconColorsFor(tone: PolishButtonTone): ButtonColors =
        when (tone) {
            PolishButtonTone.Standard -> IconButtonDefaults.iconButtonColors()
            PolishButtonTone.Emphasis ->
                IconButtonDefaults.iconButtonColors(
                    containerColor = PicoTheme.colorScheme.interaction,
                    contentColor = PicoTheme.colorScheme.labelPrimaryLight,
                )
            PolishButtonTone.Quiet ->
                IconButtonDefaults.iconButtonColors(
                    containerColor = quietContainerColor(),
                    contentColor = quietContentColor(),
                )
            PolishButtonTone.Destructive ->
                IconButtonDefaults.iconButtonColors(
                    containerColor = PicoTheme.colorScheme.error,
                    contentColor = PicoTheme.colorScheme.labelPrimaryLight,
                )
        }

    /**
     * The quiet tone reads against the surface it sits on, instead of assuming a light one.
     *
     * Host-rendered frame 04 caught the failure: a fixed dark label on a 4%-black fill, placed on
     * the dark opaque surface that reduced transparency uses, was black on near-black. So the quiet
     * tone now takes the ambient content colour the surface already provided, and a container that
     * contrasts with it.
     */
    @Composable
    internal fun quietContentColor(): Color =
        LocalContentColor.current.takeOrElse { PicoTheme.colorScheme.labelPrimary }

    @Composable
    internal fun quietContainerColor(): Color =
        if (quietContentColor() == PicoTheme.colorScheme.labelPrimaryLight) {
            PicoTheme.colorScheme.fillSecondary
        } else {
            PicoTheme.colorScheme.fillTertiary
        }

    /** The indicator drawn over a loading button's hidden label, sized to the label line. */
    internal val LoadingIndicatorSize = 20.dp

    /** The progress-indicator colour that matches the content colour of a loading button. */
    @Composable
    internal fun loadingIndicatorColor(tone: PolishButtonTone) =
        when (tone) {
            PolishButtonTone.Quiet -> quietContentColor()
            else -> PicoTheme.colorScheme.labelPrimaryLight
        }
}

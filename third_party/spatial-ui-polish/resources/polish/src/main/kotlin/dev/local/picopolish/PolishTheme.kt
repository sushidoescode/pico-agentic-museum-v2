package dev.local.picopolish

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalContext
import com.pico.spatial.ui.design.ColorScheme
import com.pico.spatial.ui.design.LocalContentColor
import com.pico.spatial.ui.design.LocalTextStyle
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.systemColorScheme
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.platform.Material

/**
 * Wraps `PicoTheme` and adds the two things an app needs on top of it: a content colour for
 * ordinary layout surfaces, and the polish options every kit component reads.
 *
 * Why the foreground matters: `PicoTheme` provides the colour scheme, the typography, the
 * indication and the design tokens, but it does not provide `LocalContentColor` — whose default is
 * `Color.Unspecified`. `Text` resolves explicit colour, then style colour, then that local, and
 * neither the default text style nor any typography role carries a colour. So a plain
 * `Text("Hello")` on an app-drawn surface inside `PicoTheme` alone renders the framework default
 * (black), which is how dark-on-dark panels happen. The built-ins are fine because each provides
 * its own content colour internally; everything else needs this wrapper.
 *
 * The accent is the one configurable colour. `ColorScheme` has no `accent` role — the accent-like
 * semantic role is `interaction`, which is what a `Switch` track, a selection edge and an emphasis
 * button use. Passing [accent] copies the resolved system scheme with that role replaced; the
 * foreground that pairs with it stays `labelPrimaryLight`.
 *
 * @param options the requested quality and accessibility policy.
 * @param capabilities what the host can render; [PolishOptions.resolve] applies it once, here.
 * @param windowSurface who owns this container's background. It seeds the glass budget described in
 *   [PolishTokens.MaxGlassDepth], so a theme that misdeclares it will let one material too many
 *   stack, or one too few. The kit cannot detect it: a container's material is resolved at
 *   declaration time and there is no runtime read-back.
 * @param accent replaces `ColorScheme.interaction`; `Color.Unspecified` keeps the system accent.
 * @param colorScheme an explicit scheme, for a product that owns more than the accent. It defaults
 *   to the **ambient** scheme rather than to a freshly resolved one, so a nested `PolishTheme` — a
 *   comparison column, a preview, a dialog that re-themes part of a screen — keeps the accent the
 *   app already applied instead of silently reverting to the system default. Outside any theme the
 *   ambient value is the same adaptive scheme `systemColorScheme` returns. When set, [accent] still
 *   applies on top of it.
 */
@Composable
fun PolishTheme(
    options: PolishOptions = PolishOptions(),
    capabilities: PolishCapabilities = PolishCapabilities(),
    windowSurface: PolishWindowSurface = PolishWindowSurface.SystemGlass,
    accent: Color = Color.Unspecified,
    colorScheme: ColorScheme = PicoTheme.colorScheme,
    content: @Composable () -> Unit,
) {
    val scheme = if (accent.isSpecified) colorScheme.copy(interaction = accent) else colorScheme
    val resolved = options.resolve(capabilities)
    // Reduced transparency takes every app surface opaque, so the app spends no material whatever
    // the window does. The window's own panel is a system decision and is not changed by the
    // setting — that is why `polishWindowRoot` only goes opaque on a container the app already owns.
    val baseDepth = if (resolved.reduceTransparency) 0 else windowSurface.baseGlassDepth

    PicoTheme(colorScheme = scheme) {
        // Inside PicoTheme's content, so PicoTheme.colorScheme / .typography resolve to the scheme
        // and typography this call just provided.
        CompositionLocalProvider(
            LocalPolishOptions provides resolved,
            LocalPolishCapabilities provides capabilities,
            LocalPolishWindowSurface provides windowSurface,
            LocalPolishGlassDepth provides baseDepth,
            LocalContentColor provides PicoTheme.colorScheme.labelPrimary,
            LocalTextStyle provides PolishTypography.body(),
            content = content,
        )
    }
}

/**
 * The background for a window container root that the **app** owns.
 *
 * Use this only on a container whose system material was switched off at declaration time — the
 * launcher `DefaultWindowContainer` through its `<activity>` meta-data
 * `pico.spatial.windowcontainer.materialbackground="0"`, or a DSL-declared `WindowContainer` /
 * `Augment` through `enableMaterialBackground = false`. There is no runtime switch: the container's
 * material is resolved when the container is declared.
 *
 * On a container whose system glass is still on, do not call this and do not paint anything else on
 * the root. Painting over live system glass hides the material and collapses vibrant linkage, and
 * calling this one would stack a second material on the first.
 *
 * With `reduceTransparency` on it paints [PolishTokens.OpaqueGround]; otherwise it re-applies
 * `Material.Regular`, the same tier the system uses for a window panel. `Material.Thin` is
 * deliberately not offered here: as a window material it maps to none.
 *
 * It used to paint `ColorScheme.fillPrimary` here, and that was the same defect this phase found in
 * `PolishSurface` — missed on this path because an app-owned root is the rarer case. `fillPrimary` is
 * a vibrancy sentinel the compositor resolves by darkening what is behind the surface, so a window
 * root painted with it still shows the room through it, which is the one thing the setting exists to
 * stop. See [PolishTokens.OpaqueGround] for the frames.
 */
@Composable
fun Modifier.polishWindowRoot(): Modifier {
    val options = LocalPolishOptions.current
    return if (options.reduceTransparency) {
        // design-style: opaque-root — the user asked for reduced transparency, and this container's
        // system material is off (see the KDoc precondition), so the app owns the root fill.
        this.background(PolishTokens.OpaqueGround)
    } else {
        this.backgroundMaterial(enable = true, style = Material.Regular)
    }
}

/**
 * The content colour that pairs with [Modifier.polishWindowRoot] on the same container, for a
 * screen that puts text directly on the window root instead of inside a `PolishSurface`.
 */
@Composable
fun polishWindowRootContentColor(): Color {
    val options = LocalPolishOptions.current
    return if (options.reduceTransparency) {
        PicoTheme.colorScheme.labelPrimaryLight
    } else {
        PicoTheme.colorScheme.labelPrimary
    }
}

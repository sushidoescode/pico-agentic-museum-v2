package dev.local.picopolish

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.tokens.MotionTokens
import com.pico.spatial.ui.design.tokens.SpringEasingArgs
import com.pico.spatial.ui.design.tokens.toSpring

/**
 * Product-level layout, shape, depth and motion defaults for the polish kit.
 *
 * Colour and typography are deliberately absent: colour comes from `PicoTheme.colorScheme` so the
 * kit inherits the system's adaptive roles instead of freezing a palette, and typography comes from
 * [PolishTypography], which *ranks* the SDK's 18 roles rather than redefining their sizes.
 *
 * ## Every value here is anchored, or says it is not
 *
 * The SDK's own scale lives in `design/tokens/DimensionTokens.kt`, which is
 * `@RestrictTo(RestrictTo.Scope.LIBRARY)` with an `internal companion object` — so an app **cannot
 * read it**, and a kit that wants to agree with the shell has to mirror the numbers and cite them.
 * Each token below names the SDK token it mirrors, or states plainly that it has no SDK anchor.
 *
 * One previously-stated anchor was wrong and is corrected here: the kit used to call 32 dp "the
 * system window corner radius". It is not. 32 dp is `DimensionTokens.RadiusExtraHuge`, the radius of
 * `Subwindow`, `TabBar` and `Toolbar` (`windows/Subwindow.kt:134`, `windows/TabBar.kt:759`,
 * `windows/Toolbar.kt:311`). No public API exposes a window *container*'s radius, and nothing in the
 * 6.1.9 sources states one.
 */
object PolishTokens {

    // ---- Spacing -------------------------------------------------------------------------------
    // The SDK's rhythm is 8 / 12 / 16 for gaps and 8 / 16 / 24 / 32 for padding
    // (DimensionTokens.kt:88-115). The kit uses the same six steps and adds one below them.

    /**
     * Hairline gaps: icon to label inside one control.
     *
     * **No SDK anchor.** The SDK's smallest gap is 8 dp; 4 dp is this kit's own step, for the space
     * between two lines that belong to one thought (a title and its caption).
     */
    val SpaceTiny = 4.dp

    /** Related items in a row or a list. Mirrors `DimensionTokens.GapSmall` / `PaddingSmall` (8 dp). */
    val SpaceSmall = 8.dp

    /**
     * The step the kit used to be missing. Mirrors `DimensionTokens.GapMedium` (12 dp).
     *
     * Use it between a label and its control, where 8 dp crowds and 16 dp breaks the pair apart.
     */
    val SpaceSnug = 12.dp

    /** The default gap between controls. Mirrors `DimensionTokens.GapLarge` / `PaddingRegular` (16 dp). */
    val SpaceMedium = 16.dp

    /** Between groups inside one panel; also the default card padding. Mirrors `PaddingMedium` (24 dp). */
    val SpaceLarge = 24.dp

    /**
     * Between major regions. Mirrors `DimensionTokens.PaddingLarge` (32 dp).
     *
     * It also happens to equal the `Subwindow` gap-plus-radius family, but it is anchored to the
     * padding token, not to a window radius — see the class KDoc for the correction.
     */
    val SpaceHuge = 32.dp

    // ---- Shape ---------------------------------------------------------------------------------
    // Mirrors of DimensionTokens.Radius*, each named with the built-in that uses it, so an app
    // surface lands on the same ladder as the SDK component beside it.

    /**
     * The radius the SDK's own companion windows use: `Subwindow`, `TabBar`, `Toolbar`.
     * Mirrors `DimensionTokens.RadiusExtraHuge` (32 dp).
     *
     * This is **not** a window-container radius; no API exposes that. Use it when an app surface has
     * to sit flush beside a system companion window, not to guess at the window panel.
     */
    val RadiusCompanion = 32.dp

    /** A region that fills most of a window. Mirrors `RadiusHuge` (24 dp) — `SideNavigation`'s radius. */
    val RadiusPanel = 24.dp

    /** A content card inside a panel. Mirrors `RadiusLarge` (16 dp) — `ListItem`, `Button`/`Chips` Regular. */
    val RadiusCard = 16.dp

    /** A small control surface. Mirrors `RadiusMediumLarge` (12 dp) — `Option`, `TextField`. */
    val RadiusControl = 12.dp

    /** The edge a surface draws when it needs one. One physical hairline. */
    val Hairline = 1.dp

    /** A selected or high-contrast edge: thicker, so the state is not signalled by colour alone. */
    val HairlineStrong = 2.dp

    // ---- Depth ---------------------------------------------------------------------------------

    /** How far an interactive enhanced surface lifts toward the viewer. */
    val LiftZ = 8.dp

    /**
     * How many compositor-material layers may stack in one window, **including the window's own**.
     *
     * Restraint is the design goal, and this is the number that makes it enforceable rather than
     * advisory. Two is the budget: the window panel the system draws, and at most one app surface on
     * top of it. A third would be a card inside a panel inside a window — which is what the kit
     * rendered before this rule existed, and in an opened frame
     * (`polish-fb-glass-b.png`, build `be447bda`) the three layers were not separable: panel, card
     * and window read as one flat sheet.
     *
     * [PolishSurface] enforces it by reading [LocalPolishGlassDepth]; a surface over budget takes its
     * drawn role fill instead, which also gives it an edge and a tone the layer above does not have.
     * Nothing is removed — the surface is still there, still shaped, still legible.
     */
    const val MaxGlassDepth: Int = 2

    // ---- Ground -------------------------------------------------------------------------------

    /**
     * The one **literally opaque** fill the kit owns, for the surfaces that must not show what is
     * behind them.
     *
     * ## Why a literal, when the whole kit is built on roles
     *
     * Because on a PICO device `ColorScheme.fillPrimary` is **not opaque**, and the kit said it was.
     * `fillPrimary` is `Color.Vibrant.withVibrant(Vibrant.Darker)` — a sentinel the compositor
     * resolves by *darkening what is behind the surface*, not by filling it. Four admitted frames on
     * one build, varying only an app-drawn backdrop, show it:
     *
     * | backdrop | the `Emphasis` surface renders as |
     * |---|---|
     * | the emulator room | near-black |
     * | bright (#F4F4F4) | **medium grey** |
     * | dark (#0B0B0B) | near-black |
     * | a black-and-white checkerboard | **the checkerboard, darkened and still visible through it** |
     *
     * (`design-005-legibility-room`, `-006-bright`, `-007-dark`, `-008-detailed`, build `f91e4b7e`.)
     *
     * The kit's own JVM test asserted `fillPrimary` was opaque and passed, because off the spatial
     * platform the scheme resolves to `defaultColorScheme()` where `fillPrimary` really is the
     * literal `0xFF282828`. The test was right about the fallback and wrong about the device. That is
     * the same failure mode the skill already warns about in another form: an API's declared value is
     * not its rendered result.
     *
     * So `reduceTransparency` — a setting whose entire purpose is that a surface stops showing what
     * is behind it — cannot be honoured with a vibrancy role. It needs a colour the compositor has
     * nothing to resolve. This is that colour: the literal value the SDK's own non-vibrant
     * `ColorTokens.FillPrimary` carries (`design/tokens/ColorTokens.kt:131`, where that token's literal
     * is `0xFF282828`), mirrored here because `ColorTokens` is `@RestrictTo(LIBRARY)` with an
     * `internal` companion and an app cannot read it.
     *
     * It pairs with `ColorScheme.labelPrimaryLight`, which is a literal white in both schemes.
     *
     * **What it costs:** it does not adapt to the room the way a vibrancy role does. That is the
     * trade the setting asks for. It is used *only* where non-transparency is the point — under
     * `reduceTransparency`, and for [PolishSurfaceKind.Emphasis] — never as a general fill.
     */
    // An opaque ground is only expressible as a literal on this platform; every vibrancy role is
    // compositor-resolved. The KDoc above carries the frames that established it.
    val OpaqueGround = Color(0xFF282828) // design-style: fixed-figma-color — the kit's one opaque ground

    // ---- Interactive target --------------------------------------------------------------------

    /**
     * The smallest an interactive target may be.
     *
     * Anchored to the Compose fork's `ViewConfiguration.minimumTouchTargetSize`, which is
     * `DpSize(48.dp, 48.dp)` (`ui-android:1.2.5`, `ViewConfiguration.kt:53-54`) and which
     * `clickable` uses to expand pointer bounds around a smaller layout.
     *
     * **Several shipped SDK sizes are visually smaller than this** — `ButtonDefaults.Small` (40 dp
     * tall), `ButtonDefaults.Min` and `IconButtonDefaults.Min` (32 dp), `ChipsDefaults.Small`
     * (32 dp), `CheckBoxDefaults.CheckboxSize` (32 dp). Their *hit* area is expanded to 48 dp by the
     * framework, so they are reachable; their *visible* affordance is not 48 dp.
     *
     * **"Regular" is not a synonym for 48 dp.** Only `ButtonDefaults.Regular` (`Button.kt:312`) and
     * `IconButtonDefaults.Regular` (`IconButton.kt:114`) are 48 dp tall; `ChipsDefaults.Regular` is
     * `chipSize(40.dp)` (`Chips.kt:592`) and `ToggleButtonDefaults.Regular` aliases
     * `ButtonDefaults.Min` at 32 dp (`ToggleButton.kt:113`). At panel distance prefer the two sizes
     * that really are 48 dp, and reserve the rest for dense clusters the user is already focused on.
     */
    val MinTargetSize = 48.dp

    // ---- Motion --------------------------------------------------------------------------------
    // Anchored to MotionTokens, which is the one SDK token object that is public and unrestricted
    // (design/tokens/MotionTokens.kt:31 — no `internal`, no `@RestrictTo`).

    /**
     * Immediate feedback: a press tint settling, a value catching up. `MotionTokens.durationShort1`.
     *
     * Reading the token rather than hardcoding 120 means the kit follows an OTA that retunes the
     * platform's motion. The resource lookup falls back to the documented value if resources are not
     * reachable (`ext/Resources.kt:82-88`), so this is safe off-device.
     */
    val DurationFeedbackMillis: Int get() = MotionTokens.durationShort1

    /** A state change the eye should follow. `MotionTokens.durationShort2` (180 ms). */
    val DurationStateMillis: Int get() = MotionTokens.durationShort2

    /** A panel or secondary surface appearing, expanding or dismissing. `MotionTokens.durationShort3` (240 ms). */
    val DurationPanelMillis: Int get() = MotionTokens.durationShort3

    /**
     * The platform's standard easing curve, `cubic-bezier(0.3, 0, 0.2, 1)`.
     *
     * The kit's first version animated with a bare `tween(...)`, which takes Compose's
     * `FastOutSlowInEasing` — a different curve from the one the shell and every SDK component use
     * (`Slider.kt:476`, `ScrollIndicator.kt` ×6, `TabBar.kt` ×5 all pass `bezierEasingStandard`). An
     * app whose panels ease differently from the system chrome around them reads as a port.
     *
     * If the design resources cannot be read, `bazierEasingRes` falls back to `LinearEasing`
     * (`ext/Resources.kt:131-133`) rather than to the documented curve. That is a silent downgrade,
     * not a crash.
     */
    val EasingStandard: Easing get() = MotionTokens.bezierEasingStandard

    /**
     * The kit's animation spec for [durationMillis], or an immediate change when the user asked for
     * reduced motion. Call this instead of `tween(...)` directly so one setting covers the app and
     * every animation lands on the platform curve.
     */
    fun <T> motion(
        reduceMotion: Boolean,
        durationMillis: Int = DurationStateMillis,
    ): FiniteAnimationSpec<T> =
        if (reduceMotion) {
            snap()
        } else {
            tween(durationMillis = durationMillis, easing = EasingStandard)
        }

    /** As [motion], typed for APIs that take the wider [AnimationSpec]. */
    fun <T> motionSpec(
        reduceMotion: Boolean,
        durationMillis: Int = DurationStateMillis,
    ): AnimationSpec<T> = motion(reduceMotion, durationMillis)

    /**
     * A spring from one of `MotionTokens`' spring tokens, with the token's failure mode handled.
     *
     * **Why this wrapper exists.** `springEasingArgsRes` does not fall back to the documented spring
     * when the resource lookup fails — it returns `SpringEasingArgs.Unspecific`, which is
     * `(dampingRatio = NaN, stiffness = NaN)` (`MotionTokens.kt:150`; the `catch` that returns it is `ext/Resources.kt:107-109`).
     * `toSpring()` will hand that straight to `spring(...)`, and a NaN spring does not animate; it
     * produces an undefined value for the whole run. Durations fail safe, springs do not. So the kit
     * validates before use and falls back to a real spring.
     *
     * @param args one of `MotionTokens.springEasing*`.
     * @param reduceMotion returns [snap] instead, as everywhere else in the kit.
     */
    fun <T> spring(
        reduceMotion: Boolean,
        args: SpringEasingArgs = MotionTokens.springEasingRapid,
    ): AnimationSpec<T> =
        when {
            reduceMotion -> snap()
            args.dampingRatio.isNaN() || args.stiffness.isNaN() ->
                spring(dampingRatio = FallbackSpringDampingRatio, stiffness = FallbackSpringStiffness)
            else -> args.toSpring()
        }

    /** `springEasingRapid`'s documented damping ratio, used when the token reads back as NaN. */
    private const val FallbackSpringDampingRatio = 1.0f

    /** `springEasingRapid`'s documented stiffness, used when the token reads back as NaN. */
    private const val FallbackSpringStiffness = 850f

    internal fun shapeForRadius(radius: Dp): Shape = RoundedCornerShape(radius)
}

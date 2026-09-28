package dev.local.picopolish

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.pico.spatial.ui.foundation.vibrant.obtainVibrant
import com.pico.spatial.ui.foundation.vibrant.withVibrant
import com.pico.spatial.ui.design.LocalContentColor
import com.pico.spatial.ui.design.LocalDisableAlpha
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.haptic.controllerHapticFeedback
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.layout.offset
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.platform.Material

/**
 * What a surface is for. The kind picks the shape, the padding, the material tier and the edge —
 * so a screen expresses hierarchy by naming roles, not by hand-tuning four values per box.
 */
enum class PolishSurfaceKind {
    /** A region that fills most of a window: a list column, a detail pane. */
    Panel,

    /** A content card inside a panel. The default. */
    Card,

    /** A small control shell: a field well, a chip group, a toolbar cluster. */
    Control,

    /**
     * A surface that must read as the primary object on screen. **Literally** opaque, light
     * foreground.
     *
     * "Literally" is load-bearing. It takes [PolishTokens.OpaqueGround], not `ColorScheme.fillPrimary`
     * — that role is a vibrancy sentinel the compositor resolves by darkening what is behind the
     * surface, so it shows a busy background through itself. See that token for the four frames.
     */
    Emphasis,
}

/**
 * The kind of the nearest enclosing [PolishSurface].
 *
 * Every control in the kit derives its foreground from a surface kind, and a child that guesses
 * `Card` while sitting inside an `Emphasis` surface produces exactly the dark-on-dark text the kit
 * exists to prevent. Publishing the kind means the child never has to be told twice.
 */
val LocalPolishSurfaceKind = compositionLocalOf { PolishSurfaceKind.Card }

/** How a surface is filled. Exactly one of these is applied — never a material and a fill. */
@Immutable
internal sealed interface PolishFill {
    @Immutable data class Glass(val style: Material) : PolishFill

    @Immutable data class Solid(val color: Color) : PolishFill
}

/**
 * A shaped inner surface with explicit, single-layer material ownership.
 *
 * This is the kit's one answer to "a card with glass on it". It is NOT for a window container root:
 * the system owns that material, and the app's root background is
 * [Modifier.polishWindowRoot] under the precondition documented there.
 *
 * What it guarantees:
 *
 *  - **One surface per element.** Either the compositor material or a solid role fill, never both,
 *    never a solid over a material. A composable placed inside must not paint its own background —
 *    which is why `ListItem` (it fills itself) belongs beside a `PolishSurface`, not inside one.
 *  - **Clip and border share one shape**, so the visible edge and the fill cannot disagree. The
 *    *hit area* is not shaped: `clickable` takes no shape and the touch target is the node's
 *    rectangle, so a heavily rounded surface is clickable slightly outside its visible corners. The
 *    kit does not correct this — the framework's own components behave the same way — but it should
 *    not be described as a three-way agreement, which an earlier version of this list did.
 *  - **The foreground is provided**, matched to the fill that was actually chosen, so text and
 *    icons stay legible when `reduceTransparency` swaps glass for an opaque role.
 *  - **Hover, press indication and haptics come from the platform**, applied once, and only when
 *    the surface is actually clickable.
 *
 * @param onClick makes the surface a control: hover, native press indication, haptics and a
 *   `Button` role. Leave it null for a static surface — a static surface must not hover.
 * @param selected renders the selection edge and reports `selected` to accessibility. Selection is
 *   also signalled by the thicker edge, so it is not carried by colour alone; a caller that shows
 *   selection in a list should still place a check or another non-colour cue in its content.
 * @param errorMessage non-null puts the surface in its error state: the edge moves to the `error`
 *   role at the thicker weight, and the message is reported through the semantics `error` property
 *   so the state is not carried by colour alone. The caller still owns rendering the message as
 *   text — a surface that only turns red says nothing to someone who cannot see the red.
 *
 *   The SDK's own error presentation is `TextField`-family only and its `errorBorder` is `internal`
 *   (`TextField.kt:940`), so this is the kit re-implementing a treatment the design system does
 *   not expose, deliberately and in the public API only.
 */
@Composable
fun PolishSurface(
    modifier: Modifier = Modifier,
    kind: PolishSurfaceKind = PolishSurfaceKind.Card,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    errorMessage: String? = null,
    shape: Shape = PolishSurfaceDefaults.shapeFor(kind),
    contentPadding: PaddingValues = PolishSurfaceDefaults.paddingFor(kind),
    verticalArrangement: Arrangement.Vertical = PolishSurfaceDefaults.arrangementFor(kind),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val options = LocalPolishOptions.current
    val disableAlpha = LocalDisableAlpha.current
    val glassDepth = LocalPolishGlassDepth.current
    val interactive = onClick != null
    // Observed, never requested. The kit does not add `Modifier.focusable` — that would put every
    // surface into a traversal order the app did not ask for. It reads the focus the app's own
    // focus handling produces, so a surface that is never focused simply never shows the ring.
    val focused by interactionSource.collectIsFocusedAsState()
    val fill = PolishSurfaceDefaults.fillFor(kind, options, interactive, glassDepth)
    val tookGlass = fill is PolishFill.Glass
    val foreground = PolishSurfaceDefaults.foregroundFor(kind, options)
    val edge =
        PolishSurfaceDefaults.edgeFor(
            kind = kind,
            options = options,
            selected = selected,
            tookGlass = tookGlass,
            interactive = interactive,
            isError = errorMessage != null,
            focused = focused,
        )
    val lift = interactive && options.quality == PolishQuality.Enhanced

    // Order mirrors the SDK's own BasicButton: caller layout, then clip+alpha, then hover, then the
    // single fill, then the edge, then semantics, then click, then haptics, then inner padding.
    var decorated = modifier
    if (lift) {
        decorated = decorated.offset(z = PolishTokens.LiftZ)
    }
    decorated =
        decorated.graphicsLayer {
            this.clip = true
            this.shape = shape
            this.alpha = if (enabled) 1f else disableAlpha
        }
    if (interactive) {
        decorated = decorated.spatialHoverEffect(enabled = enabled)
    }
    decorated =
        when (fill) {
            is PolishFill.Glass -> decorated.backgroundMaterial(enable = true, style = fill.style)
            is PolishFill.Solid -> decorated.background(fill.color)
        }
    if (edge != null) {
        decorated = decorated.border(width = edge.width, color = edge.color, shape = shape)
    }
    if (interactive || selected || errorMessage != null) {
        // Captured outside the lambda: inside `semantics { }` the bare names `selected`, `role` and
        // `error` resolve to the receiver's write-only properties, whose getters throw.
        val isInteractive = interactive
        val isSelected = selected
        val errorText = errorMessage
        decorated =
            decorated.semantics {
                if (isInteractive) this.role = Role.Button
                if (isSelected) this.selected = true
                if (errorText != null) this.error(errorText)
            }
    }
    if (onClick != null) {
        decorated =
            decorated
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                )
                .controllerHapticFeedback(interactionSource = interactionSource)
    }

    Column(
        modifier = decorated.padding(contentPadding),
        verticalArrangement = verticalArrangement,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides foreground,
            LocalPolishSurfaceKind provides kind,
            // Only a material spends the budget. A drawn surface stacks freely: it is one opaque or
            // translucent paint, not another compositor layer for the system to resolve.
            LocalPolishGlassDepth provides if (tookGlass) glassDepth + 1 else glassDepth,
            content = { content() },
        )
    }
}

/** The edge a surface draws, if any. */
@Immutable
internal data class PolishEdge(val width: Dp, val color: Color)

/** Shape, padding, fill, foreground and edge decisions for [PolishSurface]. */
object PolishSurfaceDefaults {

    fun shapeFor(kind: PolishSurfaceKind): Shape =
        when (kind) {
            PolishSurfaceKind.Panel -> RoundedCornerShape(PolishTokens.RadiusPanel)
            PolishSurfaceKind.Card -> RoundedCornerShape(PolishTokens.RadiusCard)
            PolishSurfaceKind.Control -> RoundedCornerShape(PolishTokens.RadiusControl)
            PolishSurfaceKind.Emphasis -> RoundedCornerShape(PolishTokens.RadiusCard)
        }

    /**
     * The gap between a surface's children.
     *
     * A surface that stacks its content flush is the defect host-rendered frame 02 caught: every
     * line touched the next one and the card read as a paragraph rather than as a group of
     * controls. A control surface is tighter than a card because its children are one cluster.
     */
    fun arrangementFor(kind: PolishSurfaceKind): Arrangement.Vertical =
        when (kind) {
            PolishSurfaceKind.Control -> Arrangement.spacedBy(PolishTokens.SpaceSmall)
            else -> Arrangement.spacedBy(PolishTokens.SpaceMedium)
        }

    fun paddingFor(kind: PolishSurfaceKind): PaddingValues =
        when (kind) {
            PolishSurfaceKind.Panel -> PaddingValues(PolishTokens.SpaceLarge)
            PolishSurfaceKind.Card -> PaddingValues(PolishTokens.SpaceLarge)
            PolishSurfaceKind.Control -> PaddingValues(PolishTokens.SpaceMedium)
            PolishSurfaceKind.Emphasis -> PaddingValues(PolishTokens.SpaceLarge)
        }

    /**
     * The one fill a surface gets.
     *
     * With `reduceTransparency` every kind takes [PolishTokens.OpaqueGround], the kit's one literal
     * opaque colour — **not** `fillPrimary`, which this phase measured and found is not opaque on a
     * device. No role is used to mean "opaque"; hierarchy moves to the edge and the spacing instead,
     * which is why [edgeFor] always draws one then.
     *
     * Otherwise a Panel or a Card takes the base window-grade material, a Control takes the
     * element-grade material in **both** quality modes (it used to draw a quiet role fill under
     * [PolishQuality.Native]; see below for the frame that ended that), and an Emphasis surface takes
     * the literal [PolishTokens.OpaqueGround].
     *
     * **An interactive surface never takes a material**, whatever its kind. That is not a taste
     * call; it is what the emulator measured and what the SDK's own sources say:
     *  - Hovering a material-filled, clickable surface at fixed coordinates produced a capture
     *    byte-identical to the frame before it — zero change anywhere in 1962x2018 px. The same
     *    surface, same coordinates, same build, with an app-drawn fill moved +16.48 mean gray while
     *    three sibling controls in the same frame pair moved 0.00
     *    (`hover-photometry-002-opaque-surface.json`, `-003-material-surface-control.json`).
     *  - `PicoIndication` draws only the press colour on the spatial platform and says why:
     *    "hover layer is rendered by spatial hover effect modifier" — so nothing in-process can
     *    stand in for it.
     *  - The SDK has a material-aware hover path, `Modifier.materialSpatialHoverEffect`, but it is
     *    `internal` and `windows/TabBar.kt` is its only caller, so an app cannot reach it.
     *  - Every interactive built-in fills with a drawn role colour instead: `ListItem` takes
     *    `fillLight`, `Option` takes `fillSecondary`/`fillTertiary`, `BasicButton` takes
     *    `fillPrimary`. `windows/Toolbar.kt` is the only design file that fills with a material.
     *
     * So an interactive Panel or Card takes `fillLight` — `ListItem`'s role — and an interactive
     * Control takes the `fillTertiary` it already uses on the native path. The surface still looks
     * like glass sitting on the window material; it simply draws its own fill so the compositor has
     * something to light up. Measured on the emulator; not yet re-measured on a headset.
     *
     * **The second reason a surface drops its material is the glass budget.** A surface at or over
     * [PolishTokens.MaxGlassDepth] takes the same drawn role fill, whether or not it is clickable.
     * That is the restraint rule made enforceable: see that constant for the frame that motivated it.
     *
     * @param glassDepth how many material layers are already stacked above this one, counting the
     *   window's own. Defaults to the ambient [LocalPolishGlassDepth], which `PolishTheme` seeds from
     *   [PolishWindowSurface]; pass it explicitly only from a test or a preview.
     */
    @Composable
    internal fun fillFor(
        kind: PolishSurfaceKind,
        options: PolishOptions,
        interactive: Boolean = false,
        glassDepth: Int = LocalPolishGlassDepth.current,
    ): PolishFill {
        if (options.reduceTransparency) {
            return PolishFill.Solid(PolishTokens.OpaqueGround)
        }
        val fill =
            when (kind) {
                PolishSurfaceKind.Panel -> PolishFill.Glass(Material.Regular)
                PolishSurfaceKind.Card -> PolishFill.Glass(Material.Regular)
                // Thin is the element tier in the design system: "Box, Button, CheckBox". It is the
                // Control kind's fill in BOTH quality modes now, not only the enhanced one. The
                // native mode used to draw `fillTertiary` instead — a 3.9 %-black wash — and an
                // admitted frame showed what that costs: over a dark backdrop
                // (`design-007-legibility-dark`, build `f91e4b7e`) the Control surface's title, body
                // and supporting line were black on near-black, and over a checkerboard
                // (`design-008-legibility-detailed`) the pattern read straight through the text.
                // A 3.9 % fill is not a surface; it is a tint, and a tint cannot promise a ground.
                PolishSurfaceKind.Control -> PolishFill.Glass(Material.Thin)
                PolishSurfaceKind.Emphasis -> PolishFill.Solid(PolishTokens.OpaqueGround)
            }
        if (fill !is PolishFill.Glass) return fill
        if (interactive) return PolishFill.Solid(hoverableFillFor(kind))
        if (glassDepth >= PolishTokens.MaxGlassDepth) return PolishFill.Solid(nestedFillFor(kind))
        return fill
    }

    /**
     * The drawn role a surface falls back to when the glass budget is spent.
     *
     * It is not the same role as [hoverableFillFor]. A clickable surface needs a fill the compositor
     * can *lighten* on hover, so it takes the role the closest interactive built-in uses. A nested
     * static surface needs the opposite: to sit quietly inside its parent and be told apart from it
     * by tone rather than by competing with it. `fillTertiary` is the scheme's "least important
     * elements" role (`ColorScheme.kt:53`) and is the quietest translucent fill available.
     */
    @Composable
    private fun nestedFillFor(kind: PolishSurfaceKind): Color =
        when (kind) {
            PolishSurfaceKind.Emphasis -> PolishTokens.OpaqueGround
            else -> PicoTheme.colorScheme.fillTertiary
        }

    /**
     * The drawn role a clickable surface falls back to when its kind would otherwise take a
     * material. Each one is the role the closest SDK built-in uses for the same job.
     */
    @Composable
    private fun hoverableFillFor(kind: PolishSurfaceKind): Color =
        when (kind) {
            // ListItem.kt:293 — the built-in list row's background role.
            PolishSurfaceKind.Panel, PolishSurfaceKind.Card -> PicoTheme.colorScheme.fillLight
            // The role this kind already takes on the native path.
            PolishSurfaceKind.Control -> PicoTheme.colorScheme.fillTertiary
            // Unreachable today — `fillFor` returns an Emphasis surface's solid fill before it gets
            // here — but it is written correctly anyway: `fillPrimary` is the role this phase
            // measured and found is not opaque, so no arm of this kit may name it for a ground.
            PolishSurfaceKind.Emphasis -> PolishTokens.OpaqueGround
        }

    /** The content colour that pairs with the fill [fillFor] chose for the same inputs. */
    @Composable
    fun foregroundFor(kind: PolishSurfaceKind, options: PolishOptions): Color =
        if (usesLightForeground(kind, options)) {
            // fillPrimary is the dark opaque role; its documented foreground is the light label.
            PicoTheme.colorScheme.labelPrimaryLight
        } else {
            PicoTheme.colorScheme.labelPrimary
        }

    /**
     * The supporting-text colour that pairs with [foregroundFor] without dropping below it.
     *
     * **It is `labelSecondary`, not `labelTertiary`, and that is a measurement, not a preference.**
     *
     * The kit used `labelTertiary` here — the scheme's "tertiary content color", `Vibrant.Semidark`.
     * Measured from an admitted emulator frame (`design-009-fix-detailed`, build `37452262`), through
     * uniform probe swatches rendered by the showcase and sampled with the Display-P3 luminance
     * coefficients the capture's own `cICP` chunk declares:
     *
     * | surface | supporting text on `labelTertiary` | target | verdict |
     * |---|---:|---:|---|
     * | Panel | **3.18 : 1** | 4.5 | fail |
     * | Card | **3.26 : 1** | 4.5 | fail |
     * | Control | **3.49 : 1** | 4.5 | fail |
     *
     * All three clear 3:1, so the same colour would be fine on a large-text rung or as the boundary
     * of a control. It is neither: the kit sets supporting text at [PolishTypography.caption], 12 sp
     * W500, which is normal text, and normal text is held to 4.5:1. A role that reads as a polite
     * grey on a mockup is a failure on a rendered frame, and only the frame could say so — the same
     * three pairings compute fine from the non-vibrant fallback values on the JVM.
     *
     * So the default steps up one rung to `labelSecondary` (`Vibrant.UltraDark`), and
     * `increaseContrast` steps up again to `labelPrimary`, which is the same colour the surface's own
     * title uses — at that point the distinction is carried by size and weight alone, which is what
     * the contrast setting is for.
     */
    @Composable
    fun secondaryForegroundFor(kind: PolishSurfaceKind, options: PolishOptions): Color =
        when {
            // A light-foreground surface has no secondary *role* to step down to, so the kit
            // derives one. See [lightSecondaryForeground]; `increaseContrast` steps back up to the
            // full-strength label, exactly as the dark-foreground branch steps up to `labelPrimary`.
            usesLightForeground(kind, options) ->
                if (options.increaseContrast) {
                    PicoTheme.colorScheme.labelPrimaryLight
                } else {
                    lightSecondaryForeground(PicoTheme.colorScheme.labelPrimaryLight)
                }
            options.increaseContrast -> PicoTheme.colorScheme.labelPrimary
            else -> PicoTheme.colorScheme.labelSecondary
        }

    /**
     * How far the derived light-secondary tone steps down from the light primary, as a fraction of
     * the distance from the ground to the label.
     *
     * `0.70` is chosen, not tuned: it puts the composed tone at 190/255 over the kit's opaque
     * ground, which is a **21 L\*** step below white — comparable to the 17 L\* step that
     * `labelSecondary` gives on a dark-foreground surface — while measuring about **8:1** against
     * that ground, far above the 4.5:1 that normal text is held to. The margin is deliberate: it has
     * to survive the Display-P3 capture primaries and any future ground that is a little lighter.
     */
    const val LightSecondaryStep: Float = 0.70f

    /**
     * The supporting-text tone for a surface whose foreground is the **light** label.
     *
     * ## Why this has to be derived rather than looked up
     *
     * The SDK's sixteen-role `ColorScheme` contains exactly **one** light label role,
     * `labelPrimaryLight`. There is no `labelSecondaryLight`. So on a dark ground the kit used to
     * hand the supporting line the *same* role as the title, and the second tier of the type
     * hierarchy simply vanished — rank carried by size alone, which is what §1(b) of the design
     * contract says never to do. A frame showed it (`detail-04-surface-kinds-2x`): "supporting"
     * steps down in tone on Panel, Card and Control, and did not on Emphasis.
     *
     * **The scope was wider than Emphasis.** [usesLightForeground] is
     * `reduceTransparency || kind == Emphasis`, so with reduced transparency on, *every* surface took
     * the light foreground and *every* surface lost its second tier. The retained measurement said so
     * all along and was read as a pass: `contrast-design-015-rt-on` reports all **eight** pairings —
     * four surfaces, body and supporting alike — as an identical `[255,255,255]` on `[40,40,40]`,
     * 14.74:1. Eight passing ratios, four missing tiers.
     *
     * ## Why it is composed opaque rather than given an alpha
     *
     * Three reasons, and the third is decisive.
     *
     * 1. **The ground is always known.** [fillFor] returns `Solid(PolishTokens.OpaqueGround)` on
     *    every path where [usesLightForeground] is true — the `reduceTransparency` early return
     *    covers every kind, and `Emphasis` returns the same solid before any glass, hover or budget
     *    branch can reach it. `designSystem_lightForegroundAlwaysSitsOnTheOpaqueGround` pins that, so
     *    a future fill that breaks the assumption fails a test rather than a frame.
     * 2. **`reduceTransparency` means what it says.** A user who asks for less transparency should
     *    not be handed a translucent foreground to compensate for an opaque fill. Composing the tone
     *    leaves nothing translucent at draw time; the opacity change is scoped to the colour, and
     *    there is no `Modifier.alpha` anywhere near it.
     * 3. **An alpha would make the result unmeasurable by this kit's own contrast API.**
     *    `PolishContrast.ratio` refuses a foreground with `alpha < 1` as
     *    `PolishContrastLimit.TranslucentForeground` — correctly, because the ratio would depend on
     *    what is behind it. A composed opaque tone is measurable, and is measured in the tests.
     *
     * ## The vibrancy tag, which is the part that is easy to get wrong
     *
     * `labelPrimaryLight` is `SubWhite100.withVibrant(Vibrant.None)`, and that level lives in bits
     * 24–31 of the colour's `ULong` — the region Compose leaves **unused** for an sRGB colour, not
     * the alpha byte (alpha is bits 32–39; red/green/blue are 40–63). Two consequences:
     *
     * * Reading `.red`/`.green`/`.blue` off a tagged role is safe, because the component accessors
     *   never look at those bits. The arithmetic below is therefore done on the real channels.
     * * Building a fresh `Color(r, g, b, a)` writes **zero** into bits 24–31, so the derived colour
     *   would silently lose the tag. `Vibrant.None` is level 31 and its job is to *terminate* an
     *   inherited vibrant effect; dropping to level 0 is `Vibrant.Unspecified`, which **inherits**
     *   instead. Inside a vibrant context those are not the same thing.
     *
     * So the tag is re-applied, which is the pattern the SDK itself documents for this situation
     * ("Animate the raw color, then apply `.withVibrant()`", `ColorVibrant.kt:52-55`). Off the
     * spatial platform the role is untagged, `obtainVibrant()` returns `Vibrant.Unspecified`, and
     * re-applying it is a no-op — so the same code is correct in both schemes.
     *
     * @param primaryLight the light label role this tone steps down from.
     * @param ground the opaque fill it will be drawn on; defaults to the only ground it is ever
     *   drawn on.
     * @param step how far to step, defaulting to [LightSecondaryStep].
     */
    fun lightSecondaryForeground(
        primaryLight: Color,
        ground: Color = PolishTokens.OpaqueGround,
        step: Float = LightSecondaryStep,
    ): Color {
        val f = step.coerceIn(0f, 1f)
        val composed =
            Color(
                red = ground.red + (primaryLight.red - ground.red) * f,
                green = ground.green + (primaryLight.green - ground.green) * f,
                blue = ground.blue + (primaryLight.blue - ground.blue) * f,
                alpha = 1f,
            )
        return composed.withVibrant(primaryLight.obtainVibrant())
    }

    /**
     * When a surface draws an edge, and in what colour.
     *
     * Always when selected (in the accent role, thicker, so the state is not colour-only), always
     * under `increaseContrast` or `reduceTransparency` (an opaque stack needs the separation the
     * glass used to give it), in the enhanced mode as the restrained edge definition that mode is
     * for, and — new with the glass budget — whenever a **static** surface had to give up its
     * material. A surface that drew `fillTertiary` instead of taking a material is a 4 %-black wash
     * over its parent; without an edge it is not a surface, it is a smudge. `dividerLine` is the
     * system hairline; `labelQuaternary` is the stronger edge.
     *
     * **Precedence, highest first:** error, selected, focused, increased contrast, then the
     * separation cases. Error outranks selection because a selected row that has gone wrong needs to
     * say so; selection outranks focus because focus is transient and selection is the state the
     * user set. Each of the first three is a thicker edge in a distinct role, so two of them are
     * never confusable with each other at a glance.
     *
     * @param tookGlass whether [fillFor] actually returned a material for the same inputs. An
     *   interactive surface never takes one and does not want an extra edge for it — its feedback is
     *   the compositor hover highlight on a drawn fill — so the caller passes [interactive] too.
     * @param focused whether the surface's interaction source currently reports focus. The design
     *   system ships **no** focus-visible treatment of any kind — a grep of `design/` and
     *   `foundation/` finds only `TextField`'s focused *background* swap, window `focusable` flags
     *   that control input routing, and `LocalFocusManager.clearFocus()` — so this ring is the kit's
     *   own addition. It has not been verified on a device in this lane.
     */
    @Composable
    internal fun edgeFor(
        kind: PolishSurfaceKind,
        options: PolishOptions,
        selected: Boolean,
        tookGlass: Boolean = true,
        interactive: Boolean = false,
        isError: Boolean = false,
        focused: Boolean = false,
    ): PolishEdge? {
        if (isError) {
            return PolishEdge(PolishTokens.HairlineStrong, PicoTheme.colorScheme.error)
        }
        if (selected) {
            return PolishEdge(PolishTokens.HairlineStrong, PicoTheme.colorScheme.interaction)
        }
        if (focused) {
            // The focus ring is a graphical object, so 3:1 governs it rather than 4.5:1, and it is
            // deliberately the same accent the selection edge uses at a lighter weight: one accent
            // per screen, two weights, rather than a second hue nobody has been taught.
            return PolishEdge(PolishTokens.Hairline, PicoTheme.colorScheme.interaction)
        }
        // An edge has to contrast with the fill it outlines. Host-rendered frame 08 caught the
        // failure: on the dark opaque fill that reduced transparency uses, a dark hairline role is
        // invisible, so all four surface kinds collapsed into one flat shape.
        val onDarkFill = usesLightForeground(kind, options)
        if (options.increaseContrast) {
            val strong =
                if (onDarkFill) PicoTheme.colorScheme.fillSecondary else PicoTheme.colorScheme.labelQuaternary
            return PolishEdge(PolishTokens.HairlineStrong, strong)
        }
        val droppedItsMaterial = !tookGlass && !interactive && !options.reduceTransparency
        val needsSeparation =
            options.reduceTransparency ||
                options.quality == PolishQuality.Enhanced ||
                droppedItsMaterial
        if (!needsSeparation) return null
        if (kind == PolishSurfaceKind.Emphasis && !options.reduceTransparency) return null
        val hairline =
            if (onDarkFill) PicoTheme.colorScheme.fillSecondary else PicoTheme.colorScheme.dividerLine
        return PolishEdge(PolishTokens.Hairline, hairline)
    }

    /**
     * Whether this surface carries the light label, which is the same question as "is its fill the
     * dark opaque role" — the one bit every contrast decision on a surface turns on.
     */
    fun usesLightForeground(kind: PolishSurfaceKind, options: PolishOptions): Boolean =
        options.reduceTransparency || kind == PolishSurfaceKind.Emphasis
}

# Component catalog — signatures, state contracts and composition

Every component lives in `resources/polish/src/main/kotlin/dev/local/picopolish/`. Parameter order
follows the SpatialUI convention: required business values, then `modifier`, then `enabled`, then
visual options, then slots, then `interactionSource`, then `content`.

The rule that runs through all of it: **state and callbacks belong to the consuming app**. Nothing
here holds app state, opens a window, or knows what your data means.

## PolishOptions / PolishCapabilities

```kotlin
enum class PolishQuality { Native, Enhanced }

data class PolishOptions(
    val quality: PolishQuality = PolishQuality.Native,
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
    val increaseContrast: Boolean = false,
) {
    fun resolve(capabilities: PolishCapabilities): PolishOptions      // a member, not an extension
}

data class PolishCapabilities(val enhancedSupported: Boolean = true)

enum class PolishWindowSurface {
    SystemGlass, AppGlass, AppOpaque;
    val baseGlassDepth: Int                          // 0 for AppOpaque, 1 for the other two
}

val LocalPolishOptions:       ProvidableCompositionLocal<PolishOptions>
val LocalPolishCapabilities:  ProvidableCompositionLocal<PolishCapabilities>
val LocalPolishWindowSurface: ProvidableCompositionLocal<PolishWindowSurface>
val LocalPolishGlassDepth:    ProvidableCompositionLocal<Int>
val LocalPolishSurfaceKind:   ProvidableCompositionLocal<PolishSurfaceKind>
```

`PolishWindowSurface` is the one thing the kit cannot detect: a container's material is resolved when
the container is *declared* (manifest `pico.spatial.windowcontainer.materialbackground`, or the DSL's
`enableMaterialBackground`), and there is no runtime read-back. The app states it, the way it states
`PolishCapabilities`, and it seeds the glass budget — see **PolishTokens** below.

`PolishQuality.Enhanced` no longer buys element-grade glass on small controls; that moved into the
baseline when a frame showed what the native path cost (see the `Control` row in the kind table).
Enhanced now means the hairline edge and the depth lift, and nothing else.

`resolve` downgrades `Enhanced` to `Native` when the host says the enhancement is unsupported and
never touches the three accessibility flags. Note the asymmetry: `reduceTransparency` and
`increaseContrast` are honoured by the components themselves, while `reduceMotion` is a value the
kit only *carries* — the kit ships no animations, so the app must route its own through
`PolishTokens.motion(reduceMotion, …)`. The kit does not probe the platform: the SDK's own
support predicates are library-internal, so the app decides from evidence it has. Persist the
options yourself (DataStore is the usual choice) and pass them to `PolishTheme`.

All sixteen combinations are usable, and a test asserts it: every combination yields a specified
foreground and a specified secondary foreground; every reduced-transparency combination yields an
opaque fill; every increased-contrast combination yields an edge.

## PolishTheme

```kotlin
@Composable fun PolishTheme(
    options: PolishOptions = PolishOptions(),
    capabilities: PolishCapabilities = PolishCapabilities(),
    windowSurface: PolishWindowSurface = PolishWindowSurface.SystemGlass,
    accent: Color = Color.Unspecified,
    colorScheme: ColorScheme = PicoTheme.colorScheme,
    content: @Composable () -> Unit,
)

@Composable fun Modifier.polishWindowRoot(): Modifier
@Composable fun polishWindowRootContentColor(): Color
```

Wraps `PicoTheme` and adds `LocalContentColor = labelPrimary`, `LocalTextStyle =
PolishTypography.body()` (`bodyMedium`), the resolved options, the declared `windowSurface`, and the
glass depth seeded from it. `accent` replaces the `interaction` role — the design system has no
`accent` role, and `interaction` is what a switch track, a selection edge and an emphasis button
already use. `colorScheme` defaults to the **ambient** scheme, not to a freshly resolved one, so a
nested `PolishTheme` keeps the accent the app already applied.

`Modifier.polishWindowRoot()` belongs only on a container whose system material was switched off at
declaration time. It has exactly two arms: under `reduceTransparency` it paints
`PolishTokens.OpaqueGround`, and otherwise it re-applies `Material.Regular`, the same tier the system
uses for a window panel. `Material.Thin` is deliberately not offered here — as a *window* material it
maps to none. Pair it with `polishWindowRootContentColor()`, which returns `labelPrimaryLight` on the
opaque arm and `labelPrimary` on the glass arm.

It used to paint `ColorScheme.fillPrimary` on the opaque arm, and that was the same defect this phase
found in `PolishSurface` — missed on this path because an app-owned root is the rarer case.
`fillPrimary` is a vibrancy sentinel the compositor resolves by *darkening* what is behind the
surface, so a root painted with it still showed the room through it, which is the one thing the
setting exists to stop. The role is gone from this function; **no arm of the kit names it for a
ground any more.** What `fillPrimary` does was established on a `PolishSurface`, in four opened
frames that vary only the backdrop behind it (`design-005-legibility-room`, `-006-bright`,
`-007-dark`, `-008-detailed`, build `f91e4b7e`); the root fix landed in build `37285f06`.

What the frames establish for the root now: the **opaque arm is measured**. The showcase exercises
`polishWindowRoot` on its focus window (declared `enableMaterialBackground = false`), and
`final-14-focus-rt` (build `a78d8109`, 17/17 gate checks, gate-admitted) captures that window under
`reduceTransparency`: its ground measures exactly **40.00**, the literal value of `OpaqueGround`
(`final-windowroot-pair.json`, which pairs it against the glass arm on the same build,
`final-17-focus-glass`, where the same root regions read 164.75–184.43). Both of those frames were
**measured by tool, not opened and judged by eye**. The **glass arm has been opened** on an earlier
build: `polish-focus-window` and `polish-focus-window-2` (build `be447bda`, taken with
`reduceTransparency` off) show a translucent light panel with the main window still readable through
it. Still unverified: no opened frame of the root under reduced transparency, and no headset frame of
either arm — every frame named here is an emulator capture. Nothing in this lane varied what is
behind the *window* either (that would mean changing the emulator room).

Put it directly inside each container body, once per container:

```kotlin
fun mainApp(scope: SpatialAppScope) = with(scope) {
    DefaultWindowContainer {
        PolishTheme(options = settings.polishOptions, accent = brandAccent) {
            LibraryScreen(...)
        }
    }
}
```

## PolishTokens

```kotlin
object PolishTokens {
    val SpaceTiny: Dp; val SpaceSmall: Dp; val SpaceSnug: Dp          //  4 /  8 / 12
    val SpaceMedium: Dp; val SpaceLarge: Dp; val SpaceHuge: Dp        // 16 / 24 / 32

    val RadiusCompanion: Dp; val RadiusPanel: Dp                      // 32 / 24
    val RadiusCard: Dp; val RadiusControl: Dp                         // 16 / 12

    val Hairline: Dp; val HairlineStrong: Dp                          //  1 /  2
    val LiftZ: Dp                                                     //  8
    val MinTargetSize: Dp                                             // 48
    const val MaxGlassDepth: Int = 2
    val OpaqueGround: Color                                           // Color(0xFF282828)

    val DurationFeedbackMillis: Int                                   // MotionTokens.durationShort1
    val DurationStateMillis: Int                                      // MotionTokens.durationShort2
    val DurationPanelMillis: Int                                      // MotionTokens.durationShort3
    val EasingStandard: Easing                                        // MotionTokens.bezierEasingStandard

    fun <T> motion(reduceMotion: Boolean, durationMillis: Int = DurationStateMillis): FiniteAnimationSpec<T>
    fun <T> motionSpec(reduceMotion: Boolean, durationMillis: Int = DurationStateMillis): AnimationSpec<T>
    fun <T> spring(reduceMotion: Boolean, args: SpringEasingArgs = MotionTokens.springEasingRapid): AnimationSpec<T>
}
```

Every token names the SDK token it mirrors, or states that it has no anchor. The SDK's own
`DimensionTokens` is `@RestrictTo(LIBRARY)` with an `internal companion object`, so an app cannot read
it and a kit that wants to agree with the shell has to mirror the numbers and cite them. Spacing
mirrors `GapSmall`/`GapMedium`/`GapLarge` and `PaddingSmall`…`PaddingLarge`; only `SpaceTiny` (4 dp)
has no SDK anchor. `SpaceSnug` (12 dp) is the step the kit was missing: between a label and its
control, where 8 dp crowds and 16 dp breaks the pair apart.

**Corrected claim.** 32 dp is `DimensionTokens.RadiusExtraHuge` — the radius of `Subwindow`, `TabBar`
and `Toolbar` — and the kit used to call it "the system window corner radius". It is not. No public
API exposes a window *container*'s radius, and nothing in the 6.1.9 sources states one. The token is
now named `RadiusCompanion` for what it actually is: use it when an app surface has to sit flush
beside a system companion window, not to guess at the window panel.

`MinTargetSize` is anchored to the Compose fork's `ViewConfiguration.minimumTouchTargetSize`
(48 × 48 dp), which `clickable` uses to expand pointer bounds around a smaller layout. Several shipped
SDK sizes are visually smaller — `ButtonDefaults.Small` (40 dp), `ButtonDefaults.Min`,
`IconButtonDefaults.Min`, `ChipsDefaults.Small`, `CheckBoxDefaults.CheckboxSize` (32 dp). Their *hit*
area is expanded to 48 dp by the framework; their *visible* affordance is not.

**"Regular" is not a synonym for 48 dp**, and the kit's own token KDoc used to imply it was. Only
`ButtonDefaults.Regular` (`Button.kt:312`) and `IconButtonDefaults.Regular` (`IconButton.kt:114`) are
48 dp tall; `ChipsDefaults.Regular` is `chipSize(40.dp)` (`Chips.kt:592`) and
`ToggleButtonDefaults.Regular` aliases `ButtonDefaults.Min` at 32 dp (`ToggleButton.kt:113`). At panel
distance prefer the two that really are 48 dp, and reserve the rest for dense clusters the user is
already focused on.

### The glass budget

`MaxGlassDepth = 2` is how many compositor-material layers may stack in one window, **including the
window's own**. `PolishTheme` seeds `LocalPolishGlassDepth` from `windowSurface.baseGlassDepth`
(0 for `AppOpaque`, 1 otherwise, and 0 under `reduceTransparency` because the app then spends no
material at all); every `PolishSurface` that takes a material increments it for its children; a
surface at or over the budget takes its drawn role fill instead — which also gives it an edge and a
tone the layer above does not have. Nothing is removed: the surface is still there, still shaped,
still legible. A **drawn** surface does not spend the budget, because it is one paint, not another
compositor layer for the system to resolve.

Two is the number because three layers were not separable in an opened frame: panel, card and window
read as one flat sheet (`polish-fb-glass-b`, build `be447bda`). Restraint was already the design goal;
this is the number that makes it enforceable rather than advisory.

### Motion

The durations read `MotionTokens`, which is the one SDK token object that is public and unrestricted
(no `internal`, no `@RestrictTo`), so the kit follows an OTA that retunes the platform's motion. Call
`PolishTokens.motion(...)` rather than `tween(...)`: a bare `tween` takes Compose's
`FastOutSlowInEasing`, a different curve from the `bezierEasingStandard` that `Slider`,
`ScrollIndicator` and `TabBar` all pass, and an app whose panels ease differently from the chrome
around them reads as a port.

**Two documented failure modes, which is why `spring(...)` exists.** `MotionTokens` durations fall
back to their documented values if the design resources are unreadable — safe. The **spring** tokens
do not: `springEasingArgsRes` returns `SpringEasingArgs.Unspecific`, which is
`(dampingRatio = NaN, stiffness = NaN)`, and `toSpring()` hands that straight to `spring(...)`, where
it produces an undefined value for the whole run rather than an animation.
`PolishTokens.spring(...)` validates the token and substitutes a real spring. Separately,
`bazierEasingRes` falls back to `LinearEasing` rather than to the documented curve — a silent
downgrade, not a crash, and not something the kit can detect.

## PolishTypography

```kotlin
object PolishTypography {                  // every member is @Composable @ReadOnlyComposable
    val display: TextStyle          // displaySmall     28 sp / 700
    val windowTitle: TextStyle      // headlineMedium   20 sp / 700
    val sectionTitle: TextStyle     // headlineSmall    16 sp / 700
    val objectTitle: TextStyle      // titleMedium      16 sp / 600
    val control: TextStyle          // labelLarge       14 sp / 600
    val caption: TextStyle          // bodySmall        12 sp / 500
    val overline: TextStyle         // labelMedium      12 sp / 600

    @Composable fun bodyLead(multiline: Boolean = false): TextStyle   // bodyLarge / bodyLargeMultiline
    @Composable fun body(multiline: Boolean = false): TextStyle       // bodyMedium / bodyMediumMultiline

    @Composable fun ladder(): List<Pair<String, TextStyle>>
}
```

**Nine jobs, eleven rungs** — `bodyLead` and `body` each carry a single-line and a multiline variant,
so `ladder()` returns eleven entries — each pinned to one SDK role. **The kit invents no sizes** —
inventing one would fight the shell, which draws its own chrome with these roles — it chooses which
role carries which job. That is worth doing because `PicoTheme.typography`'s 18 roles are
*alphabetical, not a ramp*: **four** are 16 sp (`bodyLarge`, `bodyLargeMultiline`, `headlineSmall`,
`titleMedium`), **four** are 14 sp (`bodyMedium`, `bodyMediumMultiline`, `labelLarge`, `titleSmall`),
two are 12 sp (`bodySmall`, `labelMedium`) and two are 20 sp (`headlineMedium`, `titleLarge`) — and
`labelLarge` and `titleSmall` are byte-identical (14 sp / 18 lh / W600), so a screen using one for a
row title and the other for a button label has expressed no distinction at all.

Distinct sizes run **28 → 20 → 16 → 14 → 12**, with a deliberate **floor at 12 sp**: the SDK's
`labelSmall` (11 sp) and `bodyTiny` (10 sp) are not on the ladder. Weight does the rest — 700 for the
two header rungs, 600 for titles and control text, 500 for prose. The rule it encodes: **rank is
carried by size *and* weight, never by one alone, and never by colour.** The kit's first version broke
it, setting an object title at `titleSmall` (14/600) directly above body at `bodyMedium` (14/500) — a
weight-only step at identical size, the weakest distinction the SDK can express. `objectTitle` is
`titleMedium` (16 sp) for exactly that reason.

Pass `multiline = true` whenever text can wrap. The multiline roles are the same size and weight with
the line height opened (20 → 24, 18 → 22); the kit's first version used neither, so every wrapped
paragraph in the showcase was set at single-line leading, which is the most common reason a panel
reads as a wall.

`ladder()` returns all eleven entries (the nine rungs plus the two multiline variants) in rank order,
paired with the kit's name for each. It is what the specimen screen renders and what the ladder tests
walk, so a rung added to the object without being ranked fails a test rather than quietly appearing.

**What a frame settled.** `design-004-specimen-fit` (build `a493c172`) shows all eleven rungs at once,
and the weight steps at 16 sp, at 14 sp and at 12 sp are each visibly distinct. Two layout defects
were found by frames and fixed on the way there: an eleven-rung ladder at the Panel's default 16 dp
rhythm overflowed the window, and a four-across row of surface kinds at half the window width broke
"supporting" mid-word and wrapped the word "Emphasis" itself. **What a frame cannot settle:** an
emulator capture frames a whole room, so the window occupies a small part of it and absolute
legibility at wearing distance is not decidable from one. Every claim here is a claim about *relative*
rank until a headset frame and a wearer say otherwise.

## PolishContrast

```kotlin
object PolishContrast {
    const val NormalText: Float = 4.5f
    const val LargeText: Float = 3.0f
    const val NonText: Float = 3.0f

    fun isLargeText(style: TextStyle): Boolean
    fun targetFor(style: TextStyle): Float
    fun composite(foreground: Color, background: Color): Color
    fun relativeLuminance(color: Color): Float
    fun ratio(foreground: Color, background: Color, target: Float = NormalText): PolishContrastResult
    fun ratioOver(foreground: Color, background: Color, backdrop: Color,
                  target: Float = NormalText): PolishContrastResult
}

sealed interface PolishContrastResult {
    data class Measured(val ratio: Float, val target: Float) : PolishContrastResult {
        val meets: Boolean
    }
    data class Undecidable(val limit: PolishContrastLimit) : PolishContrastResult
}

enum class PolishContrastLimit {
    VibrantForeground, VibrantBackground, VibrantBackdrop,
    TranslucentBackground, TranslucentForeground, TranslucentBackdrop,
}
```

**It refuses rather than misleads.** On a PICO device with vibrancy on, 10 of the 16 `ColorScheme`
roles are not colours: they are `Color.Vibrant.withVibrant(level)` sentinels — `Color.Black` with a
level written into bits 24–31 of the `ULong`, bits that `Color.red/green/blue` and `toArgb()` never
read — so all ten read back as `#FF000000` in the app process and the real pixel is produced out of
process. A ratio computed from two of them is not a wrong number; it is a number about the wrong
thing, reporting 1.00:1 for a pairing the compositor renders perfectly legibly. So `ratio` returns
`Undecidable` when either colour has **unresolved** vibrancy, when the background is translucent with
no stated backdrop, or when the foreground is translucent and uncomposited. Treat `Undecidable` as
"measure this from a rendered frame instead" — never as a pass, never as a failure.

**"Unresolved" is the load-bearing word, and the kit got it wrong once.** The private predicate is
`PolishContrast.hasUnresolvedVibrancy()` = `containsVibrant() && obtainVibrant() != Vibrant.None`, not
`containsVibrant()` alone. Of the six roles this page calls measurable, only **four** are literal:
`error`, `alert`, `passable` and `interaction` are `ColorTokens…asDisplayP3()` in both schemes
(`ColorScheme.kt:242-245`). The other two **do** carry an encoding —
`labelPrimaryLight` is `SubWhite100.withVibrant(Vibrant.None)` (`ColorScheme.kt:235`) and
`dividerLine` is `SubBlack12.withVibrant(Vibrant.None)` (`:246`) — but `Vibrant.None` is the level
that *disables* the effect (`Vibrant.kt:102`, *"can be used to disable vibrant effect"*), so their RGB
stays authoritative. Asking only `containsVibrant()` refused
exactly those two, on a device, silently, and for two of the six roles this page promised were
measurable. The predicate now asks the question that matters: is there a level that will *replace*
the channels? So six roles are measurable — but not for the reason the old text gave.

Where it *is* valid: off the spatial platform (a JVM or Robolectric test, a preview, a non-PICO
build), where `systemColorScheme` falls back to `defaultColorScheme()` and all 16 roles are literal
ARGB; for those 6 roles (the four semantic ones carry a DisplayP3 colour space and this object
converts nothing, so treat those as sRGB approximations); and for app-owned colours.

`ratioOver` is the form to use for a real pairing in this design system — backdrop, then the surface's
own translucent fill, then the label — because it composites both layers before measuring. `composite`
is source-over, and it matters: `fillSecondary` is 40 % white, `fillTertiary` 3.9 % black, `fillLight`
14.9 % white, `labelSecondary` 80 % black, `labelTertiary` 65 %, `labelQuaternary` 40 %, `dividerLine`
12.2 %. Measuring one of those against a backdrop without compositing it ignores its alpha entirely.

`isLargeText` reads WCAG's "18 pt, or 14 pt bold" in the conventional Android way — 18 sp, or 14 sp
bold — and takes bold as `FontWeight.Bold` (700) or heavier. **The SDK's `title*` and `label*` roles
are W600 SemiBold and do not qualify**, so only three of the nine rungs get the 3:1 allowance:
`display`, `windowTitle` and `sectionTitle`. That is deliberately stricter than counting 600 as bold.
A style whose size is unspecified, or expressed in em rather than sp, returns `false` — the stricter
answer.

**On device, contrast has to be measured from rendered pixels.** That is what the showcase's uniform
probe swatches and `polish_contrast.py` are for, and the two lanes answer different questions and are
reported separately. Nothing this object computes substitutes for a frame.

## PolishSurface

```kotlin
@Composable fun PolishSurface(
    modifier: Modifier = Modifier,
    kind: PolishSurfaceKind = PolishSurfaceKind.Card,   // Panel | Card | Control | Emphasis
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    errorMessage: String? = null,
    shape: Shape = PolishSurfaceDefaults.shapeFor(kind),
    contentPadding: PaddingValues = PolishSurfaceDefaults.paddingFor(kind),
    verticalArrangement: Arrangement.Vertical = PolishSurfaceDefaults.arrangementFor(kind),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
)

object PolishSurfaceDefaults {
    fun shapeFor(kind: PolishSurfaceKind): Shape
    fun paddingFor(kind: PolishSurfaceKind): PaddingValues
    fun arrangementFor(kind: PolishSurfaceKind): Arrangement.Vertical
    @Composable fun foregroundFor(kind: PolishSurfaceKind, options: PolishOptions): Color
    @Composable fun secondaryForegroundFor(kind: PolishSurfaceKind, options: PolishOptions): Color
    const val LightSecondaryStep: Float = 0.70f
    fun lightSecondaryForeground(primaryLight: Color, ground: Color = PolishTokens.OpaqueGround, step: Float = LightSecondaryStep): Color
    fun usesLightForeground(kind: PolishSurfaceKind, options: PolishOptions): Boolean
    // fillFor(...) and edgeFor(...) are `internal`: they are the module's own decisions, and the
    // tests reach them, but an app does not.
}
```

A surface spaces its own children — 16 dp in a Panel, Card or Emphasis, 8 dp in a Control. Group a
title and its subtitle in a nested `Column` with a tighter arrangement rather than letting the
surface space them apart.

**Clip and border share one shape; the hit area does not.** An earlier version of this page described
the three as one agreement. They are not: the clip (`graphicsLayer`) and the `border` both take
`shape`, so the visible edge and the fill cannot disagree — but `clickable` takes no shape at all, so
the touch target is the node's **rectangle**, and a heavily rounded surface is clickable slightly
outside its visible corners. The kit does not correct this; the framework's own components behave the
same way.

| Kind | Shape | Fill (glass mode) | Clickable, or over the glass budget | Fill (reduced transparency) | Use for |
|---|---|---|---|---|---|
| `Panel` | 24 dp | `Material.Regular` | `fillLight` clickable / `fillTertiary` over budget | `PolishTokens.OpaqueGround` | a region filling most of a window |
| `Card` | 16 dp | `Material.Regular` | `fillLight` clickable / `fillTertiary` over budget | `PolishTokens.OpaqueGround` | a content card inside a panel |
| `Control` | 12 dp | `Material.Thin`, in **both** quality modes | `fillTertiary` | `PolishTokens.OpaqueGround` | a field well, a chip group |
| `Emphasis` | 16 dp | `PolishTokens.OpaqueGround` | unchanged — it is already a drawn fill | `PolishTokens.OpaqueGround` | the primary object on screen |

**`Control` used to draw `fillTertiary` on the native path, and that was wrong.** `fillTertiary` is a
3.9 %-black wash: over a dark backdrop (`design-007-legibility-dark`, build `f91e4b7e`) the Control
surface's own title, body and supporting line were black on near-black, and over a checkerboard
(`design-008-legibility-detailed`) the pattern read straight through the text. A tint is not a
surface. `Material.Thin` moved down into the baseline — legibility is not a quality tier — and
`design-009-fix-detailed` (build `37452262`) shows the card fully legible, which also answers the
open question of whether `Material.Thin` renders on the view path at all. It does.

**`Emphasis` and `reduceTransparency` take a literal, not a role.** `ColorScheme.fillPrimary` is
`Color.Vibrant.withVibrant(Vibrant.Darker)` — a sentinel the compositor resolves by *darkening what
is behind the surface*. Four admitted frames on build `f91e4b7e`, varying only an app-drawn backdrop,
rendered the same `Emphasis` surface as near-black over the room (`design-005-legibility-room`),
**medium grey** over #F4F4F4 (`-006-bright`), near-black over #0B0B0B (`-007-dark`), and **the
checkerboard, darkened and still visible through it** (`-008-detailed`). A setting whose entire
purpose is that the background stops showing through cannot be built on a role whose value the
background decides, so the kit owns `PolishTokens.OpaqueGround = Color(0xFF282828)` — the literal the
SDK's own non-vibrant `ColorTokens.FillPrimary` carries — and pairs it with `labelPrimaryLight`.

After the fix the surfaces are invariant to app-drawn content behind them, and there is now a retained
artifact for it:
`design-backdrop-invariance.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/design-backdrop-invariance.json`).
Same build (`ae4bc4ed`), same flow, same settings, same probe boxes; the app-drawn backdrop fixture is
the only variable, Dark → Bright (frames `design-011-fixed-dark` → `design-012-fixed-bright`):

| region | Dark | Bright | delta |
|---|---:|---:|---:|
| `backdropStripLeft` | 11.00 | 244.00 | **+233.00** |
| `backdropStripRight` | 11.00 | 244.00 | **+233.00** |
| `panelSurfaceFill` | 195.155 | 195.155 | **0.00** |
| `cardSurfaceFill` | 184.024 | 184.024 | **0.00** |
| `controlSurfaceFill` | 168.227 | 168.227 | **0.00** |
| `emphasisSurfaceFill` | 40.000 | 40.000 | **0.00** |

The two `backdropStrip` regions sit on the app-drawn fixture itself; the four `surfaceFill` regions
sit on the empty probe-bg box inside each surface, which is the ground its text sits on. All four
room-only stability boxes (`roomCeilingLeft`, `roomAcUnit`, `roomFloorLeft`, `roomRugRight`) moved
**0.000**, so the camera held still and the deltas are the fixture's. All eight text pairings also
reported identical ratios to 2 dp on all four backdrops (`contrast-design-011-fixed-dark.json`,
`-012-fixed-bright.json`, `-013-fixed-room.json`).

**Limit:** that is invariance to app-drawn content *inside the window*. What is behind the *window*
was constant and was not varied.

State contract:

- `onClick == null` → a static surface: no hover, no press, no role. A static surface that hovers
  lies about being interactive.
- `onClick != null` → `spatialHoverEffect`, `PicoIndication` press, controller haptics and
  `Role.Button`, each applied exactly once, in the SDK's own order (clip and alpha, hover, fill,
  edge, semantics, click, haptics, padding). **An interactive surface also stops taking a material
  and draws its own fill instead** — see "Hover needs a drawn fill" below. An earlier version of
  this page said `onClick != null` gives you "hover" full stop. On the emulator it did not: a
  clickable, material-filled surface produced no hover change at all.
- `enabled = false` → the whole node takes `LocalDisableAlpha` through `graphicsLayer`, and the
  click is refused. Never hardcode `0.3f`.
- `selected = true` → a thicker edge in the accent role plus `selected` in semantics. Colour alone
  never carries the state; add a check or another non-colour cue in your content for a list.
- `errorMessage != null` → the edge moves to the `error` role at `HairlineStrong`, and the message is
  reported through the semantics `error` property, so the state is not carried by colour alone. **The
  caller still owns rendering the message as text** — a surface that only turns red says nothing to
  someone who cannot see the red. The SDK's own error presentation is `TextField`-family only and its
  `errorBorder` is `internal`: the declaration is `internal fun Modifier.errorBorder(` at
  `TextField.kt:940` (the 947-958 this page used to cite is inside the function body and excludes the
  `internal` keyword the claim rests on). So this is the kit re-implementing a treatment the design
  system does not expose, in the public API and deliberately.
- **focus** → a `Hairline` ring in the `interaction` role. It is *observed, never requested*: the kit
  adds no `Modifier.focusable`, it reads whatever focus the app's own focus handling produces from
  the `interactionSource`, so a surface that is never focused never shows the ring. The design system
  ships no focus-visible treatment of any kind, so this is the kit's own addition.

  **A frame now shows it, and getting that frame took two tries.** Nothing in the showcase's state
  gallery gives a surface focus, so the focus cell first rendered as an ordinary clickable surface:
  `design-024-states-p2` (build `90b10316`) shows the cell with **no ring at all**. Emitting
  `FocusInteraction.Focus()` once did not fix it either — `MutableInteractionSource` publishes through
  a shared flow with no replay and `collectIsFocusedAsState` subscribes from its own `LaunchedEffect`,
  so a single emit races the collector and is simply lost when it wins. Re-emitting on an interval
  keeps the focus count positive whenever the collector is listening, and `design-025-focus-ring`
  (build `6b19ca10`) is the first frame in this lane in which the ring renders: a thin blue outline
  around the "Focused" cell, where `design-024-states-p2` had shown the same cell bare. The thicker
  selection edge in the same accent is one page away, on `design-023-states-p1` (build `90b10316`),
  where the "Selected" cell carries it. **Still emulator only; no headset frame and no wearer.**

**Edge precedence, highest first: error, selected, focused, increased contrast, then the separation
cases.** Error outranks selection because a selected row that has gone wrong needs to say so;
selection outranks focus because focus is transient and selection is the state the user set. Error has
its own role; selection and focus share the `interaction` accent and are told apart by weight —
selection is `HairlineStrong`, focus is `Hairline` — which is one accent per screen at two weights
rather than a second hue nobody has been taught. A **static** surface that had to give up its material — to the glass budget —
also draws an edge: a `fillTertiary` wash over its own parent without an edge is not a surface, it is
a smudge.

The top of that order is no longer only a unit test (`errorOutranksSelection`): on
`design-024-states-p2` (build `90b10316`) and again on `design-025-focus-ring` (build `6b19ca10`) the
"Error + selected" cell renders the **red** edge, not the accent, and is indistinguishable from the
plain "Error" cell beside it.

### Hover needs a drawn fill

The module's `fillFor(kind, options, interactive, glassDepth)` (internal) never returns a material for
an interactive surface. The reason is measured, not stylistic.

On the emulator, the same list row was hovered at the same pointer coordinates (287.5, 575.5) three
times. Each row is a self-contained same-build pair; the point of the table is the comparison *across*
the fix, so the build is named on every row.

| the row's fill | build | hovered row | sibling controls in the same frames |
| --- | --- | --- | --- |
| `Modifier.backgroundMaterial` (compositor glass) | `384aea19` | **0.00** — the whole 1962x2018 capture was byte-identical | four siblings, all **0.00** |
| `fillPrimary`, drawn (reduced transparency, as it was then) | `a7d4b9b0` | **+16.481** mean gray | four siblings: **1.344**, 0.00, 0.00, 0.00 |
| `fillLight`, drawn (ordinary glass mode, after the fix) | `be447bda` | **+16.921** mean gray | five siblings, all **0.00** |

Records: `hover-photometry-003-material-surface-control.json`, `-002-opaque-surface.json`,
`-006-after-fix-glass-mode.json`. The middle row is the one to read carefully: an earlier version of
this page said "three sibling controls … 0.00" for all three rows, and that flattened a real number —
its artifact records **four** siblings and one of them, `control_rowBelow_Kiln`, moved **1.344**, the
row directly beneath the hovered one. All four room-only stability boxes read 0.000 in every one of
the three pairs, so none of this is camera drift; a 1.344 on the adjacent row is a small real signal
against a +16.481 on the pointed-at one, not noise, and it is not claimed to be zero.

The SDK sources agree with the measurement:

- `PicoIndication.kt` draws only the press colour on the spatial platform, and says why: *"We only
  need to draw press color in spatial platform, because hover layer is rendered by spatial hover
  effect modifier."* Nothing in-process substitutes for it.
- `SpatialHoverEffect.kt` documents that the effect is *"applied out of process, so it cannot be
  visible to the app process"*, and the DSL it exposes builds geometric effects — `clipShape`,
  `scale`, `alpha`, `offset`, `animation` — not a colour tint.
- There **is** a material-aware hover path, `Modifier.materialSpatialHoverEffect`
  (`SpatialMaterialHoverEffect.kt:46`), but it is `internal`, and `windows/TabBar.kt:258` is its
  only caller in the whole design library. An app cannot reach it.
- Every interactive built-in draws a role colour rather than a material: `ListItem` takes
  `fillLight` (`:293`), `Option` takes `fillSecondary`/`fillTertiary` (`:213,215`), `BasicButton`
  takes `fillPrimary`. `windows/Toolbar.kt` is the only design file that fills with a material.

So an interactive `Panel` or `Card` takes `fillLight` — `ListItem`'s own role — and an interactive
`Control` takes `fillTertiary`. (That middle row was measured when `reduceTransparency` still drew
`fillPrimary`; it now draws `PolishTokens.OpaqueGround`, which changes the colour and not the
finding — the finding is that a *drawn* fill hovers and a material does not.) The surface draws a fill
of its own so the compositor has something to light up. This page used to add "the surface still reads
as glass sitting on the window material" — that is an appearance claim, and no frame in this lane
isolates an interactive surface against its static twin, so it is dropped rather than kept unsourced.
Non-interactive surfaces within the glass budget keep their material, and the test
`interactiveSurfacesNeverTakeAMaterialFill` pins both halves.

**Limit.** Every number above is from the emulator. No headset has confirmed it, and a headset's
compositor could behave differently.

**Contrast follows the fill, not the option.** One predicate, `PolishSurfaceDefaults.usesLightForeground(kind, options)`,
decides the foreground, the secondary foreground and the edge colour together, so they cannot
disagree. On a light-foreground (dark, opaque) surface the edge is `fillSecondary`; elsewhere it is
`dividerLine`, or `labelQuaternary` under increased contrast. A dark hairline on a dark fill is
invisible, which is exactly the defect the host-rendered frames caught.

**Supporting text is `labelSecondary` on an ordinary surface, and that is a measurement, not a
preference.** `secondaryForegroundFor` has three branches, in code order: (1) when
`usesLightForeground(kind, options)` — an `Emphasis` surface, or anything under `reduceTransparency`
— the derived opaque tone `lightSecondaryForeground(labelPrimaryLight)`, `LightSecondaryStep` (0.70)
of the way from `PolishTokens.OpaqueGround` to the light label, unless `increaseContrast` is on, in
which case the full `labelPrimaryLight`; (2) `labelPrimary` under `increaseContrast` on a
dark-foreground surface; (3) `labelSecondary` otherwise. The first branch is measured on build
`6cdb7306`: `[191,191,191]` on `[40,40,40]` = **8.02 : 1** (`contrast-ref-03-room.json`, Emphasis
at default settings; `contrast-ref-04-rt-on.json`, all four surfaces under reduced transparency;
`detail-08-surface-kinds-AFTER-2x` and `detail-09-rt-all-four-2x` opened), and it returns to
`[255,255,255]` / 14.74 : 1 with `increaseContrast` by design (`contrast-ref-07-rt-ic.json`). Call
the function rather than copying the arithmetic into an app. Only the third branch is the one
the measurement below is about. It used to return `labelTertiary`. Measured from an admitted emulator frame
(`design-009-fix-detailed`, build `37452262`) through uniform probe swatches the showcase renders,
sampled with the Display-P3 luminance coefficients the capture's own `cICP` chunk declares:

| surface | supporting text on `labelTertiary` | on `labelSecondary` | target |
|---|---:|---:|---:|
| `Panel` | **3.18 : 1** fail | **8.60 : 1** pass | 4.5 |
| `Card` | **3.26 : 1** fail | **8.57 : 1** pass | 4.5 |
| `Control` | **3.49 : 1** fail | **8.22 : 1** pass | 4.5 |

All three clear 3:1, so the same colour would be fine on a large-text rung or as the boundary of a
control. It is neither: supporting text is set at `PolishTypography.caption`, 12 sp W500, which is
normal text. The `labelSecondary` column is build `ae4bc4ed` (`design-010-supporting-fix`), where every
*body* pairing came back byte-identical to the frame before it — 11.92 / 10.58 / 8.84 / 14.74 — which
is the sibling control that says the role swap is what moved, and all four room-only stability boxes
moved 0.000 across the pair.

`increaseContrast` steps up again to `labelPrimary`, at which point the distinction is carried by size
and weight alone, which is what the setting is for — and that is measured too, not inferred.
`contrast-design-016-ic-on.json` (frame `design-016-increase-contrast`, counted action `n=10`, build
`ae4bc4ed`) reports supporting text at **11.92 / 10.58 / 8.84** on Panel / Card / Control and
**14.74** on Emphasis: byte-for-byte the *body* ratios from the same frames, because with the setting
on the supporting line is drawn in the same role the body is.

Only a rendered frame could say this. The same three pairings compute fine from the non-vibrant
fallback values on the JVM.

**The premise these tables rest on was open, and is now settled.** Every `polish_contrast.py` report
in this lane still carries `"premise": "UNVERIFIED"` in its JSON header, because the tool writes that
field before anything is looked at. The premise it names is specific: *the ratios mean something only
if a role colour passed to `Modifier.background(...)` is resolved by the compositor rather than drawn
as flat `#FF000000`*, and the decisive check it names is that the `labelPrimary` probe patch and the
`labelTertiary` probe patch render as **different** tones. They do. In
`design-contrast-detailed-fixbuild.json` (frame `design-009-fix-detailed`, build `37452262`) the
`labelPrimary` patch means `[0.000, 0.000, 0.000]` and the `labelTertiary` patch means
`[107.552, 103.057, 98.154]`, each uniform to within 1.7 levels of stddev across 1600 pixels. Two
sentinels that were *not* being resolved would both have read flat black. Opening that frame shows the
same thing without arithmetic: on `design-009-fix-detailed` the Panel, Card and Control probe pairs
each render as one **black** square beside one **mid-grey** square, and only on `Emphasis` — where the
light foreground makes both patches `labelPrimaryLight` — are the two the same white. **Read the header as
"unverified at the time the tool wrote it", not as a live caveat on the numbers** — and keep the three
limits that report does still list as open: the emulator's compositor is not the headset's, the iCCP
profile is not parsed against the cICP declaration that the tool trusts, and these are 2D legibility
targets applied to a spatial UI.

**A surface does not scroll, and it does not tell you when it overflows.** A `Panel` filling a
window whose content is taller than the window silently gives its last child whatever space is left
— in the first native frame of the showcase that was an action bar clipped to a sliver with its
three buttons undrawn, and nothing in the host tests or the host-rendered frames showed it. Give a
full-height panel a scrolling body and pin the actions beside it:

```kotlin
PolishSurface(kind = PolishSurfaceKind.Panel, modifier = Modifier.fillMaxSize()) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { /* body */ }
    PolishActionBar { /* actions stay reachable */ }
}
```

Two `PolishFactRow`s (or any two intrinsically-sized rows) side by side need `Modifier.weight(1f)`
each. Without it the second is squeezed to its minimum width and wraps its label one letter per line
— also from that first frame.

**But a scroller is not an option for anything you have to prove with a frame.** On this emulator
there is no host route to a guest scroll — the wheel drives the camera and never reaches the app — so
a section below the fold is a section no capture can ever show. The showcase's state gallery learned
this the expensive way: seven sections in a scroller, and only the first two were ever capturable.
Two columns were not enough either — six sections still overflowed the app's declared 1080x640 dp
window, and three frames show the overflow rather than argue it: `design-018-states-overlap`
(build `37285f06`), `design-020-states-matrix-fit` (build `3f71458a`) and, even after the sections
were cut to four, `design-021-states-page1` (build `2fbc4576`), where the right column's "Surface
states" section is clipped at the window's bottom edge — Static and Interactive cut mid-height,
Selected and Disabled absent entirely. (An earlier version of this sentence also said the loading
button was clipped. It is not: the button renders complete in the admitted capture. What cut it was
the crop `emulator/crops/design-021-window.png`, which is a crop boundary and not the window.) The gallery now **pages**, with the page turn
bound to a counted action (`action=galleryPage`) where a scroll would have been nothing in the log;
`design-023-states-p1` (build `90b10316`) is the first frame in which a whole page fits.

**Do not nest a self-filling composable inside it.** `ListItem` paints `fillLight` itself, so it is
a sibling of a `PolishSurface`, not a child. Two stacked fills is the most common polish defect.

## PolishButton / PolishIconButton

```kotlin
enum class PolishButtonTone { Standard, Emphasis, Quiet, Destructive }

@Composable fun PolishButton(
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
)

@Composable fun PolishIconButton(          // a STATELESS icon action
    onClick: () -> Unit,
    contentDescription: String,      // required: an icon-only control is unusable without it
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: PolishButtonTone = PolishButtonTone.Quiet,
    size: ButtonSize = IconButtonDefaults.Regular,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
)

@Composable fun PolishToggleIconButton(    // an icon control WITH an on state
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ToggleIconButtonDefaults.Regular,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
)
```

`PolishIconButton` has **no** `selected` parameter, deliberately. An earlier version had one that
only swapped the container colour: the state was carried by hue alone, reported nothing to a screen
reader, and silently overrode the caller's `tone`. A control with an on state is a toggle, so it uses
`PolishToggleIconButton`, which delegates to the design system's `ToggleIconButton` and therefore
reports `toggleableState`. The caller still owes a non-colour cue — swap the icon or place an
indicator beside it.

Tones map to colour pairs: `Standard` = the built-in default (`fillPrimary`/`labelPrimaryLight`),
`Emphasis` = `interaction`/`labelPrimaryLight`, `Destructive` = `error`/`labelPrimaryLight` (gate it
behind a confirmation surface). `Quiet` is the one that reads its surroundings: it takes the ambient
`LocalContentColor` the surface provided and a container that contrasts with it, so the same call
site stays legible on a glass panel and on the opaque surface reduced transparency produces.

`loading` is a real state: activation is suppressed, `loadingDescription` is reported through
`stateDescription` so it is not visual-only, and the label stays laid out at zero alpha under the
indicator so the button keeps its width and its neighbours do not jump. Everything else — shape, minimum size,
typography, disabled alpha, hover, press indication, click audio, haptics, the `Button` role — comes
from the built-in. **Never add `spatialHoverEffect` here**; the built-in already applies it.

## PolishControls

```kotlin
data class PolishSegment<T>(val value: T, val label: String)

@Composable fun PolishControlRow(title, modifier, supportingText, kind, control: @Composable RowScope.() -> Unit)
@Composable fun PolishSwitchRow(title, checked, onCheckedChange, modifier, supportingText, enabled, kind, onDescription, offDescription, interactionSource)
@Composable fun PolishSliderRow(title, value, onValueChange, modifier, valueRange, onValueChangeFinished, enabled, supportingText, kind, valueLabel, interactionSource)
@Composable fun <T> PolishSegmented(options: List<PolishSegment<T>>, selected: T, onSelect: (T) -> Unit, modifier)
@Composable fun PolishFactRow(label, value, modifier, kind)
```

Each defaults `kind` to `LocalPolishSurfaceKind.current`, which `PolishSurface` provides — so a
control dropped into an `Emphasis` or reduced-transparency surface derives the light foreground
automatically instead of taking the dark one and disappearing. Pass `kind` explicitly only outside a
surface.

Each row sets its own rungs off `PolishTypography`, so a screen never picks a raw role for them: the
title is `objectTitle`, the supporting line `caption`, a `PolishFactRow` label `overline`, and a
`PolishSliderRow`'s value readout `control`. The supporting line and the fact label take
`secondaryForegroundFor`, whose branches in code order are: on a light-foreground surface
(`Emphasis`, or any surface under `reduceTransparency`) the derived opaque tone
`lightSecondaryForeground(labelPrimaryLight)`, or the full `labelPrimaryLight` under
`increaseContrast`; `labelPrimary` under `increaseContrast` on a dark-foreground surface;
`labelSecondary` otherwise. On build `6cdb7306` the derived tone measures `[191,191,191]` on
`[40,40,40]`, 8.02 : 1. See the measurement under **PolishSurface**.

These are layout only — they paint no background, so they compose inside a `PolishSurface` without
creating a second one. `PolishSwitchRow` reports on/off through `stateDescription`;
`PolishSliderRow` always renders the current value as text, because a slider whose position is its
only feedback is unreadable at arm's length. Pass `kind` matching the surface they sit in so the
supporting line picks the right foreground.

Use `PolishSegmented` for two to four peer options on one line. Beyond that, a column of
`PolishSurface(onClick = ...)` rows reads better at headset distance.

## PolishField

```kotlin
@Composable fun PolishField(
    label: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null, supportingText: String? = null, errorMessage: String? = null,
    enabled: Boolean = true, readOnly: Boolean = false,
    minLines: Int = 1, maxLines: Int = Int.MAX_VALUE, showScrollIndicator: Boolean = false,
    kind: PolishSurfaceKind = LocalPolishSurfaceKind.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)

object PolishFieldDefaults {
    @Composable fun colorsFor(kind: PolishSurfaceKind, options: PolishOptions): TextFieldColors
    @Composable fun errorMessageColor(kind: PolishSurfaceKind, options: PolishOptions): Color
}
```

Wraps the built-in `TextArea`, which owns the shell, the focus behaviour, the selection toolbar, the
scroll indicator and the hover it correctly suppresses while focused. A non-null `errorMessage` puts
the field in its error state, renders the message in the `error` role, and sets the semantics error
on the node `TextArea` applies the caller's modifier to — narrower than the label/input/helper
column, but **not verified to be the node a screen reader focuses**: in 6.1.9 `TextArea` applies that
modifier to its outer wrapper, and the editable `BasicTextField` inside is a separate node. Treat the
announcement as unverified until a real screen reader has been observed on it. With no error the
supporting line keeps the same slot so validation does not shift the layout.

The field also carries its own palette, because `TextArea` does **not** read `LocalContentColor` — it
paints typed text with `colors.textColor`, whose default is `labelPrimary`, the dark label. On a
light-foreground surface `PolishFieldDefaults.colorsFor(kind, options)` gives it a **light shell**
(`fillSecondary`) and keeps the **dark** label on it, which is how a form field on a dark panel
normally reads and what the design system's own roles imply.

**The four numbers in this section are host-rendered JVM measurements, not device ones.** They come
from `evidence/2026-09-16-spatial-ui-polish-001/host-rendered/README.md` (`evidence/2026-09-16-spatial-ui-polish-001/host-rendered/README.md`)
and its `contrast-samples.json` — Robolectric's native graphics pipeline at a 1280×720 dp window
qualifier, where `systemColorScheme` resolves through `defaultColorScheme()` rather than the
Vibrant-encoded roles a compositor adapts. They describe `defaultColorScheme()`; they do not describe
the device. On a headset the same roles are resolved out of process against whatever is behind the
surface, and no device measurement of this field exists in any lane.

With that attribution: the first version paired the light shell with the light label and measured
**4.06:1**, short of the 4.5:1 normal text needs; `labelPrimary` on the same shell measures
**5.17:1**. The placeholder takes `labelSecondary` for the same reason supporting text does — and
there the evidence *is* from the device lane: `labelTertiary` measured 3.18–3.49:1 on rendered
emulator surfaces (`design-009-fix-detailed`, build `37452262`).

`errorMessageColor` is separate from the border colour on purpose. The `error` role carries the state
on the field's own border, where the 3:1 large-element target governs and it measures **4.10:1**
against the opaque fill — again host-rendered. The message underneath is normal text at 4.5:1, which
that same red does not reach, so on a light-foreground surface the message takes `labelPrimaryLight`
instead, which measures **14.74:1** on the same lane. The state is still not colour-only: the border
stays in the error role and the field carries the semantics error property.

## PolishWindowContent / PolishSplitLayout / PolishToolbar / PolishActionBar

```kotlin
@Composable fun PolishWindowContent(modifier, title, titleLeadingActions, titleTrailingActions, actions, contentPadding, verticalArrangement, content)
@Composable fun PolishSplitLayout(leading, modifier, leadingWeight, detail)
@Composable fun PolishToolbar(content: @Composable RowScope.() -> Unit)
@Composable fun PolishActionBar(modifier, horizontalArrangement, content)
```

`PolishWindowContent` composes content only — a `TitleBar`, a body and an optional action row. It
never creates, opens or closes a container and never touches the container's background. Apply
`windowConstraints(...)` yourself at the container call site; it is a member of the container scope
and cannot be reached from inside a plain composable.

`PolishToolbar` is a thin pass-through to the system `Toolbar`, which is a real bottom-anchored
companion window the platform materialises and positions — one per container, never one per item.
`PolishActionBar` is the in-window alternative for screens that should not spend it.

## Composition example — a selectable list beside a detail pane

```kotlin
PolishWindowContent(
    title = { Text("Library", style = PolishTypography.windowTitle) },
    titleTrailingActions = { PolishIconButton(onClick = onOpenSettings, contentDescription = "Settings") { … } },
) {
    PolishSplitLayout(
        leading = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
                items(items, key = { it.id }) { item ->                  // stable id, never the index
                    PolishSurface(
                        onClick = { onSelect(item.id) },
                        selected = item.id == selectedId,
                        kind = PolishSurfaceKind.Card,
                    ) {
                        Text(item.title, style = PolishTypography.objectTitle)
                        Text(item.subtitle, style = PolishTypography.caption,
                             color = PolishSurfaceDefaults.secondaryForegroundFor(PolishSurfaceKind.Card, LocalPolishOptions.current))
                    }
                }
            }
        },
        detail = {
            PolishSurface(kind = PolishSurfaceKind.Panel, modifier = Modifier.fillMaxSize()) { … }
        },
    )
}
```

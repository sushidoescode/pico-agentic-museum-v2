# Adoption — putting the kit into another app

Adoption is opt-in and additive. Nothing in an existing app changes until you change it, and the
repository's own generators keep emitting exactly what they emitted before.

## 1. Reference the module

The module is consumed from an explicit local path. Never copy its sources into an app: a copy
forks silently and the version in the changelog stops meaning anything.

```kotlin
// settings.gradle.kts
val polishDir: String =
    providers.gradleProperty("pico.polish.dir").orNull
        ?: providers.environmentVariable("PICO_POLISH_DIR").orNull
        ?: rootDir.resolve("<relative path to>/skills/spatial-ui-polish/resources/polish").canonicalPath
require(file(polishDir).resolve("build.gradle.kts").isFile) { "pico.polish.dir is wrong: $polishDir" }
include(":polish")
project(":polish").projectDir = file(polishDir)
```

```kotlin
// app/build.gradle.kts
dependencies { implementation(project(":polish")) }
```

```kotlin
// build.gradle.kts (root) — the module is an Android library using the Compose compiler
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false      // add this
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false     // add this
}
```

**Vendoring instead.** If the app must not depend on a path outside its own tree, copy the module
directory once, and record in the copy's README: the source commit, the module version from
`CHANGELOG.md`, and the SHA-256 of each file copied. A vendored copy without that provenance is
indistinguishable from a fork.

### Two forms the adoption planner can read statically

The read-only adoption report (next subsection) resolves the module reference without running
Gradle, so it recognises exactly two spellings of `project(":polish").projectDir`. The literal form:

```kotlin
include(":polish")
project(":polish").projectDir = file("../../skills/spatial-ui-polish/resources/polish")   // relative to this settings file, or absolute
```

and the property form:

```kotlin
include(":polish")
project(":polish").projectDir = file(providers.gradleProperty("pico.polish.dir").get())
```

with `pico.polish.dir=/absolute/or/settings-relative/path` in `gradle.properties`. The
`orNull ?: … ?: rootDir.resolve(…)` chain in the snippet at the top of this section is a convenience
the showcase uses; the planner reports it as **unresolved**, because which of its fallbacks applies
cannot be known without evaluating the settings script — that is, without running Gradle. Either
static form is a correct module reference; use one of them if you want the report to reach a verdict.

### Read-only adoption report

`node <package root>/scripts/polish-adoption.mjs --project <app dir> --kit <this skill's directory>`
— `<package root>` is the installed `pico-spatial-mcp` package that carries this skill. Exit codes:
**0** ready, **10** changes required, **20** unresolved, **30** invalid input. The report reads a
bounded allowlist of files under the two directories you name, runs nothing (no Gradle, no
`pico-cli`, no adb, no package manager, no project code, no network) and writes nothing. It is a
statement about what those files say, not a build or runtime claim: an app reported `ready` still has
to build, and still has to earn a frame.

## 2. Declare the catalog aliases the module reads

The module takes its dependency versions from the **consuming app's** version catalog, so the app
stays the single place where the SDK version is decided. It needs these aliases:

`spatial-bom`, `spatial-core`, `spatial-ui-platform`, `spatial-ui-foundation`, `spatial-ui-design`,
`androidx-annotation`, `junit`.

The showcase's catalog — started from the official `pico-cli project create` template, then edited
to pin 6.1.9 — declares all of them; check the catalog of the project you actually have, because what
a template emits is version-specific: `sdk-source-map.json` records that pico-cli 0.4.4's planar
template emitted `spatialBom = "6.0.0"` even when asked for 6.1.9, and this skill makes no claim
about what a later `pico-cli` emits. A missing alias fails configuration with a message naming it,
rather than resolving something unexpected.

Check the BOM the app actually resolves. The kit was written against `com.pico.spatial:bom:6.1.9`
and cites it line by line in `sdk-source-map.json`; on a different BOM, re-check the rows listed
under "Version drift" in `native-glass-contract.md` before trusting the contract.

## 3. Decide the container background once, per container

This is the decision that cannot be made at runtime.

- **Keep the system glass** (the default, and the right answer for most screens): change nothing.
  Do not call `polishWindowRoot()`. Do not paint on the root.
- **Own the root** (needed only if the app must switch the window opaque for reduced transparency):
  turn the system material off at declaration time — manifest
  `pico.spatial.windowcontainer.materialbackground="0"` inside the launcher `<activity>` for the
  `DefaultWindowContainer`, or `enableMaterialBackground = false` on a DSL `WindowContainer` /
  `Augment` — and then apply `Modifier.polishWindowRoot()` on the root box, with
  `polishWindowRootContentColor()` for any text placed directly on it.

  **What that modifier does now.** With `reduceTransparency` on it paints
  `PolishTokens.OpaqueGround`, the kit's one literal opaque colour (`0xFF282828`), and
  `polishWindowRootContentColor()` returns `labelPrimaryLight` to pair with it. Otherwise it
  re-applies `Material.Regular` — the tier the system uses for a window panel — and the content
  colour is `labelPrimary`. `Material.Thin` is deliberately not offered here: as a window material
  it maps to none.

  **It used to paint `ColorScheme.fillPrimary` in the reduced case, and that did not make the window
  opaque.** `fillPrimary` is a vibrancy sentinel the compositor resolves by *darkening what is
  behind the surface*, so a root painted with it still showed the room through itself — the one
  thing the setting exists to stop. What `fillPrimary` does was established on a `PolishSurface`, in
  four opened frames that vary only the backdrop behind it (`design-005-legibility-room`,
  `-006-bright`, `-007-dark`, `-008-detailed`, build `f91e4b7e`; see FRAMES-DESIGN.md at
  `evidence/2026-09-17-spatial-ui-polish-native-001/FRAMES-DESIGN.md`), and the root fix landed in
  build `37285f06`.

  **What is established for the root now.** The opaque arm is measured: `final-14-focus-rt` (build
  `a78d8109`, 17/17 gate checks, gate-admitted) exercises `polishWindowRoot()` under
  `reduceTransparency`, and its ground measures exactly **40.00** — the literal value of
  `OpaqueGround` — in `final-windowroot-pair.json`, which pairs it against the glass arm on the same
  build (`final-17-focus-glass`; the same root regions read 164.75–184.43 there). Both of those
  frames were **measured by tool, not opened and judged by eye**. The glass arm has been opened on an
  earlier build: `polish-focus-window` and `polish-focus-window-2` (build `be447bda`) show a
  translucent light panel with the main window readable through it.

  **Still unverified.** No frame of the root under reduced transparency has been opened and judged,
  and no headset frame of either arm exists — everything above is an emulator observation.

There is no third option. Painting over live system glass hides the material and collapses the
vibrant linkage; calling `polishWindowRoot()` while the system material is still on stacks two.

Most apps want reduced transparency to change their *surfaces*, not the window panel — that needs no
manifest change at all, because `PolishSurface` handles it.

Whichever you chose, you now have to **tell the kit** which one it was. That is step 4.

## 4. Wrap each container body in PolishTheme — and declare its windowSurface

```kotlin
fun mainApp(scope: SpatialAppScope) = with(scope) {
    DefaultWindowContainer {
        val options by settingsStore.polishOptions.collectAsStateWithLifecycle(PolishOptions())
        PolishTheme(
            options = options,
            windowSurface = PolishWindowSurface.SystemGlass,   // say which one, every time
            accent = BrandAccent,
        ) { AppRoot() }
    }
}
```

One `PolishTheme` per container body — a second window is a separate composition and needs its own.

### Why `windowSurface` is not optional in practice

The kit enforces a **glass budget**: at most `PolishTokens.MaxGlassDepth` = 2 compositor-material
layers in one window, *counting the window's own*. `PolishTheme` seeds that count from
`windowSurface`, every `PolishSurface` that takes a material increments it for its children, and a
surface that would exceed the budget quietly takes a drawn role fill and an edge instead.

The kit cannot work this out for itself. A container's material is resolved when the container is
**declared**, and there is no runtime read-back — so a wrong `windowSurface` is not caught anywhere:
it is a silent miscount in both directions. Declare it too low and one material too many stacks,
which is the defect the budget exists to prevent (in `polish-fb-glass-b.png`, build `be447bda`,
panel, card and window read as one flat sheet). Declare it too high and a surface that had room for
glass gives it up for no reason.

### The three values, and how to tell which one is yours

Read your **declaration**, not your runtime state:

| Value | Your container declared | Spends | Leaves the app |
|---|---|---:|---:|
| `SystemGlass` | no `materialbackground` meta-data at all, or `="1"`; a DSL container left at default or `enableMaterialBackground = true` | 1 | 1 |
| `AppGlass` | `materialbackground="0"` / `enableMaterialBackground = false`, **and** the root calls `Modifier.polishWindowRoot()` | 1 | 1 |
| `AppOpaque` | `materialbackground="0"` / `enableMaterialBackground = false`, **and** the root is an opaque fill — a backdrop fixture, or a product that simply does not want glass | 0 | 2 |

`SystemGlass` is the default and is right for almost every screen, because almost every screen takes
option one in step 3. `AppGlass` and `AppOpaque` are only reachable if you switched the material off
in the manifest or the DSL; if you did not, you cannot be in either of them, whatever your root
paints.

Two things that are easy to get backwards:

- **`AppGlass` still spends a layer.** The app is drawing the window's material instead of the
  system, not instead of nothing. `polishWindowRoot()` re-applies `Material.Regular`, the same tier
  the system uses for a window panel.
- **Do not compute `windowSurface` from `reduceTransparency`.** `PolishTheme` already zeroes the
  budget itself whenever reduced transparency is on, because every app surface goes opaque and
  spends no material at all. `windowSurface` states the container's *declaration*, which does not
  change when a setting does.

### Nested themes re-seed the budget

A nested `PolishTheme` — a comparison column, a preview, a dialog that re-themes part of a screen —
seeds the glass depth from **its own** `windowSurface` and does not inherit the depth already spent
above it. That is fine at the top of a container body and wrong halfway down one. If you nest a
theme inside a `PolishSurface` that took a material, pass the nested theme a `windowSurface` that
accounts for what is already spent, or move the nesting out.

(`accent` and `colorScheme` behave the other way round: `colorScheme` defaults to the *ambient*
scheme, so a nested theme keeps the accent the app already applied.)

## 5. Set every piece of text through PolishTypography

`PolishTypography` is the required way to set text in an adopting app. It is **nine semantic jobs**
— `display`, `windowTitle`, `sectionTitle`, `objectTitle`, `bodyLead()`, `body()`, `control`,
`caption`, `overline` — carried on **eleven rungs**: `bodyLead()` and `body()` each take a
single-line and a multiline variant, so `ladder()` returns eleven entries and a specimen renders
eleven. Each rung is pinned to one of the SDK's 18 typography roles, with the step between
consecutive jobs stated so the choice is checkable.

```kotlin
Text("Library", style = PolishTypography.windowTitle)
Text(item.title, style = PolishTypography.objectTitle)
Text(item.blurb, style = PolishTypography.body(multiline = true))
Text(item.note, style = PolishTypography.caption)
```

**A raw `PicoTheme.typography.*` role in app code is now a smell.** The SDK's 18 roles are
alphabetical, not a ramp: **four** are 16 sp (`bodyLarge`, `bodyLargeMultiline`, `headlineSmall`,
`titleMedium`), **four** are 14 sp (`bodyMedium`, `bodyMediumMultiline`, `labelLarge`,
`titleSmall`), two are 12 sp and two are 20 sp — and two of them, `labelLarge` and `titleSmall`, are
byte-identical, so a screen that uses one for a row title and the other for a button label has
expressed no distinction at all. Picking a role is not the same
act as picking a rank, and the ladder is where the ranking lives. Reach past it only for something
genuinely outside the nine jobs, and say why at the call site.

Two consequences worth knowing before you convert a screen:

- **The ladder has a floor at 12 sp.** The SDK's `labelSmall` (11 sp) and `bodyTiny` (10 sp) are
  deliberately not on it. They are the shell's dense chrome, not app content read at arm's length
  through a compositor.
- **Rank is carried by size *and* weight, never by one alone, and never by colour.** If your design
  distinguishes two levels only by making one greyer, it will not survive the contrast check in
  step 8.

`PolishTypography.ladder()` returns all eleven rungs in rank order, which is what a specimen screen
should render — see design-system.md (historical source citation) for the full table and the reasoning behind
each pinning.

## 6. Persist the options

The three accessibility flags are app-owned settings, and reduced motion in particular *has* to be:
the SDK pins the Compose motion duration scale to 1 at process start, so the system "remove
animations" preference does not reach SpatialUI animations. Persist them (DataStore is the usual
choice) and expose them in a settings screen; you may seed reduced motion once from
`Settings.Global.ANIMATOR_DURATION_SCALE == 0` as an advisory default.

**Reduced motion needs one more step from you.** The kit ships no animations of its own, so passing
the flag to `PolishTheme` changes nothing by itself — it carries the policy. Route every animation
*your app* writes through `PolishTokens.motion(reduceMotion, durationMillis)` instead of calling
`tween(...)` directly, the way the showcase's flow crossfade does. Reduced transparency and increased
contrast, by contrast, are honoured by the components themselves.

**Use `PolishTokens.spring(reduceMotion, args)` for springs rather than `args.toSpring()`.** The
SDK's motion *durations* fall back to their documented values when the design resources cannot be
read, but the *spring* tokens fall back to `SpringEasingArgs.Unspecific`, which is
`(dampingRatio = NaN, stiffness = NaN)` — and a NaN spring does not animate, it produces an
undefined value for the whole run. The wrapper validates the token before using it. Durations fail
safe; springs do not.

## 7. Replace surfaces incrementally

Adopt one screen at a time. For each surface you convert:

- A hand-rolled `Box(Modifier.background(...))` card → `PolishSurface`.
- A hand-rolled clickable card → `PolishSurface(onClick = ...)`; delete the hand-rolled hover, press
  animation and haptics with it.
- A hand-rolled invalid-state outline → `PolishSurface(errorMessage = ...)`, which moves the edge to
  the `error` role at the thicker weight *and* reports the message through the semantics `error`
  property. You still render the message as text: a surface that only turns red says nothing to
  someone who cannot see the red.
- A custom button → `PolishButton` with the tone that matches its meaning; delete any
  `spatialHoverEffect` on it.
- A `ListItem` inside your card → move it out; it fills itself.

The focus ring `PolishSurface` draws is **observed, never requested**: the kit adds no
`Modifier.focusable` and would not put your surfaces into a traversal order you did not ask for. It
reads the focus your own focus handling produces, so a surface that is never focused never shows it.
The design system ships no focus-visible treatment of any kind, so this one is the kit's own
addition.

It does render. `design-025-focus-ring` (build `6b19ca10`, opened and judged) is the first frame in
which the ring appears, after the showcase's gallery fixture was changed to re-emit its focus
interaction — a single `emit` had raced the collector, and `design-024-states-p2` (build
`90b10316`, opened) shows the same cell with no ring at all. Both are **emulator** frames: the ring
still has not been seen on a device, and nothing in this kit has been on a PICO headset. See
acceptance.md (historical source citation) for what that distinction costs and
interaction-contract.md (historical source citation) for the rest of the focus story.

## 8. Check your own colour pairings with PolishContrast

`PolishContrast` gives an adopting app the same WCAG 2 arithmetic the kit uses on itself:
`ratio(foreground, background)`, `ratioOver(foreground, background, backdrop)` for the usual case
where a translucent role fill sits over something, `composite(...)`, `targetFor(style)` and
`isLargeText(style)`.

**Read its refusals correctly, or it will do you more harm than good.** It returns
`PolishContrastResult.Undecidable(limit)` — never a number — when either colour is a vibrancy
sentinel, or when a translucent value has no stated backdrop to composite onto. That is not a pass
and it is not a failure. It means the app process genuinely does not hold the pixel: with vibrancy
on, 10 of the SDK's 16 `ColorScheme` roles read back as `#FF000000` in-app and the real colour is
produced out of process by the compositor. A checklist that ticks `Undecidable` green has recorded
the opposite of what happened.

Where it *is* exact: off the spatial platform (your JVM and Robolectric tests, previews, non-PICO
builds), for the six roles whose channels survive, and for colours your app owns itself. Those six
do not all survive for the same reason. `error`, `alert`, `passable` and `interaction` are literal in
both schemes; `labelPrimaryLight` and `dividerLine` are real colours tagged `Vibrant.None`, the level
that *disables* the effect, so their RGB stays authoritative. `PolishContrast` tells the cases apart
with `containsVibrant() && obtainVibrant() != Vibrant.None` — "will a level replace these channels",
not "is there a tag" — which is what lets those two through rather than refusing them. On a device,
contrast has to be measured from rendered pixels instead — see acceptance.md (historical source citation) for
how, and for the limits of that number too.

One target-selection detail that surprises people: only three of the eleven `PolishTypography` rungs
qualify for WCAG's 3:1 large-text allowance — `display`, `windowTitle` and `sectionTitle`. The kit
does not count W600 SemiBold as bold, so `objectTitle`, `control` and `overline` are held to 4.5:1.
That is deliberate and it is the stricter reading.

## 9. Verify before you call the screen done

1. `scripts/verify-design-style.sh <app src> <path to>/resources/polish/src/main` from the official
   `spatial-ui-design-style` skill, clean, with the real palette and no bypass flag.

   Pass **both** paths. R1 is a textual `require` for `PicoTheme(`; an app that follows step 4 calls
   `PolishTheme`, and the `PicoTheme` call it wraps lives in the module. Scanning app sources alone
   therefore reports `MISSING R1 PicoTheme wrapping is required` even though the app is compliant.
   No verifier rule is changed or bypassed — the module is simply part of the code being verified.

   Measured 2026-09-20 with verifier 0.4.1 over both Stage 1 consumers, the app + module run is clean:
   `errors: 0`, `warnings: 0`, `PASS`. Measured at 0.3.3 it reported two `R5 hardcoded color literal`
   errors from the module's `PolishTokens.kt` and none from either app, and no earlier run was retained; 0.3.4 resolved both by moving the
   existing `design-style: fixed-figma-color` annotation onto the literal's own line and rewording a KDoc
   example, with no change to `OpaqueGround`'s value. Any `[error]` under your own source path is yours.
2. `./gradlew :polish:testDebugUnitTest` plus your own app tests.
3. A rendered frame of the screen, opened and judged. See `acceptance.md` for what each class of
   frame proves.

A passing JVM test is specifically not enough for anything colour-shaped. The kit's own test
asserted that `ColorScheme.fillPrimary` was opaque, and it passed — because off the spatial platform
the scheme falls back to `defaultColorScheme()`, where that role really is a literal `0xFF282828`.
On a device it is a vibrancy sentinel the compositor resolves by darkening whatever is behind the
surface, and a frame over a checkerboard showed the checkerboard straight through it. The test was
right about the fallback and wrong about the device. Open a frame.

## What adoption does not do

It does not restyle screens you did not convert, does not change the repository's existing
generators, and does not make any other app "polished" by association. Each app earns its own
evidence.

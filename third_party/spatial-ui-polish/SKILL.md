---
name: spatial-ui-polish
description: Use when building or reviewing polished buttons, cards, panels, windows, toolbars, controls or settings screens with PICO SpatialUI (Spatial SDK 6.x Compose); when a request asks for Apple Liquid Glass, visionOS-style or "frosted glass" surfaces on PICO; when reduced motion, reduced transparency or increased contrast must be supported; when text rank, glass layering or contrast on a spatial surface has to be decided; or when a SpatialUI surface looks doubled, unreadable (black text on dark glass) or hovers twice.
---

# spatial-ui-polish

## Overview

Native PICO glass, built-in components and native interaction are the baseline. Polish is one
theme wrapper, one surface recipe and thin wrappers over built-ins — never a second material stack,
never a re-implemented button, never an optical effect the public API does not expose. Every
visual claim needs a rendered frame; code alone proves nothing about glass.

This skill is additive to the official PICO plugin. **REQUIRED BACKGROUND:**
`spatial-ui-design-style` (rules R1–R8 and `scripts/verify-design-style.sh`) and
`spatial-ui-ability` from `pico-spatial-agentic-tools`. Load them if the host has them; if the
host does not, say so instead of pretending they were applied. Where a vendor example and this
skill disagree, the disagreement is listed in references/native-glass-contract.md (historical source citation)
with the 6.1.9 source line that decides it.

## Quick reference

| Situation | Do | Do not |
|---|---|---|
| Window root (`DefaultWindowContainer`, `WindowContainer`, `Subwindow`, `Sheet`, `AlertDialog`) | Leave the system material alone; compose content only | Paint `background(...)` or `backgroundMaterial(...)` on the root; use `Material.Thin` as a window material (it maps to none) |
| App must switch a window opaque at runtime | Declare the switch off once (manifest `pico.spatial.windowcontainer.materialbackground="0"` for the launcher window, `enableMaterialBackground = false` for a DSL window) and own the root with `Modifier.polishWindowRoot()` | Stack an opaque box over glass that is still on |
| Glass layers in one window | Two, counting the window's own (`PolishTokens.MaxGlassDepth`); tell `PolishTheme(windowSurface = ...)` who owns the container background so the budget starts from the right number | Stack a card inside a panel inside a window — in an opened frame (`polish-fb-glass-b`, build `be447bda`) the three layers were not separable |
| Inner card / panel / control fill | `PolishSurface(kind = ...)` — one surface per element, clipped to one shape, material or fill, never both | Put a `ListItem` (it paints `fillLight` itself) or a second glass inside a painted card |
| Clickable card, row or panel | `PolishSurface(onClick = ...)` — it drops the material and draws `fillLight`, because that is the only way the hover highlight appears | Leave a clickable surface filled by `backgroundMaterial` and expect hover feedback — measured: none at all |
| Focus ring on a surface | It is *observed, never requested*: `PolishSurface` adds no `Modifier.focusable` and only reads the `interactionSource`, so a surface your app never focuses shows no ring. To render one for a specimen or a test, emit `FocusInteraction.Focus()` **repeatedly** — `MutableInteractionSource` publishes through a shared flow with no replay, so a single `emit` races `collectIsFocusedAsState`'s own `LaunchedEffect` and is lost if it wins | Assume the kit focuses anything, or that one `emit` is enough — it was not: `design-024-states-p2` (build `90b10316`) shows the focus cell with **no ring at all**, and `design-025-focus-ring` (build `6b19ca10`) is the first frame in this lane where the ring renders |
| Buttons | `PolishButton` / `PolishIconButton` (wrap the built-ins; hover, press, haptics, audio come from the built-in) | Add `spatialHoverEffect` on a built-in; re-implement a button with `clickable` |
| Ordinary `Text` on a plain surface | `PolishTheme` provides `LocalContentColor = labelPrimary`; explicit `color =` when outside it | Assume `PicoTheme` provides a foreground (it does not) |
| Choosing a text size | `PolishTypography` — nine semantic jobs (`display` … `overline`) across eleven rungs, mapped onto the SDK's 18 roles | Reach for a raw `PicoTheme.typography` role (they are alphabetical, not a ramp, and `labelLarge`/`titleSmall` are byte-identical); use `labelSmall` (11 sp) or `bodyTiny` (10 sp) for app content — the ladder floors at 12 sp |
| Supporting / secondary text | `PolishSurfaceDefaults.secondaryForegroundFor(kind, options)` — three branches, in code order: (1) on a light-foreground surface (`usesLightForeground`: an `Emphasis` surface, or any surface under `reduceTransparency`) the derived opaque tone `lightSecondaryForeground(labelPrimaryLight)` — `LightSecondaryStep` (0.70) of the way from `PolishTokens.OpaqueGround` to the light label — unless `increaseContrast` is on, in which case the full `labelPrimaryLight`; (2) `labelPrimary` under `increaseContrast` on a dark-foreground surface; (3) `labelSecondary` otherwise. Measured on build `6cdb7306`: the derived tone reads `[191,191,191]` on `[40,40,40]` = **8.02 : 1** (`contrast-ref-03-room.json` for Emphasis at default settings; `contrast-ref-04-rt-on.json` for all four surfaces under reduced transparency; `detail-08-surface-kinds-AFTER-2x` and `detail-09-rt-all-four-2x` opened). Call the function — do not copy the tone arithmetic into an app | `labelTertiary` — measured **3.18 / 3.26 / 3.49 : 1** on Panel / Card / Control from a rendered frame (`design-009-fix-detailed`, build `37452262`), against the 4.5:1 that 12 sp W500 needs |
| "This fill role is opaque" | `PolishTokens.OpaqueGround` (the literal `0xFF282828`) wherever non-transparency is the point | Call `ColorScheme.fillPrimary` opaque — it is `Color.Vibrant.withVibrant(Vibrant.Darker)`, which the compositor resolves by *darkening what is behind the surface*; a checkerboard stayed visible through it (`design-008-legibility-detailed`) |
| Checking contrast | Measure it from rendered device pixels against an admitted capture (references/design-system.md (historical source citation)); `PolishContrast` computes only where the values are real and returns `Undecidable` otherwise | Compute a ratio from role values on device — with vibrancy on, 10 of the 16 `ColorScheme` roles read back as `#FF000000` in the app process |
| Reduced transparency | `PolishOptions(reduceTransparency = true)`: every surface takes `PolishTokens.OpaqueGround` + `labelPrimaryLight`, and so does `Modifier.polishWindowRoot()` on an app-owned root. Measured on the emulator (build `6cdb7306`, `contrast-ref-04-rt-on.json`, frame `ref-04-legibility-rt-on-r1`, gate-admitted and measured; `detail-09-rt-all-four-2x` opened): the ground reads back as exactly `[40,40,40]` = `#282828` and the text is two tiers on all four surfaces — body `[255,255,255]` at **14.74 : 1**, supporting `[191,191,191]` at **8.02 : 1**. With `increaseContrast` also on, supporting returns to `[255,255,255]` / 14.74 : 1 by design (`contrast-ref-07-rt-ic.json`, frame `ref-07-legibility-rt-ic`). Dated history: build `ae4bc4ed`, frame `design-015-rt-on-room` (`contrast-design-015-rt-on.json`) measured all eight pairings at 14.74 : 1 — which was the collapsed second tier that the 0.3.2 refinement (design checkpoint §14) later fixed. An app-owned root under reduced transparency measured exactly 40.00 = `OpaqueGround` (`final-14-focus-rt`, build `a78d8109`, gate-admitted, **measured by tool, not opened**). Emulator observations only; no headset frame | Honour the setting with any vibrancy role — the compositor decides what it renders, so the background still shows through; and never call `fillSecondary` / `fillTertiary` / `fillLight` "opaque" (they are translucent roles) |
| Reduced motion | Carry `PolishOptions(reduceMotion = true)` and route **your own** animations through `PolishTokens.motion(reduceMotion, …)` | Assume the kit animates for you — it ships no animations — or expect the OS animator scale to reach SpatialUI (the SDK pins it to 1) |
| Any animation you write | `PolishTokens.motion(...)` / `PolishTokens.motionSpec(...)` for tweens and `PolishTokens.spring(...)` for springs — `MotionTokens` durations on the platform curve `bezierEasingStandard` | Bare `tween(...)`, which takes Compose's `FastOutSlowInEasing` — a different curve from the one every SDK component uses; or a raw `MotionTokens` spring token, which is fine until the resource lookup throws — then `springEasingArgsRes` catches and returns `SpringEasingArgs.Unspecific` = `(NaN, NaN)` (`ext/Resources.kt:107-109`), and a NaN spring produces an undefined value for the whole run instead of animating. `PolishTokens.spring(...)` validates the token first |
| "Liquid Glass refraction / lensing / blur radius / passthrough sampling" | Say it is unavailable in the public API; offer `Material.Thin` element glass, hover lift and Vibrant colour | Invent a refraction modifier or a blur parameter |
| "This modifier is applied, so the effect happens" | Check that the effect *rendered*: apply-site ≠ visible result, and this skill's own catalogue was wrong about exactly that until a frame disproved it | Read an effect's presence in source as evidence of its appearance |
| A screen you need a frame of | Fit it in the window, or **page** it with a counted action. On the PICO emulator there is no host route to a guest scroll — the wheel drives the camera and never reaches the app — so anything below the fold is a thing no capture can ever show. Three successive layouts overflowed the showcase's 1080x640 dp window and three frames showed it rather than argued it: one full-width column (`design-018-states-overlap`, build `37285f06`), six sections in two columns (`design-020-states-matrix-fit`, build `3f71458a`), and two columns cut to four sections (`design-021-states-page1`, build `2fbc4576`). `design-023-states-p1` (build `90b10316`) is the first that fits | Put the state you need to prove inside a scroller and call the capture evidence for it |
| Success claim | Cite an opened emulator or headset frame (and the wearer for depth/comfort) | Claim from a build, a test or a screenshot nobody looked at |
| Describing how something looks | Say it only with a frame behind it, and close by naming what you did not verify | Write "reads as frosted" / "holds against passthrough" from source alone — the measured residual failure of this skill |

## The component kit

`resources/polish/` is an Android library module (`dev.local.picopolish`). Consume it from an
explicit local path — never copy sources into an app:

```kotlin
// settings.gradle.kts of the app
include(":polish"); project(":polish").projectDir = file("<path to>/spatial-ui-polish/resources/polish")
```

The module needs the app's version catalog to define `spatial-bom`, `spatial-core`,
`spatial-ui-platform`, `spatial-ui-foundation`, `spatial-ui-design`, `androidx-annotation`, `junit`
(the showcase's catalog, started from the official `pico-cli project create` template, does; check
the catalog of the project you actually have — references/adoption.md (historical source citation) §2). Signatures, state contracts and
composition examples: references/component-catalog.md (historical source citation).
Adoption steps for a second app: references/adoption.md (historical source citation).
The type ladder, the glass budget, the opaque ground and the measured contrast numbers behind them:
references/design-system.md (historical source citation). What a click, a hover and a route do and do
not prove when the pointer is the emulator's synthetic one:
references/interaction-contract.md (historical source citation).

## Verification before saying "done"

1. `scripts/verify-design-style.sh <app src> <path to>/resources/polish/src/main` from the vendor
   skill — clean, no `--no-colors` bypass. **Pass the module path too.** R1 is a textual `require`
   for `PicoTheme(`, which an adopting app satisfies *inside* `PolishTheme`; scanning app sources
   alone reports a false `MISSING R1`. Both runs are retained in this repo's evidence.
   **Measured 2026-09-20 over two consumer apps, the app + module run is clean: `errors: 0`,
   `warnings: 0`, `PASS`.** It was not always. **Measured at 0.3.3**, the run reported two
   `R5 hardcoded color literal` errors, both inside the module's `PolishTokens.kt` and none in either app — the `OpaqueGround` literal,
   whose `design-style: fixed-figma-color` annotation sat one line above it while the verifier honours the
   hatch only on the literal's own line, and a KDoc line quoting the SDK value as a `Color(...)` call.
   0.3.4 moved the annotation and reworded the KDoc; `OpaqueGround`'s value never changed. Both source
   constructs go back to 0.3.2, so earlier versions very likely reported the same pair, but no verifier
   run was retained before 0.3.3 and that is an inference from the source, not a measurement. Read the
   report the same way regardless: any `[error]` under your own source path is yours to fix.
2. `./gradlew :polish:testDebugUnitTest` — the kit's behaviour tests (enabled/disabled/loading
   activation, option fallback, foreground provision, opaque ownership).
3. A frame. Emulator frames prove layout and state; only a headset frame proves depth, vibrant
   adaptation and passthrough legibility; only the wearer proves comfort and reachability.
   Evidence limits: references/acceptance.md (historical source citation).

4. **Close with the limits.** Name, in one or two lines, what you did not establish: which lane was
   not run, which claim has no frame behind it. Measured evaluation of this skill found that agents
   using it stopped inventing APIs and stopped doubling surfaces, but still wrote appearance claims
   like "reads as frosted" or "holds up against bright passthrough" with nothing rendered — and then
   said nothing about what was unverified. An unqualified appearance claim is the failure mode that
   survives every other rule here, so it gets its own step.

## Common mistakes (seen in no-skill baselines — and three this kit made itself)

- **Believing a role named "opaque" is opaque.** `ColorScheme.fillPrimary` is a vibrancy sentinel;
  four admitted frames varying only an app-drawn backdrop rendered the same surface as near-black,
  medium grey, near-black and a visible checkerboard (`design-005-legibility-room`, `-006-bright`,
  `-007-dark`, `-008-detailed`, build `f91e4b7e`). The kit's own JVM test asserted it *was* opaque and
  passed, because off the spatial platform the scheme falls back to `defaultColorScheme()` where
  `fillPrimary` is the literal `0xFF282828`. The test was right about the fallback and wrong about the
  device. Use `PolishTokens.OpaqueGround`.
- **Using a 3.9 % fill as a surface.** `PolishSurfaceKind.Control` drew `fillTertiary` in the native
  quality mode. Over a dark backdrop its own title, body and supporting line were black on near-black
  (`design-007-legibility-dark`); over a checkerboard the pattern read straight through the text
  (`design-008-legibility-detailed`). A tint cannot promise a ground: `Control` now takes
  `Material.Thin` in **both** quality modes, and `design-009-fix-detailed` shows the card legible —
  which also settles the open question of whether `Material.Thin` renders on the view path. It does.
- Reduced-transparency card filled with `fillSecondary` — still 40 % translucent.
- Card paints a material, then a `ListItem` paints `fillLight` inside it — doubled surface.
- `backgroundMaterial(...)` + `vibrantEffect(...)` + `background(Color.Vibrant)` on one node — the
  vibrant sentinel is not a paint colour; one surface, one fill.
- `alpha(...)` applied after the background instead of on the whole node (built-ins apply disabled
  alpha through `graphicsLayer` before hover).
- Describing `enableMaterialBackground` as `Boolean = true`; it is `Boolean? = null` and inherits the
  manifest value. Same outcome, wrong claim.
- **Reading a platform log line as an SDK type.** `ViewRootImpl`'s `Space Info` line carries a `kind=`
  field and a set of `*Event3D` structs; those are the PICO platform's own input plumbing, not the
  Spatial SDK's public API. 70 of 70 host clicks in this lane logged `kind=GAZE_PINCH`
  (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/logs/route-probe-spaceinfo.log`, build
  `ae4bc4ed`), which establishes what the *platform* labels a host click — and nothing about what a
  Spatial SDK gesture detector would report, because `KIND.toInteractionKind()` and
  `SpaceInteractionKind.toInteractionKind()` are both `internal`. Never call `TapEvent3D` an SDK type,
  and never report `kind=GAZE_PINCH` as an observation of `InteractionKind`.
- Grounding a 6.1.9 target in 6.0 docs without saying so. Cite
  references/sdk-source-map.json (historical source citation) or mark the API unverified.

Version: see CHANGELOG.md (historical source citation). Upstream dependency observed: `pico-spatial-agentic-tools`
0.4.1 (Claude host) / 0.5.0 (Codex host); the rules referenced are R1–R8 of `spatial-ui-design-style`.

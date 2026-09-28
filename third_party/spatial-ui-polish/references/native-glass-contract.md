# Native glass contract — supported materials, layer ownership, version caveats

Every statement below names the 6.1.9 source that decides it (member:lines) as recorded in
sdk-source-map.json (historical source citation). "Declared" means the declaration exists in the resolved
6.1.9 artifacts; it does not mean the effect has been seen rendered. The checkpoint lists which rows
have native evidence.

## 1. Two material paths, not one

| Path | API | Values that render | Owner |
|---|---|---|---|
| Window material (the panel behind a whole container) | container declaration: manifest `pico.spatial.windowcontainer.materialbackground`, DSL `enableMaterialBackground`, `Augment(enableMaterialBackground)`, and the fixed choices inside `Subwindow` (Regular), `Toolbar` (Regular), `AlertDialog`/`Sheet`/`SpatialPopup`/`Menu` (Thick), `Snackbar` (Thickest) | `Regular`, `Thick`, `Thickest`; `Thin` and `None` map to 0 (`platform/Material.kt:41-48`) | the system, per container |
| View material (a material behind one Compose node) | `Modifier.backgroundMaterial(enable, style)` (`foundation/material/BackgroundMaterial.kt:44-51`) | `Regular` 1, `Thick` 2, `Thickest` 3, `Thin` 4 (`:127-134`); no-op unless `SpatialBuild.isSpatialPlatform()` or a debug core build (`:123-125`) | the app, per node |

Consequences:

- `Material.Thin` is an **element** material ("Box, Button, CheckBox"; `platform/Material.kt:34-38`).
  It is not a window material and it draws nothing in the non-SpaceOS fallback
  (`foundation/window/WindowMaterials.kt:39-45`). The vendor `window-background.md` example that
  re-applies `Thin` on a window root after switching the system glass off is not carried into this skill.
- `backgroundMaterial` consumes one pixel of depth and is a layout node; clip the node to its shape
  before applying it so border, visual bounds and hit bounds agree (`BackgroundMaterial.kt:69-121`;
  vendor ability example omits the clip).
- The modifier exposes `enable` and `style` only. There is no blur radius, refraction, lensing,
  passthrough/backdrop sampling or specular parameter anywhere in the resolved public API.

## 2. Effective default of the window switch

`WindowContainer(enableMaterialBackground: Boolean? = null)` (`foundation/dsl/WindowContainer.kt:106`,
`WindowContainerProperties.kt:43,65`) means **inherit**: core merges a null DSL value with the target
Activity's manifest meta-data, whose default is REGULAR (`core/container/SpatialContainerManager.kt:1194-1195,1229-1230`;
`SCParamsExtractor.kt:201,307`; `ContainerMaterialBackground.kt:16-41`). The platform then provides
`LocalIsMaterialBackgroundEnabled = isSupportWindowMaterial && (value ?: true)`
(`platform/containers/SUIWindowContainer.kt:82-84`) and applies `Material.Regular` to the Activity
window when the merged value is true (`SpatialActivityDelegateImpl.kt:169-176`). So "true by default"
is the effective behaviour; the declared type is nullable. `Augment` is a plain `Boolean = true`
(`foundation/window/Augments.kt:85,138`).

Rule: to own a root background, flip the switch that applies to that container **at declaration
time** (manifest for the launcher `DefaultWindowContainer`, DSL for others), then own the root with
exactly one background. There is no runtime toggle of the launcher window's system glass.

## 3. Foreground provision

`PicoTheme` provides colour scheme, typography, `LocalIndication` and tokens — not `LocalContentColor`
(`design/PicoTheme.kt:60-65`). `LocalContentColor` defaults to `Color.Unspecified`
(`design/CompositionLocals.kt:29`); `Text` falls through explicit colour → style colour → that local
(`design/Text.kt:101-108`), and `DefaultTextStyle` carries no colour (`tokens/TypographyTokens.kt:26-27`).
An ordinary `Text("…")` on a plain surface inside `PicoTheme` therefore renders the framework
default (black). Built-ins provide their own foreground (`Button.kt:191`, `ListItem.kt:166`,
`AlertDialog.kt:263`). `PolishTheme` provides `LocalContentColor = labelPrimary` and
`LocalTextStyle = bodyMedium`; every `PolishSurface` re-provides the foreground that pairs with its
own fill.

## 4. Hover ownership

These built-ins apply `spatialHoverEffect` internally; a wrapper must not add another:
`Button` (`:179`), `IconButton` (through `BasicButton`), `ToggleButton`, `ButtonChip` /
`ToggleableChip` / `RemovableChip` (`Chips.kt:188,304,440`), `CheckBox` (`:149`), `Switch` (`:254`),
`Slider` family (`:664,799`), `TextField`/`TextArea` (`:831`), `SegmentItem` (`:149`), `ListItem`
(`:72`), `Option` (`:89`), `PageControl`, `ScrollIndicator`, `DatePicker`/`TimePicker`/`NumberField`,
`MenuItem`, `TabBar`. There is **no `Card` built-in** in design 6.1.9; the vendor `hover.md` Card
example names a component that does not exist here. Custom surfaces that are not built-ins may apply
`spatialHoverEffect` exactly once, before `clickable`.

**Applying it is not the same as getting it.** A custom surface whose only fill is
`Modifier.backgroundMaterial` applies `spatialHoverEffect` and shows nothing: on the emulator the
capture taken while hovering such a surface was byte-identical to the frame before it, while a
built-in `Button` in the same app and the same glass configuration moved +7.14 mean gray. Give the
surface a drawn fill and the same hover at the same coordinates moves +16.92. The SDK's own
material-aware hover, `materialSpatialHoverEffect` (`SpatialMaterialHoverEffect.kt:46`), is
`internal` and `windows/TabBar.kt:258` is its only caller, so there is no public route to it — which
is why every interactive built-in fills with a role colour instead. **An interactive surface draws
its own fill; only a static surface keeps a material.** Emulator-measured; no headset has confirmed
it.

## 5. Colour roles and the accent

`ColorScheme` has exactly 16 roles (`design/ColorScheme.kt:49-67`). There is no `accent` / `onAccent`
(the 6.0 vault theme doc is stale on this; the vendor verifier R8 rejects them). The configurable
accent is `interaction` (fixed DisplayP3 semantic colour, default `#3377FF`), overridden with
`systemColorScheme(context).copy(interaction = …)`; its foreground is `labelPrimaryLight`.

Opaque versus translucent roles on the spatial platform (`ColorScheme.kt:230-249`):

| Role | Encoding | Use in the kit |
|---|---|---|
| `fillPrimary` | `Vibrant.Darker` (default scheme: opaque `#282828`) | the owned opaque surface / standard button fill, foreground `labelPrimaryLight` |
| `fillSecondary` | `Vibrant.SemiLight` (default `#66FFFFFF`) | translucent emphasis fill, foreground `labelPrimary` |
| `fillTertiary` | `Vibrant.Neutral` (default `#0A000000`) | quiet control fill |
| `fillLight` | `Vibrant.Light` (default `#26FFFFFF`) | list rows (what `ListItem` paints) |
| `labelPrimary…Quaternary` | `Darkest…Dark` | foreground on glass and light fills |
| `labelPrimaryLight` | fixed white | foreground on `fillPrimary`, `interaction`, `error` |
| `dividerLine` | fixed `#1F000000` | hairlines; too weak for increased-contrast borders (use `labelQuaternary`) |

Because the scheme has exactly one opaque fill role, a surface that goes opaque loses the fill
contrast it had and must carry its hierarchy in the edge and the spacing instead — and the edge has
to invert with the fill, or it is dark-on-dark. The kit routes all three decisions (foreground,
secondary foreground, edge colour) through one predicate so they cannot drift apart.

Vibrant colours are resolved by the compositor against the material behind them; they read
correctly only through translucent surfaces. Whether `Vibrant.Darker` renders fully opaque on the
headset is a native check the checkpoint must record.

## 6. Motion

`MotionTokens` durations are public (`tokens/MotionTokens.kt:76-106`): short 120/180/240, medium
300/360/420/1000, long 540–720. The kit's defaults (feedback 120–180 ms, panels 240 ms, no
overshoot, ≤ 8 dp translation) sit on the short tier. The SDK pins the Compose motion duration scale
to 1 from a ContentProvider at process start (`design/SpatialUIProvider.kt:22-28`,
`utils/MotionScaleHelper.kt:23-33`), so an OS "remove animations" setting does not reach SpatialUI
animations: reduced motion is an app-owned option; the kit may seed it from
`Settings.Global.ANIMATOR_DURATION_SCALE == 0` as an advisory default.

## 7. Fixed by the system

- Window corner radius (32 dp visual) and window shape: no DSL/manifest entry (6.0 vault FAQ on
  window corners). Only the glass panel can be disabled.
- `Subwindow`: width 360 dp, gap 24 dp, corner 32 dp, Regular material (`Subwindow.kt:74,131-135`).
- `Toolbar`: bottom-anchored Augment, Regular material, Level12 (`Toolbar.kt:85`).
- Dialog-class surfaces: `Thick`, modal, Level3 (`AlertDialog.kt:160`), sheets Level23 (`Sheets.kt:225`).

## 8. Version drift to re-check on every SDK change

Recheck these when the resolved BOM changes: the role count of `ColorScheme`, the `Material` enum
and both mapping functions, the `enableMaterialBackground` type and merge, `LocalContentColor`'s
provider, the internal-hover list, and `MotionScaleHelper`. Update `sdk-source-map.json` first; this
file cites it.

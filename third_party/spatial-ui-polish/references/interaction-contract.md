# Interaction contract — what the SDK routes, what it cancels, and what the app must own

## 1. What this page is, and what it is worth

The companion to native-glass-contract.md (historical source citation), for input instead of
appearance. It says which hand and controller interaction the public 6.1.9 API actually exposes,
which of it is `internal` and therefore a compile error, and which behaviour the SDK simply does not
have — so a kit that needs it has to own it in product design rather than look for a modifier.

Every statement names the source line that decides it, with **one marked exception**: the "Reaches
the app as" column of §2's route table, which is inference from constant names and is flagged as such
at the table. Those lines were read from the sources jars
that `com.pico.spatial:bom:6.1.9` resolves in the local Gradle cache. Prefixes:

| Prefix | Archive | Note |
|---|---|---|
| `design/`, `foundation/`, `platform/`, `core/` | `com.pico.spatial.ui:{design,platform,foundation}:6.1.9`, `com.pico.spatial.core:core:6.1.9` | the four archives sdk-source-map.json (historical source citation) records |
| `ui/` | `com.pico.spatial.ui:ui-android:1.2.5` | the Compose **ui** fork — `1.2.5`, not 6.1.9; it is what `foundation-6.1.9.pom` depends on |
| `fnd/` | `com.pico.spatial.ui.foundation:foundation-android:1.2.5` | the Compose **foundation** fork (`clickable`, `draggable`, `PressInteraction`) |
| `tracking/` | `com.pico.spatial.tracking:tracking:6.1.9` | the only raw hand/controller feed |

Three of those seven archives are outside `sdk-source-map.json`, which was built for the glass
contract and lists only design/foundation/platform/core. Half the interaction surface lives in the
two forked Compose artifacts, and their version string is `1.2.5`: do not describe an
`InteractionKindExtra` or a `ViewConfiguration` fact as "a 6.1.9 API" without that qualifier.

**Nothing on this page has been verified on a headset in this lane**, and nothing on it has been
verified on the emulator either except where a line says so. It is source-derived — the lowest class
on the ladder in acceptance.md (historical source citation). It tells you what will compile and what the code
does; it does not tell you what a wearer will feel.

**Carried-forward open question — route information may be empty under emulator verification.**
`detectSpatialTapGesture` falls back to `InteractionKind.Unknown` when the down or up event carries
no spatial pointer info, under the comment `// maybe on none PICO OS`
(`foundation/gesture/SpatialTapGestureDetector.kt:97-107`). Whether the emulator's synthetic pointer
populates `SpacePointerInfo` at all was not established. If it does not, every route-discrimination
branch in §2 collapses to `Unknown` on the emulator, a routing bug stays invisible there, and only a
real headset run would surface it. Do not write an emulator frame up as evidence that route
discrimination works.

## 2. The three routes, and how to tell them apart

The coarse discriminator is a public 6-value enum, verbatim
(`foundation/gesture/data/InteractionKind.kt:22-39`):

```kotlin
enum class InteractionKind {
    Unknown,        // :24
    DirectPinch,    // :27
    Poke,           // :30
    GazePinch,      // :33
    RayBasedPinch,  // :36
    Pointer,        // :39
}
```

| Route | Reaches the app as | What is additionally available |
|---|---|---|
| **Direct touch** — a finger or a controller tip touching the panel | `Poke`, and `DirectPinch` for a pinch made in touch range | nothing route-specific; it is an ordinary press |
| **Hand ray + pinch** — far interaction | `RayBasedPinch`, and `GazePinch` for eye-hand | `SpatialDragValue.inputDevicePose` gives the ray origin and orientation as `InputDevicePose(rawPosition, rawRotation)` (`foundation/gesture/SpatialDragGestureDetector.kt:303`, type at `gesture/data/InputDevicePose.kt:25`) |
| **Controller** — ray plus trigger | `Pointer`, or `RayBasedPinch`, depending on the interactor | haptics (§9); raw trigger state only through `ControllerTrackingProvider.addControllerActionListener` (`tracking/controller/ControllerTrackingProvider.kt:85`), which is `@RequiredFullSpace` and therefore unavailable to a Mixed-stage panel (§7) |

⚠ **The "Reaches the app as" column is the one thing on this page with no deciding line — it is §1's
one marked exception.** No source in the extracted trees maps a physical route to `Poke`,
`DirectPinch`, `RayBasedPinch` or `Pointer`. The only mappings that exist are platform-enum to
SDK-enum — `KIND.toInteractionKind()` (`InteractionKind.kt:42-58`) and
`SpaceInteractionKind.toInteractionKind()` (`:60-76`) — and those carry the same names on both
sides, so they say nothing about which hardware produced which constant (grep for
`InteractionKind.Poke|DirectPinch|RayBasedPinch|Pointer|GazePinch` outside that one file → 0 hits).
The column is **inference** from the constant names plus the `InteractionKindExtra` →
`InteractionModality` mapping (`:86-129`). It was confirmed from neither source nor a runtime
observation. Treat it as a hypothesis to check on a headset, not as a contract.

Every `Spatial*Value` carries the coarse kind and nothing finer, and the value classes split three
and three on handedness.

**One kind for the whole gesture:** `SpatialTapValue.interactionKind`
(`SpatialTapGestureDetector.kt:157`), `SpatialDragValue.interactionKind` (`:302`),
`SpatialPointerInfo.kind` (`SpatialPointerEventDetector.kt:171`).

**A kind per side** — each of these declares `leftInteractionKind` *and* `rightInteractionKind`,
because each is a two-pointer gesture: `SpatialRotateValue`
(`SpatialRotateGestureDetector.kt:261-262`, KDoc `:253-254`), `SpatialScaleValue`
(`SpatialScaleGestureDetector.kt:270-271`, KDoc `:260-263`) and `SpatialTransformValue`
(`SpatialTransformGestureDetector.kt:413-414`, KDoc `:403-404`). Rotate is not special here; the
§3 rows say the same thing.

### The finer layer

Reachable only from a raw `PointerInputChange`, i.e. from
`pointerInput { awaitPointerEventScope { … } }` — the `Spatial*Value` classes do not expose it.

| API | file:line | What it is |
|---|---|---|
| `InteractionKindExtra` | `ui/commonMain/com/pico/spatial/ui/input/pointer/InteractionKindExtra.kt:19` | public enum, **39 constants** — `Unknown` at `:20` through `HandPointerHoverInteractorRight` at `:58`. (38 is the count of named interactors *excluding* `Unknown`; say 39 or say "38 interactors plus `Unknown`", not "38 values".) |
| `InteractionModality` | `InteractionKindExtra.kt:65` | public enum: `GazeHand`, `HeadHand`, `Hand`, `Controller`, `Pointer`, `Unknown` (`:67-77`) |
| `InteractionKindExtra.toInteractionModality()` | `InteractionKindExtra.kt:84` | public; the exhaustive `when` at `:86-129` |
| `PointerInputChange.interactionKindExtra` | `ui/androidMain/com/pico/spatial/ui/input/pointer/PointerInteractionExt.kt:20` | public extension property |
| `PointerInputChange.readInteractionKindExtra()` | `ui/commonMain/…/PointerEventExt.kt:200` | public extension function |
| `PointerInputChange.position3d()` / `.previousPosition3d()` | `PointerEventExt.kt:207` / `:204` | public; `Offset3D` |
| `InteractionKindExtra.toHandController()` | `platform/SpatialHandControllerHaptic.kt:278` | public; returns `HandController.Left`/`.Right` for **8** of the 39 constants and `Unknown` for the other 31 (`when` at `:279-289`, `else` at `:288`) — see the coverage gap below |

`InteractionModality` exists for exactly one discrimination the coarse enum cannot make. KDoc,
`InteractionKindExtra.kt:62-63`: *"used to tell apart head-hand (HMD ray + pinch) from eye-hand
(gaze + pinch) and other input sources."* `GazeHand` covers the five gaze interactors (`:86-90`);
`HeadHand` covers the six Hmd/head interactors (`:92-97`).

**The naming trap.** The SDK spells a bare hand **`Hand`** and a controller **`Handle`**.
`HandDirectPokeInteractorLeft` (`:27`) is a finger; `HandleDirectPokeInteractorLeft` (`:31`) is a
controller tip. Fourteen `Handle*` constants map to `InteractionModality.Controller` (`:110-123`);
the two `GazeHandle*` constants map to `GazeHand` instead (`:88-89`) and are correctly not in that
fourteen. Two of the `Handle*` values are poke interactors, so **a controller can produce the direct
touch route** — treating `Poke` as "hand" is wrong.

**`toHandController()` covers less than half the controller surface, and the gap is the far ray.**
The `when` at `SpatialHandControllerHaptic.kt:279-289` names exactly eight constants:
`GazeHandleInteractorLeft`/`Right` (`:280`, `:284`), `HandleDirectInteractorLeft`/`Right`
(`:281`, `:285`), `HandleDirectPokeInteractorLeft`/`Right` (`:282`, `:286`) and
`HandleInteractorLeft`/`Right` (`:283`, `:287`). Everything else falls through to
`else -> HandController.Unknown` (`:288`). Eight of the fourteen constants this page lists as
`InteractionModality.Controller` (`:110-123`) are in that remainder:
`HandleScrollInteractorLeft`/`Right`, `HandleDirectScrollInteractorLeft`/`Right`,
`HandleDirectCollisionInteractorLeft`/`Right` and `HandleRayInteractorLeft`/`Right`.

`ControllerHapticFeedback` then filters `Unknown` out of the press list
(`foundation/haptic/ControllerHapticFeedback.kt:132`,
`pressedChanges.filter { it != HandController.Unknown }`). **So a press that arrives as
`HandleRayInteractorLeft`/`Right` — the far-ray controller route, the one a wearer uses across a
room — resolves to `Unknown` and produces no haptic at all.** That is a design fact, not a citation
detail: a kit cannot promise "every controller press buzzes", and §9's rule that feedback is never
haptics-only has a second reason on top of the system vibration setting. Which physical input
actually emits `HandleRayInteractor*` was not observed here, so the size of the hole is unmeasured;
the mapping gap itself is read straight from the `when`.

**Require an `else` branch.** The two logging functions are `KIND.toInteractionKind()`
(`foundation/gesture/data/InteractionKind.kt:42`, log at `:53`) and
`SpaceInteractionKind.toInteractionKind()` (`:60`, log at `:71`) — both `internal` extensions on
the *platform* types, not a method on `InteractionKind`. They log *"unknown interaction kind:
$this, please upgrade spatial sdk"* and return `InteractionKind.Unknown`, which means an unknown
platform value is already collapsed before the app sees it: that log does **not** justify an
`else -> Unknown` in app code. The real reason to write one is different — `InteractionKind` and
`InteractionKindExtra` carry no `@Keep` and no experimental marker, nothing promises the constant
set is fixed, and a `when` compiled exhaustively against today's 6 or 39 constants throws
`NoWhenBranchException` at runtime if a later SDK adds one. Terminate with `else -> Unknown` so an
already-shipped build degrades instead of crashing.

## 3. The public detector table

All of these live in `com.pico.spatial.ui.foundation.gesture` and are public.

| Detector | file:line | Cancel callback | Value class carries |
|---|---|---|---|
| `detectSpatialTapGesture` | `SpatialTapGestureDetector.kt:52` | **none** (§6) | `SpatialTapValue` (`:155`): `position` (`Offset3D`), `interactionKind`, `targetEntity` |
| `detectSpatialDragGesture` | `SpatialDragGestureDetector.kt:64` | `onDragCancel: (SpatialDragValue) -> Unit` (`:69`) | `SpatialDragValue` (`:300`): `dragAmount` — a 3D delta since the last event **in pixels** (KDoc `:288-291`) — `interactionKind`, `inputDevicePose`, `targetEntity` |
| `detectSpatialRotateGesture` | `SpatialRotateGestureDetector.kt:64` | `onRotateCancel` (`:70`) | `SpatialRotateValue` (`:258`): `centroid`, `rotation`, `leftInteractionKind` **and** `rightInteractionKind` (`:261-262`), `targetEntity` |
| `detectSpatialScaleGesture` | `SpatialScaleGestureDetector.kt:62` | `onScaleCancel` (`:67`) | `SpatialScaleValue` (`:267`): `centroid`, `scaleValue`, `leftInteractionKind` **and** `rightInteractionKind` (`:270-271`), `targetEntity` |
| `detectSpatialTransformGesture` | `SpatialTransformGestureDetector.kt:56` | `onTransformCancel: () -> Unit` (`:62`) — **carries no value at all** | `SpatialTransformValue` (`:408`): `centroid`, `dragAmount`, `scaleValue`, `rotation`, `leftInteractionKind` **and** `rightInteractionKind` (`:413-414`), `targetEntity`; combined drag + rotate + scale |
| `detectSpatialPointerEvent` | `SpatialPointerEventDetector.kt:60` | n/a — raw stream | `SpatialPointerInfo` (`:133`, `@Stable`): `x/y/z`, `position3D`, `pointerId`, `pressed`, `uptimeMillis`, `targetedEntity`, `kind` (`:171`), `inputDevicePose` (`:181`), `isDownEvent()`, `isUpEvent()` |

Entity-aware wrappers over the stock Compose detectors, same package, also public — 2D `Offset`
deltas. **Only the plain `detectDragGestures` wrapper opts into touch slop**
(`ComposeDragGestureDetectorExt.kt:74`, `shouldAwaitTouchSlop = { true }`); that flag is passed
nowhere else in the file. The directional wrappers delegate to detectors that await *directional
pointer* slop instead — `awaitHorizontalPointerSlopOrCancellation`
(`spatialui/x/foundation/gestures/DragGestureDetector.kt:765`) and
`awaitVerticalPointerSlopOrCancellation` (`:581`) — and
`detectDragGesturesAfterLongPress` gates on `awaitLongPressOrCancellation` (`:391`) with no slop at
all. "The 2D wrappers use slop" is true of `detectDragGestures` and needs a qualifier for the rest:

| Wrapper | file:line | What it waits for |
|---|---|---|
| `detectTapGestures` | `ComposeTapGestureDetectorExt.kt:67` | n/a — tap |
| `detectDragGestures` | `ComposeDragGestureDetectorExt.kt:62` | **touch slop** (`shouldAwaitTouchSlop = { true }`, `:74`) |
| `detectHorizontalDragGestures` | `ComposeDragGestureDetectorExt.kt:114` | horizontal pointer slop (`DragGestureDetector.kt:765`) |
| `detectVerticalDragGestures` | `ComposeDragGestureDetectorExt.kt:164` | vertical pointer slop (`DragGestureDetector.kt:581`) |
| `detectDragGesturesAfterLongPress` | `ComposeDragGestureDetectorExt.kt:208` | a long press, **no slop** (`DragGestureDetector.kt:391`) |

Binding a gesture to ECS entities: `TargetEntity` is a public sealed class with a private
constructor and `internal` subclasses (`TargetEntity.kt:21`); construct it through
`TargetEntity.hit(entity)` (`:89`), `TargetEntity.any { … }` (`:98`) or `TargetEntity.any()` (`:105`).

**The package that looks right and is not.** Every symbol under
`spatialui/x/foundation/gestures/` is `internal` — `detectDragGestures` (`DragGestureDetector.kt:181`
and the overload at `:242`), `detectDragGesturesAfterLongPress` (`:376`), `detectVerticalDragGestures`
(`:563`), `detectHorizontalDragGestures` (`:747`), `awaitTouchSlopOrCancellation` (`:86`),
`awaitLongPressOrCancellation` (`:1077`), `detectTapGestures` (`TapGestureDetector.kt:108`),
`waitForUpOrCancellation` (`:401`), `LongPressResult` (`:492`). The package holds two files and no
public top-level declaration at all. These are the implementations the wrappers above delegate to;
importing one does not compile. Also internal, inside the PICO gesture package:
`SpatialPointerEventVerifier` (`GestureHelper.kt:261`), `createEventVerifier` (`:266`),
`isRuntimeTapSuccess` (`GestureHelper.kt:138`), `RuntimeDragData`
(`SpatialDragGestureDetector.kt:332`), `RuntimeRotateData` (`SpatialRotateGestureDetector.kt:225`),
`RuntimeScaleData` (`SpatialScaleGestureDetector.kt:303`).

## 4. Targeting

The only touch-target mechanism is the Compose fork's, inherited unchanged from upstream and public
(`ui/commonMain/androidx/compose/ui/platform/ViewConfiguration.kt:53-54`):

```kotlin
val minimumTouchTargetSize: DpSize
    get() = DpSize(48.dp, 48.dp)
```

KDoc `:49-52`: *"The minimum touch target size. If layout has reduced the pointer input bounds below
this, the touch target will be expanded evenly around the layout to ensure that it is at least this
big."* The expansion is applied in `fnd/foundation/Clickable.kt:898-902` and
`ui/…/SuspendingPointerInputFilter.kt:592-595`. It is an interface default; no override exists in
`ui/androidMain` (grep `minimumTouchTargetSize` over that tree → 0 hits), so 48×48 dp is what ships
unless a platform `ViewConfiguration` outside these sources overrides it.

**Say the consequence precisely: the hit area is expanded, the visible affordance is not.** A 32 dp
control gets 48×48 dp of pointer bounds and stays a 32 dp thing to look at and to aim a ray at from
two metres. Several shipped sizes are below the minimum:

| Constant | minWidth × minHeight | file:line |
|---|---|---|
| `ButtonDefaults.Max` | 104 × 56 | `design/Button.kt:309` |
| `ButtonDefaults.Regular` | 81 × 48 | `Button.kt:312` |
| `ButtonDefaults.Small` | 69 × **40** ⚠ | `Button.kt:315` |
| `ButtonDefaults.Min` | 57 × **32** ⚠ | `Button.kt:318` |
| `IconButtonDefaults.Min` | **38** ⚠ × **32** ⚠ | `design/IconButton.kt:108` |
| `IconButtonDefaults.Small` | **46** ⚠ × **40** ⚠ | `IconButton.kt:111` |
| `IconButtonDefaults.Regular` | 56 × 48 | `IconButton.kt:114` |
| `IconButtonDefaults.Max` | 66 × 56 | `IconButton.kt:117` |
| `ToggleButtonDefaults.Max / Regular / Small / Min` | aliases of `ButtonDefaults.Max` / **`Min`** / `Small` / `Min` | `design/ToggleButton.kt:110,113,116,119` |
| `ToggleIconButtonDefaults.Min / Small / Regular / Max` | aliases of the `IconButtonDefaults` of the same name | `design/ToggleIconButton.kt:79,82,85,88` |
| `ChipsDefaults.Small` | `chipSize(32.dp)` ⚠ | `design/Chips.kt:589` |
| `ChipsDefaults.Regular` | `chipSize(40.dp)` ⚠ | `Chips.kt:592` |
| `CheckBoxDefaults.CheckboxSize` | `32.dp` ⚠ | `design/CheckBox.kt:328` |

⚠ = below 48 dp. Note the alias trap: **`ToggleButtonDefaults.Regular` is `ButtonDefaults.Min`**
(57 × 32), not `ButtonDefaults.Regular` (`ToggleButton.kt:113`) — a toggle button that reads
"Regular" is the smallest button in the set.

`Switch` is the one component that ships an explicit hit-zone expansion of its own, and it is
`internal`: `HorizontalHotZonePadding = 4.dp`, `VerticalHotZonePadding = 8.dp` around a 32 × 20
visual (`design/Switch.kt:329-332`).

An app cannot read any of this back. `ButtonSize` is a public type with an `internal constructor`
(`Button.kt:256-257`) and all six of its properties are `internal val` (`:258-263`); construct one
through `ButtonDefaults.buttonSize(...)` (`:342`), `IconButtonDefaults.iconButtonSize(...)`
(`IconButton.kt:127,136,149`), `ToggleButtonDefaults.toggleButtonSize(...)` (`ToggleButton.kt:132`)
or `ToggleIconButtonDefaults.toggleIconButtonSize(...)` (`ToggleIconButton.kt:98,107`).
`DimensionTokens` is `@RestrictTo(LIBRARY)` with an `internal companion`
(`design/tokens/DimensionTokens.kt:31-34`), so the shipped dp tokens are not app-reachable either.

There is **no** `Modifier.minimumInteractiveComponentSize`; that is a Material3 API and Material3 is
banned in this stack.

## 5. Dragging, and a drag that starts on a scrolling surface

The fact that decides the design: **`detectSpatialDragGesture` uses no touch slop.** It waits for the
platform runtime to declare the drag —
`SpatialDragGestureDetector.kt:192-194`:

```kotlin
val dragging =
    spatialDragData?.state == SpaceDragGestureInfo.GestureState.BEGIN ||
        spatialDragData?.state == SpaceDragGestureInfo.GestureState.DRAGGING
```

and it consumes the change directly in a raw `pointerInput` loop (`:130`, `change.consume()`), so it
does **not** participate in nested scroll. `Modifier.nestedScroll(connection, dispatcher)` is public
in the fork (`ui/commonMain/androidx/compose/ui/input/nestedscroll/NestedScrollModifier.kt:363`) with
two current `NestedScrollSource` values, `UserInput` (`:248`) and `SideEffect` (`:254`). The rest of
that companion is `@Deprecated` aliasing: `Drag` (`:266`) and `Wheel` (`:294`) are both `= UserInput`,
`Fling` (`:278`) is `= SideEffect`, and `Relocate` (`:282`) is marked *"Do not use. Will be removed in
the future."* — so a list that names five or six sources is naming two with aliases. Nested scroll
is real, and it is not the only arbitration in the fork (pointer-event consumption and
`PointerEventPass` ordering arbitrate too) — but the spatial drag detector sits outside all of it.

So when a spatial drag starts on a scrolling surface, **the OS decides who wins, and there is no
public knob on that decision.** No `dragPriority` parameter, no spatial nested-scroll connection, no
way to raise a slop threshold on the spatial path.

What an app can do instead:

- Use the **2D `detectDragGestures` wrapper** (§3) where slop matters — it is the one that passes
  `shouldAwaitTouchSlop = { true }` (`ComposeDragGestureDetectorExt.kt:74`) and so goes through
  `awaitTouchSlopOrCancellation`, and a jittery ray does not immediately steal a scroll. The
  directional wrappers give you *directional* pointer slop instead, which only filters movement
  across the one axis, and `detectDragGesturesAfterLongPress` gives you no slop at all — it trades
  it for a long-press gate. Pick the one whose filter matches the conflict; "use the 2D wrappers"
  is not by itself a slop guarantee.
- **Give the spatial drag its own region.** Put `detectSpatialDragGesture` on a dedicated handle or a
  non-scrolling area rather than on the scrolling content, so the arbitration never has to happen.
- **Scope it with `TargetEntity`** so a drag that began on the wrong thing is filtered by identity
  rather than by timing.
- Use `nestedScroll` for scroll-versus-scroll, which is the case it actually covers.
- Accept that the remaining conflicts resolve in the runtime, and make the losing outcome cheap
  (§8) rather than trying to prevent it.

## 6. Cancellation

Four cancel callbacks exist, and they carry **no reason**:

| API | Callback | file:line |
|---|---|---|
| `detectSpatialDragGesture` | `onDragCancel: (dragValue: SpatialDragValue) -> Unit = {}` | `SpatialDragGestureDetector.kt:69` |
| `detectSpatialRotateGesture` | `onRotateCancel: (value: SpatialRotateValue) -> Unit = {}` | `SpatialRotateGestureDetector.kt:70` |
| `detectSpatialScaleGesture` | `onScaleCancel: (scale: SpatialScaleValue) -> Unit = {}` | `SpatialScaleGestureDetector.kt:67` |
| `detectSpatialTransformGesture` | `onTransformCancel: () -> Unit = {}` | `SpatialTransformGestureDetector.kt:62` |
| `detectDragGestures` (2D wrapper) | `onDragCancel: () -> Unit = {}` | `ComposeDragGestureDetectorExt.kt:65` |
| `detectTapGestures` (2D wrapper) | `PressGestureScope.tryAwaitRelease()` returning `false` | `ComposeTapGestureDetectorExt.kt:72`; scope at `fnd/…/TapGestureDetector.kt:64` |
| any `clickable`/`combinedClickable` surface | `PressInteraction.Cancel` on the `InteractionSource` | `fnd/foundation/interaction/PressInteraction.kt:68` |

They carry the **last valid value**, not a cause. KDoc, `SpatialDragGestureDetector.kt:58-59`:
*"The callback that is called when the drag gesture is canceled. It provides the last valid drag
context with [SpatialDragValue.dragAmount] set to [Offset3D.Zero]."* No `CancellationReason` type
exists anywhere in the four SDK modules. A cancel therefore cannot distinguish "the user let go
outside the target" from "tracking dropped" from "the runtime handed the gesture to something else".

**`detectSpatialTapGesture` has no cancel callback at all** (signature, `SpatialTapGestureDetector.kt:52-56`).
A tap the runtime rejects, or one whose down and up events name different entities, produces
*nothing* on the API surface: the entity-mismatch branch logs at `:75-79` and the rejected branch
logs at `:91-95` —

```kotlin
Logger.e(TAG) {
    "[SpatialTapGesture]: spatial tap is not success, " +
        "isRuntimeTapSuccess = $isRuntimeTapSuccess"
}
```

— and `isRuntimeTapSuccess` is `internal` (`GestureHelper.kt:138`), so the app can neither observe
nor configure the decision. It is silent at the API level and visible only in logcat. If you need a
"that didn't take" affordance on a tap, you have to build it from the press signals
(`PressInteraction.Press` then `Cancel` with no `onTap`), not from the tap detector.

## 7. Tracking loss

**There is no per-frame tracking-loss signal in the public API.** None of `SpatialTapValue`,
`SpatialDragValue`, `SpatialPointerInfo`, `SpatialRotateValue`, `SpatialScaleValue` or
`SpatialTransformValue` carries a validity or confidence field.

What does exist:

- `DataProvider.state` (`tracking/DataProvider.kt:35`, enum at `:83`:
  `CREATED/STARTED/STOPPED/PENDING/UNKNOWN`) and `DataProvider.supportState` (`:38`, enum at `:109`:
  `NONE/SUPPORTED/DEVICE_NOT_SUPPORTED/NOT_IN_FULL_SPACE/WITHOUT_PERMISSION/UNKNOWN`). Both change on
  a **provider-lifecycle cadence**. `SupportState.DEVICE_NOT_SUPPORTED` KDoc (`:117-119`) is about
  the device not being connected or in use — availability, not per-frame loss.
- **Nullable poses**: `HandTrackingData.left: HandPose?` (`tracking/hand/HandTrackingData.kt:33`),
  `ControllerTrackingData.left: ControllerPose?` (`tracking/controller/ControllerTrackingData.kt:29`).
  Null when absent, with no documented "lost" versus "never present" distinction.
- **Exactly two per-frame validity flags**, both pinch/aim-specific and both on hand data:
  `HandPinch.isReady` (`HandTrackingData.kt:128`) and `HandAim.isReady` (`HandTrackingData.kt:217`).
  There is no controller equivalent.

**And a SpatialUI panel app does not get even that.** `HandTrackingData`, `HandTrackingProvider` and
`ControllerTrackingProvider` are all `@RequiredFullSpace` (`tracking/hand/HandTrackingData.kt:29`,
`tracking/hand/HandTrackingProvider.kt:45`, `tracking/controller/ControllerTrackingProvider.kt:36`), and `SupportState.NOT_IN_FULL_SPACE` (`DataProvider.kt:128`)
exists precisely because the runtime refuses the feed outside full space. A Mixed-stage panel app —
the kit's lane — therefore has **no tracking feed and so no loss signal of any kind**. What it sees
is the gesture stream stopping, and if a gesture was in flight, an unexplained cancel per §6.
(The `@RequiredFullSpace` annotation's own source is not in any extracted tree; its semantics are
inferred from `NOT_IN_FULL_SPACE`, not read from its KDoc.)

**Design consequence, and it is not optional: never leave a control in a half-committed state that
only a gesture completion can clear.** A drag handle that is "picked up" until `onDragEnd`, a slider
that holds a provisional value until release, a panel mid-reposition — each of those can be
abandoned by a hand leaving the tracking volume, and the app will be told nothing, or told a
reasonless cancel. Every in-flight interaction needs a timeout or a state that is valid on its own,
and `onDragCancel` must restore, not wait.

## 8. Accidental activation

**The SDK ships no API for this.** No dwell, no debounce, no confirm delay, no anti-mistouch.
Grepped `dwell|debounce|accidental|misclick|mistouch|confirm-on-release|throttle|antiMistouch|activationDelay`
across the four SDK modules, `ui/` and `fnd/`. Nothing in the result touches input, and the row in §12
has to list all of it:

- The same `throttleMillis`/`debounceMillis` pair on *layout observation*, declared in three public
  places — `Modifier.onLayoutRectChanged(throttleMillis = 0, debounceMillis = 64, …)`
  (`ui/…/layout/OnLayoutRectChangedModifier.kt:57-58`), `registerOnLayoutRectChanged` (`:148-149`)
  and `registerOnGlobalLayoutListener` (`ui/…/layout/OnGlobalLayoutListener.kt:57-58`) — over the
  `internal` machinery that implements them (`ui/…/spatial/ThrottledCallbacks.kt:33`,
  `ui/…/spatial/RectManager.kt:45`).
- The internal 30 ms audio-queue delay in §9.
- One unrelated keyboard debounce channel (`ui/androidMain/…/TextInputServiceAndroid.android.kt:114`),
  and otherwise prose: `throttled` and `accidentally` inside comments and an a11y notes file.

`dwell` returns nothing at all.

What does exist is inherited Compose semantics:

- **Confirm on release, cancel on leave.** `fnd/…/gestures/TapGestureDetector.kt:87-90`: *"After the
  initial [onPress], if the pointer moves out of the input area, the position change is consumed, or
  another gesture consumes the down or up events, the gestures are considered canceled. That means
  [onDoubleTap], [onLongPress], and [onTap] will not be called after a gesture has been canceled."*
  The same paragraph is reproduced in the PICO wrapper's KDoc (`ComposeTapGestureDetectorExt.kt:50-53`).
  A press that lands wrong can still be aborted by moving off the target before release.
- **`PressInteraction.Cancel`** (`fnd/…/interaction/PressInteraction.kt:68`) is the observable form,
  and both `PicoIndication` (`design/PicoIndication.kt:116-119`) and `controllerHapticFeedback`
  (`foundation/haptic/ControllerHapticFeedback.kt:102-103`) treat `Cancel` and `Release` identically
  as "not pressed" — so a cancelled press leaves no stuck pressed colour and fires no second haptic.
- **Readable timeouts** an app can build its own guard on: `ViewConfiguration.longPressTimeoutMillis`,
  `doubleTapTimeoutMillis`, `doubleTapMinTimeMillis`, `touchSlop`
  (`ui/…/platform/ViewConfiguration.kt:27,33,39,42`).
- **A runtime-side filter the app cannot see.** Spatial tap requires matching entities and the OS's
  own agreement (§6); spatial drag has no slop to raise (§5). That filtering is real, and it is
  neither queryable nor configurable.

**So the kit's protection is product design, not API:**

1. **Make a stray activation cheap.** Prefer actions that are reversible in one gesture, and give
   them undo rather than a pre-confirmation.
2. **Confirm destructive actions**, with an explicit dialog-class surface — the one place a second
   deliberate act is worth its cost.
3. **No immediately-destructive control in a toolbar** or anywhere a ray sweeps across on its way
   somewhere else. Destructive lives behind a menu or a detail view.
4. **Do not build a dwell timer** and present it as the platform's behaviour. If an app adds one, it
   is the app's invention, it fights the native press model, and it must be labelled as such.

**This lane recorded two unsolicited activations on the emulator during interaction work, and their
cause was never established.** The artifact is
`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/unsolicited-activations-001.json`, and the
checkpoint carries it twice (SPATIAL_UI_POLISH_CHECKPOINT.md (historical source citation)
§6 and the §12 row "Two unsolicited activations | Recorded, cause not claimed"). What the artifact
holds: package `dev.local.picopolish.showcase`, two logged lines 3.4 s apart —
`action=showFlow n=1 flow=Settings` at 01:56:58.295 local and `action=showFlow n=2 flow=Library` at
01:57:01.761 — both marked `sentByThisSession: false`, against a window in which this session sent
two pointer moves and a gate and no click. The artifact does not name a serial; the only emulator in
this evidence tree is `<EMULATOR_SERIAL>`. Its own reading is that `showFlow` has two callers and the
toolbar toggle produces exactly that Settings-then-Library pair, and that the cause **is not
claimed**. Nothing on this page explains them, nothing here rules them out, and no claim that the kit
"prevents accidental activation" is supported. The reason rule 1 comes first is that the failure was
observed and not diagnosed.

## 9. Haptics and audio

**Haptics.** `Modifier.controllerHapticFeedback(type: HandControllerHapticType? = null, interactionSource: InteractionSource? = null)`
(`foundation/haptic/ControllerHapticFeedback.kt:50-54`, `@Stable`). With an `interactionSource` it
fires on `PressInteraction.Press` and clears on `Cancel`/`Release` (`:101-103`); without one it fires
on pointer-down (`:133-135`). It resolves the left/right controller per pointer through
`it.interactionKindExtra.toHandController()` (`:131`) and drops `HandController.Unknown` (`:132`).

**That pair of lines is a silent coverage hole, and it is the far ray.** `toHandController()` maps
only eight `InteractionKindExtra` constants (`platform/SpatialHandControllerHaptic.kt:279-289`);
everything else returns `Unknown` (`:288`) and is then filtered out at `:132`. Eight of the fourteen
constants that `toInteractionModality()` calls `InteractionModality.Controller` are in that
remainder, `HandleRayInteractorLeft`/`Right` among them — so **a controller press arriving on the
far-ray route produces no haptic**, and the app is told nothing. Both routes into the modifier fail
the same way, because both end at `playFeedback()`, which returns early when the list is empty
(`ControllerHapticFeedback.kt:116`) — the `interactionSource` path through `onInteraction` (`:110`)
and the pointer-down path through `onPointerEvent` (`:134`). See §2 for the exact mapped set. This is
a second, independent reason not to make haptics the only feedback.

`controllerHapticFeedback` is also a **complete no-op off the spatial platform**
(`ControllerHapticFeedback.kt:81`, `if (SpatialBuild.isSpatialPlatform()) this.then(modifier) else this`).
Direct call:
`LocalHandControllerHaptic.current.feedback(type, handController)`
(`platform/SpatialHandControllerHaptic.kt:31`); outside a SpatialUI host the default
`InvalidSpatialHandControllerHaptic` only logs (`:211-216`).

Shipped presets, exact `(level, frequency, duration)` (`platform/SpatialHandControllerHaptic.kt`):

| Preset | level | frequency | duration | line |
|---|---|---|---|---|
| `Press` | 70 | 70 | 5 ms | `:100` |
| `Hover` | 38 | 177 | 5 ms | `:102` |
| `Step` | 51 | 65 | 10 ms | `:104` |
| `None` | 0 | 0 | 0 | `:106` |

`HandControllerHapticType.custom(level, frequency, duration)` (`:117`) builds one; documented ranges
are level 0–255, frequency 40–500, duration 0–1000 ms (KDoc `:58-63`), clamped by
`coerceToValidRange()` (`:126-131`). All three constructor parameters are `internal val`
(`:59,61,63`), so **an app can write a haptic but cannot read one back** — a kit that wants "the
press preset, slightly softer" has to hard-code 70/70/5 and say so. Whether there is a supported way
to derive one from a preset was not established.

**The system vibration setting silences everything, and there is no public query.** The hardware
path is guarded by `if (controllerInterface?.systemVibrationEnable == 1)`
(`SpatialHandControllerHaptic.kt:243-250`); `systemVibrationEnable` is reached through a private
`com.pico.controller.PxrControllerInterface` field (`:223`) and appears exactly once in the extracted
surface. So haptic feedback may simply not happen, the app cannot detect that, and **haptics must
never be the only feedback for a state change.**

**Audio.** `SpatialAudioEffectPlayer.playSystem(soundEffect)`
(`platform/SpatialAudioEffectPlayerImpl.kt:73`), obtained from `LocalAudioEffectPlayer` (`:81`), with
the 12-value `SpatialSoundEffect` enum (`:40-65`): `OpClick`, `OpDragBegin`, `OpDragEnd`,
`OpLongPress`, `OpDragScale`, `StateSelected`, `StateUnselected`, `StateOn`, `StateOff`, `OpClose`,
`StateSuccess`, `StateFailure`. Role-to-effect mapping is remappable through
`SpatialAudioEffectConfiguration` (`:205`) / `LocalSpatialAudioEffectConfiguration` (`:198`).
`playSystem` returns immediately off PICO (`:135-137`), and a hard-coded 30 ms gap serialises queued
effects (`:312`, applied at `:128`).

**`LocalAudioEffectPlayer` carries `@hide` in its KDoc** (`:79-80`, on the `val` at `:81`). It is a
plain public Kotlin `val` and it compiles, but flag it the way this page flags `@RestrictTo` on
`DimensionTokens` (§4) and `@ExperimentalSpatialUIApi` on `enableSpatialHittestProvider` (§12): the
SDK is telling you it is not part of the promised surface. Whether any tooling enforces the tag was
not established. The built-ins reach the sound through `PicoIndication` instead, which is the route a
recipe should prefer.

**There is no `Modifier.clickAudio`.** The click sound comes from the indication:
`PicoIndication` (`design/PicoIndication.kt:45`) drives an internal `AuditoryFeedbackNode` (`:94,205`)
that calls `playSystem(opClickEffect)` on press (`:219,227`) and treats `Cancel` and `Release`
identically (`:116-128`). `PicoTheme` provides `PicoIndication` as `LocalIndication`, and every
built-in already wires hover + indication + haptics — `Button` applies `.spatialHoverEffect` (`:179`),
`.clickable(indication = LocalIndication.current)` (`:181-186`) and `.controllerHapticFeedback(interactionSource = …)`
(`:188`), with the same pattern in `Chips.kt:440-441`, `CheckBox.kt:149-150`, `Option.kt:89,99`,
`SegmentControls.kt:148-149`, `TextField.kt:831-832`, `NumberField.kt:419-420`, `PageControl.kt:317,334`,
`Slider.kt:499`, `DatePicker.kt:381,550`, `SideNavigation.kt:162`.

**So a wrapper over a built-in must not add haptics or audio — it would double-fire.** Add them only
on a custom interactive surface that does not go through a built-in, and then once. If you replace
`LocalIndication` with something that is not `PicoIndication`, you lose the click sound and must call
`playSystem` yourself.

## 10. Hover, restated for interaction

Hover is **drawn out of process**: `foundation/hover/SpatialHoverEffect.kt:39-41` — *"Unlike
[androidx.compose.foundation.hoverable], the SpatialHoverEffect is applied out of process, so it
cannot be visible to the app process."* You declare the effect; the system renders it. An app-side
recomposition cannot mirror it frame-accurately, so declare hover visuals through the DSL and do not
rebuild them from `isHovered`.

**There are five public hover modifiers, and four of the five no-op — not all of them.** The whole
public set, with the guard each one actually applies:

| Modifier | file:line | Guarded? |
|---|---|---|
| `Modifier.spatialHoverEffect(block: SpatialHoverEffectRootScope.(SpatialHoverEffectContext) -> Unit)` | `hover/SpatialHoverEffect.kt:47` | yes (`:50`) |
| `Modifier.spatialHoverEffect(style: SpatialHoverStyle = SpatialHoverStyle.Default, enabled: Boolean = true)` | `hover/SpatialHoverEffectStyle.kt:29` | yes (`:33`, with `enabled`) |
| `Modifier.disableSpatialHoverEffect(disabled: Boolean)` | `hover/DisableSpatialHoverEffect.kt:26` | yes (`:27`) |
| `Modifier.spatialHoverEffectGroup(group: SpatialHoverEffectGroup, enable: Boolean = true)` | `hover/SpatialHoverEffectGroup.kt:63` | yes (`:67`, with `enable`) |
| `Modifier.spatialHoverEffectGroup()` | `hover/SpatialHoverEffectGroup.kt:100` | **no** |

The first three share the `spatialHoverEffect`/`disable…` naming; the last two are the group pair
from the subsection below. The no-argument group overload is one line and has no guard at all —
`fun Modifier.spatialHoverEffectGroup(): Modifier = this.then(SpatialHoverEffectGroupElement(-1))`
(`:100`) — so unlike its two-argument sibling it attaches its element unconditionally, on any
platform. What the attached element then does off-platform was not established; what is established
is that it is attached. So "all the hover modifiers no-op off-platform" is not true of this one, and
a recipe that leans on that property for off-device previews should not use it as the example.

**And the guard itself is not platform-only.**
`internal fun isSupportSpatialHoverEffect() = SpatialBuild.isSpatialPlatform() || DEBUG`
(`SpatialHoverEffect.kt:209`), where `private val DEBUG = BuildConfig.DEBUG` (`:211`) resolves to
`com.pico.spatial.core.BuildConfig` (import at `:29`) — the core SDK module's own build flag, not
the consuming app's. So the four guarded modifiers are attached off-platform too whenever that flag
is true. Whether it is false in the published release artifact was not checked here.

### The block form and its DSL

`Modifier.spatialHoverEffect(block)` — the first row above — is the expressive one. Its block
receives `SpatialHoverEffectContext` (`hover/Dsl.kt:32`): `isActive: Boolean` (`:34`) and
`size: IntSize` (`:37`). The complete public DSL — seven methods on `SpatialHoverEffectScope`
(`Dsl.kt:53`) plus one on `SpatialHoverEffectRootScope` (`Dsl.kt:116`):

| # | Signature (verbatim) | line |
|---|---|---|
| 1 | `fun clipShape(shape: RoundedCornerShape, size: IntSize, offset: IntOffset = IntOffset.Zero)` | `Dsl.kt:63` |
| 2 | `fun scale(scale: Float, origin: TransformOrigin = TransformOrigin.Center)` | `Dsl.kt:71` |
| 3 | `fun scale(scaleX: Float, scaleY: Float, origin: TransformOrigin = TransformOrigin.Center)` | `Dsl.kt:80` |
| 4 | `fun alpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float)` | `Dsl.kt:88` (clamped at `:170`) |
| 5 | `fun offset(offset: DpOffset)` | `Dsl.kt:95` |
| 6 | `fun offset(x: Dp = 0.dp, y: Dp = 0.dp)` | `Dsl.kt:103` |
| 7 | `fun rotate(degree: Float, origin: TransformOrigin = TransformOrigin.Center)` | `Dsl.kt:111` — z-axis only (KDoc `:106`) |
| 8 | `fun animation(animation: SpatialHoverAnimation = tween(), block: SpatialHoverEffectScope.() -> Unit)` | `Dsl.kt:132-135` (root scope only) |

Animations are root-scope extensions in `hover/Animation.kt`: `tween(durationMillis, delayMillis, bezier)`
(`:64-68`), `tween(durationMillis, delayMillis, easing = FastOutSlowInEasing)` (`:79-83` — only
`CubicBezierEasing` is honoured, per KDoc `:75-76`), `spring(dampingRatio, stiffness, delayMillis)`
(`:95-99`, defaults `DampingRatioNoBouncy` / `StiffnessMedium` at `:134-135`), plus `CubicBezier(a,b,c,d)`
(`:34`) and `Easing.toCubicBezier()` (`:42`). The `SpatialHoverAnimation` and `CubicBezier`
implementations are `internal` (`:119,121,127`).

### Groups — one element's hover driving another's effect

`Modifier.spatialHoverEffectGroup(group: SpatialHoverEffectGroup, enable: Boolean = true)`
(`hover/SpatialHoverEffectGroup.kt:63`) *"Adds a HoverEffectGroup to all effects defined on descendant
views, and activates the group whenever this view or any descendant views are hovered"* (KDoc `:25-26`).
The no-argument overload (`:100`) gives each call site its **own unique** group — isolation, not
sharing (KDoc `:76`). `enable = false` disables only the group set at that level and does not detach
an inherited one (KDoc `:31-34`). Groups come from `SpatialHoverEffectGroup.obtain()` (`:130`) and
`group.behavior(b)` returns a new group with that behaviour (`:139`).

`SpatialHoverEffectGroup.Behavior` is a public `@JvmInline value class` over an `internal` mask
(`:173`), combinable with `infix or` (`:181`):

| Behavior | mask | Meaning (KDoc `:166-170`, `:197-221`) | line |
|---|---|---|---|
| `Trigger` | `0x01` | this view's state change triggers the group's | `:200` |
| `Responder` | `0x10` | this view responds to the group's state change | `:205` |
| `TriggerAndResponder` | `0x11` | both | `:213` |
| `Standalone` | `0x00` | behaves as if outside the group; activates only on its own hover | `:218` |

This is **the only public way to build a "hovering the row lifts its action" relationship**: mark the
row `Trigger` and the action `Responder` in the same group. Nothing else in the public surface lets
one element's hover state reach another's effect.

### The crash trap

`Modifier.spatialHoverEffect(block)` **evaluates the block twice** — once with `isActive = true`, once
with `isActive = false` (`SpatialHoverEffect.kt:151-155`) — and then pairs the two node lists by
index. Two invariants are enforced, at different places:

- **Same number of nodes**: `check(activeNodes.size == inactiveNodes.size) { "Active and inactive nodes should be same size" }`
  (`SpatialHoverEffect.kt:157-159`).
- **Same type at each index**: `if (activeNode.javaClass != inactiveNode.javaClass) { error(errorMessage()) }`
  inside `convertToFwkEffect` (`:189-191`, message built at `:170-173`, *"should be same type"*).

The two lists are *supposed* to differ in **value** — that difference is the effect. What they may not
differ in is shape. So:

```kotlin
// crashes at layout time: one node in the active pass, zero in the inactive pass
Modifier.spatialHoverEffect { if (it.isActive) scale(1.1f) }

// correct: same node, different value
Modifier.spatialHoverEffect { scale(if (it.isActive) 1.1f else 1.0f) }
```

Any conditional inside the block needs the `else` branch that emits the same node type.

Two further facts a polish recipe gets wrong: `materialSpatialHoverEffect` — the material-aware
variant with a `Material` parameter — is `internal` and unreachable
(`hover/SpatialMaterialHoverEffect.kt:46`), so glass-plus-hover has to be `backgroundMaterial` and
`spatialHoverEffect` stacked separately; and applying the modifier is not the same as getting a
visible result, which is measured in native-glass-contract.md (historical source citation) §4.

**Open question, not resolved here:** whether an app-side `Modifier.hoverable` +
`collectIsHoveredAsState` (`fnd/foundation/Hoverable.kt:41`) actually receives Enter/Exit on a PICO
panel. The seven `*HoverInteractor*` constants in `InteractionKindExtra` (`:47-50`, `:52`, `:57-58`
— `HeadCenterInteractor` at `:51` sits between them and is not one) imply hover
pointer events are delivered to the process, but that the fork's `HoverableNode` sees them was not
confirmed from source and there was no fixture to test it. A recipe that depends on app-side hover
state needs a device check first.

## 11. What the kit therefore owns

1. **Route handling is `when` with an `else` — and the route table is a hypothesis.** Read
   `InteractionKind` for coarse routing, `interactionKindExtra` only when handedness or
   gaze-versus-head matters, and always terminate with `else -> Unknown` so a constant added by a
   later SDK degrades instead of throwing `NoWhenBranchException`. Never infer "hand" from `Poke`.
   But do not build behaviour that *depends* on §2's route column: which physical input produces
   which constant is inferred from the names, confirmed from neither source nor runtime, and the
   emulator may flatten the whole discrimination to `Unknown` (§1). Branch on it for polish, check
   it on a headset before branching on it for correctness.
2. **Every in-flight interaction has a state that is valid on its own.** No half-committed control
   waiting for a completion that tracking loss can delete (§7). `onDragCancel` restores.
3. **Visible affordances carry their own size.** Do not rely on the invisible 48 dp expansion; for
   anything that will be aimed at from across a room, use a size at or above 48 dp and treat
   `ButtonDefaults.Small`/`Min`, `IconButtonDefaults.Min`/`Small`, the chips and the checkbox as
   dense-context components. Remember `ToggleButtonDefaults.Regular` is the *smallest* button.
4. **Spatial drag goes on its own handle**, not on scrolling content; a slop-sensitive drag uses the
   2D `detectDragGestures` wrapper specifically, which is the only one that asks for touch slop —
   the directional wrappers give directional pointer slop and the long-press wrapper gives none
   (§3, §5).
5. **Stray activation is made cheap, not prevented.** Undo over confirmation; confirmation for
   destructive; nothing immediately destructive in a sweep path; no invented dwell timer (§8).
6. **Feedback is never haptics-only** — for two independent reasons: the system vibration setting can
   silence it and cannot be queried, and `toHandController()` maps only 8 of 39 interactor constants,
   so a far-ray controller press (`HandleRayInteractor*`) is filtered out and never buzzes (§2, §9).
   Wrappers over built-ins add no second hover, haptic or click sound (§9).
7. **Hover is declared, never simulated**, every DSL block is shape-symmetric across `isActive`, and a
   cross-element hover relationship is built with `spatialHoverEffectGroup` or not at all (§10).
8. **The interaction claims on this page are source-derived.** Anything about how an interaction
   *feels* — press latency, ray precision, whether a 40 dp chip is hittable at two metres — needs a
   headset and a wearer, per acceptance.md (historical source citation). None of it has been established here.

## 12. Not available in the public API

Each row is a "not found" and the grep that established it. Scope: the four SDK modules
(`design`, `foundation`, `platform`, `core`) plus `ui/` and `fnd/`, and `tracking/` where noted.

| Wanted | Verdict | Grep |
|---|---|---|
| Dwell / debounce / confirm-delay / anti-mistouch | **none** | `dwell\|debounce\|accidental\|misclick\|mistouch\|confirm-on-release\|throttle\|antiMistouch\|activationDelay` — the same layout throttle/debounce pair in three public declarations (`onLayoutRectChanged` `:57-58`, `registerOnLayoutRectChanged` `:148-149`, `registerOnGlobalLayoutListener` `OnGlobalLayoutListener.kt:57-58`) over internal `ThrottledCallbacks`/`RectManager`, the internal 30 ms audio delay, one keyboard-debounce channel (`TextInputServiceAndroid.android.kt:114`), and comment prose. `dwell`: 0 hits |
| Per-frame tracking-loss / confidence signal | **none** | `tracking ?lost\|trackingLost\|lostTracking\|trackingState\|isTracked\|trackingLoss\|handLost\|controllerLost\|confidence\|isValid` over the SDK modules and `tracking/` — only `DataProvider.state`/`supportState` and nullable poses survive |
| Gesture-cancellation *reason* | **none** | `CancellationReason\|cancelReason\|GestureCancellation` — only `GestureCancellationException` named in the fork's `PressGestureScope.awaitRelease()` KDoc |
| `onTapCancel` on `detectSpatialTapGesture` | **none** | signature at `SpatialTapGestureDetector.kt:52-56`; rejection is logged internally and dropped |
| PICO nested-scroll / drag-priority knob | **none** | `dragPriority\|drag_priority\|nestedScroll\|NestedScrollSource\|awaitTouchSlop` over the four modules — only the stock `androidx.compose.ui.input.nestedscroll` fork, which the spatial drag detector does not participate in |
| `Modifier.minimumInteractiveComponentSize` | **none** (Material3 API) | `minimumInteractiveComponentSize\|MinimumInteractiveComponentSize\|minimumInteractive` over the SDK modules, `ui/`, `fnd/` — 0 hits |
| An override of `minimumTouchTargetSize` in the PICO impl | **none found** | `minimumTouchTargetSize` over `ui/androidMain` — 0 hits; the 48×48 dp interface default is what ships |
| `Modifier.clickAudio` or a one-line click sound | **none** | `clickAudio\|soundEffect(` over design/foundation/platform — audio comes from `PicoIndication`'s internal node, or you call `playSystem` yourself |
| Public `materialSpatialHoverEffect` | **internal** | `SpatialMaterialHoverEffect.kt:46` |
| Public access to `spatialui.x.foundation.gestures.*` | **internal, whole package** | `^(suspend )?fun \|^class \|^val \|^object \|^interface \|^enum \|^sealed ` over both files in the package — no public top-level declaration at all |
| Readback of a shipped `ButtonSize` or haptic preset | **internal properties** | `Button.kt:256-263`; `SpatialHandControllerHaptic.kt:59,61,63` |
| Readback of the shipped dimension tokens | **`@RestrictTo(LIBRARY)`** | `design/tokens/DimensionTokens.kt:31-34` |
| Query of the system vibration setting | **none public** | `systemVibrationEnable` appears once (`SpatialHandControllerHaptic.kt:243`) behind a private `PxrControllerInterface` field (`:223`) |
| Public window move/drag handles | **none** | `detectDragGestures\|draggable\|dragHandle\|onDrag` over the whole design module — two files match and only one drags: `Slider.kt` (`detectDragGestures` at `:887`, `SliderDraggableState` at `:1083`). The other is `text/BasicTextFieldWrapper.kt:232-268`, where the matches are `TextFieldFeedBackOption.OnDrag*` feedback names, not gesture wiring. **Timepicker does not drag**: its only `drag` token is the audio effect `opDragScaleEffect` (`Timepicker.kt:431`). Window movement is shell-owned |
| `Modifier.offset3D(x, y, z)` | **none** | `fun Modifier.offset3D\|offset3d` — not found. What ships in `foundation/layout/Offset3D.kt` is `Modifier.zOffset` (`:45`), followed at `:47` by a naming-unification comment, not a TODO: `// Look back at the plan to unify the function name to Offset`. Line 48 is blank |

One near-miss worth knowing rather than reaching for: `Modifier.enableSpatialHittestProvider`
(`foundation/spatialhittest/SpatialHittestProvider.kt:121`) is public but `@ExperimentalSpatialUIApi`
(`:120`) and **throws at runtime** unless the consuming app's manifest sets
`pico.spatial.use_experimental_api=1` (`foundation/config/SuiExperimentalApiChecker.kt:38-41`).
Whether the showcase manifest sets that flag was not checked. Do not put it in a recipe without
adding the manifest instruction and saying it is experimental.

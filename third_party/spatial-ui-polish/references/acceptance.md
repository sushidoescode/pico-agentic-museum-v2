# Acceptance — what each kind of evidence proves, and what it cannot

Polish is a visual claim, and a visual claim needs a frame someone opened. This page says which
frame, and what each class of evidence is still silent about.

## Evidence ladder

| Class | Produced by | Proves | Silent about |
|---|---|---|---|
| `source-derived` | reading the resolved SDK sources / this repo | which API exists, what a default is | anything about rendering |
| `fixture` | host tests against injected inputs | activation, suppression, semantics, option resolution, colour-role choice | appearance, glass, depth, timing |
| `host-executed` | a real build/lint/test run on this machine | the module compiles and its behaviour holds on the JVM | the same, plus anything device-specific |
| `host-rendered` | real pixels from Robolectric's graphics pipeline, off the spatial platform, at a fixed dp window qualifier | layout rhythm, hierarchy, type scale, edge contrast, foreground pairing — anything a wrong decision breaks identically everywhere | glass, vibrancy, hover and every number that depends on them: `backgroundMaterial` and `spatialHoverEffect` are no-ops off-platform, and the scheme falls back to the non-adaptive literals |
| `emulator-observed` | an opened emulator frame | layout, hierarchy, state changes, text legibility on a known background | stereo depth, vibrant adaptation against passthrough, real refresh, comfort |
| `device-observed` | an opened headset frame at a verified serial | the above, plus real materials, real vibrant adaptation, real passthrough | how it feels to wear |
| `wearer-reported` | the person wearing it | depth comfort, reachability, readability at true distance, input precision, haptic feel | nothing else; it is the only source for these |

Never present a lower class as a higher one. A passing build is not a rendered frame; an emulator
frame is not device evidence; a device frame is not the wearer's judgement.

## Where this kit's frames actually sit on that ladder

Every **emulator** frame behind a claim in this kit was admitted by the inherited
`render-liveness/1.0.0` package-targeted gate with all 17 checks unweakened, on serial
`<EMULATOR_SERIAL>` — a PICO Swan emulator running Android 16 (`ro.build.fingerprint`
`<EMULATOR_FINGERPRINT>`, read back by the gate itself). The design phase's 25
admitted frames, the build each one belongs to, and which of them a person actually opened are
tabulated in FRAMES-DESIGN.md (`evidence/2026-09-17-spatial-ui-polish-native-001/FRAMES-DESIGN.md`); the
earlier phase's are in FRAMES.md (`evidence/2026-09-17-spatial-ui-polish-native-001/FRAMES.md`).

**But not every frame in this kit is an emulator frame, and two of the exceptions decide shipped
behaviour.** The host-rendered set (`evidence/2026-09-16-spatial-ui-polish-001/host-rendered/README.md`)
(`evidence/2026-09-16-spatial-ui-polish-001/host-rendered/`) holds eleven Robolectric frames plus
nine `before/` copies of the same views. There is no device in
that lane, so there is no gate receipt and no serial to record: they are `host-rendered` on the
ladder above, not `emulator-observed`, and nothing in them says anything about glass. The module
cites two of them by name. `PolishSurfaceDefaults.arrangementFor` exists because "host-rendered
frame 02" showed a surface stacking its children flush, and `edgeFor`'s fill-following edge exists
because "Host-rendered frame 08" showed four surface kinds collapsing into one flat shape on the
opaque fill. A rung that decides shipped behaviour belongs on the ladder with its silences stated,
which is what the new row does.

**There is no PICO headset evidence and there is no wearer evidence.** Not weak evidence, not
pending evidence: none. The only physical headset attached during this work was a Meta Quest 3
(`<DEVICE_IDENTIFIER>`), which is not PICO acceptance hardware and had nothing from this kit installed on
it — so it is not a lower rung of the same ladder, it is a different ladder. Everything below stops
at `emulator-observed`; the two rungs above it are empty, and a reader who needs them has to go and
earn them.

## The matrix to fill

| Dimension | What must be observed |
|---|---|
| Visual | Opened frames showing clean corners, no doubled border or doubled glass, legible foregrounds, clear hierarchy |
| Backgrounds | Controlled bright, dark and detailed backgrounds; real passthrough when a headset is available. The app-owned backdrop fixture (Room / Bright / Dark / Detailed) run against every surface kind, with the Room fixture kept as the undrawn control |
| Contrast | Both lanes, reported separately and never merged: what `PolishContrast` can compute in-process, and what is measured from an admitted capture's uniform probe swatches — the second always stating the capture's declared colour primaries |
| Type rank | Every rung of `PolishTypography.ladder()` visible in one frame, each rung distinguishable from the one above it — including the steps that are weight-only |
| Interaction | Hover, press, select, toggle, edit, dismiss actually performed, with the resulting state visible in an after-frame |
| Interaction route | Which input route actually produced the event, read out of the log, never inferred from the name of the gesture that was synthesised |
| Layout | Minimum, default and expanded supported window sizes; long text; larger text; no essential action clipped |
| Reduced settings | On/off comparison for motion, transparency and contrast, with feature parity in both states |
| Motion | Recordings inspected across the transition interval — never two endpoint stills |
| Reuse | Both showcase flows consuming the same module, with no copied component implementation |
| Performance | Baseline and candidate sampled under the same refresh, thermal and input conditions |

## Interaction evidence

A debug state injection is supplemental, never the proof. The sequence that counts is
before-frame → the real input event → after-frame, with the event and the frames correlated by
timestamp and by the package under test. For emulator input, derive coordinates from a magnified
crop of a retained frame and hover-verify the first target before clicking; retain the cursor
evidence.

**A worked example of the supplemental kind, and of how it lies.** The showcase's States gallery
draws every component state at once, and its focus cell is injected rather than driven: it emits
`FocusInteraction.Focus()` into the surface's own `MutableInteractionSource`. That is an honest
fixture — it drives the same `collectIsFocusedAsState` path a real focus drives — and it is still
not interaction evidence: it shows a state can be *drawn*, not that it is *reachable*. It also
failed silently first. `MutableInteractionSource` publishes through a shared flow with no replay and
the collector subscribes from its own `LaunchedEffect`, so a single `emit` races it and can simply
be lost. It was: `design-024-states-p2` (build `90b10316`, opened) shows the focus cell with **no
ring at all**. `design-025-focus-ring` (build `6b19ca10`, opened) is the first frame in this lane in
which the **focus ring renders**, after the fixture was changed to re-emit; the same frame shows
`edgeFor`'s precedence holding in pixels, with an Error-plus-selected cell drawing the error edge
rather than the accent.

**Why that gallery is paged rather than scrolled — a layout finding frames made.** This emulator has
no host route to a guest scroll (the wheel drives the camera and never reaches the app), so a
section below the fold is a section no capture can ever show. Four frames walked it down:
`design-018-states-overlap` (build `37285f06`, opened) fitted only two of seven sections;
`design-019-states-2col` (build `9b0527c8`, opened) still overflowed in two columns and wrapped the
four-across rows mid-word; `design-020-states-matrix-fit` (build `3f71458a`, opened) still clipped
at the window's bottom edge; `design-021-states-page1` (build `2fbc4576`, opened) still clipped
`Surface states` inside the cells' card padding. Six sections do not fit the app's declared
1080x640 dp window (`pico.spatial.windowcontainer.defaultsize`) even in two columns. The gallery now
pages, and the page turn is a **counted action** (`action=galleryPage`) where a scroll would have
been nothing — so which half a frame shows is in the log rather than in a caption.

## Motion evidence

Record the interval, not the endpoints. Two stills that differ prove a state change happened, not
that the transition was smooth, correctly timed or free of overshoot. Inspect the recording.

The kit's timings (120 ms feedback, 180 ms state, 240 ms panel) are product defaults on the design
system's short tier, to tune against evidence — not platform requirements. Native interaction
timing (hover, press, window animations) belongs to the system; do not replace it to hit a number.

**What this lane recorded.** Two clips of the same three flow switches, in the same order, each
switch bound to a counted action, differing only in the app's own Reduce motion setting:
`design-motion-on.mp4` and `design-motion-reduced.mp4`, both build `ae4bc4ed`, written up in
`DESIGN-MOTION.md` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/motion/DESIGN-MOTION.md`).
Decoded at 60 fps and measured by `polish_motion_runs.py`
(`design-motion-runs-on.json`, `design-motion-runs-reduced.json`):

| reduce motion | transition runs | durations | peak frame-to-frame change |
|---|---|---|---|
| **off** | 9, 11, 11 frames | 150.0, 183.3, 183.3 ms | 0.455, 0.618, 0.761 |
| **on** | 1, 1, 1 frames | 16.7, 16.7, 16.7 ms | 1.675, 2.007, 2.217 |

The transition collapses to a single frame and the per-frame change grows roughly fourfold, which is
what `PolishTokens.motion()` returning `snap()` looks like from outside the app. What the recording
does **not** establish is the animation's duration: `DurationPanelMillis` is 240 ms, the measured
un-reduced runs are 150–183 ms, and the threshold discards the low-amplitude tail of a fade. The
file states that limit itself, and it is an emulator recording either way — nothing here is headset
motion or a wearer's judgement of comfort.

## Contrast

Target 4.5:1 for normal text and 3:1 for large text on the controlled test backgrounds, sampled
from the **rendered frame**, not computed from token values. Nominal contrast is meaningless here:
the fill roles are Vibrant-encoded and resolved by the compositor against whatever is behind the
surface, so the number only exists once something has been drawn. State which text you sampled and
how. These are adopted 2D legibility targets, not a claim of XR accessibility conformance.

There are two lanes that produce a contrast number, they answer different questions, and a report
that merges them is worthless. Name the lane every time.

### Lane 1 — computed in-process (`PolishContrast`)

`PolishContrast.ratio` / `.ratioOver` are WCAG 2 arithmetic on values the app process holds. That
lane is valid in exactly three places:

- **Off the spatial platform** — a JVM or Robolectric test, a preview, a non-PICO build, where
  `systemColorScheme` resolves to `defaultColorScheme()` and all sixteen roles are literal ARGB.
  This is the lane the kit's own tests run in.
- **For the six roles whose channels survive** — but not all six for the same reason. `error`,
  `alert`, `passable` and `interaction` are literal in both schemes. `labelPrimaryLight` and
  `dividerLine` are **not** literal in the vibrant scheme: they are built as a real colour tagged
  `Vibrant.None`, the level that disables the effect, so their RGB stays authoritative.
  `PolishContrast` tells the two cases apart with a private
  `hasUnresolvedVibrancy()` = `containsVibrant() && obtainVibrant() != Vibrant.None`, and asks "is
  there a level that will *replace* these channels", not "is there a tag". An earlier version asked
  only `containsVibrant()` and therefore refused exactly the two roles this page promised were
  measurable — the count was right and the reason was wrong. The four semantic roles carry a
  Display-P3 colour space and this API converts nothing, so treat those as sRGB approximations.
- **For colours the app owns itself.**

Everywhere else it **refuses**: `PolishContrastResult.Undecidable` with a `PolishContrastLimit`
naming why — a vibrancy sentinel in the foreground, background or backdrop, or a translucent value
with no stated backdrop to composite onto. `Undecidable` is not a pass and it is not a failure. It
means *go and measure this from a frame*, and a checklist that ticks it as green has recorded the
opposite of what happened.

Its target selection is stricter than it looks: `PolishContrast.isLargeText` reads WCAG's
"18 pt, or 14 pt bold" in the conventional Android form (18 sp, or 14 sp at `FontWeight.Bold`/700
or heavier), so **only three of the eleven `PolishTypography` rungs qualify for the 3:1 allowance** —
`display`, `windowTitle` and `sectionTitle`. The kit does not count W600 SemiBold as bold, which
holds `objectTitle`, `control` and `overline` to 4.5:1 on purpose.

### Lane 2 — measured from rendered pixels (the only device-shaped number)

A number that describes what a user sees has to come out of an admitted capture, and it has to come
out of **uniform probe swatches**, not out of text. Text pixels are antialiased: every glyph edge is
a blend of the foreground and the background, so a sample over text estimates two colours it never
actually contains. The showcase therefore draws, beside the real text of each surface kind, a row
of flat patches (`ProbePair`, `LegibilityFlow.kt`): `probe-bg-*` is left deliberately empty, so what
it shows is the surface's own composited fill; `probe-fg-*` takes the surface's foreground role and
`probe-fg2-*` its supporting-text role. Each is flat by construction, so the mean of the box *is*
the colour, and `evidence/.../tooling/polish_contrast.py` samples them with no estimator in the
path.

Three things must be stated with any such number, or it is not reportable:

1. **The capture, by path and digest**, and the gate receipt that admitted it.
2. **The capture's declared colour primaries.** Every emulator capture in this lane carries a `cICP`
   chunk of `0c 0d 00 01` — colour primaries 12 (Display P3), transfer characteristics 13 (sRGB),
   full range — so luminance is weighted with the Display-P3 row, `0.2289746 / 0.6917385 /
   0.0792869`, not with BT.709's. Both coefficient rows sum to 1.0, so for a **neutral** pair the
   P3-vs-sRGB choice changes the answer by one float ULP; it only moves a **saturated** pair. State
   the primaries anyway: a reader cannot tell which kind of pair it was from the ratio alone.
3. **That the scene held still.** Fixed-box sampling assumes the camera did not drift, and in this
   lane it drifted once. Room-only stability boxes are read before any ratio is.

### What this lane has already caught

Supporting text set on `labelTertiary`, build `37452262`, Detailed backdrop, capture
`design-009-fix-detailed`
(`design-contrast-detailed-fixbuild.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/design-contrast-detailed-fixbuild.json`))
— **all three fail**:

| surface | ratio | target | verdict |
|---|---:|---:|---|
| Panel | 3.18 : 1 | 4.5 | fail |
| Card | 3.26 : 1 | 4.5 | fail |
| Control | 3.49 : 1 | 4.5 | fail |

Moving supporting text one rung up, to `labelSecondary` (build `ae4bc4ed`, capture
`design-010-supporting-fix`,
`design-contrast-detailed-supportingfix.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/design-contrast-detailed-supportingfix.json`)),
gives **8.60 / 8.57 / 8.22**, all passing — while every body pairing in the same pair of reports
stays byte-identical (11.92 / 10.58 / 8.84 / 14.74). That identical sibling is what makes the result
a measurement of the change rather than a measurement of the weather, and the after-report's four
room-only stability boxes (`roomCeilingLeft`, `roomAcUnit`, `roomFloorLeft`, `roomRugRight`) all
read 0.000 against the before-capture it names as its reference. The same three pairings compute perfectly fine from the non-vibrant fallback
values on the JVM, which is precisely why Lane 1 could not have found this.

### Two more measurements from the same lane

**`reduceTransparency` on.**
`contrast-design-015-rt-on.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/contrast-design-015-rt-on.json`),
from capture `design-015-rt-on-room` (build `ae4bc4ed`, Room backdrop, counted action `n=6`,
opened and judged). **All eight pairings measure 14.74 : 1**, because every surface's ground samples
at exactly `[40, 40, 40]` — `#282828`, which is `PolishTokens.OpaqueGround` — at stddev 0.000, and
every foreground at `[255, 255, 255]`. That is the setting's whole point measured rather than
asserted: one literal opaque ground, nothing left for the compositor to resolve, and no dependence
on what is behind the surface.

**`increaseContrast` on.**
`contrast-design-016-ic-on.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/contrast-design-016-ic-on.json`),
from capture `design-016-increase-contrast` (build `ae4bc4ed`, Room backdrop, counted action
`n=10`, opened and judged). The four supporting-text pairings measure **11.92 / 10.58 / 8.84 /
14.74** — identical to the body ratios in the table above, pairing for pairing, because
`secondaryForegroundFor` steps supporting text up to `labelPrimary`, the colour the surface's own
title already uses. From there the title-to-supporting distinction is carried by size and weight
alone, which is what the setting asks for and is worth saying out loud.

Both of those reports record `sceneStability.stable: null`. Neither was given a stability region, so
each is a single-capture measurement, not a controlled before/after.

### The premise this lane used to carry as UNVERIFIED, and where it now stands

Every report this tool writes embeds its own premise: the foreground patch is
`Modifier.background(...)` fed from a role, and on a vibrancy-enabled device that role could itself
be one of the ten sentinels — in which case both foreground patches would render as identical flat
black and every ratio in the report would be meaningless. The decisive check is that the two
foreground patches render as **different** tones. It has now been made, twice:

- **Looked at.** `design-005-legibility-room` (build `f91e4b7e`) was opened and judged: `probe-fg`
  renders black and `probe-fg2` renders grey.
- **Measured.**
  `design-contrast-detailed-fixbuild.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/design-contrast-detailed-fixbuild.json`)
  samples them on `design-009-fix-detailed` (build `37452262`) as `[0, 0, 0]` and
  `[107.552, 103.057, 98.154]`, each flat to a stddev of about 1.2.

So a role colour passed to `Modifier.background(...)` *is* compositor-resolved, the foreground
swatch is not a placeholder, and these ratios are about pixels a viewer would see. The
`premise: UNVERIFIED` string is still embedded in each JSON, because the reports were written before
the check; read it as superseded by the two rows above rather than as an open item.

**What the second patch actually is.** `probe-fg2-*` is filled from
`PolishSurfaceDefaults.secondaryForegroundFor(kind, options)` (`LegibilityFlow.kt`), so its role
follows the build and the options rather than being fixed: `labelTertiary` (`Vibrant.Semidark`) on
`37452262` and earlier, `labelSecondary` (`Vibrant.UltraDark`) from `ae4bc4ed` on — that move *is*
the supporting-text fix — and `labelPrimary` whenever `increaseContrast` is on, which is why report
016 reads the body ratios. The premise string inside each JSON still says `labelTertiary`: accurate
for `design-contrast-detailed-fixbuild.json`, stale for every `ae4bc4ed` report.

None of this promotes the numbers off the emulator. They remain `emulator-observed`.

## Backgrounds

The backdrop fixture is **app-drawn content inside the window's content area**, switched by a
segmented control in the showcase's legibility flow: `Room` (nothing drawn — the control), `Bright`
(#F4F4F4), `Dark` (#0B0B0B) and `Detailed` (a black-and-white checkerboard). The window keeps its
system material throughout and nothing is painted on the window root, so the fixture never violates
the kit's own rule about painting over live system glass.

What it settled, on build `f91e4b7e`: `ColorScheme.fillPrimary` is **not opaque on a device**. It is
`Color.Vibrant.withVibrant(Vibrant.Darker)`, which the compositor resolves by darkening what is
behind the surface, and four admitted frames varying only the backdrop show it — room → near-black,
bright → **medium grey**, dark → near-black, checkerboard → **the checkerboard, darkened and still
visible through the surface** (`design-005-legibility-room`, `-006-bright`, `-007-dark`,
`-008-detailed`). The kit's JVM test asserted the opposite and passed, because off the spatial
platform the scheme falls back to `defaultColorScheme()` where `fillPrimary` really is the literal
`0xFF282828`. The test was right about the fallback and wrong about the device.

What it settled after the fix: the surfaces are **invariant to app-drawn content behind them**.
The measurement is
`design-backdrop-invariance.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/design-backdrop-invariance.json`),
a fixed-box photometry pair over `design-011-fixed-dark` → `design-012-fixed-bright` (both build
`ae4bc4ed`, same flow, same settings, counted actions `n=2` and `n=3`, the backdrop fixture the only
variable). The two regions that sit on the fixture itself, `backdropStripLeft` and
`backdropStripRight`, moved **11.00 → 244.00 mean gray, a delta of +233.00**. The four that sit on
the empty probe patch inside each surface — `panelSurfaceFill`, `cardSurfaceFill`,
`controlSurfaceFill` and `emphasisSurfaceFill` — moved **0.00**, holding at 195.155, 184.024,
168.227 and 40.000. All four room-only stability boxes read 0.000, so the camera held still and the
strip's delta is the fixture's.

**The four-backdrop version of that claim is weaker, and the reports say so themselves.** The eight
pairings do report identical ratios to 2 dp on all four backdrops, from four distinct captures with
four distinct digests — `design-010-supporting-fix` (Detailed), `-011-fixed-dark`,
`-012-fixed-bright` and `-013-fixed-room`, all on `ae4bc4ed`. But only the Detailed report,
`design-contrast-detailed-supportingfix.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/design-contrast-detailed-supportingfix.json`),
carries stability boxes (four regions, `stable: true`). The other three record
`sceneStability.stable: null`, and their own `verdictWhy` states the consequence: *"every pair
clears its stated target; no stability region was given, so nothing here rules out a camera drift
between this capture and whatever it is being compared to."* Three of the four are four separate
PASS reports, not a controlled comparison. The controlled comparison is the photometry file above,
and that is where the invariance claim is anchored.

**Its limit, and it is the whole point of the row.** This varies app-drawn content *inside the
window*. What is behind the **window** — the emulator room the compositor's own material samples —
was constant throughout and was **not** varied, because varying it would mean changing Wing. So this
says nothing yet about vibrant adaptation against a changing environment, and nothing at all about
passthrough. A headset frame against real passthrough is still the row's unmet requirement.

## Type rank

`design-004-specimen-fit` (build `a493c172`, opened and judged) renders all eleven rungs of
`PolishTypography.ladder()` at once, and the weight-only steps — at 16 sp, at 14 sp and at 12 sp —
are each visibly distinct in it. Two layout defects were found by frames and only by frames, and
both are fixed in that build:

- `design-002-specimen` (build `160e8dc4`, opened) showed the first: at the Panel's default 16 dp
  rhythm **only 6 of the 11 rungs fitted the window**, and the scroll that would have reached the
  rest is unreachable from the host.
- `design-003-specimen-2col` (build `ea378f48`, opened) showed the ladder still overflowing in two
  columns, and showed the second defect beside it: a four-across row of surface kinds at roughly
  110 dp each broke the word "supporting" mid-word and wrapped the word "Emphasis" itself.

**What a capture like that can settle: relative rank.** Whether two rungs are visibly different
from each other, in a frame, at the sizes the SDK actually ships.

**What it cannot settle: absolute legibility at wearing distance.** An emulator capture frames a
whole room, so the window occupies a small part of it, and no arithmetic on that image recovers the
angular size of a 12 sp glyph in a headset. Every claim about the ladder is a claim about rank until
a headset frame and a wearer say otherwise — which, per the section above, has not happened.

## Interaction route

The emulator's synthetic pointer is not route-neutral, and this is the row most likely to be
over-claimed by accident — including by this page, which previously claimed more than the artifact
supports. The correction is to keep the platform layer and the app layer apart.

**What was observed, at the platform layer.** A logcat probe of the showcase's own process
(`--pid=<PID>`, build `ae4bc4ed`, serial `<EMULATOR_SERIAL>`) holds 70 `ViewRootImpl: onInputEvent
Space Info[0]` lines produced by this lane's host clicks, and **every one of the 70 carries
`kind=GAZE_PINCH`**; no other value appears. Artifact and framing:
`ROUTE-PROBE.md` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/logs/ROUTE-PROBE.md`) over
`route-probe-spaceinfo.log` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/logs/route-probe-spaceinfo.log`).
So the guest platform labels a host click as the eye-hand route.

**That is a log field, not an API reading.** `kind=GAZE_PINCH` is printed by the platform's own
input plumbing; it is not the Spatial SDK's public `InteractionKind`, and the two are separated by a
mapping this app cannot see — `KIND.toInteractionKind()` and `SpaceInteractionKind.toInteractionKind()`
are both `internal` (`foundation/…/gesture/data/InteractionKind.kt:42, :60`). `TapEvent3D` is another
field name printed inside the same log line — populated in 23 of the 70 lines and `{INVALID}` in the
other 47. **It is not an SDK type and must never be cited as one**, as an earlier version of this
section did when it wrote a host click up as "carrying a valid `TapEvent3D`".

**The app-layer question is still open.** Nothing in either tree reads the route: grepping
`InteractionKind`, `detectSpatialTap` and `interactionKindExtra` across the showcase's `app/src` and
the module's `src` returns **0 hits in both**. So the question
interaction-contract.md (historical source citation) carries forward — whether a Spatial SDK gesture
detector populates route information at all under emulator verification, or collapses it to
`InteractionKind.Unknown` — is **not** answered here. Answering it needs a build that calls
`detectSpatialTapGesture` and logs `SpatialTapValue.interactionKind`, and no such build exists.

**The design consequence holds either way.** No emulator evidence in this lane exercises the
direct-touch route or the controller route. A frame pair proving that a tap worked proves that the
path a host click travels worked; if the behaviour under test discriminates by route, the other
branches are untested code, and saying so is part of the claim. See
interaction-contract.md (historical source citation) for what each route is contracted to do.

Read the route out of the log for the capture you are claiming from. Do not infer it from the name
of the host gesture you performed, and do not promote a platform log field into an app-layer API
observation.

## Performance

Identify the refresh rate and the metric's scope *first*. Warm up at least 30 s; collect three
60-second baseline samples and three 60-second candidate samples, interleaved where the tooling
allows. Retain temperature and throttling context with every sample.

Proposed project budget, when the profiler exposes comparable measurements: app CPU and GPU p95
each ≤ 85 % of the relevant frame interval, p99 ≤ one frame interval, and no new sustained
missed-frame pattern against the stock baseline. Do not add CPU and GPU as if they were serial, and
do not compare app time to a different compositor refresh. If the metric scope cannot support the
budget, report the lane unestablished and retain the recordings — do not invent a frame rate from a
compositor metric or from screenshots. Prefer removing an optional effect to lowering a target.

## Emulator admission

Emulator work carries the inherited gate, unchanged: a fresh unfiltered 20-second main-buffer
sample; the emulator window frontmost and unoccluded before sampling, after sampling and after every
capture; a clean timed window with zero early-vsync warnings; ≥ 18 watchdog samples all naming the
target package; monotonic render-time advancement ≥ 18000 ms with no consecutive non-advancing
sample; every age within 0–100 ms including rejection of negative ages; one emitter, no malformed or
duplicate samples; the installed build hash and PID unchanged throughout. Occlusion permits raising
the window, ten seconds of settling and a complete fresh gate, and does not consume a retry. One
fresh retry after a diagnosed numeric failure; if that fails too, retain both failures and hold.

These thresholds say nothing about a physical headset and do not replace measurement against the
device's actual refresh rate.

## Device routing

Use explicitly targeted routes. Several MCP doors build or capture against a default emulator
serial and expose no serial input — they are not a physical-headset path. Establish the model,
runtime, ABI and serial from the connected hardware before installing anything, match the APK's
native ABIs to that target, and record the installed package identity, the PID and the capture
paths for every claim.

A headset that is attached is not a headset that was tested. **Nothing from this kit was installed
on the Quest 3** that was connected during this work, and a non-PICO headset could not have carried a
PICO acceptance claim even if something from this kit had been.

## What to write down

For every claim: the evidence class, the exact artifact path, the target identity (serial, package,
APK hash), and what the claim still does not establish. Failed attempts stay in the evidence
directory — a lane that failed and a lane that was never run are different states, and only one of
them is honest to omit.

Every frame names its build. The builds behind the findings on this page:

| build | what it is |
|---|---|
| `160e8dc4` | the first design build — type ladder, glass budget, contrast API, error and focus states |
| `ea378f48`, `a493c172` | the two specimen layout fixes |
| `f91e4b7e` | the legibility fixture, before the surface-fill fix |
| `37452262` | after `Control` took `Material.Thin` and Emphasis/`reduceTransparency` took the literal ground |
| `ae4bc4ed` | after supporting text moved from `labelTertiary` to `labelSecondary` |
| `37285f06`, `9b0527c8`, `3f71458a`, `2fbc4576`, `90b10316` | the states-gallery layout attempts |
| `6b19ca10` | the focus fixture re-emits, so the ring renders |

FRAMES-DESIGN.md (`evidence/2026-09-17-spatial-ui-polish-native-001/FRAMES-DESIGN.md`) names the build for every
frame, and says for each whether it was opened, measured only, or never looked at. A frame without a
build is a frame about nothing.

# spatial-ui-polish — changelog

Versioning covers the skill text, its references and the component module together, because a call
site written against one is only correct against the matching other.

## 0.3.4 — 2026-09-20

Resolves the two verifier findings 0.3.3 recorded. **No API change, no behaviour change, and no change
to any rendered value** — both edits are to a comment and an annotation's placement.

The vendor `verify-design-style.sh` (0.4.1) reported two `R5 hardcoded color literal` errors, both in
`PolishTokens.kt` and neither in any consuming app. Both are now gone, and the rule that found them is
untouched:

- The KDoc explaining where `OpaqueGround`'s value comes from quoted the SDK token as a `Color(...)`
  call, which is the constructor shape R5 matches. It now states the token's literal value instead. The
  citation and the reasoning are unchanged.
- The `design-style: fixed-figma-color` annotation justifying the literal sat on the line *above* it. The
  verifier honours that hatch only on the matched line itself, so the exception it grants was never
  reaching the literal. The annotation now sits on the literal's own line, carrying the same reason.

`val OpaqueGround = Color(0xFF282828)` is byte-for-byte the same value it has been since 0.3.0. Measured
over both Stage 1 consumers plus this module: `errors: 0`, `warnings: 0`, `PASS`. The failing logs from
0.3.3 are preserved beside the passing ones rather than replaced.

**No frame was re-taken for this.** A comment-only correction does not justify repeating an emulator run,
so every admitted frame, APK digest and module-test result in the Stage 1 evidence stays bound to the
build it was actually taken from, which predates these two edits.

A review of this entry then narrowed two sentences in `SKILL.md` and `references/adoption.md`. They said
the verifier reported this pair from 0.3.0 onward; only the 0.3.3 run was ever measured. Both source
constructs do go back to 0.3.2, so the earlier versions very likely behaved the same way, but that is an
inference from the source and the documents now say so. No version of this kit has ever been packed at
0.3.4, so these corrections land in the same unreleased version rather than starting another one.

## 0.3.3 — 2026-09-19

Documentation only. **No API change, no behaviour change, and the module's bytes are unchanged.**
0.3.2 recorded the light-foreground second tier here and in `references/design-system.md`, but the
three pages an adopting app reads first — the `SKILL.md` quick reference, `references/adoption.md`
and `references/component-catalog.md` — still described the collapsed tier and still said no frame
showed the app-owned root. This entry reconciles them with the accepted refinement.

Four reconciliations:

- **Supporting text (`SKILL.md` quick reference; `component-catalog.md`, in the `PolishSurface`
  measurement and the `PolishControls` row).** The recipe is
  `PolishSurfaceDefaults.secondaryForegroundFor(kind, options)`, and its branches are now stated in
  code order: on a light-foreground surface (`Emphasis`, or any surface under `reduceTransparency`)
  the derived opaque tone `lightSecondaryForeground(labelPrimaryLight)` — `LightSecondaryStep` (0.70)
  of the way from `PolishTokens.OpaqueGround` to the light label — or the full `labelPrimaryLight`
  under `increaseContrast`; `labelPrimary` under `increaseContrast` on a dark-foreground surface;
  `labelSecondary` otherwise. All three passages had still listed the first branch as
  `labelPrimaryLight` outright, which is the 0.3.1 defect. They cite build `6cdb7306`:
  `[191,191,191]` on `[40,40,40]` = 8.02:1 (`contrast-ref-03-room.json` for Emphasis at default
  settings, `contrast-ref-04-rt-on.json` for all four surfaces under reduced transparency;
  `detail-08-surface-kinds-AFTER-2x` and `detail-09-rt-all-four-2x` opened), and they say explicitly
  not to copy the tone arithmetic into an app. The `labelTertiary` measurement (build `37452262`,
  `design-009-fix-detailed`) and the `ae4bc4ed` tables stay as history.
- **Reduced transparency (`SKILL.md` quick reference).** The row now states the two-tier result —
  body `[255,255,255]` at 14.74:1 and supporting `[191,191,191]` at 8.02:1 on all four surfaces
  (build `6cdb7306`, frame `ref-04-legibility-rt-on-r1`, gate-admitted and measured;
  `detail-09-rt-all-four-2x` opened) — and that supporting returns to `[255,255,255]` / 14.74:1 with
  `increaseContrast` by design (`contrast-ref-07-rt-ic.json`, frame `ref-07-legibility-rt-ic`). The
  earlier "every one of the eight text pairings is 14.74:1" is kept as a dated fact — build
  `ae4bc4ed`, frame `design-015-rt-on-room` — and named as the collapsed tier 0.3.2 fixed. The row
  adds the app-owned root: `final-14-focus-rt`, build `a78d8109`, ground exactly 40.00 =
  `OpaqueGround`, measured by tool and not opened; and it closes with the hardware limit — emulator
  observations only, no headset frame.
- **The app-owned window root (`adoption.md` §3; `component-catalog.md` under `PolishTheme`).**
  Both pages said no frame in this lane showed the opaque arm. That has been false since 0.3.1:
  `final-14-focus-rt` (build `a78d8109`, 17/17 gate checks) measures it at exactly 40.00, and
  `final-windowroot-pair.json` pairs it against the glass arm on the same build
  (`final-17-focus-glass`). Both pages now give the history — what `fillPrimary` does was
  established on a `PolishSurface` in `design-005-legibility-room` … `design-008-legibility-detailed`
  (build `f91e4b7e`), and the root fix landed in build `37285f06` — then the current evidence (the
  measured opaque arm; the opened glass-arm frames `polish-focus-window` / `polish-focus-window-2`,
  build `be447bda`), then what is still unverified: no opened frame of the root under reduced
  transparency, and no headset frame at all.
- **`PolishSurfaceDefaults` signature block (`component-catalog.md`).** Now lists
  `lightSecondaryForeground(primaryLight: Color, ground: Color = PolishTokens.OpaqueGround, step: Float = LightSecondaryStep): Color`
  and `const val LightSecondaryStep: Float = 0.70f`, the API 0.3.2 added.

Two further edits to `adoption.md`. §1 names the two module-reference forms a static reader can
resolve — a literal `file("<path>")`, or `file(providers.gradleProperty("pico.polish.dir").get())`
with `pico.polish.dir=<path>` in `gradle.properties` — and says the `orNull ?: … ?: rootDir.resolve(…)`
chain the showcase uses is reported as unresolved because it cannot be evaluated without running
Gradle. It also adds the **read-only adoption report**,
`node <package root>/scripts/polish-adoption.mjs --project <app dir> --kit <this skill's directory>`
(exit 0 ready, 10 changes required, 20 unresolved, 30 invalid input), which ships in the same
package as this skill, reads a bounded allowlist, runs nothing, writes nothing, and is not a build or
runtime claim. §2 no longer asserts that a `pico-cli project create` project declares every catalog
alias the module reads: the showcase's catalog — started from the template, then edited to pin
6.1.9 — does, `sdk-source-map.json` records that pico-cli 0.4.4's planar template emitted
`spatialBom = "6.0.0"` when asked for 6.1.9, and the reader is told to check the catalog they
actually have. The `SKILL.md` parenthetical that made the same claim is softened to match.

Hardware limit, unchanged: every measurement cited is an emulator observation; no PICO headset has
run any build of this kit; the Robolectric cross-check of the two contrast lanes is not done.

**A verifier interaction found while adopting this version in two consumer apps (2026-09-19).**
SKILL.md step 1 and adoption.md §9 said the app + module run of the vendor `verify-design-style.sh`
comes back clean. That was measured on 2026-09-16, before 0.3.0 added `PolishTokens.OpaqueGround`. Under
verifier 0.4.1 the app + module run now reports exactly two `R5 hardcoded color literal` errors, both in
`PolishTokens.kt` — the `OpaqueGround` literal (`:168`), whose `design-style: fixed-figma-color`
annotation sits on the line above it while the verifier honours the escape hatch only on the literal's
own line, and a KDoc line (`:157`) quoting the SDK value it mirrors — and none in either app. The module
bytes are deliberately unchanged in 0.3.3 (documentation only); the fix for the next module revision is to
move the annotation onto the literal's line and reword the KDoc. Both documents now say what to expect.

## 0.3.2 — 2026-09-18

Fixes the defect 0.3.1 recorded and withdraws an over-claim 0.3.1 made.

**Light-foreground surfaces get a real second text tier.** `secondaryForegroundFor` returned
`labelPrimaryLight` on any light-foreground surface — the same role its primary uses — so the
supporting line and the title rendered in one colour and rank was carried by size alone.

**The scope was wider than the Emphasis kind.** `usesLightForeground` is
`reduceTransparency || kind == Emphasis`, so with reduced transparency on it was **every** surface.
The retained `contrast-design-015-rt-on` report had shown it all along: all eight pairings, four
surfaces, body and supporting alike, an identical `[255,255,255]` on `[40,40,40]` at 14.74:1. Eight
passing ratios and four missing tiers.

**New public API.** `PolishSurfaceDefaults.lightSecondaryForeground(primaryLight, ground, step)` and
`PolishSurfaceDefaults.LightSecondaryStep` (0.70). The tone is **composed opaque** — 0.70 of the way
from `PolishTokens.OpaqueGround` to the light label — rather than given an alpha, because the ground
is invariably known (a new test pins that across every kind x option x interactive x depth), because
`reduceTransparency` should not be answered with a translucent foreground, and because
`PolishContrast.ratio` refuses a translucent foreground as undecidable, so an alpha-based tone could
never be held to a target. The source role's vibrancy tag is re-applied after the arithmetic: the
level lives in bits 24–31, which a fresh `Color(r, g, b, a)` zeroes, and dropping `Vibrant.None`
(terminate) to `Vibrant.Unspecified` (inherit) changes behaviour inside a vibrant context.

`increaseContrast` steps the tone back up to the full label, symmetric with the dark-foreground
branch.

**Measured on build `6cdb7306`**, same probe geometry as before: Emphasis supporting
`[191,191,191]` at **8.02:1** under default settings and under `reduceTransparency` on all four
surfaces; `[255,255,255]` at 14.74:1 with `increaseContrast`, deliberately. The three
dark-foreground surfaces are unchanged to the digit and primary text is untouched in every state.
Ten new regression tests; `PolishDesignSystemTest` is now 49 and the module 97, 0 failures.

One existing test was **pinning the defect**:
`PolishBehaviourTest.controlsInheritTheEnclosingSurfaceKind` asserted the secondary colour *equals*
`labelPrimaryLight`, which was only a proxy for "took the light branch" and held only while the tier
was collapsed. Its intent is unchanged; the proxy now names the derived tone.

**Withdrawn: a statistical over-claim.** 0.3.1 said the fill-cost experiment showed the earlier
2.03 FPS `reduceTransparency` gap "is not attributable to the compositor material". A null result
cannot establish that. The experiment **did not detect a difference under its own conditions** —
estimated +0.29 sysFps, 95 % CI [−0.61, +1.18] — and the 2.03 gap **remains unexplained**.

The analysis was also wrong in form and is redone: it pooled ~72 per-second samples per arm and ran
Welch on the pools, claiming **142 degrees of freedom for an experiment with ten experimental
units**. The condition was set once per run, so the run is the replication unit. The retained run
data is unchanged and was not re-collected; only the inference is new. Worth noting because it is
not the usual story: pooling flattered the material arm's precision by 1.8x but *understated* the
drawn arm's by 0.5x — the unambiguous defect is the degrees of freedom, not a uniform shrinking of
the standard error.

## 0.3.1 — 2026-09-18

Acceptance of 0.3.0 on the delivered build `a78d8109`, and the four open questions it closed. **No
API change and no behaviour change** — every edit is to `references/design-system.md`. 0.3.0 was
never packaged (the retained `38.2.1` archive predates it and carries the 0.2.0 skill), so this is
the first version of the design revision to ship.

**Three statements this page had been carrying were closed by measurement.**

- **`PolishQuality.Enhanced` is measured.** It returns the same eight contrast ratios as Native to
  the digit, worst pair `controlSupporting` at 8.22, verdict PASS. That is a result, not a gap: on a
  flow whose surfaces all take a compositor material Enhanced changes **0 pixels**, while on the
  drawn, interactive States flow the same switch changes **11,464**. Enhanced acts on drawn
  surfaces; a material surface gives it nothing to add.
- **The app-owned window root is measured.** `final-14-focus-rt` exercises it under
  `reduceTransparency` and its ground measures exactly **40.00**, the literal value of
  `OpaqueGround` (`0xFF282828`). It had been verified by reading the code.
- **The named test pin now exists and runs.**
  `PolishDesignSystemTest.theSixSupposedlyLiteralRolesAreActuallyReadable` was cited by KDoc while
  absent from the tree. 39 tests in that class, 87 in the module, 0 failures, from a forced
  `--rerun-tasks` run.

**One new defect, found by opening images that had only ever been measured.**

The `Emphasis` surface has **no secondary text tier**. Its body and supporting probes both measure
`[255, 255, 255]` and are indistinguishable at 3x magnification, because
`secondaryForegroundFor` hands any light-foreground surface `labelPrimaryLight` — the same role its
primary foreground uses. The kit does that because the SDK's sixteen-role `ColorScheme` has exactly
one light label role; there is no `labelSecondaryLight`. Two consequences: `emphasisBody` and
`emphasisSupporting` are one measurement reported twice in every contrast report in this lane, and
on Emphasis rank is carried by size alone, which falls short of this skill's own first law
("never by one alone"). **Not fixed here** — the delivered build is fixed and its acceptance frames
are taken against it. Recorded for a decision.

> **Superseded by 0.3.2**, which fixes this and finds the scope was wider: it was every surface under
> `reduceTransparency`, not only the Emphasis kind.

**One question closed negatively.**

> **WITHDRAWN IN 0.3.2. Do not cite this paragraph.** Its conclusion over-reads a null result, and
> the p-value behind it came from pooling per-second samples within each run — pseudo-replication,
> claiming 142 degrees of freedom for ten experimental units. The run-level estimate is **+0.29
> sysFps, 95 % CI [−0.61, +1.18]**: no difference detected, and the 2.03 gap **remains unexplained**.
> The paragraph is left standing because the `38.3.0` archive ships this text and a reader of that
> archive should find the correction attached to it rather than find nothing.

"What does the compositor material cost?" A fixture that varies the fill and **nothing** else — same
geometry, clip, edge, strings, type and foregrounds, no interaction — measures 64.24 against 63.95
Spatial-Runtime FPS over five interleaved runs per arm: a 0.28 gap in the *opposite* direction,
p = 0.41, with the per-run averages interleaving. The 2.03 FPS difference checkpoint 002 saw between
`reduceTransparency` off and on is therefore **not** attributable to the material. `appFps` and every
GPU counter were absent in all ten runs, so nothing here attributes anything to app rendering.

## 0.3.0 — 2026-09-17

A design-system revision. 0.2.0 asked whether a surface reacts; this one asks whether it is legible,
and answers it from measured pixels. Everything below was found by running the showcase on the PICO
emulator and looking at admitted frames.

**Eleven builds appear below by their APK sha256 prefix**, and an earlier version of this preamble
said three, which was wrong about its own contents. Each is cited somewhere in this section, in
roughly the order the work happened:

| prefix | what it is |
| --- | --- |
| `a7d4b9b0` | a 0.2.0 development build — the drawn-`fillPrimary` hover pair |
| `a493c172` | the type specimen that fits the window (`design-004-specimen-fit`) |
| `be447bda` | 0.2.0's final build — the interactive-fill fix; the three-glass-layer frame; the focus-window frames |
| `f91e4b7e` | before the surface-fill fix — the four legibility backdrops |
| `37452262` | after the surface-fill fix (`Control` takes `Material.Thin`) |
| `ae4bc4ed` | after the supporting-text fix; the backdrop-invariance, reduce-transparency, increase-contrast, route-probe and motion evidence |
| `37285f06` | the state gallery in one column — the third section already clipped |
| `3f71458a` | the state gallery in two columns — six sections, still overflowing |
| `2fbc4576` | the first paged state gallery, still clipped |
| `90b10316` | the paged gallery that fits, and the focus cell with no ring |
| `6b19ca10` | the re-emitting focus fixture — the first frame in which the ring renders |

### Added — the kit

- **`PolishTypography`, a semantic type ladder of nine jobs across eleven rungs** over the SDK's 18
  roles (`bodyLead` and `body` each carry a multiline variant), plus `ladder()`
  for the specimen screen and the tests. The roles are alphabetical, not a ramp — `labelLarge` and
  `titleSmall` are byte-identical — so the kit chooses which role carries which job rather than
  inventing sizes. Distinct sizes run 28 → 20 → 16 → 14 → 12 with a deliberate **floor at 12 sp**:
  `labelSmall` (11 sp) and `bodyTiny` (10 sp) are not on the ladder. `design-004-specimen-fit` (build
  `a493c172`) renders all eleven entries at once and the weight steps at 16, 14 and 12 sp are each
  visibly distinct; that same frame family caught two layout defects on the way — an eleven-rung
  ladder overflowed the window at the Panel's default 16 dp rhythm, and a four-across row of surface
  kinds at half the window width broke "supporting" mid-word and wrapped the word "Emphasis" itself.

- **The glass budget.** `PolishTokens.MaxGlassDepth = 2`, `LocalPolishGlassDepth`,
  `PolishWindowSurface` (`SystemGlass` / `AppGlass` / `AppOpaque`) and a new `PolishTheme`
  `windowSurface` parameter that seeds it. At most two compositor-material layers in a window,
  **counting the window's own**; a surface over budget takes a drawn role fill and an edge instead of
  a third material. The number comes from `polish-fb-glass-b` (build `be447bda`), where panel, card
  and window were not separable and read as one flat sheet. A drawn surface spends nothing.

- **`PolishTokens.OpaqueGround`** — the literal `Color(0xFF282828)`, the one genuinely opaque fill the
  kit owns, used wherever non-transparency is the point. Also `SpaceSnug` (12 dp, mirroring
  `DimensionTokens.GapMedium`), `RadiusCompanion` (32 dp), `MinTargetSize` (48 dp), `EasingStandard`
  and `PolishTokens.spring(...)`.

- **`PolishContrast` / `PolishContrastResult` / `PolishContrastLimit`** — WCAG 2 contrast that
  **refuses** rather than misleads. With vibrancy on, 10 of the 16 `ColorScheme` roles are sentinels
  that read back as `#FF000000` in the app process, so a ratio computed from two of them reports
  1.00:1 for a pairing the compositor renders perfectly legibly; `ratio` returns
  `Undecidable(limit)` for those, for a translucent background with no stated backdrop, and for an
  uncomposited translucent foreground. `isLargeText` / `targetFor` give the 3:1 allowance to only
  three of the nine rungs — `display`, `windowTitle`, `sectionTitle` — because the kit does not count
  W600 SemiBold as bold.

- **An error state and a focus ring on `PolishSurface`.** `errorMessage` moves the edge to the `error`
  role at the thicker weight and reports the message through the semantics `error` property; the
  caller still owns rendering it as text. The focus ring is *observed, never requested* — the kit adds
  no `Modifier.focusable`, it reads the focus the app's own handling produces. The design system ships
  no focus-visible treatment at all, so the ring is the kit's own addition. Edge precedence is error,
  selected, focused, increased contrast, then the separation cases.

  **Both now have a frame, and the focus one took two attempts.** Because the kit never requests
  focus, the showcase's focus cell first rendered bare: `design-024-states-p2` (build `90b10316`)
  shows it with no ring at all. Emitting `FocusInteraction.Focus()` once did not fix it either —
  `MutableInteractionSource` publishes through a shared flow with no replay and
  `collectIsFocusedAsState` subscribes from its own `LaunchedEffect`, so a single emit races the
  collector and is lost when it wins. The fixture re-emits on an interval, and `design-025-focus-ring`
  (build `6b19ca10`) is the first frame in this lane in which the ring renders. The same two frames
  also show the top of the precedence order: the "Error + selected" cell draws the **red** edge, not
  the accent, and is indistinguishable from the plain "Error" cell beside it.

### Changed — the kit

- **`PolishSurfaceKind.Control` takes `Material.Thin` in both quality modes.** It drew `fillTertiary`
  natively — a 3.9 %-black wash — and over a dark backdrop (`design-007-legibility-dark`, build
  `f91e4b7e`) its own title, body and supporting line were black on near-black, while over a
  checkerboard (`design-008-legibility-detailed`) the pattern read straight through the text. A tint
  is not a surface. `design-009-fix-detailed` (build `37452262`) shows the card fully legible, which
  also answers an open question: **`Material.Thin` does render on the view path.** Element-grade glass
  on small controls is therefore no longer an Enhanced-only feature; Enhanced now means the hairline
  edge and the depth lift.

- **`Emphasis` and `reduceTransparency` take the literal ground, not `fillPrimary`.** See the
  corrected claim below.

- **Supporting text moved from `labelTertiary` to `labelSecondary`**, and `increaseContrast` steps it
  again to `labelPrimary`. (`secondaryForegroundFor` has three branches, and only the last is this
  one: it returns `labelPrimaryLight` on a light-foreground surface first.) Measured from
  `design-009-fix-detailed` (build `37452262`) through uniform probe swatches the showcase renders,
  using the Display-P3 luminance coefficients the capture's own `cICP` chunk declares: Panel
  **3.18:1**, Card **3.26:1**, Control **3.49:1**, all three failing the 4.5:1 that 12 sp W500 normal
  text needs. After the move (`design-010-supporting-fix`, build `ae4bc4ed`): **8.60 / 8.57 / 8.22**,
  all pass — with every *body* pairing byte-identical across the pair (11.92 / 10.58 / 8.84 / 14.74)
  as the sibling control, and all four room-only stability boxes moving 0.000. `PolishField`'s
  placeholder moved for the same reason.

  The `increaseContrast` half is measured too: `contrast-design-016-ic-on.json`
  (`design-016-increase-contrast`, counted `n=10`, build `ae4bc4ed`) reports supporting text at
  **11.92 / 10.58 / 8.84** on Panel / Card / Control and **14.74** on Emphasis — byte-for-byte the
  body ratios, because with the setting on the supporting line is drawn in the role the body uses.

- **`Modifier.polishWindowRoot()` takes the literal ground too.** It painted `ColorScheme.fillPrimary`
  under `reduceTransparency` — the same defect fixed in `PolishSurface`, missed on this path because
  an app-owned root is the rarer case. It now paints `PolishTokens.OpaqueGround`, and **no arm of the
  kit names `fillPrimary` for a ground any longer.** Stated from the source: the showcase exercises
  this function only on its focus window, and the frames of that window (`polish-focus-window`,
  `polish-focus-window-2`, build `be447bda`) were taken with `reduceTransparency` off, so they show
  the glass arm — a translucent light panel with the main window readable through it — not this fix.

- **`reduceTransparency` is measured end to end.** `contrast-design-015-rt-on.json`
  (`design-015-rt-on-room`, counted `n=6`, build `ae4bc4ed`): the ground reads back as exactly
  `[40, 40, 40]` = `#282828`, the foreground as `[255, 255, 255]`, and every one of the eight text
  pairings is **14.74:1**. Opening that frame shows all four surface kinds as the same flat dark
  ground with white text, while the window's own panel still shows the room through it — which is the
  documented split: the setting takes the *app's* surfaces opaque, not the system's window material.

- **Motion is anchored to `MotionTokens`**, the SDK's only public, unrestricted token object: the
  kit's three durations read `durationShort1/2/3` and `EasingStandard` reads `bezierEasingStandard`,
  the curve `Slider`, `ScrollIndicator` and `TabBar` all pass. The kit's first version animated with a
  bare `tween(...)`, which takes Compose's `FastOutSlowInEasing` — a different curve from the shell's.

  **`reduceMotion` was re-measured after the re-anchoring, and still collapses the transition.** Two
  recordings of the same three flow switches on build `ae4bc4ed`, differing only in the app's own
  Reduce motion setting (`emulator/motion/DESIGN-MOTION.md`, `design-motion-{on,reduced}.mp4`,
  `design-motion-runs-{on,reduced}.json`): with the setting **off**, transitions run 9 / 11 / 11
  frames — 150.0 / 183.3 / 183.3 ms — at peak frame-to-frame change 0.455 / 0.618 / 0.761; with it
  **on**, 1 / 1 / 1 frame at 16.7 ms each, peak 1.675 / 2.007 / 2.217. The content swaps in one step
  instead of blending, which is what `PolishTokens.motion()` returning `snap()` looks like from
  outside the app. Two things that pair does *not* establish: it is not a timing instrument for the
  animation (`DurationPanelMillis` is 240 ms and the un-reduced runs measure 150–183 ms, because the
  method's threshold discards a fade's low-amplitude tail), and the shift from 5–6 frames on build
  `be447bda` to 9–11 here is **not** attributed to the easing change — the two builds differ in more
  than the curve and nothing here isolates it.

- `PolishDesignSystemTest` adds 37 tests for the ladder, the contrast object, the glass budget, the
  error and focus states, the supporting-text rung, the motion tokens and the token scales. Counted
  the way 0.2.0 counted, the suite is now 84.

### Changed — the skill

- New reference page references/design-system.md (historical source citation): the type ladder, the
  glass budget, the opaque ground and the measured contrast numbers behind them.
- New reference page references/interaction-contract.md (historical source citation): what a
  click, a hover and a route do and do not prove when the pointer is the emulator's synthetic one.
- `SKILL.md` gained eight quick-reference rows — the type ladder, the glass budget, the opaque-ground
  rule, supporting text at `labelSecondary`, contrast measured from pixels, the platform motion curve,
  the focus ring's observe-never-request contract, and the rule that a screen you need a frame of must
  fit or page rather than scroll. Its reduced-transparency row now names `PolishTokens.OpaqueGround`
  instead of `fillPrimary` and carries the measured `#282828` / 14.74:1. Three entries were added to
  **Common mistakes**: believing a role named "opaque" is opaque, using a 3.9 % fill as a surface, and
  reading a platform log line as an SDK type.
- `references/component-catalog.md` carries the new signatures (`PolishSurface(errorMessage)`,
  `PolishTheme(windowSurface)`, `PolishTypography`, `PolishContrast`, `PolishTokens`,
  `PolishWindowSurface`, the composition locals) and corrects the surface-fill table, the
  `PolishTheme` `colorScheme` default, `PolishField`'s `kind` default and its palette description.

  A later pass corrected nine more claims on that page, each against the source or the artifact:
  `polishWindowRoot` no longer described as the kit's last `fillPrimary` site; the backdrop-invariance
  numbers given their retained artifact and its real region names; the typography tally corrected from
  three/three to four/four at 16 and 14 sp and restated as nine jobs across eleven rungs;
  `secondaryForegroundFor` described as the three branches it has; the six measurable roles described
  as four literal plus two `Vibrant.None`-tagged, which is why the kit's predicate had to become
  `hasUnresolvedVibrancy()`; `errorBorder` re-cited at its declaration (`TextField.kt:940`, not a span
  inside its body); the hover table's sibling column corrected to the counts and the 1.344 its
  artifacts actually record; `PolishField`'s palette ratios attributed to the host-rendered
  Robolectric lane they came from; and one unframed appearance claim ("still reads as glass sitting on
  the window material") dropped rather than kept unsourced.

  The same pass added what the later frames established: the paged state gallery and why a scroller is
  not an option on this emulator, the focus-ring fixture and the two frames that bracket it, the
  `increaseContrast` measurement, and the note that `clip` and `border` share one shape while the hit
  area is the node's rectangle because `clickable` takes no shape.

### Verified — the contrast premise these numbers rest on

Every `polish_contrast.py` report in this lane carries `"premise": "UNVERIFIED"` in its JSON header,
and an earlier version of this changelog quoted the numbers as settled without saying so. The tool
writes that field before anything is looked at, and the premise it names is precise: the ratios mean
something only if a role colour passed to `Modifier.background(...)` is resolved by the compositor
rather than drawn as flat `#FF000000`, and the decisive check is that the `labelPrimary` probe patch
and the `labelTertiary` probe patch render as **different** tones.

They do. In `design-contrast-detailed-fixbuild.json` (frame `design-009-fix-detailed`, build
`37452262`) the `labelPrimary` patch means `[0.000, 0.000, 0.000]` and the `labelTertiary` patch means
`[107.552, 103.057, 98.154]`, each uniform to within 1.7 levels of stddev over 1600 pixels; two
unresolved sentinels would both have read flat black. Opening the frame shows the same thing without
arithmetic — on Panel, Card and Control each probe pair is one black square beside one mid-grey
square, and only on `Emphasis`, where both patches are `labelPrimaryLight`, are they the same white.

**The premise is settled; three limits that report lists are not.** The emulator's compositor is not
the headset's, the iCCP profile these captures also carry is not parsed against the cICP declaration
the tool trusts, and these are 2D legibility targets applied to a spatial UI.

### Corrected — claims this skill published and this phase disproved

- **32 dp is `DimensionTokens.RadiusExtraHuge`** — the radius of `Subwindow`, `TabBar` and `Toolbar` —
  **not** "the system window corner radius". No public API exposes a window container's radius, and
  nothing in the 6.1.9 sources states one. The token is now `RadiusCompanion`.
- **`ColorScheme.fillPrimary` is not opaque on a device.** It is
  `Color.Vibrant.withVibrant(Vibrant.Darker)`, which the compositor resolves by *darkening what is
  behind the surface*. Four admitted frames on build `f91e4b7e`, varying only an app-drawn backdrop,
  rendered the same surface as near-black over the room (`design-005-legibility-room`), **medium
  grey** over #F4F4F4 (`-006-bright`), near-black over #0B0B0B (`-007-dark`), and **the checkerboard,
  darkened and still visible through it** (`-008-detailed`). The kit's own JVM test asserted it *was*
  opaque and passed, because off the spatial platform the scheme falls back to `defaultColorScheme()`
  where `fillPrimary` is the literal `0xFF282828`. The test was right about the fallback and wrong
  about the device.
- **`MotionTokens` springs do not fail safe.** Durations fall back to their documented values when the
  design resources are unreadable; `springEasingArgsRes` catches and returns
  `SpringEasingArgs.Unspecific` = `(NaN, NaN)` (`ext/Resources.kt:107-109`), and `toSpring()` hands
  that straight to `spring(...)`, which then produces an undefined value for the whole run instead of
  an animation. That is the failure path only — a spring token read successfully is fine, and an
  earlier quick-reference row stated the fallback unconditionally. `PolishTokens.spring(...)` validates
  before use. Separately, `bazierEasingRes` falls back to `LinearEasing` rather than to the documented
  curve — a silent downgrade the kit cannot detect.
- **`PolishContrast` refused two of the six roles it promised were measurable.** It asked
  `containsVibrant()` alone, but `vibrantColorScheme()` builds `labelPrimaryLight` as
  `SubWhite100.withVibrant(Vibrant.None)` (`ColorScheme.kt:235`) and `dividerLine` as
  `SubBlack12.withVibrant(Vibrant.None)` (`:246`) — and `Vibrant.None` is the level that *disables*
  the effect (`Vibrant.kt:102`), so their RGB is authoritative. Only four roles (`error`, `alert`,
  `passable`, `interaction`) are literal in both schemes. The private predicate is now
  `hasUnresolvedVibrancy()` = `containsVibrant() && obtainVibrant() != Vibrant.None`: six roles are
  measurable, but not for the reason the old text gave.
- **A hover sibling that was reported as 0.00 was 1.344.** The reference page's hover table said
  "three sibling controls … 0.00" across all three rows. The artifacts record four, four and five
  siblings, and in the drawn-`fillPrimary` row (`hover-photometry-002-opaque-surface.json`, build
  `a7d4b9b0`) `control_rowBelow_Kiln` — the row directly under the hovered one — moved **1.344**
  against the hovered row's **+16.481**. All four room-only stability boxes read 0.000 in that pair, so
  it is not camera drift: it is a small real signal, and it is no longer reported as zero.

### Known limits

- **Every frame in this release is an emulator capture on `<EMULATOR_SERIAL>` (PICO Swan emulator,
  Android 16), admitted by the inherited `render-liveness/1.0.0` package-targeted gate with all 17
  checks unweakened. No PICO headset evidence exists, and no wearer evidence exists.** Unrelated connected-hardware details are omitted from this review derivative.
- After the fix the surfaces are invariant to app-drawn content behind them. The artifact is
  `evidence/2026-09-17-spatial-ui-polish-native-001/emulator/design-backdrop-invariance.json`
  (build `ae4bc4ed`, frames `design-011-fixed-dark` → `design-012-fixed-bright`): the two backdrop
  regions, `backdropStripLeft` and `backdropStripRight`, moved **11.00 → 244.00**, a delta of
  **+233.00** mean gray, while `panelSurfaceFill`, `cardSurfaceFill`, `controlSurfaceFill` and
  `emphasisSurfaceFill` each moved **exactly 0.00**, and all four room-only stability boxes moved
  0.000 so the camera is not the explanation. All eight pairings reported identical ratios to 2 dp on
  all four backdrops. That is invariance to app-drawn content *inside the window*. What is behind the
  **window** — the emulator room — was constant throughout and was not varied, because varying it
  would mean changing Wing.
- **What the emulator's synthetic pointer presents as, and what that does not tell you.** 70 of 70
  `ViewRootImpl` `Space Info` lines produced by this lane's host clicks carry `kind=GAZE_PINCH` and no
  other value (`emulator/logs/route-probe-spaceinfo.log`, build `ae4bc4ed`; the framing is in
  `ROUTE-PROBE.md` beside it). That establishes one thing: **the guest platform labels a host click as
  gaze-plus-pinch**, so no emulator evidence in this lane exercises the direct-touch or controller
  routes. It is **not** an observation of `InteractionKind`. `kind=` is a field in the platform's own
  input-plumbing log line, as `TapEvent3D` and its siblings are — none of them is a Spatial SDK type —
  and the SDK's `KIND.toInteractionKind()` and `SpaceInteractionKind.toInteractionKind()` are both
  `internal`. An earlier version of this bullet claimed code branching on `InteractionKind` "sees
  `GazePinch`"; nothing establishes that, because neither the kit nor the showcase reads
  `InteractionKind` at all (0 grep hits in both trees). The app-layer question stays open and needs a
  build that calls `detectSpatialTapGesture` and logs `SpatialTapValue.interactionKind`.
- **A rendered state gallery proves a state can be drawn, not that it is reachable.** The showcase's
  gallery had to stop scrolling to be evidence at all: this emulator has no host route to a guest
  scroll, so a section below the fold is a section no capture can ever show. Three frames show the
  progression rather than argue it — `design-018-states-overlap` (build `37285f06`) reaches only the
  third section in one column; `design-020-states-matrix-fit` (build `3f71458a`) still clips the
  bottom row with six sections in two columns; and `design-021-states-page1` (build `2fbc4576`) clips
  it again even after the page was cut to four sections. It now **pages**, with the turn bound to a counted `action=galleryPage`, and
  `design-023-states-p1` (build `90b10316`) is the first frame in which a page fits. The focus cell in
  it is driven by a fixture that emits `FocusInteraction.Focus()`, not by real focus traversal.
- The focus ring now has an emulator frame (`design-025-focus-ring`, build `6b19ca10`) and still has
  **no headset frame and no wearer**.
- `Modifier.polishWindowRoot()` now paints `PolishTokens.OpaqueGround` under `reduceTransparency`, but
  **that arm has no frame**: the showcase exercises the function only on its focus window, and those
  frames (build `be447bda`) were taken with the setting off. Nothing in this lane varied what is
  behind the window either.
- No performance numbers. Profiler provisioning is a user action and none was run.

## 0.2.0 — 2026-09-17

First revision driven by a real device rather than by reading more source. Everything here was
found by running the showcase on the PICO emulator.

### Changed — the kit

- **An interactive `PolishSurface` no longer takes a compositor material.** `fillFor` gained an
  `interactive` parameter; when a surface is clickable and its kind would have chosen a material it
  draws `fillLight` instead (a `Panel` or `Card`, matching `ListItem`), or `fillTertiary` (a
  `Control`). Non-interactive surfaces are untouched and keep their glass.

  Why: hovering a clickable, material-filled row produced a capture **byte-identical** to the frame
  before it — zero change across 1962x2018 px — while a built-in `Button` in the same app and the
  same configuration moved +7.14 mean gray. With a drawn fill, the same row at the same pointer
  coordinates moves +16.92 while every sibling control moves 0.00. The SDK explains it: hover is
  rendered out of process by `spatialHoverEffect`, `PicoIndication` deliberately draws only the
  press colour on the spatial platform ("hover layer is rendered by spatial hover effect modifier"),
  and the material-aware `materialSpatialHoverEffect` is `internal` with `TabBar` as its only
  caller.

- New test `interactiveSurfacesNeverTakeAMaterialFill` pins both directions: a clickable surface
  never keeps a material, and a static one never loses it. The suite is now 47 tests.

### Changed — the skill

- `references/component-catalog.md` said `onClick != null` gives you "hover". It does not, on its
  own. The page now carries the measurement, the SDK citations and the rule that replaced it.
- `references/native-glass-contract.md` §4 now separates *applying* `spatialHoverEffect` from
  *getting* a visible hover.
- `SKILL.md` gained two quick-reference rows: one for clickable surfaces, and one against reading an
  effect's presence in source as evidence of its appearance — the failure this release is an
  instance of, committed by this skill's own reference pages.

### Known limits

- Every measurement above is from the emulator. No headset has confirmed any of it.
- No performance numbers. Profiler provisioning is a user action and none was run.

## 0.1.0 — 2026-09-16

First release. Host-side and emulator/headset-pending.

**Skill.** `SKILL.md` plus five references: the native glass contract, the component catalog, the
acceptance ladder, the adoption recipe, and `sdk-source-map.json` — 39 declarations and 7 rulings
read from the resolved SDK sources, each with an archive hash, a member path and a line span.

**Component module** `dev.local.picopolish` (`resources/polish/`): `PolishOptions`,
`PolishCapabilities`, `PolishTokens`, `PolishTheme` + `Modifier.polishWindowRoot`, `PolishSurface`,
`PolishButton` / `PolishIconButton`, `PolishWindowContent` / `PolishSplitLayout`, `PolishToolbar` /
`PolishActionBar`, `PolishControlRow` / `PolishSwitchRow` / `PolishSliderRow` / `PolishSegmented` /
`PolishFactRow`, `PolishField`, `PolishToggleIconButton`.

**Resolved against.** `com.pico.spatial:bom:6.1.9` (design / foundation / platform / core sources
jars hashed in the source map), AGP 8.13.2, Kotlin 2.0.0, compileSdk 35.

**Rulings that differ from simplified vendor documentation**, each decided from source:

- `Material.Thin` is an element material, not a window material: on the window path it maps to 0,
  the same as `None`. A window root never re-applies it.
- `WindowContainer(enableMaterialBackground)` is `Boolean? = null`, meaning *inherit* from the
  target Activity's manifest meta-data, whose default is on. "Default true" describes the effective
  behaviour, not the declared type.
- `PicoTheme` does not provide `LocalContentColor`; its default is `Color.Unspecified`, so ordinary
  `Text` on an app-drawn surface renders the framework default. `PolishTheme` provides it.
- Seventeen built-ins already apply `spatialHoverEffect` internally. There is no `Card` built-in in
  design 6.1.9, and `ColorScheme` has no `accent` role — the accent-like role is `interaction`.

**Four defects found by looking at rendered frames**, none of which the behaviour tests could see:
surfaces stacked their children flush; the quiet button tone was black-on-near-black on an opaque
surface; the surface edge used a dark hairline role on the dark opaque fill, so hierarchy collapsed
under reduced transparency; and a loading button collapsed to its spinner's width. All four are
fixed, with the before/after retained in the run's evidence directory.

**Evidence at this version.** 36 host behaviour and render tests pass under Robolectric (activation,
suppression, semantics, foreground provision, opaque-role choice, the full sixteen-combination
option matrix), and nine host-rendered frames are retained. Those frames are real pixels but not
device evidence: `backgroundMaterial` and `spatialHoverEffect` no-op on the JVM and the scheme
resolves through its non-adaptive fallback, so nothing here claims how the glass looks. Emulator and
headset lanes are recorded in `docs/findings/SPATIAL_UI_POLISH_CHECKPOINT.md`.

**Nine defects found by review and fixed before release.** An independent five-lens review of the
module, the showcase and these documents produced findings that were then put to two skeptics each;
sixteen survived, and de-duplicated to nine distinct defects, all fixed here:

- `loading` was routed through the built-in's `enabled` flag, which is the SDK's *disabled
  presentation* — it multiplies the whole node, spinner included, by `LocalDisableAlpha`. A busy
  button rendered identically to a disabled one. Activation is now suppressed in the lambda.
- `PolishIconButton(selected)` carried the state in colour alone, reported nothing to accessibility
  and silently overrode the caller's tone. The parameter is gone; `PolishToggleIconButton` delegates
  to the design system's `ToggleIconButton`, which reports `toggleableState`.
- `PolishField` let `TextArea` paint the dark label over the kit's own opaque dark fill —
  black-on-dark *inside* the kit — because `TextArea` ignores `LocalContentColor`. It now carries an
  option-aware `TextFieldColors`.
- A disabled `PolishSwitchRow` passed a null callback, which makes the built-in skip its whole
  `toggleable` modifier and drop `Role.Switch` and the toggle state from the accessibility tree.
- Five controls defaulted `kind` to `Card`, so one dropped into an `Emphasis` surface derived the
  dark foreground. `PolishSurface` now publishes `LocalPolishSurfaceKind` and they inherit it.
- A nested `PolishTheme` re-resolved the scheme from scratch, discarding the app's accent for
  exactly the content a comparison column exists to judge. `colorScheme` now defaults to the ambient
  scheme.
- `SKILL.md` and `adoption.md` prescribed a verifier command that hard-fails for any app adopting
  the kit as documented; both now pass the module path and say why.
- "kit animations snap" described behaviour no component has: the kit ships no animations and only
  carries the flag. Restated in three places.
- The reduced-motion test compared spec *classes*, so an inverted mapping still passed; the catalog
  claimed the field's error semantics land on the node a screen reader focuses, which is unverified;
  and a token comment named the wrong platform constant. All corrected.

**Upstream.** Additive to `pico-spatial-agentic-tools` (observed: 0.4.1 on the Claude host, 0.5.0 on
the Codex host). No vendor cache file is modified. The rules referenced are R1–R8 of
`spatial-ui-design-style`, whose `scripts/verify-design-style.sh` stays the admission check.

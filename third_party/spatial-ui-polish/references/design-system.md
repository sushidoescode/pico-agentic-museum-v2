# The design contract

The rules a PICO SpatialUI screen follows to read as one system, each stated so it can be checked,
each carrying the evidence that forced it. Load this page when you are designing or reviewing a
screen. For the API signatures, use component-catalog.md (historical source citation); for what a frame
does and does not prove, acceptance.md (historical source citation); for what a tap's route proves,
interaction-contract.md (historical source citation).

**Evidence base for this page.** Every frame named below is an emulator capture admitted by the
inherited `render-liveness/1.0.0` package-targeted gate with all 17 checks passing, on serial
`<EMULATOR_SERIAL>` — a PICO Swan emulator, guest fingerprint
`<EMULATOR_FINGERPRINT>` —
**with one stated exception**: §8 also cites a *host-rendered* Robolectric frame, named as such at
the point of use, which is not an emulator capture at all. Builds are APK sha256 prefixes read off
the device by the gate itself (`adb shell sha256sum` on the installed `base.apk`, before and after
the window, both recorded). Every build named anywhere on this page:

| build | what changed | where this page cites it |
| --- | --- | --- |
| `384aea19` | layout fixes | §1(a), the material-filled hover pair |
| `a7d4b9b0` | action logging | §1(a), the drawn-fill hover pair |
| `be447bda` | the interactive-fill fix; the delivered build of the earlier checkpoint | §5, the three-layer glass frame |
| `a493c172` | the type specimen, after both layout defects were fixed | §2 |
| `f91e4b7e` | before the surface-fill fix | §4 |
| `37452262` | `Control` takes `Material.Thin`; `Emphasis` and `reduceTransparency` take the literal ground | §6 |
| `ae4bc4ed` | supporting text moves from `labelTertiary` to `labelSecondary` | §4, §6, §7, §9 |
| `37285f06` | `polishWindowRoot` fixed; the vibrancy predicate corrected; the focus fixture added | §4, §6, §8 |
| `3f71458a`, `2fbc4576` | States-gallery layout attempts that still overflowed | §8 |
| `90b10316` | the States gallery pages fit; the focus cell renders no ring | §8 |
| `6b19ca10` | the focus fixture re-emits, so the ring renders | §8 |

The per-frame inventory — bytes, capture digest, and whether each frame was **opened and judged**,
**measured only**, or never looked at — is
FRAMES-DESIGN.md (`evidence/2026-09-17-spatial-ui-polish-native-001/FRAMES-DESIGN.md`).

**No PICO headset evidence exists, and no wearer evidence exists.** The only physical headset
attached during this work was a Meta Quest 3 (`<DEVICE_IDENTIFIER>`), which is not PICO acceptance
hardware; **nothing from this kit was installed on it**
(attached-headset-identity.json (`evidence/2026-09-17-spatial-ui-polish-native-001/physical-device/attached-headset-identity.json`)).
Read §9 before citing anything here as settled.

---

## 1. The three laws

### (a) One surface, one fill

A surface takes **either** a compositor material **or** a drawn colour. Never a material with a
colour painted over it, never two materials on one node, never a `ListItem` (which paints
`fillLight` itself) inside a painted card. `PolishSurface` makes this structural: `PolishFill` is a
sealed interface with exactly two cases, `Glass(style)` and `Solid(color)`
(`PolishSurface.kt:76-80`), and the decoration chain applies one of them in a single `when`
(`PolishSurface.kt:170-174`). **Clip and border take the same `shape`**, so the visible edge and the
fill cannot disagree. The *hit area* does not: `clickable` takes no shape, so the touch target is the
node's rectangle and a heavily rounded surface is clickable slightly outside its visible corners. The
kit does not correct that — the framework's own components behave the same way — and its KDoc says so
(`PolishSurface.kt:96-98`). An earlier version of this page called it a three-way agreement. It is a
two-way one.

Two things force a surface off glass and onto a drawn fill, and both are measured, not stylistic:

- **Interactive surfaces never take a material.** Hovering a material-filled clickable surface at
  fixed host coordinates produced a capture byte-identical to the frame before it — zero change
  anywhere in 1962×2018 px, with all **four** sibling controls in that pair at **0.00**
  (`hover-photometry-003-material-surface-control.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/hover-photometry-003-material-surface-control.json`),
  build `384aea19`). The same surface at the same host coordinates with an app-drawn fill moved
  **+16.48 mean gray**; of the four siblings in *that* pair, three moved **0.00** and one —
  `control_rowBelow_Kiln` — moved **1.344**
  (`hover-photometry-002-opaque-surface.json` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/hover-photometry-002-opaque-surface.json`),
  build `a7d4b9b0`).

  **The two results are different builds, and that is what the comparison is.** Each is a
  self-contained same-build, same-pose pair; the comparison runs *across* the fix, which is why the
  builds differ and are named — the repo's checkpoint states it the same way
  (SPATIAL_UI_POLISH_CHECKPOINT.md (historical source citation), §2).
  An earlier version of this page said "same build". It was wrong. The SDK's own
  reason is in `PicoIndication`, which draws only the press colour on the spatial platform and says
  *"hover layer is rendered by spatial hover effect modifier"*; the material-aware hover path
  `Modifier.materialSpatialHoverEffect` exists but is `internal`, with `windows/TabBar.kt` as its
  only caller. So an interactive Panel or Card falls to `fillLight` (`ListItem`'s role,
  `ListItem.kt:293`) and an interactive Control to `fillTertiary` — `PolishSurface.kt:321, 346-357`.
- **The glass budget** — §5.

### (b) Rank is carried by size *and* weight, never by one alone, and never by colour

The kit's first version broke this: an object title at `titleSmall` (14 sp / W600) sat directly
above body text at `bodyMedium` (14 sp / W500) — a 100-unit weight step at identical size, which is
the weakest distinction the SDK can express. `PolishTypography.objectTitle` is `titleMedium`
(16 sp) for exactly that reason (`PolishTypography.kt:86-87`).

Colour is never the rank signal. It is not available as one: with vibrancy on, the label roles are
compositor-resolved sentinels whose rendered value the app cannot read (§4), and the gap between
two of them is not a quantity the app controls. Colour distinguishes *kind* of text (primary vs.
supporting vs. error), not *rank*.

The same rule governs state: selection is a thicker edge **and** an accent role, error is a thicker
edge **and** a semantics `error` property (§8).

### (c) Glass marks the page, not the parts

At most **two** compositor-material layers in one window, **counting the window's own**
(`PolishTokens.MaxGlassDepth = 2`, `PolishTokens.kt:123`). §5.

---

## 2. The type ladder

`PicoTheme.typography` ships **18 roles** in 6.1.9 (`design/Typography.kt:45-62`, recorded in
sdk-source-map.json (historical source citation)) and they are *alphabetical, not a ramp*: **four** are
16 sp (`bodyLarge`, `bodyLargeMultiline`, `headlineSmall`, `titleMedium`), **four** are 14 sp
(`bodyMedium`, `bodyMediumMultiline`, `labelLarge`, `titleSmall`), two are 12 sp (`bodySmall`,
`labelMedium`), and two are 20 sp (`headlineMedium`, `titleLarge`). "Pick a role" is therefore not
the same question as "pick a rank". `PolishTypography` does not invent sizes — inventing a size would
fight the shell, which draws its own chrome with these roles — it assigns **nine jobs** to nine roles
and states the step between consecutive rungs so the choice is checkable.

**Nine jobs, eleven rungs.** `bodyLead` and `body` each carry a multiline variant, so
`PolishTypography.ladder()` returns eleven entries and the specimen renders eleven
(`PolishTypography.kt:9-12, 141-154`).

| Rung | SDK role | size / weight / line height | step to the rung above | What it is for |
| --- | --- | --- | --- | --- |
| `display` | `displaySmall` | 28 / 700 / 34 | — | a hero line for a focus or detail window; one per app at most |
| `windowTitle` | `headlineMedium` | 20 / 700 / 26 | 1.40× | the window's own name; one per window |
| `sectionTitle` | `headlineSmall` | 16 / 700 / 20 | 1.25× | the header of a group of controls inside a window |
| `objectTitle` | `titleMedium` | 16 / 600 / 20 | same size, −100 weight | the subject of one row, card or list item |
| `bodyLead` | `bodyLarge` / `bodyLargeMultiline` | 16 / 500 / 20 or 24 | same size, −100 weight | the lead paragraph of a panel |
| `body` | `bodyMedium` / `bodyMediumMultiline` | 14 / 500 / 18 or 22 | 1.14× | ordinary prose |
| `control` | `labelLarge` | 14 / 600 / 18 | same size, +100 weight | text inside a control: a button label, a segment, a chip |
| `caption` | `bodySmall` | 12 / 500 / 16 | 1.17× | a supporting line under a title or a value |
| `overline` | `labelMedium` | 12 / 600 / 16 | same size, +100 weight | the small label naming a value in a details pane |

Distinct sizes: **28 → 20 → 16 → 14 → 12**. Weight does the rest — 700 for the two header rungs,
600 for titles and control text, 500 for prose. Source for the numbers: 6.1.9
`design/tokens/TypeScaleTokens.kt:148-203` and `design/tokens/TypographyTokens.kt:33-140`, cited
from the kit's KDoc; neither file has an entry in sdk-source-map.json (historical source citation), whose
39 declarations stop at `Typography`'s role list.

**The `multiline` pair is not optional decoration.** `bodyLargeMultiline` and `bodyMediumMultiline`
are the same size and weight with the line height opened 20→24 and 18→22. Pass `multiline = true`
whenever the text can wrap. The kit's first version never used either, so every wrapped paragraph
in the showcase was set at single-line leading.

### The floor is 12 sp, and it is deliberate

The SDK ships `labelSmall` at 11 sp and `bodyTiny` at 10 sp. **Neither is on this ladder.** A label
that names a value is text someone reads at arm's length through a compositor; the two smallest
roles in the scale are the shell's dense chrome, not app content. `PolishTypography.kt:97-106`
states this at the `overline` rung, which is where the temptation to reach for `labelSmall` arises.

### The trap: two roles are byte-identical

`labelLarge` and `titleSmall` are both **14 sp / 18 lh / W600** over the same base style in 6.1.9.
A screen that uses one for a row title and the other for a button label has expressed **no**
distinction at all. This is the concrete reason the ladder exists rather than a convention that
says "use the `title*` family for titles".

### What a frame settled, and what it did not

`design-004-specimen-fit` (build `a493c172`) renders all eleven ladder entries in one frame — the
nine rungs plus the two multiline variants — each labelled with its role name and its measured
`size/weight` read out of the live `TextStyle`. Opened and judged: the weight steps at 16 sp
(700 → 600 → 500), at 14 sp (500 → 600) and at 12 sp (500 → 600) are each visibly distinct, and the
size steps are unambiguous.

**That settles relative rank only.** An emulator capture frames a whole room, the window occupies a
small part of it, and no arithmetic on that image recovers the angular size of a 12 sp glyph in a
headset. Absolute legibility at wearing distance is undecided. See
acceptance.md (historical source citation).

### Two layout defects that only a frame found

Both were invisible in source review and in the JVM tests, and both are fixed in `a493c172`:

- **Eleven rungs at the Panel's default 16 dp rhythm overflowed the window.** 16 dp × 10 gaps is
  160 dp spent on gaps alone, which pushed the last two rungs below the window edge. The ladder
  now passes `verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)` — the samples
  are one group, so they take the group's own step (`SpecimenFlow.kt`, `LadderSpecimen`).
- **A four-across row of surface kinds at half the window width broke "supporting" mid-word** and
  wrapped the word "Emphasis" itself. At that width each card got roughly 110 dp. The specimen is
  now 2 × 2 (`SpecimenFlow.kt`, `SurfaceKindSpecimen`).

The second one is the generalisable finding: **a width that is legal is not a width that reads.**
Nothing in the layout system objects to 110 dp; only the frame does.

---

## 3. Spacing, shape, depth

The SDK's own scale lives in `design/tokens/DimensionTokens.kt`, which is
`@RestrictTo(RestrictTo.Scope.LIBRARY)` with an `internal companion object` — an app **cannot read
it**. A kit that wants to agree with the shell has to mirror the numbers and cite them. Every token
below names the SDK token it mirrors, or says plainly that it has none.

### Spacing

| Token | dp | SDK anchor | Use |
| --- | ---: | --- | --- |
| `SpaceTiny` | 4 | **none** — the SDK's smallest gap is 8 dp | two lines of one thought: a title and its caption |
| `SpaceSmall` | 8 | `GapSmall` / `PaddingSmall` | related items in a row or list |
| `SpaceSnug` | 12 | `GapMedium` | a label and its control, where 8 crowds and 16 breaks the pair |
| `SpaceMedium` | 16 | `GapLarge` / `PaddingRegular` | the default gap between controls |
| `SpaceLarge` | 24 | `PaddingMedium` | between groups inside a panel; the default card padding |
| `SpaceHuge` | 32 | `PaddingLarge` | between major regions |

`SpaceTiny` is the one step the kit added below the SDK's rhythm, and it is marked as having no
anchor rather than described as if it did.

### Shape

| Token | dp | SDK anchor | The built-in that uses it |
| --- | ---: | --- | --- |
| `RadiusCompanion` | 32 | `RadiusExtraHuge` | `Subwindow`, `TabBar`, `Toolbar` |
| `RadiusPanel` | 24 | `RadiusHuge` | `SideNavigation` |
| `RadiusCard` | 16 | `RadiusLarge` | `ListItem`, `Button`/`Chips` Regular |
| `RadiusControl` | 12 | `RadiusMediumLarge` | `Option`, `TextField` |
| `Hairline` | 1 | **none** | one physical hairline edge |
| `HairlineStrong` | 2 | **none** | a state edge, thicker so state is not colour-only |

**Correction to a previously published claim.** The kit used to call 32 dp *"the system window
corner radius"*. It is not. 32 dp is `DimensionTokens.RadiusExtraHuge`, the radius of `Subwindow`
(`windows/Subwindow.kt:134`), `TabBar` (`windows/TabBar.kt:759`) and `Toolbar`
(`windows/Toolbar.kt:311`). **No public API exposes a window container's radius**, and nothing in
the 6.1.9 sources states one. Use `RadiusCompanion` when an app surface must sit flush beside a
system companion window — never to guess at the window panel itself.

`SpaceHuge` also equals the `Subwindow` gap-plus-radius family, but it is anchored to
`PaddingLarge`, not to a window radius. The coincidence is not the citation.

### Depth and target size

| Token | value | Anchor |
| --- | --- | --- |
| `LiftZ` | 8 dp | **none** — how far an interactive Enhanced surface lifts toward the viewer |
| `MaxGlassDepth` | 2 | **none** — the kit's own rule; §5 |
| `MinTargetSize` | 48 dp | the Compose fork's `ViewConfiguration.minimumTouchTargetSize`, `DpSize(48.dp, 48.dp)` (`ui-android:1.2.5`, `ViewConfiguration.kt:53-54`) |

`MinTargetSize` carries a caveat worth repeating at design time: **several shipped SDK sizes are
visually smaller than 48 dp** — `ButtonDefaults.Small` (40), `ButtonDefaults.Min` and
`IconButtonDefaults.Min` (32), `ChipsDefaults.Small` (32), `CheckBoxDefaults.CheckboxSize` (32).
Their *hit* area is expanded to 48 dp by `clickable`, so they are reachable; their *visible*
affordance is not.

**"Regular" is not a synonym for 48 dp either.** Only `ButtonDefaults.Regular` (`Button.kt:312`) and
`IconButtonDefaults.Regular` (`IconButton.kt:114`) are 48 dp tall. `ChipsDefaults.Regular` is
`chipSize(40.dp)` (`Chips.kt:592`), and `ToggleButtonDefaults.Regular` aliases `ButtonDefaults.Min`
at **32 dp** (`ToggleButton.kt:113`). At panel distance prefer the two that really are 48 dp, and
reserve the rest for dense clusters the user is already focused on (`PolishTokens.kt:172-190`). Note
the version qualifier: 48 dp is a `1.2.5` Compose-fork fact, not a "6.1.9 API".

---

## 4. Colour and hierarchy

`ColorScheme` has **exactly 16 public roles** in 6.1.9 (`design/ColorScheme.kt:49-67`). There is no
`accent` / `onAccent` role — the accent-like semantic role is `interaction`, and the foreground that
pairs with an accent fill is `labelPrimaryLight`.

### Which role does which job

| Job | Role |
| --- | --- |
| a surface that must read as the primary object | `PolishTokens.OpaqueGround` — see below, **not** `fillPrimary` |
| a clickable Panel or Card's drawn fill | `fillLight` (`ListItem`'s role) |
| a clickable or over-budget Control's drawn fill | `fillTertiary` (the scheme's "least important elements" role, `ColorScheme.kt:53`) |
| a nested static surface that gave up its material | `fillTertiary`, plus a mandatory edge (§5) |
| primary text on a light surface | `labelPrimary` |
| primary text on the opaque ground | `labelPrimaryLight` |
| supporting text | `labelSecondary` — **not** `labelTertiary`; §6 |
| supporting text under `increaseContrast` | `labelPrimary` (rank then rests on size and weight alone, which is what the setting is for) |
| selection edge, focus ring, accent | `interaction` |
| error edge and error message | `error` |
| the system hairline | `dividerLine` |
| the stronger hairline on a light fill | `labelQuaternary` |
| the hairline on a dark fill | `fillSecondary` |

`PicoTheme` provides the colour scheme, the typography, the indication and the design tokens, but
**it does not provide `LocalContentColor`** (`design/PicoTheme.kt:53-69`). `Text` resolves explicit
colour, then style colour, then that local (`design/Text.kt:101-108`), and neither the default text
style nor any typography role carries a colour. A plain `Text("Hello")` on an app-drawn surface
inside `PicoTheme` alone therefore renders the framework default — **black** — which is how
dark-on-dark panels happen. `PolishTheme` provides it (`PolishTheme.kt:73`); the built-ins are fine
because each provides its own internally.

### Ten of sixteen roles are compositor-resolved sentinels

With vibrancy on, `fillPrimary`, `fillSecondary`, `fillTertiary`, `fillLight`, `labelPrimary`,
`labelSecondary`, `labelTertiary`, `labelQuaternary`, `lightenHover` and `lightenPressed` are
`Color.Vibrant.withVibrant(level)` values: `Color.Black` with a level written into bits 24–31 of the
`ULong`, bits that `Color.red/green/blue` and `toArgb()` never read
(`ColorVibrantEncoder.kt:24, 65-81`; `Color.kt:53-58` — *"This is NOT a standard RGBA color"*). All
ten read back as `#FF000000` in the app process. The real pixel is produced out of process by the
system compositor.

The **six measurable** roles are `labelPrimaryLight`, `error`, `alert`, `passable`, `interaction`
and `dividerLine`. Four of them — `error`, `alert`, `passable`, `interaction` — are fixed DisplayP3
semantic colours carrying no vibrancy tag at all. The other two are *not* untagged, and calling all
six "literal" was the error that produced a bug: in the vibrant scheme `labelPrimaryLight` is
`SubWhite100.withVibrant(Vibrant.None)` (`ColorScheme.kt:235`) and `dividerLine` is
`SubBlack12.withVibrant(Vibrant.None)` (`ColorScheme.kt:246`). Both carry an encoding whose level
**disables** the effect, which is why their RGB is authoritative — and why a predicate that asked
only "does it carry an encoding" refused them. See §6, Lane A.

Off the spatial platform, `systemColorScheme` falls back through `adaptVibrantColor` to
`defaultColorScheme()` (`ColorScheme.kt:184-201`), whose 16 roles are **all** literal ARGB. That
fallback is why a JVM test can be simultaneously correct and useless — see below.

Several roles are translucent in that fallback and must be composited before any ratio is taken:
`fillSecondary` 40 % white, `fillTertiary` 3.9 % black, `fillLight` 14.9 % white, `labelSecondary`
80 % black, `labelTertiary` 65 %, `labelQuaternary` 40 %, `dividerLine` 12.2 %. None of them is
"opaque", whatever the name suggests.

### The `OpaqueGround` rule

> **Where non-transparency is the point, use `PolishTokens.OpaqueGround` — the literal
> `Color(0xFF282828)`. Never a vibrancy role.**

`ColorScheme.fillPrimary` — the role an app would reach for as "the opaque fill" — is
`Color.Vibrant.withVibrant(Vibrant.Darker)`, which the compositor resolves **by darkening what is
behind the surface**, not by filling it. Four admitted frames on one build, varying only an
app-drawn backdrop inside the window and nothing else:

| backdrop | how the `Emphasis` surface rendered | frame | window crop opened |
| --- | --- | --- | --- |
| the emulator room (control; app draws nothing) | dark charcoal | `design-005-legibility-room` | `design-005-window.png` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/crops/design-005-window.png`) |
| bright, `#F4F4F4` | **medium grey** | `design-006-legibility-bright` | `design-006-window.png` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/crops/design-006-window.png`) |
| dark, `#0B0B0B` | near-black | `design-007-legibility-dark` | `design-007-window.png` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/crops/design-007-window.png`) |
| black-and-white checkerboard | **the checkerboard, darkened and still visible through the surface** | `design-008-legibility-detailed` | `design-008-window.png` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/crops/design-008-window.png`) |

All four on build `f91e4b7e`, and **all four window crops were opened and judged** — not only the
last row. The last row is the decisive one: in `design-008-window.png` the checker squares are
plainly visible *inside* the Emphasis card, darkened but intact, and the same crop shows the
`Control` surface's title, body and supporting line as black on near-black.

**The kit's own JVM test asserted `fillPrimary` was opaque, and passed** — because off the spatial
platform the scheme resolves to `defaultColorScheme()`, where `fillPrimary` really is the literal
`0xFF282828`. The test was right about the fallback and wrong about the device. This is the general
failure mode in its sharpest form: *an API's declared value is not its rendered result.*

`OpaqueGround` mirrors the literal that the SDK's own non-vibrant `ColorTokens.FillPrimary` carries
(`design/tokens/ColorTokens.kt:131`), mirrored rather than read because `ColorTokens` is
`@RestrictTo(LIBRARY)` with an `internal` companion. It pairs with `labelPrimaryLight`, a literal
white in both schemes.

**What it costs:** it does not adapt to the room the way a vibrancy role does. That is the trade
`reduceTransparency` asks for. Use it *only* where non-transparency is the point — under
`reduceTransparency`, and for `PolishSurfaceKind.Emphasis` — never as a general fill.

It is verifiable in the rendered pixels: on all four backdrops the Emphasis probe background samples
**exactly `[40, 40, 40]`** — `0x282828` — with per-channel stddev `0.000`
(`contrast-design-011-fixed-dark.json`, `-012-fixed-bright.json`, `-013-fixed-room.json`, and
`design-contrast-detailed-supportingfix.json`).

**All three sites follow this rule now, and one of them was a live bug.**
`Modifier.polishWindowRoot()` used to paint `PicoTheme.colorScheme.fillPrimary` under
`reduceTransparency` — a window root filled with a sentinel the compositor resolves by *darkening
what is behind it*, which is the one thing that setting exists to stop. It paints
`PolishTokens.OpaqueGround` now (`PolishTheme.kt:104-113`, with the history kept in its KDoc at
`:97-101`), fixed in build `37285f06`. Two stale KDoc blocks went with it:
`PolishSurfaceDefaults.fillFor`'s doc names `OpaqueGround` (`PolishSurface.kt:254-260`) where the
code returns it (`:303`), and `hoverableFillFor`'s `Emphasis` arm returns `OpaqueGround` and says in
the same breath that the arm is unreachable, because `fillFor` returns an Emphasis surface's solid
fill first (`:353-356`, `:318`).

**The window-root fix is measured.** `final-14-focus-rt` on build `a78d8109` exercises an app-owned
window root under `reduceTransparency`, and the ground measures exactly **40.00** — the literal
value of `OpaqueGround` (`0xFF282828`, 0x28 = 40). An earlier version of this page kept it as a gap
in §9 on the grounds that no frame exercised it; one does now.

---

## 5. The glass budget

> **At most two compositor-material layers in one window, counting the window's own.**

`PolishTokens.MaxGlassDepth = 2` (`PolishTokens.kt:123`). Restraint is the design goal; this is the
number that makes it enforceable rather than advisory. Two layers means: the window panel the system
draws, and at most **one** app surface on top of it.

### The frame that motivated it

A card inside a panel inside a window is three layers. In an opened frame
(`polish-fb-glass-b.png`, build `be447bda`) the three were **not separable** — panel, card and
window read as one flat sheet. Glass does not stack into hierarchy; past the second layer it stops
carrying information and starts costing legibility.

### How it is enforced

`LocalPolishGlassDepth` is a composition local. `PolishTheme` seeds it from
`PolishWindowSurface.baseGlassDepth` — `1` for `SystemGlass` and `AppGlass`, `0` for `AppOpaque`
(`PolishOptions.kt:127-129`) — and `0` outright when `reduceTransparency` is on
(`PolishTheme.kt:63`). Every `PolishSurface` that actually takes a material increments it for its
children; a surface that drew a colour does not, because a drawn surface is one paint, not another
compositor layer for the system to resolve (`PolishSurface.kt:210-212`).

`PolishSurfaceDefaults.fillFor` checks `glassDepth >= MaxGlassDepth` and, when it is spent, returns
a drawn role fill instead (`PolishSurface.kt:322`).

### What an over-budget surface does instead

It is **not removed**. It is still there, still shaped, still legible. It takes `fillTertiary` —
the quietest translucent role, chosen because a nested static surface should sit inside its parent
and be told apart by tone rather than compete with it — and, critically, it takes a **mandatory
edge**. `edgeFor` draws a hairline whenever a *static* surface had to give up its material
(`PolishSurface.kt:461-470`): a 3.9 %-black wash over its parent without an edge is not a surface,
it is a smudge.

Note that the over-budget fill is deliberately **not** the same role as the interactive fallback.
A clickable surface needs a fill the compositor can *lighten* on hover, so it takes the role the
closest interactive built-in uses. A nested static surface needs the opposite.

### The one thing the kit cannot detect

A window container's material is resolved **when the container is declared** — manifest meta-data
`pico.spatial.windowcontainer.materialbackground` for the launcher window, `enableMaterialBackground`
for a DSL `WindowContainer` or `Augment` — and **there is no runtime read-back**. So the app states
it, through `PolishTheme(windowSurface = ...)`. A theme that misdeclares it will let one material
too many stack, or one too few. `enableMaterialBackground` is `Boolean? = null`, not
`Boolean = true`: null means *inherit from the manifest*, and the manifest default is REGULAR
(`core/container/WindowContainer.kt:53-54`, `SpatialContainerManager.kt:1194-1195`).

`Material.Thin` is **not** a window material: `toFrameworkWindowBackgroundInt` maps it to `0`,
identical to `None` (`platform/Material.kt:41-48`). It is the element tier — *"Box, Button,
CheckBox"* in its own KDoc — and that is where the kit uses it.

---

## 6. Contrast

There are **two lanes** and they answer different questions. Reporting one as the other is the
mistake this section exists to prevent.

### Lane A — JVM role-value computation (`PolishContrast`)

Valid **only where a role's own channels are what gets drawn**: off the spatial platform (a JVM or
Robolectric test, a preview, a non-PICO build, where `systemColorScheme` falls through to
`defaultColorScheme()`), for the six roles whose channels survive the vibrant scheme (§4), and for
app-owned colours.

It **refuses rather than misleads**. `PolishContrast.ratio` returns
`PolishContrastResult.Undecidable(limit)` when either colour's channels will be *replaced* by the
compositor, when the background is translucent with no stated backdrop, or when the foreground is
translucent (`PolishContrast.kt:173-194`). `PolishContrastLimit` names six distinct reasons. A ratio
computed from two sentinels is not a wrong number — it is a number about the wrong thing: it would
report black on black, 1.00:1, for a pairing the compositor renders perfectly legibly.

**The refusal test is not "does it carry a vibrancy encoding".** It is
`hasUnresolvedVibrancy() = containsVibrant() && obtainVibrant() != Vibrant.None`
(`PolishContrast.kt:73-74`). The kit's first version asked only `containsVibrant()`, and therefore
refused `labelPrimaryLight` and `dividerLine` as undecidable — on a device, silently, and for exactly
the two of the six roles its own documentation promised were measurable (§4). Corrected in build
`37285f06`. So **"six roles are measurable" is true now, and true for a reason the earlier text got
wrong**: those two roles *do* carry an encoding, and what makes them readable is that the level is
`Vibrant.None` — the one that disables the effect — not the absence of a level.

One caveat on that fix's own evidence. The KDoc names a test,
`PolishDesignSystemTest.theSixSupposedlyLiteralRolesAreActuallyReadable`, as the pin
(`PolishContrast.kt:69-70`), and the expectation constant it would read is in the file
(`EXPECTED_ENCODED_ROLES = {labelPrimaryLight, dividerLine}`, `PolishDesignSystemTest.kt:678`) — but
**no test function of that name exists in the tree**, and none appears in the retained
`testDebugUnitTest` results. The predicate is verified by reading the code; the pin is a dangling
reference.

`ratioOver(foreground, background, backdrop)` is the form to use for a real pairing, because a role
fill is usually translucent and the text sits on top of it. It composites background onto backdrop,
then foreground onto that (`PolishContrast.kt:203-224`).

Treat the four DisplayP3 semantic roles' ratios as **sRGB approximations**: `PolishContrast`
converts no colour space.

### Lane B — rendered device pixels (`polish_contrast.py`)

**The only valid lane on a device.** The showcase renders, beside the real text, **three** uniform
probe boxes per surface kind — not a pair (`LegibilityFlow.kt`, `ProbePair`, lines 138-168):

- `probe-bg-<kind>` draws *nothing at all*, so what is sampled there is the surface's own composited
  fill showing through, which is precisely the background the text sits on;
- `probe-fg-<kind>` is filled with `LocalContentColor` — the surface's body foreground;
- `probe-fg2-<kind>` is filled with the supporting-text role, the pairing most likely to fall short.

All three are flat, so the box mean **is** the colour, with no antialiased glyph edge to bias it.
Only two of the three read as boxes in a frame; the background one is a hole.

Measurement details that matter for reproducing a number:

- **Coefficients.** `0.2289746, 0.6917385, 0.0792869` — the second row of the Display-P3-to-XYZ
  (D65) matrix — chosen because the captures' own `cICP` chunk reads `0c 0d 00 01`: colour
  primaries **12** (Display P3), transfer characteristics **13** (sRGB). Both coefficient rows sum
  to 1.0, so for a **neutral** pair the P3-vs-sRGB choice moves the answer by about one float ULP;
  it only matters for a saturated pair. Do not present the P3 row as the reason a neutral result
  came out the way it did.
- **Refusals.** A box that is clipped by the capture edge, encloses no area, carries a non-opaque
  pixel, or whose per-channel stddev exceeds the uniformity tolerance is reported **INVALID**, never
  averaged. A stability region that moved discards the whole run (**DISCARD**); a stability region
  that sampled no pixels reports **UNGATED**, because a misplaced gate box is a different mistake
  from a drift.
- **Verdict epsilon.** `1e-4`, the same tolerance as `PolishContrast.Tolerance`, so the two lanes
  cannot disagree about a borderline pairing. Both the 2 dp display value and the unrounded value
  the verdict was taken on are reported.
- **A premise the tool reports unverified in every report it writes.** The foreground patch is
  `Modifier.background(LocalContentColor.current)`, and that colour may itself be a sentinel; if it
  were drawn as flat `#FF000000` the whole lane would be measuring a placeholder. The decisive check
  is that the `probe-fg-*` patch and the `probe-fg2-*` patch render as **different** tones. Opened
  and judged in `design-005-window.png` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/crops/design-005-window.png`) (build
  `f91e4b7e`, Room control): on Panel and on Card the two patches read as plainly different tones —
  the first black, the second a mid grey — which is what the premise predicts and not what a
  flat-sentinel failure would look like. **That is a human look at one frame, not a check the tool
  performs**, so every report still carries `premise: UNVERIFIED` and this page does not upgrade the
  tool's verdict.

### Targets, and which rungs qualify as large text

Default target **4.5:1** (`PolishContrast.NormalText`). **3:1** (`LargeText` / `NonText`) applies to
large text and to the visual boundary of a graphical object — a switch track, a selection edge, a
field border, a focus ring.

`isLargeText` implements WCAG's "18 pt, or 14 pt bold" in the conventional Android reading —
**18 sp, or 14 sp bold** — and takes "bold" as `FontWeight.Bold` (700) or heavier
(`PolishContrast.kt:115-121`). **The SDK's `title*` and `label*` roles are W600 SemiBold and do not
qualify.** That is deliberately stricter than counting 600 as bold. A style whose size is
unspecified or expressed in `em` returns `false`, the stricter answer.

**Only three of the nine rungs qualify for the 3:1 allowance:** `display` (28/700), `windowTitle`
(20/700), `sectionTitle` (16/700). `objectTitle` (16/600), `bodyLead` (16/500), `body` (14/500),
`control` (14/600), `caption` (12/500) and `overline` (12/600) are all held to 4.5:1.

### The measurement that moved supporting text off `labelTertiary`

Supporting text is set at `caption` — 12 sp W500 — which is normal text, so 4.5:1 governs it.
On `labelTertiary` (`Vibrant.Semidark`), measured from `design-009-fix-detailed`, build `37452262`,
Detailed backdrop:

| surface | supporting on `labelTertiary` | supporting on `labelSecondary` | target | body pairing (unchanged) |
| --- | ---: | ---: | ---: | ---: |
| Panel | **3.18 : 1** — fail | **8.60 : 1** — pass | 4.5 | 11.92 : 1 |
| Card | **3.26 : 1** — fail | **8.57 : 1** — pass | 4.5 | 10.58 : 1 |
| Control | **3.49 : 1** — fail | **8.22 : 1** — pass | 4.5 | 8.84 : 1 |
| Emphasis | 14.74 : 1 — pass | 14.74 : 1 — pass | 4.5 | 14.74 : 1 |

Right column is build `ae4bc4ed`, `design-010-supporting-fix`. Whole-report verdicts: **FAIL**
(worst pair `panelSupporting`, 3.18) then **PASS** (worst pair `controlSupporting`, 8.22).

Two things make this a measurement rather than a preference:

1. **All three failures clear 3:1.** The same colour would be fine on a large-text rung or as the
   boundary of a control. It is neither.
2. **The sibling control held.** Every *body* pairing is **byte-identical** across the two builds —
   11.92 / 10.58 / 8.84 / 14.74 in both reports, to the last digit. Only the supporting row moved.
   A change that moved both rows would have been a scene change, not a role change.

And it is the kind of failure only a frame can find: the same three pairings compute *fine* from the
non-vibrant fallback values on the JVM. A role that reads as a polite grey on a mockup is a failure
on a rendered frame.

`increaseContrast` steps supporting text up again to `labelPrimary` — the same colour the surface's
own title uses — at which point the distinction is carried by size and weight alone, which is what
the setting is for (`PolishSurface.kt:396-402`).

### Both accessibility settings were measured, not assumed

Two of the three gaps an earlier version of this page listed under §9 are closed. Each is one
admitted frame on build `ae4bc4ed`, Room backdrop, sampled by the same probe boxes and the same
tool, with the same unverified premise the tool always reports.

**`reduceTransparency`** — `design-015-rt-on-room`, counted action `n=6`. **All eight pairings
measure 14.74 : 1**, because every surface takes the same ground: the probe background samples
exactly `[40, 40, 40]` — `#282828`, `PolishTokens.OpaqueGround` — on Panel, Card, Control *and*
Emphasis alike, and the foreground samples exactly `[255, 255, 255]`. Whole-report verdict **PASS**
(contrast-design-015-rt-on.json (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/contrast-design-015-rt-on.json`)). That is §4's
`OpaqueGround` rule measured from pixels, on the setting it exists for.

**`increaseContrast`** — `design-016-increase-contrast`, counted action `n=10`. Supporting text
measures **11.92 / 10.58 / 8.84 / 14.74** on Panel / Card / Control / Emphasis — *identical to the
body pairings in the right-hand column of the table above*, which is exactly what stepping to
`labelPrimary` predicts and is the check that the step actually happened. Whole-report verdict
**PASS**, worst pair `controlSupporting` at 8.84
(contrast-design-016-ic-on.json (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/contrast-design-016-ic-on.json`)).

**The `Enhanced` quality mode has now been measured, and it changes no ratio here.** On the
delivered build `a78d8109`, `final-05-legibility-enhanced` reports the same eight ratios as
`final-06-legibility-native` to the digit — 11.92 / 8.60 / 10.58 / 8.57 / 8.84 / 8.22 / 14.74 /
14.74, verdict **PASS**, worst pair `controlSupporting` at 8.22
(contrast-final-05-enhanced.json (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/contrast-final-05-enhanced.json`)).

That is a real result rather than a missing one, and it is consistent with what a frame difference
shows: on the Legibility flow, whose four surfaces all take a compositor material, Enhanced changes
**0 pixels**. It is not inert everywhere — on the States flow, whose cells are drawn and
interactive, the same switch changes **11,464 pixels**, and the host lane attributes 7,281 of those
to the edge. Enhanced acts on drawn surfaces, and a material surface has nothing for it to add.

### After the fix, the surfaces are invariant to app-drawn content behind them

Across the four backdrop fixtures on build `ae4bc4ed` — Detailed (`design-010`), Dark
(`design-011`), Bright (`design-012`), Room (`design-013`) — **all eight pairings report identical
ratios to 2 dp on all four**, and every probe-background mean RGB is identical to three decimals:
Panel `[198.7, 194.4, 189.7]`, Card `[187.7, 183.2, 178.3]`, Control `[172.0, 167.4, 162.4]`,
Emphasis `[40.0, 40.0, 40.0]`.

The backdrop itself moved as far as the fixtures allow, and that is a retained measurement rather
than a sampled aside
(design-backdrop-invariance.json (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/design-backdrop-invariance.json`), build `ae4bc4ed`,
`design-011-fixed-dark` → `design-012-fixed-bright`, digests recorded both sides):

| region | Dark | Bright | delta |
| --- | ---: | ---: | ---: |
| `backdropStripLeft` | 11.00 | 244.00 | **+233.00** |
| `backdropStripRight` | 11.00 | 244.00 | **+233.00** |
| `panelSurfaceFill` | 195.155 | 195.155 | **0.00** |
| `cardSurfaceFill` | 184.024 | 184.024 | **0.00** |
| `controlSurfaceFill` | 168.227 | 168.227 | **0.00** |
| `emphasisSurfaceFill` | 40.000 | 40.000 | **0.00** |

The two `backdropStrip` boxes sit on the app-drawn fixture itself; the four `…SurfaceFill` boxes sit
on each surface's empty `probe-bg` hole, which is the ground its text sits on. Four room-only
stability boxes — `roomCeilingLeft`, `roomAcUnit`, `roomFloorLeft`, `roomRugRight` — each moved
**0.000** as well, so the camera held still and the deltas are the fixture rather than a drift. The
four captures are distinct files with distinct sha256 digests (`e8abd0ee…`, `92b882e5…`,
`6126dedb…`, `0198cab6…`), so this is not one frame measured four times.

**The limit, and it is a real one.** This is invariance to app-drawn content *inside the window*.
What is behind the **window** — the emulator room — was constant throughout and **was not varied**,
because varying it would mean changing Wing. Whether the compositor's *window* material samples the
room differently in a different room is untested here. Do not write this up as "legible against any
background".

### The light-foreground surfaces had no secondary text tier — and it was not only Emphasis

Opening the three backdrop frames settled the `premise: UNVERIFIED` block for a **third** surface and
refuted it for a fourth. Measured foregrounds on build `ae4bc4ed`, Room / Bright / Dark alike:

| surface | body probe | supporting probe | two different tones? |
| --- | --- | --- | --- |
| Panel | `[0, 0, 0]` | `[41.7, 37.1, 32.2]` | **yes** |
| Card | `[0, 0, 0]` | `[31.9, 27.4, 22.8]` | **yes** |
| Control | `[0, 0, 0]` | `[16.1, 11.5, 6.6]` | **yes** |
| Emphasis | `[255, 255, 255]` | `[255, 255, 255]` | **no** |

Panel is visible at 3x in devbuild-panel-probes-3x.png (`evidence/2026-09-17-spatial-ui-polish-native-001/frames-review/devbuild-panel-probes-3x.png`) —
pure black beside a warm charcoal. Emphasis is two white squares separated only by the seam between
them: devbuild-emphasis-probes-3x.png (`evidence/2026-09-17-spatial-ui-polish-native-001/frames-review/devbuild-emphasis-probes-3x.png`).

**The scope was wider than Emphasis, and the retained evidence had already said so.**
[`usesLightForeground`](#) is `reduceTransparency || kind == Emphasis`, so with reduced transparency
on, *every* surface takes the light foreground. `contrast-design-015-rt-on` reports **all eight**
pairings — four surfaces, body and supporting alike — as an identical `[255,255,255]` on `[40,40,40]`,
14.74:1. Eight passing ratios and four missing tiers, and the report was read as a pass.

**Cause.** `PolishSurfaceDefaults.secondaryForegroundFor` returned `labelPrimaryLight` on any
light-foreground surface — the same role its *primary* foreground uses — because the SDK's
sixteen-role `ColorScheme` contains exactly **one** light label role. There is no
`labelSecondaryLight`.

### The fix: a derived light-secondary tone

`PolishSurfaceDefaults.lightSecondaryForeground` composes the tone rather than looking one up:
`LightSecondaryStep` (**0.70**) of the way from `PolishTokens.OpaqueGround` to `labelPrimaryLight`,
built as an **opaque** colour, with the source role's vibrancy tag re-applied.

Three things make that the right shape rather than an alpha on the label:

1. **The ground is always known.** `fillFor` returns `Solid(OpaqueGround)` on every path where
   `usesLightForeground` is true — the `reduceTransparency` early return covers every kind, and
   `Emphasis` returns the same solid before any glass, hover or budget branch can reach it. A test
   pins that invariant across every kind x option x interactive x depth combination, so a future fill
   that breaks it fails a test rather than a frame.
2. **`reduceTransparency` means what it says.** A user asking for less transparency should not be
   handed a translucent foreground to compensate for an opaque fill. Nothing is translucent at draw
   time; the opacity change is scoped to the colour and there is no `Modifier.alpha` near it.
3. **An alpha would be unmeasurable by this kit's own API.** `PolishContrast.ratio` refuses a
   foreground with `alpha < 1` as `TranslucentForeground`, correctly, because the ratio would depend
   on what is behind it. A composed opaque tone is measurable — and is measured below.

**The vibrancy tag is the part that is easy to get wrong.** The level lives in bits **24–31** of the
colour's `ULong` — the region Compose leaves unused for an sRGB colour — **not** the alpha byte
(alpha is 32–39, RGB is 40–63). So reading `.red`/`.green`/`.blue` off a tagged role is safe, but
building a fresh `Color(r, g, b, a)` writes zero into 24–31 and silently drops the tag. Dropping
`Vibrant.None` (31, *terminate the inherited effect*) to `Vibrant.Unspecified` (0, *inherit it*) is a
behaviour change inside a vibrant context that no JVM test would show. The tag is therefore
re-applied, which is the pattern the SDK documents for exactly this case ("Animate the raw color,
then apply `.withVibrant()`", `ColorVibrant.kt:52-55`). Off the spatial platform the role is
untagged, `obtainVibrant()` returns `Unspecified`, and re-applying it is a no-op — so one code path
is correct in both schemes.

**`increaseContrast` steps back up.** On a light-foreground surface the supporting line returns to
the full `labelPrimaryLight`, exactly as the dark-foreground branch steps up to `labelPrimary`. At
that point rank is carried by size and weight alone, which is what the setting is for.

### Measured on the refined build `6cdb7306`

Same probe geometry as `contrast-design-013-fixed-room`, so the rows are directly comparable.

| state | body | supporting | supporting ratio | verdict |
| --- | --- | --- | ---: | --- |
| default settings, Emphasis | `[255,255,255]` | **`[191,191,191]`** | **8.02** | PASS |
| `reduceTransparency`, all four surfaces | `[255,255,255]` | **`[191,191,191]`** | **8.02** | PASS |
| `reduceTransparency` + `increaseContrast` | `[255,255,255]` | `[255,255,255]` | 14.74 | PASS, and deliberate |

`191` is the composed value the implementation predicts to the digit, and 8.02:1 clears the 4.5:1 that
normal text is held to with a wide margin. The three dark-foreground surfaces are **unchanged**:
11.92 / 8.60 / 10.58 / 8.57 / 8.84 / 8.22, identical to the frames taken before the refinement.
Primary text is untouched in every state.

Matched rendered comparison, same crop box, only the build differs:
detail-04-surface-kinds-2x.png (`evidence/2026-09-17-spatial-ui-polish-native-001/frames-review/detail-04-surface-kinds-2x.png`) before,
detail-08-surface-kinds-AFTER-2x.png (`evidence/2026-09-17-spatial-ui-polish-native-001/frames-review/detail-08-surface-kinds-AFTER-2x.png`) after.

**A test was pinning the defect.** `PolishBehaviourTest.controlsInheritTheEnclosingSurfaceKind`
asserted that a control inside an Emphasis surface takes `labelPrimaryLight` as its *secondary*
colour. That assertion was only ever a proxy for "it took the light branch", and the proxy held
solely because the branch returned the primary role verbatim. Its intent is unchanged and still
checked; the proxy now names the derived tone.

---

## 7. Motion

The kit ships **no animations of its own**. `reduceMotion` is a policy this design system *carries*;
it changes nothing until the consuming app routes its own animations through `PolishTokens.motion`.
That is app-owned on purpose: the SDK pins the Compose motion duration scale to 1 from a
ContentProvider at process start, so a system "remove animations" preference never reaches SpatialUI
animations. Unlike `reduceTransparency` and `increaseContrast`, which the components honour
themselves, this one needs a line of code from the app.

### The anchored durations

`MotionTokens` is the one SDK token object that is **public and unrestricted** — no `internal`, no
`@RestrictTo` (`design/tokens/MotionTokens.kt:31`, and the only token object with a declaration in
sdk-source-map.json (historical source citation)).

| Kit token | SDK token | ms | For |
| --- | --- | ---: | --- |
| `DurationFeedbackMillis` | `durationShort1` | 120 | immediate feedback: a press tint settling, a value catching up |
| `DurationStateMillis` | `durationShort2` | 180 | a state change the eye should follow (the default) |
| `DurationPanelMillis` | `durationShort3` | 240 | a panel or secondary surface appearing, expanding, dismissing |

Each is a `get()`, not a constant, so the kit follows an OTA that retunes platform motion.

### The platform easing curve

`EasingStandard` is `MotionTokens.bezierEasingStandard` — `cubic-bezier(0.3, 0, 0.2, 1)`. The kit's
first version animated with a bare `tween(...)`, which takes Compose's `FastOutSlowInEasing`: **a
different curve from the one the shell and every SDK component use** (`Slider.kt:476`,
`ScrollIndicator.kt` ×6, `TabBar.kt` ×5 all pass `bezierEasingStandard`). An app whose panels ease
differently from the system chrome around them reads as a port.

Call `PolishTokens.motion(reduceMotion, durationMillis)` — or `motionSpec(...)` for APIs taking the
wider `AnimationSpec` — instead of `tween(...)` directly, so one setting covers the app and every
animation lands on the platform curve. With `reduceMotion` it returns `snap()`.

### The reduced-motion contract

`reduceMotion = true` means `snap()` everywhere the kit's helpers are used. It removes no action, no
control and no state. The three accessibility flags are independent of `quality` and of each other:
all sixteen combinations are usable screens.

### Two token traps

- **Durations fail safe; the platform easing does not fail to its documented value.** If the design
  resources cannot be read, `bazierEasingRes` falls back to `LinearEasing`
  (`ext/Resources.kt:131-133`), not to the documented curve. That is a silent downgrade, not a
  crash — and not a value you would predict from the token's name.
- **The spring tokens fall back to NaN.** `springEasingArgsRes` does not fall back to the documented
  spring; it returns `SpringEasingArgs.Unspecific`, which is `(dampingRatio = NaN, stiffness = NaN)`
  (`MotionTokens.kt:150`, `ext/Resources.kt:107-109` — the `catch`, not the success path at
  `:103-106`, which an earlier version of this page cited instead). `toSpring()` hands that straight
  to `spring(...)`, and **a NaN spring does not animate** — it produces an undefined value for the
  whole run. `PolishTokens.spring(reduceMotion, args)` validates both fields before use and falls
  back to a real spring (damping ratio `1.0`, stiffness `850f`, `springEasingRapid`'s documented
  values) — `PolishTokens.kt:259-268`. **Never pass a raw `MotionTokens` spring token to
  `spring(...)`.**

### What a recording settled, and what it did not

The reduced-motion contract *was* exercised on the emulator. Two recordings of the **same three flow
switches**, build `ae4bc4ed`, differing only in the app's own Reduce motion setting, captured with
`pico-cli capture record` against the explicit serial `<EMULATOR_SERIAL>`, decoded to grayscale and
measured with `polish_motion_runs.py --fps 60` —
DESIGN-MOTION.md (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/motion/DESIGN-MOTION.md`),
clips `design-motion-on.mp4` / `design-motion-reduced.mp4`, reports
`design-motion-runs-on.json` / `design-motion-runs-reduced.json`:

| reduce motion | transition runs | durations | peak frame-to-frame change |
| --- | --- | --- | --- |
| **off** | 9, 11, 11 frames | 150.0, 183.3, 183.3 ms | 0.455, 0.618, 0.761 |
| **on** | 1, 1, 1 frames | 16.7, 16.7, 16.7 ms | 1.675, 2.007, 2.217 |

The transition collapses to a single frame and the per-frame change grows roughly fourfold, because
the content swaps in one step instead of blending. That is `PolishTokens.motion()` returning `snap()`
seen from outside the app — and it still is after the kit's motion was re-anchored to `MotionTokens`,
which is what this pair was taken to check. Every switch is bound to a counted action in the device
log and both clips used the same three destinations in the same order.

**What it does not settle, stated as flatly as the measurement.** `DurationPanelMillis` is 240 ms and
the measured un-reduced runs are 150-183 ms: the recording samples the compositor and its threshold
discards the low-amplitude tail of a fade, so it is **not a timing instrument** and no claim is made
that the animation lasts 183 ms. The easing curve is not isolated either — the un-reduced transition
measures 9-11 frames here against 5-6 on the earlier `be447bda` pair, and attributing that to the
curve change is a hypothesis, because the two builds differ in much more than the easing. The
`LinearEasing` and NaN-spring fallbacks remain source-derived: neither was triggered and observed.
Emulator only; nothing here is headset motion, and no wearer judged whether any of it is
comfortable.

---

## 8. States

### The matrix the kit supports

| Surface / control | States |
| --- | --- |
| `PolishSurface` | static, interactive, `enabled`/disabled, `selected`, `errorMessage`, focused (observed) |
| `PolishButton` | four tones (Standard, Emphasis, Quiet, Destructive) × enabled / disabled / `loading` |
| `PolishIconButton`, `PolishToggleIconButton` | enabled / disabled; toggle carries checked state |
| `PolishField` | enabled / disabled, `errorMessage` (which replaces the supporting line) |
| `PolishSwitchRow`, `PolishSliderRow` | enabled / disabled, value |
| `PolishSegmented` | selected option |

Overlaps are supported and rendered, not merely tolerated: selected + disabled, error, error +
selected and focus are drawn together in the showcase's `States` gallery (`StateGallery.kt`), and
`design-025-focus-ring` (build `6b19ca10`) is the frame in which all four render at once — see the
focus subsection below for what that took.

**The gallery is paged, not scrolled, and frames are why.** This emulator has no host route to a
guest scroll — the wheel drives the camera and never reaches the app — so a section below the fold is
a section no capture can ever show. Two columns were not enough either: six sections still overflowed
the app's declared 1080×640 dp window, which `design-018-states-overlap` (`37285f06`),
`design-020-states-matrix-fit` (`3f71458a`) and `design-021-states-page1` (`2fbc4576`) each showed in
turn. So it pages, and the page turn is a **counted action** (`action=galleryPage`) where a scroll
would have been nothing (`StateGallery.kt:34-48, 75-79`).

**No state is ever carried by colour alone.** Selection is the accent role **and** `HairlineStrong`
**and** `semantics { selected = true }`. Error is the `error` role **and** `HairlineStrong` **and**
the message reported through the semantics `error` property — and the caller still owns rendering
the message as text, because *a surface that only turns red says nothing to someone who cannot see
the red*. `loading` suppresses activation, draws an indicator over the label, and reports
`stateDescription` to accessibility. `enabled = false` routes through the SDK's own disabled
presentation (`LocalDisableAlpha` applied in the `graphicsLayer`, before hover), which is why
`loading` is *not* implemented as `enabled = false`: doing so rendered a busy button as a disabled
one.

### Edge precedence, highest first

```
error  >  selected  >  focused  >  increaseContrast  >  separation
```

`PolishSurfaceDefaults.edgeFor`, `PolishSurface.kt:430-471`. The reasoning:

- **Error outranks selection** because a selected row that has gone wrong needs to say so.
- **Selection outranks focus** because focus is transient and selection is the state the user set.
- Each of the first three is a distinct role at a distinct weight — `error`/2 dp,
  `interaction`/2 dp, `interaction`/1 dp — so two of them are never confusable at a glance.
- **`increaseContrast`** then forces an edge on every surface, in `fillSecondary` on a dark fill or
  `labelQuaternary` on a light one, at `HairlineStrong`.
- **Separation** is the catch-all: `reduceTransparency`, `PolishQuality.Enhanced`, or a static
  surface that dropped its material for the budget. Hairline weight, `dividerLine` on a light fill,
  `fillSecondary` on a dark one.

That last split is itself a frame finding — and it is this page's **one host-rendered frame**, not an
emulator capture. On the dark opaque fill `reduceTransparency` uses, a dark hairline role is
invisible, and all four surface kinds collapsed into one flat shape in
`before/08-surface-kinds-reduced-transparency.png` (`evidence/2026-09-16-spatial-ui-polish-001/host-rendered/before/08-surface-kinds-reduced-transparency.png`):
a Robolectric render at a 1280×720 dp window qualifier from the earlier phase, which says nothing
about glass and is not device or emulator evidence — defect 3 in
host-rendered/README.md (`evidence/2026-09-16-spatial-ui-polish-001/host-rendered/README.md`).
An edge has to contrast with the fill it outlines — it is a graphical object, so 3:1 governs it, not
4.5:1.

### The focus ring is the kit's own, and it took two builds to see

**The design system ships no focus-visible treatment anywhere.** A grep of `design/` and
`foundation/` finds only `TextField`'s focused *background* swap, window `focusable` flags that
control input routing, and `LocalFocusManager.clearFocus()`. Nothing draws a focus indicator.

So the ring in `PolishSurface` is an addition, and it is deliberately conservative:

- **Observed, never requested.** The kit does not add `Modifier.focusable` — that would put every
  surface into a traversal order the app did not ask for. It reads the focus the app's own focus
  handling produces via `collectIsFocusedAsState` (`PolishSurface.kt:139`), so a surface that is
  never focused simply never shows a ring.
- **One accent, two weights.** It is the same `interaction` role the selection edge uses, at
  `Hairline` instead of `HairlineStrong` — rather than a second hue nobody has been taught.
- **It renders — and the first attempt to show it failed.** Because the ring is observed rather than
  requested, nothing in the gallery gave a surface focus until a fixture emitted
  `FocusInteraction.Focus()` directly. A *single* `emit` raced the collector —
  `MutableInteractionSource` publishes through a shared flow with no replay, and
  `collectIsFocusedAsState` subscribes from its own `LaunchedEffect` — and lost.
  `design-024-states-p2` (build `90b10316`) was opened
  (`design-024-p2.png` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/crops/design-024-p2.png`)): the `Focused` cell renders as an
  ordinary clickable surface with **no ring at all**, beside `Selected + disabled`, `Error` and
  `Error + selected`, which all draw their edges. The fixture now re-emits every 700 ms
  (`StateGallery.kt:64-69`, `FOCUS_REEMIT_MILLIS`), and `design-025-focus-ring` (build `6b19ca10`)
  was opened (`design-025-p2.png` (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/crops/design-025-p2.png`)): the `Focused` cell
  carries the accent edge, `Selected + disabled` carries the same accent at the disabled alpha, and
  **`Error` and `Error + selected` both carry the red edge — error outranking selected, in a frame**
  rather than only in `edgeFor`'s ordering.
- **Still emulator-only, and still a fixture.** The emit drives the same code path a real focus
  drives, so the ring it produces is the ring; but no frame in this tree shows a surface focused by
  the app's own focus handling, and no PICO headset frame shows the ring at all.

`PolishSurface(errorMessage = ...)` is likewise the kit re-implementing a treatment the design
system does not expose: the SDK's error presentation is `TextField`-family only and its
`errorBorder` is `internal` (`design/TextField.kt:940`; 947-958 is inside its body and excludes the modifier). It is re-implemented deliberately, and
in the public API only.

---

## 9. What this phase did not establish

Exhaustively. Every item here is a gap, not a caveat.

**No PICO headset ran at all.**
- No frame was rendered on PICO hardware. Every number and every judgement on this page comes from
  the emulator, with one stated exception: the host-rendered Robolectric frame §8 names for the
  edge-contrast finding, which is not device evidence either.
- Nothing about **depth** was tested. `LiftZ` (8 dp) has never been seen lift anything.
- Nothing about **vibrant adaptation in a real room** was tested. Every vibrancy conclusion here is
  about how the *emulator's* compositor resolved a sentinel.
- Nothing about **passthrough legibility** was tested.
- **No wearer.** Comfort, reachability, and whether 12 sp is readable at arm's length are all open.
  The only physical headset attached was a Meta Quest 3 (`<DEVICE_IDENTIFIER>`) — not PICO acceptance
  hardware. **Nothing from this kit was installed on it**, and it was never used as evidence.

**The type ladder.**
- `design-004-specimen-fit` settles **relative rank** and nothing else. Absolute legibility at
  wearing distance is undecided.
- The 12 sp floor is a judgement, not a measurement. No frame establishes that 11 sp would have been
  worse.

**Contrast.**
- The `polish_contrast.py` premise — that a role colour handed to `Modifier.background(...)` is
  compositor-resolved rather than drawn as flat `#FF000000` — **is discharged**, twice:
  `design-005-legibility-room` (build `f91e4b7e`) was opened and its `probe-fg` and `probe-fg2`
  patches render as visibly different tones, and `design-contrast-detailed-fixbuild.json` measures
  the same two patches as `[0,0,0]` and `[107.552, 103.057, 98.154]` — which two unresolved sentinels
  could not be, since both would read `#FF000000`.
  It is now discharged for **Control** as well. It was **refuted for Emphasis** on build `ae4bc4ed`,
  whose two probes both measured `[255, 255, 255]` — and that refutation was a real defect in the
  kit, not in the tool: `secondaryForegroundFor` handed every light-foreground surface the same role
  twice. **That is fixed** on build `6cdb7306`, where the Emphasis probes measure `[255,255,255]`
  and `[191,191,191]`, so the premise is now discharged on all four surface kinds (§6).
  What remains true is narrower and worth keeping: **the tool performs no such check itself**, so the
  `premise: UNVERIFIED` string is still printed in every report it writes. That string is now stale
  on every surface, and a reader of a raw report should be told so.
- The invariance result is invariance to **app-drawn content inside the window**. What is behind the
  *window* was constant and was not varied.
- All three of `reduceTransparency`, `increaseContrast` and **`PolishQuality.Enhanced`** have now
  been measured (§6) — the first two on `ae4bc4ed`, Enhanced on the delivered build `a78d8109`, all
  **PASS**. Enhanced returns the same eight ratios as Native to the digit, which is a result and not
  a gap: on a flow whose surfaces all take a material it changes 0 pixels, while on the drawn,
  interactive States flow the same switch changes 11,464. Successive versions of this page listed
  three unmeasured settings, then one; there are now none.
- The four DisplayP3 semantic roles are treated as sRGB by `PolishContrast`. No conversion exists.

**Interaction and route.**
- The emulator's synthetic pointer is **not route-neutral**, and that is now a retained artifact:
  70 `ViewRootImpl: onInputEvent Space Info[0]` lines out of the showcase's own process (build
  `ae4bc4ed`, pid 23423), every one carrying **`kind=GAZE_PINCH`** and no other value
  (route-probe-spaceinfo.log (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/logs/route-probe-spaceinfo.log`),
  framed in
  ROUTE-PROBE.md (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/logs/ROUTE-PROBE.md`)).
- **That is a platform-layer fact and it stops there.** `kind=GAZE_PINCH` is a field in the PICO
  platform's own input-plumbing log line. It is **not** an observation of the Spatial SDK's public
  `InteractionKind`, and the two are separated by a mapping the app cannot see
  (`KIND.toInteractionKind()` and `SpaceInteractionKind.toInteractionKind()` are both `internal` —
  `foundation/…/gesture/data/InteractionKind.kt:42, :60`). `TapEvent3D` is likewise a **field in that
  log line, not an SDK type**, and it reads `{INVALID}` in 47 of the 70 lines; `SelectEvent3D` is the
  populated one. An earlier version of this page said route-discrimination code "sees `GazePinch`".
  Nothing in this lane shows that.
- **Neither tree reads the route at all.** Grepping `InteractionKind`, `detectSpatialTap` and
  `interactionKindExtra` across the showcase (`examples/spatial-ui-polish-showcase/app/src`) and the
  kit (`skills/spatial-ui-polish/resources/polish/src`) returns **0 hits in both**. So the app-layer
  question — whether an SDK gesture detector populates route information under emulator verification
  or collapses it to `Unknown` — **remains open**. Answering it needs a build that calls
  `detectSpatialTapGesture` and logs `SpatialTapValue.interactionKind`; no such build exists.
- **No emulator evidence exercises the direct-touch route or the controller route at all.** If
  behaviour under test discriminates by route, the other branches are untested code.
- Separately, interaction-contract.md (historical source citation) records that the physical-route →
  `InteractionKind` mapping itself is **inference**, confirmed by neither source nor runtime
  observation. acceptance.md (historical source citation) still carries the older wording for this log line;
  where the two differ, the artifact above governs.

**Motion.**
- The reduce-motion **contract** was recorded on the emulator (§7: 9/11/11 frames against 1/1/1,
  build `ae4bc4ed`). An earlier version of this page said nothing in §7 was rendered; that is no
  longer true.
- The **durations** were not measured. The recording is not a timing instrument, its threshold
  discards the tail of a fade, and 240 ms was read from a token.
- The **easing curve** was not isolated. The frame-count difference against the earlier `be447bda`
  pair is a hypothesis; the two builds differ in more than the curve.
- The `LinearEasing` and NaN-spring fallbacks were read from source; neither was triggered and
  observed.
- Nothing in §7 is headset motion, and no wearer judged whether any of it is comfortable.

**States.**
- **The focus ring now has a frame, and it took two builds.** `design-024-states-p2` (`90b10316`)
  shows the cell with no ring — a single `emit` raced the collector; `design-025-focus-ring`
  (`6b19ca10`) shows the ring after the fixture was made to re-emit. Both were opened. The ring is
  still driven by a fixture rather than by the app's own focus traversal, and no headset frame shows
  it.
- The `Enhanced` quality mode's two remaining features — the hairline edge and the depth lift —
  have no headset frame, and the lift has no frame at all.
- Haptics and audio were not measured; they come from the built-ins and are asserted from source.

**Performance — one question measured, and not answered.**
- Checkpoint 002 measured `reduceTransparency` off against on at **64.00 / 64.21** versus
  **66.07 / 66.21** Spatial-Runtime FPS and was careful never to call the 2.03 gap the cost of the
  compositor material, because that one setting changes fills, foregrounds and edges together.
- A fixture was built to isolate the fill — 24 identical cells, the **only** difference being
  `backgroundMaterial(Regular)` against `background(0xFFC3C3C3)`, with the same geometry, clip,
  **edge**, strings, type and explicit foregrounds in both arms, and no interaction in either.
  Five interleaved 15-second runs per arm.
- **No difference was detected.** Estimated material minus drawn = **+0.29 sysFps, 95 % CI
  [−0.61, +1.18]** (paired by interleaved block, 5 blocks, df 4, p = 0.43; unpaired Welch on the
  ten run means agrees: +0.29, CI [−0.88, +1.45]).
- **That is not a finding that the material is free, and it settles nothing about the 2.03.** A
  null result with five replicates per arm is consistent with effects up to about 1.2 FPS in either
  direction, and the two experiments are different workloads besides. **The 2.03 gap remains
  unexplained** — this page does not attribute it to the material, and does not rule the material
  out either. Do not quote "glass costs ~2 FPS", and do not quote "glass is free".
- **An earlier version of this page said the 2.03 "is not attributable to the compositor
  material".** That was an over-reading of a null and is withdrawn.
- The arms are genuinely different mechanisms, not just different source: across six cells the
  material measures 158.78–205.07 (spread 46.29) because it samples its backdrop, while the drawn
  fill measures 195.00 everywhere (spread 3.30). Window chrome moved 0.00 between the two frames and
  all three room-only stability boxes drifted 0.000.
- **The analysis was also corrected.** The first pass pooled every per-second sample within each arm
  (~72 per arm) and ran Welch on the pools. The condition was set once per run and held for its
  duration, so the run is the experimental unit and the samples inside it are repeated measures of
  one unit; pooling is **pseudo-replication**, and it claimed **142 degrees of freedom for an
  experiment with ten experimental units**. Worth knowing: the effect on the standard error was not
  uniform and did not all point one way — pooling flattered the material arm by 1.8× and understated
  the drawn arm by 0.5×, because material's run-to-run spread (SD 0.95) is large next to its
  internal noise while drawn's runs cluster tightly (SD 0.27). The unambiguous defect is the degrees
  of freedom.
- **What it still cannot establish.** `appFps`, `appCpu`, `gpuUsage`, `gpuFreq` and `gpuTemp` were
  absent in all ten runs, so `sysFps` is compositor-level and **no app-rendering cost can be
  attributed from it**. One run was discarded for a condition that did not change, caught by
  counted-action readback rather than by the numbers, and preserved under a name that says so.
- Tooling and full record: `polish_perf_compare.py` (run-level paired and unpaired analyses, with
  the pooled form removed rather than reported alongside) and
  FILL-COST-COMPARISON.json (`evidence/2026-09-17-spatial-ui-polish-native-001/emulator/perf/FILL-COST-COMPARISON.json`).

**The kit's own consistency.**
- `Modifier.polishWindowRoot()` **was fixed** in build `37285f06`: it paints
  `PolishTokens.OpaqueGround` under `reduceTransparency` where it used to paint `fillPrimary`
  (`PolishTheme.kt:104-113`), and the two stale `PolishSurface.kt` KDoc blocks went with it (§4). An
  earlier version of this page listed the window root as a live behaviour gap; it is not one any
  more. **The fix is now measured too**: `final-14-focus-rt` on build `a78d8109` exercises an
  app-owned window root under `reduceTransparency` and its ground measures exactly **40.00** — the
  literal value of `PolishTokens.OpaqueGround` (`0xFF282828`, 0x28 = 40) — so the frame agrees with
  the source rather than standing in for it.
- `PolishContrast` **was fixed** in the same build: `hasUnresolvedVibrancy()` no longer refuses a
  `Vibrant.None`-tagged colour, so `labelPrimaryLight` and `dividerLine` are measurable as the
  documentation always claimed (§6, Lane A). The pin the KDoc names —
  `PolishDesignSystemTest.theSixSupposedlyLiteralRolesAreActuallyReadable` — **now exists and
  runs**: 39 tests in `PolishDesignSystemTest`, 87 in the module, 0 failures, from a forced
  `testDebugUnitTest --rerun-tasks` (an `UP-TO-DATE` Gradle result is not a test result). An earlier
  version of this page recorded the pin as missing, which it was; writing it is also what showed
  that `dividerLine` is refused for **translucency**, not for vibrancy.

**Provenance.**
- `DimensionTokens` and `TypeScaleTokens` line citations on this page come from the kit's own KDoc.
  Neither file has a declaration entry in sdk-source-map.json (historical source citation), so they are one
  step less independently checkable than the entries that do.
- `MinTargetSize` is a Compose-fork (`ui-android:1.2.5`) fact, not a 6.1.9 one.
- The design-phase inventory
  FRAMES-DESIGN.md (`evidence/2026-09-17-spatial-ui-polish-native-001/FRAMES-DESIGN.md`)
  now lists the whole `design-0NN` series with its build, its digest, and whether each frame was
  **opened**, **measured only**, or never looked at; the committed `FRAMES.md` still covers only the
  earlier `be447bda` checkpoint. **That inventory, `ROUTE-PROBE.md`, `DESIGN-MOTION.md`,
  `design-backdrop-invariance.json` and the `contrast-design-015` / `-016` reports were all untracked
  in the working tree when this page was written.** Every relative link here resolves in the
  worktree; a clean checkout would not yet have those files.

**The residual failure this skill measures in its own users** is an unqualified appearance claim:
"reads as frosted", "holds up against bright passthrough", written with nothing rendered. Nothing on
this page says how anything looks without naming the frame and the build it was looked at in. Keep
it that way, and close every report by naming what you did not establish.

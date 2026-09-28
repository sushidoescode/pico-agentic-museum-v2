# Everyday Futures — design draft 01

Status: provisional direction for private review. This package is an interactive browser study, not a Spatial SDK application or headset capture. Native delivery is described in the implementation plan (historical source citation).

## Product and teaching purpose

A museum of ordinary objects that reveal the habits behind them. A visitor chooses a familiar object, reads its catalogue entry beside an illustration, examines its orientation, and unfolds three short interpretations. The first native lesson must retain catalogue, detail and adjacent real 3D inspection. A tabletop narrative is a later increment, not a prerequisite for that lesson.

The agentic contribution is a reproducible authoring path: requirements, prompts, source discovery, design decisions, CLI execution, repairs and evidence. Runtime AI is not required. Museum copy here is newly authored interpretive draft copy and needs editorial review; it is not an asserted historical account or copied narration.

## Three directions considered

| Direction | Distinct experience | Cost and decision |
| --- | --- | --- |
| Everyday Futures | A compact catalogue opens into an object study, then a three-part tabletop story. | Selected provisionally. Closest fit to the first lesson; useful in the emulator before room sensing is available. |
| The Room We Lived In | Familiar objects return to meaningful places in the visitor's real room, then become a personal collection. | Reserve for a later passthrough/placement lesson. Requires room and wearer acceptance. |
| Planet Crumb | A robot vacuum reveals a miniature world of crumbs, cable forests and furniture islands. | Reserve for a surreal tabletop project. Strong visual idea, but miniature legibility and comfortable interaction need hardware testing. |

User ideas remain welcome; the provisional selection is not an approval of final art direction.

## Composition and visual language

One primary window, with a left catalogue, central object study and right interpretation. Warm brass marks selection and the primary action; cool slate and soft white keep the object prominent. A serif heading expresses the museum identity; system sans-serif carries controls and reading text. One main material surface avoids nested glass competing for attention. Original SVG studies supply recognizable silhouettes without importing unverified asset payloads.

The final review viewport is 1360 × 1040 CSS pixels. The window is 1280 pixels wide with a 32-pixel inset and radius; spacing uses a 4/8-pixel rhythm. These are browser study dimensions, not headset angular sizes, reach distances or a native SDK token mapping. An 820-pixel layout moves the catalogue above the study. Narrower screens stack detail below the study; the page remains scrollable.

Apple Liquid Glass is inspiration for clarity, hierarchy and restraint. No claim is made that this browser material reproduces Apple's rendering or PICO's hardware compositor.

## Component and material contract

The preview embeds the complete, unmodified SpatialUI Web bundle from the installed PICO plugin. All interactive controls use its public components and events. `PicoTheme.install` receives all 16 scheme roles explicitly: fillPrimary, fillSecondary, fillTertiary, fillLight, labelPrimaryLight, labelPrimary, labelSecondary, labelTertiary, labelQuaternary, lightenHover, lightenPressed, error, alert, passable, interaction and dividerLine. After the first visual review exposed white-on-light text, dark literal label roles and a dark tertiary fill were selected for this browser material; other roles inherit the vendor scheme, with a brass interaction role. Reduced-transparency content supplies its own light foreground tiers over the literal dark backing. Application accent fills are explicit on primary actions.

| Preview component | Use and event | Native boundary |
| --- | --- | --- |
| `sui-augment` | Main material shell anchored to a measured DOM root | A browser overlay renderer, not proof of a native container type. Select the SDK window/root from installed declarations when implementing. |
| `sui-button` | Selection, rotation, inspection, chapters, save, retry and dismissal through `click-action` | Map to supported SpatialUI buttons and preserve enabled/disabled semantics. |
| `sui-sheet` | Settings and saved collection; `dismiss-request` closes | Verify native window/modal ownership, focus and dismissal separately. |
| `sui-switch` | Three preferences through `checked-change` | Application-owned preferences, persisted and mapped to supported native rendering/motion behaviour. |
| `sui-alert-dialog` | Confirm collection reset | Cancel preserves state; confirm clears only the saved collection. |
| Original SVG | Schematic object and story illustration | Replace with approved real assets and native transforms; CSS rotation is not 3D model interaction. |

Do not infer native glass, opacity, vibrancy or input guarantees from Web component behaviour. The canonical Kotlin polish module must be consumed with an explicit source path and recorded identity; porting browser CSS into a new pseudo-native kit is not the implementation plan.

## Reachable states and actions

| State | Trigger | Visible result / exit |
| --- | --- | --- |
| Catalogue | First load or object selection | Quencher, vacuum and speaker; selected object resets orientation and chapter. |
| Inspection | Inspect or turn action | Inspection heading and schematic orientation; return to catalogue is explicit. |
| Story | Unfold its story | Three labelled chapters, active marker and interpretation; return restores the object view. |
| Saved | Keep this object | Count increments once; disabled Added control prevents duplication. |
| Collection sheet | Collection action | Saved interpretation list or an empty message; Done dismisses. |
| Settings sheet | Settings action | Reduce motion, reduce transparency and increase contrast; Done dismisses. |
| Reset confirmation | Reset saved collection | Keep collection cancels; Start fresh clears saved objects, retaining preferences. |
| Missing description | Review fixture | Useful fallback beside the still-available study. |
| Model failure | Review fixture | Catalogue and interpretation remain; retry restores the schematic study. |
| Empty catalogue | Review fixture | Clear message and reload action. |

Error modes are deterministic design fixtures accessed through `window.__museumReview.setDataMode`, not a claim to have tested real asset I/O. No debug mode selector appears in the visitor UI. Preferences and saved object IDs persist locally; this preview has no account, remote service or telemetry.

Reduced motion disables application transitions. Reduced transparency supplies a literal backing to exhibit content; it does not promise every vendor modal or the outer window becomes opaque. Increase contrast promotes supporting content; it is not a measured WCAG conformance claim. Native settings must be tested as the visitor actually operates them.

## Input, legibility and acceptance boundaries

The browser checks exercise vendor control events through their internal DOM controls. They establish state transitions, not a wearer gesture. The native design should support a calm far-pointer select path first, then verified hand pinch and tracking-loss recovery, with direct touch and controllers accepted independently. Avoid required two-handed gestures, unsupported haptics and accidental activation when focus changes.

No native build, emulator capture, hardware test, angular legibility measure, hand confidence measure, performance sample or wearer observation is included in this draft. Text size, depth, reachability, contrast against actual passthrough, glass and comfort remain open. Hardware availability does not settle firmware/runtime compatibility.

## Review and implementation handover

Browser review (historical source citation) identifies exact preview bytes, checked interactions, frames actually opened and retained failures. Curriculum (historical source citation) maps the design into one evolving teaching app. Reference review (historical source citation) and asset metadata (historical source citation) distinguish reviewed source from absent binary payloads.

The next design review should decide whether the museum tone, three objects and interpretation structure are appropriate. The next implementation checkpoint is a native catalogue/detail/adjacent-model slice with exact prompt and repair history. Tabletop worlds and surreal effects follow that foundation.

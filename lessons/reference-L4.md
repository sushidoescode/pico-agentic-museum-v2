# L4 — The Earth Collection

Expand a working museum into eighteen recognizable exhibits, an independent 3D inspector and a Blender-authored immersive archive. Learn to join asset provenance, state/lifecycle design and native inspection without overstating resource cleanup or input support.

## Starting, completed and current states

Start: `lesson-4-start` → `480853b849ce2dc6c1a6807d3ae787fbd5b9977e`, the accepted core plus earlier polish and recorded L4 design. Complete: `lesson-4-complete` → `720827216fbd73764c48b89602432386a89ac54d`. **This is build-007 completion**, built source `8f5e80d`, APK SHA-256 `934dd7171e4942599d4c8b50910f520ccf23fe29be47fbcc7b196bd887fb300e`.

Later demo hardening is build-008, source `c65959f66d164ee28933f3dd2c09bfe1fc760ff3`, APK 42,446,151 bytes, SHA-256 `b1a61d990c3efd15f8a17ec879cf385468bcbc5d2af7f13c2696e9a1484a3f26`. It adds the SDK search field and a disposal confirmation. Source snapshots `source/snapshots/L4-start/`, `source/snapshots/L4-complete/` and `source/snapshots/build-008/` remain separate in [checkpoints](../source/CHECKPOINTS.json). The clean documentation closeout is `0efe2718e0eeb733e2a3b3b2cd300a1703025aa0`; it does not erase build-008 receipt `dirtyLines=2`.

## Prerequisites and supplied context

Read L1–L3, [architecture](../curriculum/ARCHITECTURE_AND_GLOSSARY.md), [setup](../curriculum/SETUP_AND_VERSIONS.md) and [asset provenance/rights](../assets/PROVENANCE_AND_RIGHTS.md). Historical inputs were the expanded product/art brief, an approved concept image as direction, SDK 6.1.9 Stage/window/entity declarations, the existing build/overlay pipeline and source-mapped reference assets. The exact coordinator brief remains private. The museum design/runbook are sanitized summaries. Historical PICO workflow, SDK and SpatialUI guidance shaped the implementation; Blender generated models/room through deterministic scripts. No concept image is native evidence.

## Prompt sequence and expected output

1. [P14](../prompts/PROMPT_LIBRARY.md), sanitized expansion brief: retain ids/saved state and separate factual identification from fictional interpretation.
2. [P15–P16](../prompts/PROMPT_LIBRARY.md), condensed historical implementer prompts: bounded art and app ownership, with root integration.
3. [P17](../prompts/PROMPT_LIBRARY.md), reconstructed lifecycle repair: descriptors, loading timing and in-flight window requests.
4. [P18–P19](../prompts/PROMPT_LIBRARY.md), reconstructed hardening/check prompts: supported search submission and bounded memory conclusions.

The primary catalogue has a 3×6 thumbnail grid, three sections, title search, All/Saved and explicit empty/zero-result states. Thumbnails do not load every 3D model. The selected study loads lazily, cancels obsolete work and reports per-item errors.

Open in 3D opens a separate glass-off volumetric inspector: 30° turns, opt-in Spin/Pause at 20°/s, Reset view and Close. Inspector yaw is separate from study orientation. The study hides its duplicate while the inspector displays that selection. A one-auxiliary policy switches companion/inspector by close → observed detach → open, including opening/closing states.

Enter archive opens a Full Space Stage: a Blender room, eighteen positions across Daily rituals, Connected lives and Domestic systems/movement, bay navigation, close looks, tap-to-select, inspector access and Return to catalogue first on the panel. Named views are instant cuts. The room uses baked emissive atlases; exhibits use canonical real-size metadata. The three original stories remain available only for their own items.

## Inspection and retained evidence

L4's broad feature acceptance belongs to builds 001–007: eight gate frames admitted and opened, non-gate looks, correlated inputs, sampled spin material, build receipts and host tests. The resolved build-007 recheck included all eighteen study models, inspector/window switching, archive/return, repeated visits, a story and visitor start. Retained build-007 and build-008 app results are 128 passing tests; no tests were rerun for this package.

Build-008 has its own admitted visitor-start frame: gate `museum-dh-01-b8-visitor-start`, one attempt, all 17 checks passed; capture SHA-256 `047522da2eb690a77c8943ae3370870dff5541adce4b3abaeacca545fb52fd34`. That still is not a replay of every L4 feature. Build-008 search Enter was demonstrated with **injected key events**, including two Enters while composition was active; a person's host keyboard and headset keyboards remain unestablished. Its eight-second idle dismissal remains a fallback. Companion Close was demonstrated after re-aiming, with detach/reopen preserving selection.

## Failure and repair

Archive visits originally left nineteen additional native assets open per visit. Loading stored GLBs through an app-owned descriptor with `use` stabilized the recorded asset count at eight. The overlay and APK check preserve `.glb` uncompressed storage so `openFd` can work. This solved a measured asset-handle issue, not every memory question.

Build-008 logs `archive-dispose created=21 invalidAfter=22/22`: twenty-one created entities plus the root report invalid after recursive destroy. It **does not prove every underlying native allocation was released**. A warm-up plus five controlled visits on build-007 showed slower, non-monotonic growth and some native/binder increase. The outcome is unknown; inspection finding no app-side owner does not prove none exists or establish SDK/platform causation.

One source review found timing defects: the room viewpoint waited until loading completed; rapid auxiliary requests ignored opening/closing states. Fixes were host-tested with modeled interleavings. Those sub-second paths were not reproduced through the emulator's slower hover/click route. A separate first memory series failed to enter the archive after the user changed the head pose; it remained void evidence rather than being counted as visits.

## AI and human contributions

The human chose the serene alien-archive direction and larger collection/inspector experience. Agents authored code, fourteen Blender-original exhibits and the room, processed three eligible reference-derived items, and retained the earlier Tripo vessel. Model provenance is not blanket redistribution permission. No new provider generation was needed for L4. An earlier narration candidate was unheard and unintegrated. During hardening the user's own demo test changed the pose; the agent paused and then re-derived mappings.

## Learner exercise — editorial, newly written

Propose a nineteenth original exhibit in a disposable source copy. Supply one registry entry, model specification/rights record, thumbnail provenance, room anchor and tests for selection/filtering and a missing asset. Explain how a larger grid stays usable. Write acceptance cases for study, inspector and archive separately. Pass the design review when every asset path/identity has a purpose and no successful build is mistaken for visual acceptance. Native execution requires a separate authorized lane.

Limits: no headset, wearer scale/comfort, hand/controller, accepted drag or audio result. Literal card drag-out was not built. Close-look information is readable on the eye-level panel; plaque legibility remains unestablished. Decoded texture estimates are not measured GPU memory. Snapshots and depicted assets carry the package's item-level sharing holds; no fresh-machine build is claimed.

Sources: `docs/lessons/lesson-4/{EARTH_COLLECTION_DESIGN,RUNBOOK,ARTIFACTS,WALKTHROUGH,DEMO_SCRIPT}.md`, `evidence/earth-collection-001/REVIEW.md`, `evidence/demo-hardening-001/README.md`; hashes in [source notes](../prompts/SOURCE_NOTES.md).

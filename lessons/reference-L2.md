# L2 — A coherent spatial workspace

Make two windows behave as one workspace: one app-owned state, explicit open/focus/close semantics and a companion that shows current state after reopening. Learn to separate shared state correctness from the platform's window placement and focus behavior.

## States and supplied context

Start: `lesson-2-start` → `fd603d01207d9d8626df4f06e7e07cb09391df4b`. Complete: `lesson-2-complete` → `70f4ba8cc905644f65ba518109cd5e3115120cb3`. Candidate snapshot paths are `source/snapshots/L2-start/` and `source/snapshots/L2-complete/`; mapping and transformations are in [checkpoints](../source/CHECKPOINTS.json).

Accepted build-006 source `b01349f`; APK 33,365,642 bytes, SHA-256 `203d472cca301665036e8e6104da496029150c199abf54add63f92fc10f49cf4`. Retained build checks record 20 tests, including L1, eight shared-workspace tests and one template test. Native criteria L2-AC-0 through L2-AC-10 were met on the tested signed-in emulator. Across placement iterations and acceptance, 24 frames were admitted and 19 opened; byte-identical repeats were recognized separately.

Prerequisites: L1 and the [state/lifecycle map](../curriculum/ARCHITECTURE_AND_GLOSSARY.md). Supplied source was museum `docs/native/CORE_L2_L3.md`, L1 handoff and exact 6.1.9 declarations for `WindowContainer`, `SpatialNavigator`, placement and SDK stub activities. The core document is a sanitized brief; the actual coordinator start prompt stays private. Historical PICO SDK/workflow/design skills informed implementation, but their source declarations were checked rather than guessed from API names.

## Prompt sequence and output

1. [P08](../prompts/PROMPT_LIBRARY.md), sanitized brief, asks for one process owner shared by two roots.
2. [P09](../prompts/PROMPT_LIBRARY.md), reconstructed placement repair, asks the agent to compare solver output, markers and visible frames.
3. [P10](../prompts/PROMPT_LIBRARY.md), reconstructed review, probes state lifetime and close/reopen behavior.

Expected output moves controller creation from a window composition into `Application.onCreate`, then passes the same controller to the primary and companion. A fixed container id and tag request reuse. Attach/detach events count actual roots. Open while already open focuses the existing window; close detaches presentation while the app retains logical state. Selection/view/orientation last for the process; saved ids/preferences also survive process recreation. Window bundles are launch data, not the shared state channel.

## Checks and LOOK

The counted build-006 sequence opened the companion; selected in the primary and observed both roots; focused the companion without another attach; selected and turned/reset from the companion; viewed the primary; closed; changed selection while closed; reopened onto that current selection; and propagated a preference across roots. Markers carry `source=primary|companion` and `window-shows`. At no point did companion `instances` exceed one.

The platform drew overlapping windows focused-first, fading or hiding the other. The result was checked by bringing each forward without changing logical state. An invisible model in the occluded primary therefore did not prove state loss. The final companion was volumetric 880×480×400 dp, dynamically scaled, at system-default placement. The design is grounded in this emulator's forward view, not a universal spatial placement recommendation.

## Failure and repair

Five earlier builds tested placement before build-006. Anchored placements attached a window outside the forward frame. A negative offset was interpreted as an unsigned value and placed it far away. Default placement exposed the companion but a large window obscured the primary; a compact strip made text too small. The final smaller volumetric window left edges available for focus and depth for L3's scene.

One input after relaunch missed because the primary's pose changed. Its landing and unchanged state were retained. The repair used observed pose/landings, not repeated blind clicks. The independent source review found no material defect; minor marker/lifetime notes and tests were corrected in L3, not retroactively attributed to the accepted L2 binary.

## AI and human contributions

Agents read actual SDK overloads, chose one owner, wrote lifecycle tests, iterated placement and correlated inputs. No in-session human code edit, sign-in or window movement is recorded for L2. The user's earlier sign-in remained a prerequisite. The human-facing design constraint was an understandable optional companion while preserving the original catalogue route.

## Learner exercise — editorial, newly written

Design a collection companion showing saved objects in a disposable copy. Draw its lifecycle from request → attach → state update → detach → reopen. Supply host cases for duplicate open, keeping an item while closed and reopening after selection changes. Preserve the current one-auxiliary policy if doing the exercise from build-008. Pass when the same saved-list owner serves all roots and no test hides duplicate attachments by collapsing their count.

Limits: host clicks only; no placement accuracy, wearer comfort or account-free acceptance. Closing the primary through the system while the companion survives was host-tested only. No native test is run by this teaching package; current single-attempt/hold rules override historical runbook loops.

Sources: `docs/lessons/lesson-2/{RUNBOOK,ARTIFACTS,WALKTHROUGH}.md`, `docs/native/CORE_L2_L3.md`, `evidence/l2-workspace-001/{PLACEMENT,REVIEW,INTERACTIONS}.md`; hashes in [source notes](../prompts/SOURCE_NOTES.md).

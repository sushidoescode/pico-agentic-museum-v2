# Build-010: what was checked

**Build-010, version `1.1-feedback-002`, is the latest review APK.** It moves the archive's navigation panel into view after feedback testing found part of it cut off. The [download guide](../ASSET_SUPPLEMENT.md) keeps this APK separate from the unchanged original build-008.

The feedback session used an emulator. Most interactions below were tried on **build-009**. Build-010 received one checked archive-arrival capture and a specific Return/cleanup check; it did not repeat the whole interaction sequence. No headset or hand-tracking result is established by that session.

## What build-010 itself showed

- The archive opened and reported all eighteen exhibits ready.
- One verified arrival screenshot showed the Connected lives area, six floor circles and the panel fully within the view, with Return first and the saved smart speaker listed. The emulator toolbar overlapped the panel's leftmost edge.
- Returning through the panel restored the main museum. The exit record showed the room objects removed, with no new arrival-ring or cue events after Return. This does not prove that every underlying memory allocation was released.

The arrival screenshot was the single admitted capture for this build. It establishes the panel's placement for that view, not its readability from every head direction or on a headset. The door-ring exit was not tried.

## What was observed on build-009

| Area | Recorded observation |
| --- | --- |
| Starting screen and Reset | Selection and saved labels were visible, the last row was in view, and Reset was disabled at the front. Turning right enabled it; choosing another object returned it to the disabled front state. |
| Switching models | The turned vacuum disappeared without flashing back to its front view. There was no loading text, but the view was empty for about a quarter of a second while the next object loaded. |
| Fade | The incoming object visibly faded in on the main museum view. The separate Open in 3D view's fade was not recorded. |
| Saved objects | Save/Remove, the saved count, Show and Clear shortlist were checked. A saved speaker remained through later test reinstalls and relaunches. |
| Room targets | A floor-circle click moved to the earbuds; selecting the focused exhibit turned it. The doorbell's support target also responded. |
| Moving and returning | A panel button moved to another area. Fade and arrival-cue events were recorded, and Return restored the main museum. The cue is a visual brass floor ring; its appearance below the level view was not checked. |

Those observations explain the feedback changes carried into build-010. They are not a second set of build-010 results. Turn-left back to the front was not clicked in this sequence.

## The small window remains separate

Two experiments tried PICO's suggested attached **Subwindow**. Both were reported as attached by the system, but neither produced a visible panel in the emulator captures. Removing the tilt did not make the second experiment visible.

The cause remains unknown. Build-010 retains the separate window; the Subwindow experiment is not a delivered feature. Stories and window behavior still need a new review on the latest APK.

## Still to try

Use the [manual device checklist](RECORDING_AND_DEVICE_CHECKS.md) for arrival after turning the head, the door-ring exit, the arrival ring below the level view, hover and real hand input, comfort switches, the separate 3D-view fade and the story path. None was exercised in this feedback session. Headset comfort also remains untested.

## File identity and build checks

- APK: **build-010**
- Version: `1.1-feedback-002`
- Source revision: `4f0d8d3`
- Size: **41,641,132 bytes**
- SHA-256: `a5dd0c3e2ddbe21075778780d66e6103146ec0ce697f642cf42728060eed3e7f`

For comparison, build-009 was version `1.1-feedback-001`, source `b6a35a4`, with SHA-256 `192c0a33f95532954535003959815bc2b9b4ac033522cdc78dc4ce76e147cdb9`.

The session records passing unit tests and model-packaging checks for builds 009, 010 and both Subwindow prototypes. Each APK was 41,641,132 bytes; its installed hash matched its build record, and its nineteen packaged model files matched their inputs. These checks were not rerun while preparing these review pages.

The unchanged [original course evidence](../evidence/INDEX.md) belongs to earlier versions. It does not become evidence for the new APK. Source for this summary: the original app's `evidence/pico-feedback-001/NATIVE_RESULTS.md`, dated October 13, 2026, and its linked build/capture records. This is a citation to that app record, not a file bundled at the same path here.

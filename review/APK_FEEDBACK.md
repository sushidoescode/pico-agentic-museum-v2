# Museum feedback: current status

**Use build-010 (`1.1-feedback-002`) for the next review.** The October 13 feedback session checked most interactions on build-009, then checked build-010's corrected archive panel and Return path. [Results and APK identity](BUILD_010_RESULTS.md) give the exact scope; [downloads](../ASSET_SUPPLEMENT.md) keep the new review APK and original build-008 separate.

The changes concern Everyday Futures / Earth Collection. They do not describe the separate Luna prototype or a different demo.

## What changed, and what still needs checking

| Feedback | Current observation | Next check |
| --- | --- | --- |
| An old object flashes into the wrong pose during a switch. | On build-009, the turned vacuum disappeared without a front-facing flash. A short empty gap remained before the next model appeared. | Repeat on build-010, including the separate 3D view. |
| Reset and Inspect Object are confusing. | Build-009 showed Reset disabled at the front and enabled after a turn. The starting screen used Open in 3D and Object guide captions without the old Inspect Object control. | Try both turn directions and confirm that each action's purpose is clear. |
| Comfort switches are unclear or have little effect. | The one-line help was visible on build-009. The switches themselves were not exercised in this session. | Try reduced movement, reduced transparency and contrast on the latest app; describe what changes and what stays the same. |
| The extra window's purpose and placement are unclear. | The separate window remains. Two attached Subwindow prototypes produced no visible panel in the emulator. | Review the delivered guide's position, close/reopen behavior and stories on the intended device. |
| The room panel is hard to reach or read. | It was partly outside the view on build-009. The single checked build-010 arrival image showed it in view, with Return first. | Read it after looking around and from the headset's actual view. |
| Plinths and floor circles should help visitors move. | Build-009 clicks on a floor circle and the doorbell's support moved to their exhibits. | Check hover, aiming and real hand/controller input separately. |
| Arrival direction is wrong after a head turn. | The recorded head direction was checked only while facing forward at rest. | Turn left and right before moving, then record the arrival direction. |
| Moving between areas feels abrupt; exhibits feel static. | Build-009 recorded the short transition and visual arrival-ring events. Selecting the focused earbuds turned them. | Judge the transition’s comfort and look down for the brass floor ring, including with Reduce motion on. |
| Saved objects need a clear purpose. | The shortlist controls and counts worked on build-009; the saved speaker survived later test reinstalls and relaunches. | Save, remove, show and clear on build-010, then revisit the room. |
| Highlighted objects and circles are unclear. | Selection labels were visible and app-owned floor circles responded to clicks. Hover was not judged. | Check whether visitors understand the targets; the earlier reported white circle was not identified as a platform or app element. |
| A way out must stay obvious. | The panel's Return restored the museum on builds 009 and 010. | The door-ring exit and downward view of the arrival ring remain untested. |

An emulator click was logged as `GazePinch`. That label does **not** establish that a person successfully looked and pinched with tracked hands. The session also did not check the story path or the separate 3D-view fade.

## Your next review

Use the [recording and device checklist](RECORDING_AND_DEVICE_CHECKS.md). For each issue, record the APK version, what you did and what you saw. A short clip of the problem is more useful than guessing its cause.

Keep the [original course pictures and results](../evidence/INDEX.md) as history. The original build-007/build-008 test counts are not new results for build-010, and the earlier reported PICO headset trial did not identify an APK hash or complete test sequence.

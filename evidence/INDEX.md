# Tests and screenshots: what they show

**The latest review APK is build-010 (`1.1-feedback-002`).** Read its [feedback results](../review/BUILD_010_RESULTS.md) and [remaining device checks](../review/RECORDING_AND_DEVICE_CHECKS.md). Most new interaction observations belong to build-009; build-010 itself has one checked archive-arrival image and a specific Return/cleanup record. The feedback session was on an emulator, not a headset.

The six pictures in “Original development pictures” below come from the original museum's development. They show different versions of that earlier app; none of those six is a build-010 screenshot.

A **build** is a particular packaged version of the app. Build-008 remains the unchanged final APK of the original delivery. The newer feedback APK is packaged separately; its upload status is stated in the [download guide](../ASSET_SUPPLEMENT.md).

## Current build-010 image

The original build-010 archive-arrival image is kept in the separate PICO review package, outside this GitHub edition. See the [download status](../ASSET_SUPPLEMENT.md); its Drive upload is pending. The retained image shows the corrected navigation panel and six floor targets in the Connected lives bay. Return is visible first; the emulator toolbar overlaps the panel’s leftmost edge.

This is the exact existing PNG from one admitted check on October 13: 17 checks, one attempt, bound to APK `a5dd0c3e…`. It is an emulator still, not a full build-010 interaction test or headset evidence. Most new interaction observations used build-009. No new screenshot was made for this package.

The [gate summary](build-010/BUILD010_GATE_SUMMARY.json), [APK identity](build-010/BUILD_IDENTITY.json), [image scope and rights](build-010/MEDIA_NOTES.json), and [current results](build-010/CURRENT_RESULTS.md) retain those distinctions. The [asset comparison](build-010/ASSET_CHANGE_MAP.json) records unchanged build-008/build-010 asset bytes. Raw logs and full device records stay private.

## Original development pictures

| Picture | What to look for | Original version |
| --- | --- | --- |
| [Object after Reset](media/l1-reset.png) | The cup faces forward again. This is the earlier simple cup model. | L1 build-003, E01; an existing crop. |
| [Companion before repair](media/l2-before.png) | The second window is present but too small to read. | L2 build-005, E02. |
| [Companion after repair](media/l2-after.png) | A larger window shows the shared selection and controls. | L2 build-006, E03. |
| [Vacuum story](media/l3-story.png) | The second chapter and two rows of controls. A still cannot show motion quality. | L3 build-003, E04. |
| [Room before larger labels](media/l4-before-labels.jpg) | The tiny wall title in an earlier room layout. | L4 build-004, E06; an existing cropped/resized image, not a formal acceptance capture. |
| [Room after the change](media/l4-archive.png) | Larger bay lettering and a clear Return button. | L4 build-006, E05. |

These six image files are unchanged. They were visually inspected when the original package was prepared. We did not make new screenshots for the rewrite.

The final build-008 starting-screen image, E07, is in the [Drive supplement](../ASSET_SUPPLEMENT.md). The inspector image E08 is excluded from both parts because of the qualified artwork visible in its background.

## What was tested in the original delivery

The original build-007 and build-008 records each report **128 passing app tests**. Those were small tests run on the development computer, not 128 headset tests. Their summaries are [T07](build-007-tests-retained.txt) and [T08](build-008-tests-retained.txt). They were not rerun for this documentation rewrite.

Earlier emulator checks covered selection, turning, companion behavior, stories, the separate inspector, and the archive. Their builds and limits are kept in the [technical evidence record](TECHNICAL_RECORD.md).

Build-008's final visitor-start check passed 17 checks once and its image was inspected. This supports the recorded starting state. It does not establish a fresh, complete test of every museum feature.

## Later PICO feedback

In the review conversation supplied on October 13, PICO reviewers said they had tried the app on a headset and described several usability issues. That is reported reviewer feedback. It does not identify an APK hash, device/OS version, complete test sequence, or comfort assessment, so it does not replace the original build records. The [feedback status](../review/APK_FEEDBACK.md) now records the subsequent emulator observations and what still needs checking.

## Feedback-session limits

Build-010's checked arrival showed the room panel within view and Return first. Its specific exit record restored the main museum. The model switch, visible study fade, shortlist and most room-target interactions were observed on build-009, not repeated as a full build-010 test.

Two attached Subwindow prototypes produced no visible panel in the emulator. The delivered app keeps the separate window. Neither a system attachment record nor an input label of `GazePinch` establishes a visible Subwindow or successful hand tracking.

Arrival after a head turn, the door-ring exit, hover, headset input/comfort, comfort-option toggles, the separate 3D-view fade and stories remain outside that feedback session's checks. The [current results](../review/BUILD_010_RESULTS.md) keep those limits separate from earlier course results.

## What remains unknown

The original results do not establish headset comfort, hand/controller input, or account-free operation. Search Enter was tested through injected key events, not a person typing on the host keyboard. The memory investigation did not establish a cause or prove all native memory was released after leaving the room.

The separate [Luna experiment](../reproduction/README.md) reports its own code, tests, and limitations. It cannot borrow these old screenshots or test results as proof of its output.

For file-level checks, [EVIDENCE.json](EVIDENCE.json) lists image sizes and hashes; [CLAIM_SOURCES.json](CLAIM_SOURCES.json) identifies the original records behind the summaries. Private raw logs and account/device details remain outside the repository.

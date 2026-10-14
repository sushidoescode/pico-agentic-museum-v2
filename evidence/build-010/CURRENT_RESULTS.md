# Current museum APK — build-010

The retained review APK is `1.1-feedback-002` (versionCode 1), built from `4f0d8d3`. It keeps the working separate Object guide window. The experimental Subwindow is not enabled: two prototype variants opened logically but no guide panel was visible in the recorded emulator view. The two prototype host-window PNGs are byte-identical, so the supplement includes only one representative frame.

The October 13 implementation session recorded these **build-009** observations: clearer catalogue controls and shortlist, model switch without the old model resetting first, study fade-in, clicking a floor target to reach a close look, turning the focused exhibit 45 degrees, support/backboard interaction, bay navigation, and Return/disposal. The model still leaves a brief empty gap while the next model loads. The observed emulator click was logged as GazePinch; this is not a headset-hand test.

Build-009's panel was cut off at the left edge. Build-010 moves it into the tested view. **Build-010 has one admitted archive-arrival frame**, showing the corrected panel and floor circles; the host toolbar still overlaps its leftmost edge. A separate build-010 device screenshot is a non-gate look. Build-010's Return/disposal was also documented by the implementation session. The earlier build-009 checks were not all repeated on build-010.

Still untested in this feedback session: arrival after turning the head; door-ring exit; headset gaze/pinch and hover appearance; arrival-ring visibility below a level view; Reduce motion/Reduce transparency toggles; the separate inspector's fade; and the story path. No headset acceptance is claimed. The session's source/host-check record lists 192 unit tests passing before packaging; the build receipts record successful test/assemble commands. Packaging did not rerun them.

All 39 packaged `assets/` entries, including 19 GLBs, match build-008 byte for byte. Room/art and launcher source payloads are unchanged. New targets, circles, fades and labels are generated or controlled by app code. The existing source-export exclusions and private asset-supplement qualifications still apply; unchanged assets do not establish a fresh rebuild from the exported files.

`BUILD_IDENTITY.json` and `BUILD010_GATE_SUMMARY.json` bind the identities and selected evidence. Original logs and full receipts remain private. Images are copied byte for byte from existing evidence; no capture or image editing was performed for this package. Exact media scopes and rights accompany each supplied image.

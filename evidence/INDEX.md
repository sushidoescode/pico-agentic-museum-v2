# Evidence and artifact index

This index is a sanitized editorial summary of retained records at museum closeout `0efe271`. No build, native test, interaction or capture was run during packaging. SHA-256 and byte counts for each selected export are in [EVIDENCE.json](EVIDENCE.json) and the package [manifest](../MANIFEST.json). Exact raw originals and path mappings remain in a separate private audit directory; raw receipts contain identifiers unsuitable for this candidate.

## Inspected media and tests

| ID | Artifact | Actual build and class | What it establishes / limits |
| --- | --- | --- | --- |
| E01 | [L1 reset](media/l1-reset.png) | L1 build-003; existing crop of admitted `museum-14-reset-after` | Visible front view after Reset. The crop has its own digest and is not the original gate image. |
| E02 | [Companion before repair](media/l2-before.png) | L2 build-005; admitted `museum-l2-11-open-companion-after` | Existing but unreadable small strip; a liveness pass is not visual acceptance. |
| E03 | [Companion after repair/reopen](media/l2-after.png) | L2 build-006; admitted `museum-l2-28-reopen-companion-after` | Shared vessel selection after reopen. Historically linked to byte-identical opened frame 14; now also opened directly by package root. |
| E04 | [Vacuum chapter 2](media/l3-story.png) | L3 build-003; admitted `museum-l3-54-next-after` | Boundary state and repaired two-row controls. Not motion-quality evidence. |
| E05 | [Archive arrival](media/l4-archive.png) | Earth Collection build-006; admitted `museum-ec-05-b6-archive-arrival` | Room, Connected lives objects and explicit Return; not a build-008 image. |
| E06 | [Earlier Connected bay](media/l4-before-labels.jpg) | Earth Collection build-004; retained derived nongate look | Earlier tiny labels and room iteration. Never promoted to admitted evidence. |
| E07 | Final visitor-start image, **held** | Demo-hardening build-008; admitted `museum-dh-01-b8-visitor-start` | 17 checks passed once; original opened. Depicts unresolved Tripo/reference assets. Hash `047522da2eb690a77c8943ae3370870dff5541adce4b3abaeacca545fb52fd34`, 2,696,294 bytes. |
| E08 | Smartphone inspector image, **held** | Earth Collection build-006; admitted `museum-ec-06-b6-inspector` | Independent inspector visible; background thumbnails include qualified assets. |
| T07 | [Build-007 tests](build-007-tests-retained.txt) | Retained host-unit-test summary | 128 tests, zero failures/errors/skips. Not rerun. |
| T08 | [Build-008 tests](build-008-tests-retained.txt) | Retained host-unit-test summary | 128 tests, zero failures/errors/skips. Not rerun. |

All six included media files and both held selected gate images were visually opened by the package root. Originals were copied byte-for-byte; E01/E06 were already derivatives before this assignment. No new pixel edits or captures were made. No unrelated windows, personal paths, account names or device serials were visible in the selected included stills. PICO emulator chrome/environment remains visible and is included only for intended private technical review, without asserting public-marketing clearance.

## Claims requiring records beyond a still

| Claim | Retained source at closeout `0efe271` | Evidence boundary |
| --- | --- | --- |
| L1 interaction slice | `docs/lessons/lesson-1/ARTIFACTS.md`; `evidence/d1b-emulator-001/LOOK.md` and `INTERACTIONS.md` | Corrected totals: 12 admitted, 9 opened. Keep/Inspect counted clicks, vacuum turn and account-free launch were not established by that L1 slice. |
| L2 shared selection/yaw and lifecycle | `evidence/l2-workspace-001/LOOK.md`, `PLACEMENT.md`, `INTERACTIONS.md` | Six builds, placement iterations; primary-close/companion-survival remained host-tested. |
| L3 Return and reduced motion | `evidence/l3-story-001/REVIEW.md`, `LOOK.md`, `motion/MOTION.md` | Three original stories; historical recordings retain their original temporal-review limits. Rapid interruption and changing motion mid-transition are not native acceptance. |
| Eighteen models, inspector, bays and Return | `evidence/earth-collection-001/README.md`, `REVIEW.md`, `INTERACTIONS.md` | Final build-007 confirms broad L4 behavior; eight admitted frames span builds 001–007. It is inaccurate to say all eight are build-007. |
| Search/Close/disposal hardening | `evidence/demo-hardening-001/README.md`, `search-b8.txt` | Search used injected text and Enter; Close is counted host-pointer evidence. 22 invalid entities do not prove freed allocations. Five-visit memory investigation was on build-007 and inconclusive. |
| Final build-008 admission | Original gate receipt SHA-256 `544a556cf754caacc0b2c5e574713a5565e6e1068b9b37e9a195f37b897d61a5` | Bounded app liveness plus an inspected visitor-start frame. No full application, FPS or physical-device acceptance follows from the gate alone. |

The rows above are source citations, not links into an absent raw evidence tree. [CLAIM_SOURCES.json](CLAIM_SOURCES.json) pins their original digests; the private audit holds exact copies. The lessons and process documents provide reviewable summaries. Historical “gate deferred” prose in the hardening README is superseded by its final admission section; it is not the current conclusion.

The build-008 APK was rehashed from its preserved artifact, not the mutable Gradle output. Its original receipt hash is `4491af2bf2355fa8a2a650bb2f3eabee93406624cf2a339097fd5278037b546e`; it records source `c65959f` and `dirtyLines=2`. The clean closeout `0efe271` is a later documentation/evidence state. [Source checkpoints](../source/CHECKPOINTS.json) preserve that distinction.

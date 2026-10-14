# Saved versions of the source

A **snapshot** is a copy of the files at one point in the project. These eleven copies let you compare what changed as the museum grew: ten original checkpoints and one later feedback build. They do not include the original Git history.

For the current feedback app, start with [build-010](snapshots/build-010/SNAPSHOT_NOTICE.md). [Build-008](snapshots/build-008/SNAPSHOT_NOTICE.md) remains the original delivery. If you are following a lesson, compare its starting and completed versions.

| Lesson | Start | Completed version | What changed |
| --- | --- | --- | --- |
| L0 | [L0-start](snapshots/L0-start/SNAPSHOT_NOTICE.md) | [L0-complete](snapshots/L0-complete/SNAPSHOT_NOTICE.md) | Design work and early app code. Neither is a built PICO app. |
| L1 | L0-complete | [L1-complete](snapshots/L1-complete/SNAPSHOT_NOTICE.md) | Three objects, selection, turn, and reset. There was no original L1-start tag. |
| L2 | [L2-start](snapshots/L2-start/SNAPSHOT_NOTICE.md) | [L2-complete](snapshots/L2-complete/SNAPSHOT_NOTICE.md) | A companion window with shared selection and controls. |
| L3 | [L3-start](snapshots/L3-start/SNAPSHOT_NOTICE.md) | [L3-complete](snapshots/L3-complete/SNAPSHOT_NOTICE.md) | Three short object stories. |
| L4 | [L4-start](snapshots/L4-start/SNAPSHOT_NOTICE.md) | [L4-complete](snapshots/L4-complete/SNAPSHOT_NOTICE.md) | Eighteen exhibits, a separate 3D view, and a room. This is build-007. |
| Final fixes | L4-complete | [build-008](snapshots/build-008/SNAPSHOT_NOTICE.md) | Later search/window fixes and the final starting-screen check. |
| October PICO feedback | build-008 | [build-010](snapshots/build-010/SNAPSHOT_NOTICE.md) | Revised controls and shortlist, model fades, room targets and navigation-panel placement. Keeps the separate Object guide window. |

## Before you build from a copy

These exports leave out some models, thumbnails, and Android launcher images. Cloning the repository alone is not enough to rebuild the original app. [Drive](../ASSET_SUPPLEMENT.md) holds a separate supplement, but combining the two downloads has not been tested as an original-museum rebuild. The supplement’s asset paths also apply to build-010: all 39 packaged assets are unchanged, as recorded in the [asset comparison](../evidence/build-010/ASSET_CHANGE_MAP.json). The same omitted assets and sharing qualifications remain.

Work in a separate folder and follow [setup](../curriculum/SETUP_AND_VERSIONS.md). Some Gradle settings were made portable by removing an old computer-specific default. You must pass the included polish module's location when building.

The early browser files are design source. The HTML shell does not contain the whole original browser preview. L0-complete contains early Kotlin files that still needed to be added to a PICO starter project. Later snapshots contain that project's `native` folder.

## For reviewers checking file identities

[CHECKPOINTS.json](CHECKPOINTS.json) records the original revisions and exported folder fingerprints. [EXPORT_FILES.json](EXPORT_FILES.json) lists 720 exported inputs: the original 606 plus 114 for build-010, with source hashes and any changes made for export. [HELD_ITEMS.json](HELD_ITEMS.json) keeps the original 34 excluded source entries; the new checkpoint separately records its 15 omitted asset paths. The package [manifest](../MANIFEST.json) records the wider delivery qualifications. No later code or art has been inserted into an earlier snapshot.

The original APK was built from source `c65959f66d164ee28933f3dd2c09bfe1fc760ff3`. Its build receipt reports two worktree status lines. The later clean documentation closeout, `0efe2718e0eeb733e2a3b3b2cd300a1703025aa0`, does not show that the build-time worktree was clean.

Build-010 was built from `4f0d8d3a4904720b76c530380b485a708ffdca03`; `5224c1ac95a130dbf7d49806caba3f2ed2617028` is its later documentation closeout. Its receipt records 42 worktree-status lines. All 123 recorded source/configuration files match the built revision and finished checkout, but that comparison is not a claim that the entire build-time checkout was clean. The export contains 114 selected inputs plus its notice; the dependency graphs and [qualified results](../evidence/build-010/CURRENT_RESULTS.md) are separate.

Private account details, raw conversations, device logs, SDK downloads, and build caches are not included. Read the [source notices](NOTICES.md) before redistributing files.

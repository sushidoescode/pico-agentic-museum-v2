# The separate asset supplement

This repository is one of two parts.

- **This Git repository** holds the course, the review material, the walkthrough and the source snapshots.
- **The asset supplement** is a separate folder assembled by the project owner, titled “PICO Museum Delivery / 2026-09-28”. It holds the large and rights-qualified files this repository deliberately leaves out: exhibit models, the Blender project and the Android installer (APK).

## Access

This repository does not link to the supplement. When the repository was prepared on 2026-09-28, the owner had assembled the supplement and checked it file by file on their own computer. Its online upload was still in progress, and no sharing link or invitation had been issued. Ask the project owner for access. Do not assume it is available online.

## What the supplement contains

| Folder in the supplement | Contents |
| --- | --- |
| `01_Review_Package/` and its ZIP | The original review package this repository grew from, unchanged |
| `02_Assets_and_Blender/` | All eighteen current exhibit models and thumbnails, plus the archive room model and layout. It also has the original Blender file with its five room textures, the authoring scripts, the three reference models actually used, the original inputs for the generated vessel and seven PICO template resources. `RESTORE_MAP.json` maps each file to its place in the museum source. |
| `03_Android_App/` | The build-008 installer (APK) and its identity notes |
| `04_Current_Frame/` | The admitted build-008 visitor-start image and its scope notes |

The supplement has its own `README.md`, `SHARING_SCOPE.md`, `MANIFEST.json` and `SHA256SUMS`.

## Matching the two parts

This repository's [MANIFEST.json](MANIFEST.json) lists each file it leaves out under `heldItems`, with its SHA-256 hash. Thirty-three of those 37 hashes match a file in the supplement's own `MANIFEST.json`. The other four are in neither part:

| Held item | Why it is in neither part |
| --- | --- |
| The museum's original `THIRD_PARTY_NOTICES.md` | Kept private; the corrected notices are in [source/NOTICES.md](source/NOTICES.md) |
| `preview.html` and `preview-manifest.json` | The bundled Web preview's license scope is unresolved |
| One build-006 inspector image (E08 in the [evidence index](evidence/INDEX.md)) | Its background shows rights-qualified thumbnails |

Some documents here were written for the original package and call the APK, the four held model families, the template resources and the Blender file “held privately”. For this repository that is still accurate: those files are not in Git. The owner has since placed them in the supplement for team review.

## Having both parts is not a tested build

The restore map covers the current build-008 asset inputs only. It does not fill in the historical lesson snapshots. Nobody has yet built the app from this repository combined with the supplement. A developer who tries should work in a separate copy and follow [setup and versions](curriculum/SETUP_AND_VERSIONS.md). They should record new file and build identities before claiming any result.

## Rights stay with the files

The supplement keeps the same provenance as this repository: fourteen project-authored Blender originals, three reference-derived models (fidget, scooter, gascan), one generated vessel and PICO template resources. Neither part grants a blanket public license. The Blender file is copied byte for byte, and the supplement's guide records its remaining metadata qualifications. Read [asset provenance and rights](assets/PROVENANCE_AND_RIGHTS.md) and [notices](source/NOTICES.md) before sharing any file further.

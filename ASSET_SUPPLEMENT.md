# The separate asset supplement

This repository is one of two parts.

- **This Git repository** holds the course, the review material, the walkthrough and the source snapshots.
- **The asset supplement** is a separate folder assembled by the project owner, titled “PICO Museum Delivery / 2026-09-28”. It holds the large and rights-qualified files this repository deliberately leaves out: exhibit models, the Blender project and the Android installer (APK).

## Access

The owner supplied the [PICO Museum Delivery folder](https://drive.google.com/drive/folders/1C6693JWX1kvqgu253xLBeGumSB32UPB-) for this team handoff. It contains the dated [2026-09-28 delivery](https://drive.google.com/drive/folders/1AS_WAx3grzcMzsxmL7meZOO-pxiHTU2P). Both folders were readable without signing in on September 30, 2026. Use this Git repository for the latest navigation and delivery documentation; Drive's `05_Review_Repository_R2/` and its ZIP preserve the September 28 edition.

Open [assets and Blender](https://drive.google.com/drive/folders/1mR_iZEc2MCvzEHG0HjQWkd99ULH3RYZz), [the eighteen exhibit models](https://drive.google.com/drive/folders/1ZHyUQuv7WBgXgxFQfopJNDOczROMhnFr), or [the Blender room project](https://drive.google.com/drive/folders/1GxGsDcnly8oxpl9mcir6RU7irx8WqqGb). The preserved [build-008 APK](https://drive.google.com/file/d/1JFz00IfCpPrXmBb0oRTe0onfTUojJv6m/view) and [final visitor-start frame](https://drive.google.com/file/d/1CeWg1v13b3tWbLONKlvlnFL_-90jYz9D/view) are separate from Git.

The owner controls access to that folder. Link access does not establish a public reuse license or settle the per-item qualifications in [open items](review/OPEN_ITEMS.md). No Drive sharing setting was changed as part of this repository delivery. The September 28 statement that upload was in progress is historical; [team handoff](review/TEAM_HANDOFF.md) records the newer verification scope.

## What the supplement contains

| Folder in the supplement | Contents |
| --- | --- |
| `01_Review_Package/` and its ZIP | The original review package this repository grew from, unchanged |
| `02_Assets_and_Blender/` | All eighteen current exhibit models and thumbnails, plus the archive room model and layout. It also has the original Blender file with its five room textures, the authoring scripts, the three reference models actually used, the original inputs for the generated vessel, seven PICO template resources and the original provenance and attribution records (reference manifest, asset registers, pipeline and build-input notes, and entry/spec/build records for the four held exhibit families). `RESTORE_MAP.json` maps each file to its place in the museum source. |
| `03_Android_App/` | The build-008 installer (APK) and its identity notes |
| `04_Current_Frame/` | The admitted build-008 visitor-start image and its scope notes |
| `05_Review_Repository_R2/` and its ZIP | The preserved September 28 sharing repository, before the September 30 delivery documentation updates |

The supplement has its own `README.md`, `SHARING_SCOPE.md`, `MANIFEST.json` and `SHA256SUMS`.

## Matching the two parts

This repository's [MANIFEST.json](MANIFEST.json) lists each file it leaves out under `heldItems`, with its SHA-256 hash. Thirty-three of those 37 hashes match a file in the supplement's own `MANIFEST.json`. The other four are in neither part:

| Held item | Why it is in neither part |
| --- | --- |
| The museum's original `THIRD_PARTY_NOTICES.md` | Kept private; the corrected notices are in [source/NOTICES.md](source/NOTICES.md) |
| `preview.html` and `preview-manifest.json` | The bundled Web preview's license scope is unresolved |
| One build-006 inspector image (E08 in the [evidence index](evidence/INDEX.md)) | Its background shows rights-qualified thumbnails |

Raw evidence, original prompts and conversations, Git history and recordings are in neither part.

Some documents here were written for the original package and call the APK, the four held model families, the template resources and the Blender file “held privately”. That is historical preparation wording: these files remain excluded from Git, while the owner-supplied Drive link is now readable without signing in. Their rights qualifications remain unchanged.

## Having both parts is not a tested build

The restore map covers the current build-008 asset inputs only. It does not fill in the historical lesson snapshots. Nobody has yet built the app from this repository combined with the supplement. A developer who tries should work in a separate copy and follow [setup and versions](curriculum/SETUP_AND_VERSIONS.md). They should record new file and build identities before claiming any result.

## Rights stay with the files

The supplement keeps the same provenance as this repository: fourteen project-authored Blender originals, three reference-derived models (fidget, scooter, gascan), one generated vessel and PICO template resources. Neither part grants a blanket public license. The Blender file is copied byte for byte, and the supplement's guide records its remaining metadata qualifications. Read [asset provenance and rights](assets/PROVENANCE_AND_RIGHTS.md) and [notices](source/NOTICES.md) before sharing any file further.

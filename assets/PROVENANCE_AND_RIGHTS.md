# Asset provenance and rights

Local, unsent review candidate, 2026-09-28. The delivered app has eighteen exhibits: **fourteen project-authored Blender originals, three reference-derived assets and one earlier Tripo model**. The candidate includes the fourteen original runtime models/thumbnails and their authoring inputs; it holds the other four families. That omission makes every relevant source snapshot incomplete relative to the admitted APK. The exact retained APK also stays private because it embeds held assets and template content.

[ASSET_REGISTER.json](ASSET_REGISTER.json) records all eighteen runtime paths, full SHA-256, byte/triangle counts, thumbnail identities, origin class and inclusion status. The original unredacted asset/reference manifests and item-level attribution travel with the private audit record; this sanitized inventory never strips attribution from a distributed third-party model, because those models are not distributed here.

| Exhibit | Origin | Runtime bytes | Triangles | Candidate status |
| --- | --- | ---: | ---: | --- |
| 001 / quencher | Tripo-generated, post-processed | 658,752 | 7,744 | Held privately |
| 002 / vacuum | Original Blender authoring | 235,564 | 7,312 | Included for local review |
| 003 / smartspeaker | Original Blender authoring | 329,780 | 5,200 | Included for local review |
| 004 / coffeepod | Original Blender authoring | 145,088 | 5,376 | Included for local review |
| 005 / deliverybox | Original Blender authoring | 58,256 | 664 | Included for local review |
| 006 / qrcode | Original Blender authoring | 46,876 | 552 | Included for local review |
| 007 / fidget | Private reference-derived | 560,148 | 10,000 | Held privately |
| 008 / facemask | Original Blender authoring | 261,456 | 9,740 | Included for local review |
| 009 / smartphone | Original Blender authoring | 176,416 | 5,616 | Included for local review |
| 010 / earbuds | Original Blender authoring | 166,828 | 6,456 | Included for local review |
| 011 / smartwatch | Original Blender authoring | 174,952 | 6,756 | Included for local review |
| 012 / cryptowallet | Original Blender authoring | 85,296 | 3,012 | Included for local review |
| 013 / remote | Original Blender authoring | 118,480 | 3,512 | Included for local review |
| 014 / router | Original Blender authoring | 197,124 | 5,540 | Included for local review |
| 015 / doorbell | Original Blender authoring | 90,952 | 2,804 | Included for local review |
| 016 / scooter | Private reference-derived | 998,172 | 18,000 | Held privately |
| 017 / gascan | Private reference-derived | 486,240 | 12,000 | Held privately |
| 018 / rocket | Original Blender authoring | 250,164 | 7,608 | Included for local review |

The original group comprises vacuum, smart speaker, coffee pod, delivery box, QR menu stand, face mask, smartphone, earbuds/case, smartwatch, hardware crypto wallet, streaming remote, router, doorbell camera and rocket. Agent-authored Blender scripts implement the written brief; provenance does not imply that every output was manually modelled by a human. The retained human choices include generic, unbranded designs and avoiding unverified reference payloads. The rocket uses general public proportions; an unused reference rocket's NoDerivatives restriction was respected. No reference geometry, texture or measurement from that rocket was used according to the retained source record.

The scripts, generated texture sources and build metadata are in [payloads](payloads/README.md). Runtime GLBs and thumbnail PNGs live under the exact historical snapshots, with the current originals in `source/snapshots/build-008/native/app/src/main/assets`. Each source file retains its original hash and any transformation in [EXPORT_FILES.json](../source/EXPORT_FILES.json). Binaries are byte-identical relocations; there was no generation, mesh conversion, texture edit or new visual acceptance run for delivery.

Original textured exhibits use procedural fabric, paper/nonwoven, wood/kraft/label patterns, a generic phone gradient and an original rocket livery strip. The QR-like card is intentionally non-decoding: seeded random data/format modules rather than a URL or personal information. The delivery label contains abstract blocks/bars, not an address. No font binary is bundled. Inspection-sheet labels were drawn with an available host font; scripts expose `EC_LABEL_FONT` for a later authorized authoring run.

The three reference-derived objects are **fidget, scooter and gascan**. Their original source identity, file hashes and attribution are retained privately. User-reported permission to use the shared set is evidence of permission with limits; it is not an independently verified third-party license covering recipient redistribution. Their native models, thumbnails, source specs/build records, direct depictions and APK embedding must be cleared together. Confirm the permission giver's authority, allowed recipients/uses and any attribution obligations before sharing. A Tripo generator string found in source metadata does not establish provenance or rights by itself.

The **quencher/tumbler** is the previously accepted Tripo generation, not the unused reference tumbler. Its retained generation/task record and original processed bytes stay private. The project record cites provider terms consulted on 2026-09-27 and API credits, but account plan/credit ownership was not verified. Confirm the task's distribution entitlement before including the model, thumbnail or depictions. No provider calls or terms reinterpretation were performed for this package.

The **Earth Archive room** is an original Blender-authored environment based on a written brief and the project's own viewed concept image; the concept image was not a texture or uploaded reference. Its runtime GLB is 2,121,940 bytes with 14,908 triangles, five material groups and embedded baked JPEG atlases/starfield. [ROOM_REGISTER.json](ROOM_REGISTER.json) records its runtime/layout and source texture identities. The authoring/export/layout scripts and baked texture files are included. Existing runtime GLB metadata has no external image URI; source paths are relative. The room's Metal GPU bakes were documented as non-byte-identical across reruns; this package preserves the existing output, not a deterministic rebake claim.

The original compressed `earth_archive.blend` is held after read-only decompression exposed stale host-path fragments and an unexplained hashed image filename in saved workspace data. This does not prove the visible room uses that image; it means binary privacy and embedded-attribution inspection is incomplete. Resolve that specific file before sharing it. Authoring scripts and eligible runtime/texture files remain useful independently.

**PICO template content** is a separate qualification. `box.usdz` and six launcher PNGs are held; template-derived text remains for qualified local source review. The recorded tools license limits use to developers under agreement with PICO. Intended-recipient eligibility and permitted source/artifact redistribution must be confirmed. The old notice that box.usdz was absent was incorrect: it is embedded in build-008. [Corrected notices](../source/NOTICES.md) describe the exact template/module/Web-preview boundaries.

**Code, media and audio:** the museum has no blanket open-source license. The included SpatialUI polish module carries unchanged Apache LICENSE/NOTICE and copyright attribution, with documentation-only privacy transformations. Existing selected captures are project content plus PICO emulator chrome/environment for proposed private PICO technical review; they are not public-release clearance. The root's [evidence index](../evidence/INDEX.md) and [machine-readable evidence map](../evidence/EVIDENCE.json) identify exact included and held captures. Captures exposing held models/thumbnails remain private. Unheard, unintegrated ElevenLabs narration is excluded from a claimed accepted audio deliverable.

Sharing readiness is item-specific. No export license, public release, invitation or transmission is authorized by this package. The original source tags, app model content and APK were not altered to work around any hold.

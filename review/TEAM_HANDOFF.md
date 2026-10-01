# Museum v2 review handoff — October 1, 2026

Start at the [README](../README.md). The owner requested a new private personal repository named **pico-agentic-museum-v2** for the complete prepared Museum review package. It carries forward the September 30 [XRBootcamp team delivery](https://github.com/XRBootcamp/pico-space-agentic-track-dev) without changing that repository or the older personal design draft. GitHub reviewers need access to this private repository. The [owner-supplied Drive folder](https://drive.google.com/drive/folders/1C6693JWX1kvqgu253xLBeGumSB32UPB-) holds the larger supplement. No invitation or PICO review message is sent by publishing these files.

## Where to find the files

| Material | Location |
| --- | --- |
| Lessons L0–L4, beginner guide, setup, architecture and troubleshooting | [Course map](../curriculum/COURSE_MAP.md), `lessons/` and `curriculum/` |
| Prompt library and AI/human development decisions | [Prompt index](../prompts/PROMPT_INDEX.md), [development timeline](../process/AI_AND_HUMAN_DECISIONS.md) |
| Ten sanitized source checkpoints and exact-version UI module | [Snapshots](../source/SNAPSHOTS.md), [module guide](../third_party/POLISH_MODULE.md) |
| Offline walkthrough, historical captures and evidence index | `demo/index.html` after download, [walkthrough notes](../demo/WALKTHROUGH.md), [evidence index](../evidence/INDEX.md) |
| Asset notices, inventory, hashes and unsent review request | [Provenance](../assets/PROVENANCE_AND_RIGHTS.md), [inventory](../DELIVERABLES.json), [manifest](../MANIFEST.json), [unsent draft](REVIEW_REQUEST_UNSENT.md) |
| APK, original Blender project, full exhibit model set and build-008 frame | [Drive supplement and restore instructions](../ASSET_SUPPLEMENT.md) |

## Edition and identity

This v2 edition carries forward the complete reviewed package at `6be1f8a417f175d92c5d65bacec0e877d9585730`, published to the team repository on September 30. All three earlier sharing commits are retained. The October 1 change updates only delivery documentation and checksums for the personal repository; lesson text, prompt provenance, source payloads, assets, evidence and walkthrough bytes stay unchanged. The original museum application history is not included. This is a repository edition, not a new application build.

The dated Drive folder retains two earlier editions: `01_Review_Package/` is the original review package; `05_Review_Repository_R2/` is the September 28 sharing repository. Start with this GitHub README for current delivery directions. The r2 ZIP has SHA-256 `68c2426c2ebfa129bc9a0ff2a14fb7e192bfa73976a1e38a34e89c9622f5e6ed` and is 19,455,528 bytes. It contains 668 files. It does not contain the September 30 or October 1 delivery documentation updates.

The preserved build-008 APK is 42,446,151 bytes with SHA-256 `b1a61d990c3efd15f8a17ec879cf385468bcbc5d2af7f13c2696e9a1484a3f26`. Its built source is `c65959f66d164ee28933f3dd2c09bfe1fc760ff3`; the accepted museum closeout is `0efe2718e0eeb733e2a3b3b2cd300a1703025aa0`. These identify the retained application, not a build from the sharing repository.

## Drive verification

On September 30, anonymous access showed that the supplied link opens **PICO Museum Delivery**. It has one visible child, **2026-09-28**, matching the previously recorded delivery folder. That child contains the eleven expected top-level entries, including the r2 folder and ZIP. The r2 root lists its eighteen expected entries. The Drive audit is read-only and changes no permissions or files. An October 1 anonymous link check confirmed the same dated folder, APK and r2 ZIP IDs; the parent is now titled “PICO Agentic Spatial SDK Museum (Sushant)”. The September 30 download hashes below were not rerun by that link check.

The bounded audit inspected 79 folder pages and found all 184 expected asset-supplement files, with no missing or extra files or listed size mismatches in that scope. It also inspected the APK/frame directories and the r2 section and checkpoint names. Anonymous downloads of the root manifest, r2 ZIP and build-008 APK matched the retained byte counts and SHA-256 hashes. The root manifest's SHA-256 is `e63515b24f5c5f7a9be31087affb17c8bc6abe090ab1403bea1e312cd8d37787`.

The full local delivery contains 1,524 files. This audit does not claim individual cloud hashes for all 1,524 loose files or for every asset: the complete r2 archive and the two other named downloads were hashed, while the asset check compares folder listings and sizes. The September 29 desktop-provider record separately reported all 1,524 files uploaded.

## Integrity and limits

Run `python3 review/verify_checksums.py` from the downloaded repository. It checks all payload hashes plus the manifest. The producer also checks the tracked file set, 606 export identities, ten source checkpoint aggregates, JSON parsing, internal document links and bounded privacy patterns. The September 30 audit covers the original two sharing commits, followed by a focused review of that day’s publication delta; the original coordinator and museum histories remain private. Publication receipts and the commit identity are recorded separately from these hashed payloads.

All 37 held-file entries remain excluded from Git. Thirty-three held hashes match entries in the supplement manifest and the previously verified local staging; the independent cloud hash scope is limited to the three downloads named above. The exclusions are detailed in [ASSET_SUPPLEMENT.md](../ASSET_SUPPLEMENT.md). Source snapshots are incomplete as supplied, and combining them with the supplement has not been built or tested. The restore map applies to current build-008 assets, not every historical lesson snapshot.

Historical feature demonstrations retain their original build labels. Build-008's final visitor-start admission is narrower than the preceding L4 feature evidence. Reconstructed teaching prompts remain distinguished from retained exact text. The 128 passing app tests are historical results. This delivery creates no new native evidence, headset result, recording, generation or audio acceptance. The [rights qualifications](OPEN_ITEMS.md) remain in force; a working Drive link does not grant a public reuse license.

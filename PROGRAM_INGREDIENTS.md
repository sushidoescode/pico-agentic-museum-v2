# Program ingredients

This page maps each part of the program to its folder and the file to open first. Paths are relative to this repository's root. For a gentle start, read the [README](README.md) first.

**Status words**

- **Included:** in this repository.
- **Reconstructed:** newly written to teach what happened. It is not a recovered original.
- **Held:** it exists, but it is deliberately not in this repository. Some held items come in the separate [asset supplement](ASSET_SUPPLEMENT.md).
- **Not established:** never demonstrated or recorded, so do not assume it.

## Requested ingredients

| Ingredient | Folder | Open first | Status |
| --- | --- | --- | --- |
| Context and setup | `curriculum/`, `lessons/` | [curriculum/SETUP_AND_VERSIONS.md](curriculum/SETUP_AND_VERSIONS.md), then [lessons/L0.md](lessons/L0.md#context-supplied-to-the-agent) | **Included:** recorded versions, tools and the context given to the assistant. **Held:** SDKs, dependency caches and machine-local configuration. **Not established:** setup on a fresh machine. |
| Supplied skills and documents | `lessons/`, `prompts/`, `third_party/` | [lessons/L0.md](lessons/L0.md#context-supplied-to-the-agent), then [prompts/SOURCE_NOTES.md](prompts/SOURCE_NOTES.md) | **Included:** skill names and versions, hashes of the original project documents and the UI module ([third_party/POLISH_MODULE.md](third_party/POLISH_MODULE.md)). **Held:** the PICO plugin skill files and the original project documents themselves. |
| Starting and completed source states | `source/` | [source/SNAPSHOTS.md](source/SNAPSHOTS.md) | **Included:** ten file snapshots from L0 start to build-008, with identities in [source/CHECKPOINTS.json](source/CHECKPOINTS.json). **Reconstructed:** the L1 starting point (no original L1-start tag exists). **Held:** Git history and the files listed in [source/HELD_ITEMS.json](source/HELD_ITEMS.json). |
| Prompts | `prompts/` | [prompts/PROMPT_INDEX.md](prompts/PROMPT_INDEX.md) | **Included:** 5 exact excerpts, 2 sanitized briefs and 2 condensed prompts. **Reconstructed:** 12 teaching prompts. **Held:** full conversations and the later lesson start packets. Some exact later repair and review prompts were not kept. |
| Build and deploy workflow | `curriculum/` | [curriculum/SETUP_AND_VERSIONS.md](curriculum/SETUP_AND_VERSIONS.md) | **Included:** documented commands, recorded build identities and retained test summaries in [evidence/](evidence/INDEX.md). **Not established:** a build from this repository, which cannot be built as supplied. |
| Meaningful failure and repair | `process/`, `lessons/`, `demo/` | [process/AI_AND_HUMAN_DECISIONS.md](process/AI_AND_HUMAN_DECISIONS.md#four-instructive-decisions) | **Included:** each lesson's failure section and walkthrough panels 03–04. |
| AI successes and human corrections | `process/`, `lessons/` | [process/AI_AND_HUMAN_DECISIONS.md](process/AI_AND_HUMAN_DECISIONS.md) | **Included:** an editorial timeline and each lesson's “AI and human contributions” section. **Held:** raw conversations. |
| Rough walkthrough | `demo/` | `demo/index.html`, opened locally in a browser, or [demo/WALKTHROUGH.md](demo/WALKTHROUGH.md) | **Included:** nine panels of historical stills plus a [live demo script](demo/SHOT_LIST.md). **Held:** the current build-008 frame, which is in the asset supplement. **Not established:** a continuous video of build-008. |
| Vocabulary | `curriculum/` | [curriculum/ARCHITECTURE_AND_GLOSSARY.md](curriculum/ARCHITECTURE_AND_GLOSSARY.md#vocabulary) | **Included** |
| Architecture | `curriculum/` | [curriculum/ARCHITECTURE_AND_GLOSSARY.md](curriculum/ARCHITECTURE_AND_GLOSSARY.md) | **Included** |
| Troubleshooting | `curriculum/` | [curriculum/TROUBLESHOOTING.md](curriculum/TROUBLESHOOTING.md) | **Included:** observed failures and bounded responses |

## Supporting material

| Ingredient | Open first | Status |
| --- | --- | --- |
| Beginner guide | [curriculum/FIRST_STEPS_WITH_AI.md](curriculum/FIRST_STEPS_WITH_AI.md) | **Included** |
| Course map and lessons L0–L4 | [curriculum/COURSE_MAP.md](curriculum/COURSE_MAP.md) | **Included:** each lesson has a learner exercise, which is newly written. |
| Evidence and test results | [evidence/INDEX.md](evidence/INDEX.md) | **Included:** six historical stills and retained test summaries. **Held:** the build-008 visitor-start frame and one build-006 inspector image. |
| Asset provenance and rights | [assets/PROVENANCE_AND_RIGHTS.md](assets/PROVENANCE_AND_RIGHTS.md) | **Included:** project-original authoring scripts and textures ([assets/payloads/README.md](assets/payloads/README.md)). **Held:** four exhibit models and their thumbnails, plus template resources. |
| Exhibit models, Blender project, Android installer (APK) | [ASSET_SUPPLEMENT.md](ASSET_SUPPLEMENT.md) | **Held:** not in Git; they come in the separate asset supplement. |
| Headset, hand, controller and audio behaviour | [review/OPEN_ITEMS.md](review/OPEN_ITEMS.md) | **Not established:** everything was checked on the emulator only. |
| Package checks | [review/VALIDATION.md](review/VALIDATION.md), `review/verify_checksums.py` | **Included** |
| Draft review request | [review/REVIEW_REQUEST_UNSENT.md](review/REVIEW_REQUEST_UNSENT.md) | **Included:** unsent draft |

## Two builds, two kinds of evidence

Lesson 4's features were demonstrated on build-007 and earlier builds. The `lesson-4-complete` checkpoint records build-007. Build-008 is a later hardening build. It adds a search field that submits on Enter and a disposal check. Its only admitted gate frame shows the visitor-start state. It is not a new replay of every Lesson 4 feature. The [evidence index](evidence/INDEX.md) labels each image with its build.

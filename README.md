# Everyday Futures — the Earth Collection

Everyday Futures is a museum app for PICO's spatial platform. It imagines a future archive of eighteen ordinary objects from the early twenty-first century. Visitors browse a catalogue, open any object in its own rotating 3D view, and reveal three miniature stories about the original three objects. They can also step into an immersive archive room that was built in Blender.

This repository is also a course. It shows how one person built and checked the app step by step with an AI coding assistant. An AI coding assistant is a tool, built on a large language model (LLM), that reads instructions and project files and proposes code. The finished app does not use AI while it runs. You need no AI subscription, API key, headset or development tools to read the course or view the walkthrough.

![Archive room with six objects on a curved bench under a window onto space](evidence/media/l4-archive.png)

*Historical frame from the PICO emulator: arriving in the archive's “Connected lives” bay during Lesson 4 (build-006). It is not an image of the current build-008.*

## Who this is for

- **PICO reviewers:** the product, the teaching approach and the evidence in one place.
- **Learners new to AI-assisted development:** no coding experience is needed to follow the first two routes.
- **Developers:** only this route needs development skills, such as reading Kotlin and using Android/Gradle and the PICO tools in [setup and versions](curriculum/SETUP_AND_VERSIONS.md).

## Choose a route

**See the walkthrough.** Download or clone the whole repository and keep its folders together. Then open `demo/index.html` from your own computer in any web browser. It runs offline and takes about six minutes, as nine panels built from retained emulator stills. A code-hosting website only shows this file's HTML source; it does not run it. The same story is also available as a Markdown page: [demo/WALKTHROUGH.md](demo/WALKTHROUGH.md).

**Learn the process.** Read [First steps with an AI assistant](curriculum/FIRST_STEPS_WITH_AI.md), then the [course map](curriculum/COURSE_MAP.md) and [Lesson 0](lessons/L0.md). The [prompt library](prompts/PROMPT_LIBRARY.md) and [AI and human decisions](process/AI_AND_HUMAN_DECISIONS.md) show what the assistant produced, what failed and what people decided.

**Inspect the code/setup.** Start at [source snapshots](source/SNAPSHOTS.md), [architecture and vocabulary](curriculum/ARCHITECTURE_AND_GLOSSARY.md), [setup and versions](curriculum/SETUP_AND_VERSIONS.md) and the included [UI module](third_party/POLISH_MODULE.md).

| Lesson | What gets built |
| --- | --- |
| [L0](lessons/L0.md) | Context, requirements and exact inputs; a source-only checkpoint |
| [L1](lessons/L1.md) | Catalogue, details and a 3D study you can turn and reset |
| [L2](lessons/L2.md) | A companion window sharing the same state |
| [L3](lessons/L3.md) | Three miniature stories and reduced motion |
| [L4](lessons/L4.md) | Eighteen exhibits, a separate 3D inspector and the Blender archive |

[PROGRAM_INGREDIENTS.md](PROGRAM_INGREDIENTS.md) maps every part of the program to its folder and entry file.

## What is included and what is not

The museum was built and checked on a signed-in PICO emulator, not on a headset. This repository contains the course, prompts, development history, per-lesson source snapshots, the walkthrough, six historical images, evidence summaries and checksums.

It deliberately omits the Android installer (APK), four exhibit models with their thumbnails, PICO template resources and the original Blender file. **The source here therefore cannot be built as supplied.** The exact list is in [source/HELD_ITEMS.json](source/HELD_ITEMS.json), with reasons in [open items](review/OPEN_ITEMS.md) and [asset provenance and rights](assets/PROVENANCE_AND_RIGHTS.md). No public license is granted.

Git holds the review, course and source material. The complete assets, the Blender project and the APK form a separate supplement from the project owner. [ASSET_SUPPLEMENT.md](ASSET_SUPPLEMENT.md) explains how the two fit together.

## Details and checks

Build identities, test results and evidence types are in the [evidence index](evidence/INDEX.md), the [course map](curriculum/COURSE_MAP.md#checkpoints-and-current-identity) and [validation](review/VALIDATION.md). To check that every file arrived intact, run `python3 review/verify_checksums.py` from this folder.

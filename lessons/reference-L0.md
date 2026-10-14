# L0 — Give the agent context

Learn to produce a bounded implementation brief, inspect actual SDK declarations, pin external inputs and write acceptance cases while a native lane is unavailable. The outcome is a **source-only setup checkpoint**, not a built or accepted native application.

## Start, completion and prerequisites

Start: `lesson-0-start` → `49b53f6f87cc69f611eebf0dfe84176bd5b6fcc9`, an offline web design preview. Complete: `lesson-0-complete` → `a0ed5cd6eb17178013fc852b8b27206db3684a1b`. This completed tree also contains staged, unbuilt L1 source. Use the [checkpoint manifest](../source/CHECKPOINTS.json) and snapshots `source/snapshots/L0-start/` and `source/snapshots/L0-complete/` within this candidate. Neither a browser image nor a pure Kotlin test is native evidence.

Prerequisites: basic repository navigation, the product direction “choose → inspect beside the catalogue → reveal a miniature story,” and knowledge of [evidence vocabulary](../curriculum/ARCHITECTURE_AND_GLOSSARY.md). Read [setup and versions](../curriculum/SETUP_AND_VERSIONS.md) for the difference between app BOM, host SDK, CLI and template versions.

## Context supplied to the agent

The historical starting set comprised museum `AGENTS.md`, `README.md`, `design-doc.md`, `preview.html`, curriculum, reference review/manifest and native delivery plan. It evolved into the D0 checkpoint, scaffold procedure, SDK declaration map, external-kit identity and D1 prompts/acceptance record. These source paths are citations, not links to omitted originals; [source notes](../prompts/SOURCE_NOTES.md) identify the frozen files.

The session record names the installed PICO plugin 0.4.1 skills: `pico-env-doctor`, `spatial-app-dev-workflow`, `pico-cli`, `spatial-ui-design-style`, `spatial-app-onboarding` and `spatial-sdk-guideline`. This describes supplied historical guidance, not the versions a new learner should silently assume. The kit skill 0.3.4 and its module commit are separate inputs.

## Prompt sequence and expected work

1. Read [P00–P02](../prompts/PROMPT_LIBRARY.md): retained exact excerpts from the native-start prompt set the bounded product, evidence rule and useful work during a hold.
2. Use [P04](../prompts/PROMPT_LIBRARY.md), a reconstructed declaration-check prompt: verify imports, receiver scopes, units and factory error behavior against the pinned sources.
3. Use [P05](../prompts/PROMPT_LIBRARY.md), a reconstructed review prompt: ask for concrete contradictions between claims, source and acceptance cases.

Expected outputs are an input table with exact versions/digests, a declaration map with source-member citations, a responsibility map, staged reducers/presenters, meaningful pure-state checks and a table separating source, host and future native acceptance. The external polish module must be identified independently of the museum commit.

## Checks and failure/repair

Retained D0 evidence records 11 pure Kotlin tests passing under a cached Kotlin 2.0.0 fixture. That compiler is not the app's later Kotlin 2.1.20 Gradle compilation. A static symbol review found the top-level `androidx.compose.foundation.layout.weight` import invalid: valid uses were receiver members of `RowScope`/`ColumnScope`. The agent removed the import, corrected light intensity units to lux and guarded resource factories that could throw. Pure tests still passed; Compose source was still uncompiled. No separate repair prompt was issued for that in-session correction.

The declaration mapping check recorded 171 checked entries, with 17 refuted and corrected. The documentation review also corrected premature tag claims, an inaccurate digest recipe and missing transfer material. These are source-review results, not an SDK execution pass.

No native image was opened because no native frame existed. Storage readings at the decision points failed the inherited 10 GB prerequisite. The useful response was to finish input/source work without claiming build readiness proved by execution. Do not carry that historical storage reading forward as today's health state.

## AI and human contributions

The human supplied the three-object direction and operational constraints. Agents mapped declarations, wrote staged source, tested pure logic and repaired their own erroneous import and records. No in-session human code edit or native intervention is recorded. The source-only milestone is a substantive outcome with a deliberately limited evidence class.

## Learner exercise — editorial, newly written

Choose one prospective external module or model. In a disposable copy, produce its version/commit, allowlisted file list, per-file hashes and a digest recipe ordered by path. Add one verified declaration or loader contract and one native acceptance case that would prove the object renders. Ask a peer to recompute the digest and locate the cited signature. Pass when both can be reproduced and the note explicitly says what has not run. No native operation is part of this exercise.

Limit: this checkpoint establishes no APK, install, launch, interaction, native layout, headset behavior or comfort. Exported source may omit held assets and is not an offline runnable package. [Troubleshooting](../curriculum/TROUBLESHOOTING.md) explains safe next steps for storage and declaration failures.

Sources: `docs/D0_CHECKPOINT.md`, `docs/MUSEUM_START_PROMPT.md`, `docs/native/D1_PROMPTS_AND_ACCEPTANCE.md`, `docs/native/{SCAFFOLD_PROCEDURE,SDK_SOURCE_MAP,POLISH_KIT_IDENTITY}.md`, `evidence/native-hold-001/prompts/AGENT_PROMPTS.md`; frozen revision and hashes in [source notes](../prompts/SOURCE_NOTES.md).

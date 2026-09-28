# Everyday Futures — course map

Local review draft, 2026-09-28. This course teaches agentic authoring through one evolving native PICO museum. The finished app uses local assets; it does not call an LLM at runtime. L0–L3 are the emulator-first core. L4 is the delivered Earth Collection expansion, not an undelivered proposal. Hardware/MR and the independent tabletop project are outside this course's demonstrated scope.

The learner should already be able to read basic Kotlin, navigate a repository and distinguish source code from a built artifact. Spatial SDK expertise is not assumed. Begin with [setup and versions](SETUP_AND_VERSIONS.md) and the [architecture and vocabulary](ARCHITECTURE_AND_GLOSSARY.md). Read the [source qualifications](../source/SNAPSHOTS.md) before treating a checkpoint as build input: these are allowlisted derivative snapshots, not a complete Git history. Asset omissions make them incomplete relative to the admitted APK; portable layout checks are not a fresh build or fresh-machine reproduction.

| Lesson | Outcome | Starting condition | Assessment |
| --- | --- | --- | --- |
| [L0 — Give the agent context](../lessons/L0.md) | Turn a visual direction into a bounded native brief, input inventory and acceptance cases | Web design draft; no native app | Recompute one input identity and classify what is still unbuilt |
| [L1 — Choose and inspect](../lessons/L1.md) | Catalogue, details and an adjacent 3D study with selection, 30° turn/reset and preferences | L0 complete already includes staged, unbuilt L1 source | Trace one input through marker, admitted after-frame and LOOK; distinguish a missed click from state change |
| [L2 — A coherent workspace](../lessons/L2.md) | One optional companion sharing the process's state | Accepted L1 app | Explain selection in either window, duplicate-open prevention and close/reopen semantics |
| [L3 — Reveal the miniature story](../lessons/L3.md) | One three-chapter system for the original three objects, return context and reduced motion | Accepted L2 plus recorded story design | Explain why Return clears story state without restoring stale study state; separate host timing checks from native motion evidence |
| [L4 — The Earth Collection](../lessons/L4.md) | Eighteen exhibits, independent inspector and a Blender-built immersive archive | Accepted core plus earlier polish | Trace one registry entry into grid, study, inspector and archive; explain disposal evidence and remaining memory uncertainty |

Each sheet supplies a useful prompt sequence. [Prompt index](../prompts/PROMPT_INDEX.md) identifies retained exact excerpts, sanitized briefs, condensed historical prompts and reconstructed teaching prompts. Reconstructed text is usable teaching material, not a recovered chat transcript. [AI and human decisions](../process/AI_AND_HUMAN_DECISIONS.md) follows the actual iterations.

## Checkpoints and current identity

| State | Original Git revision |
| --- | --- |
| L0 start → complete | `49b53f6f87cc69f611eebf0dfe84176bd5b6fcc9` → `a0ed5cd6eb17178013fc852b8b27206db3684a1b` |
| L1 teaching start → complete | editorial mapping to L0 complete → `7157a37f4039deb95dc198c92f7011d49f472a62` |
| L2 start → complete | `fd603d01207d9d8626df4f06e7e07cb09391df4b` → `70f4ba8cc905644f65ba518109cd5e3115120cb3` |
| L3 start → complete | `2855de92b824edf34696fdc475297e6568790b24` → `fa750a9a9b01d380868aa4cb192d40f16d597c19` |
| L4 start → complete | `480853b849ce2dc6c1a6807d3ae787fbd5b9977e` → `720827216fbd73764c48b89602432386a89ac54d` |
| Later build-008 source | `c65959f66d164ee28933f3dd2c09bfe1fc760ff3` |

There is **no `lesson-1-start` tag**. L0 complete is a transparent teaching start, not a pristine empty native project. The original `lesson-4-complete` tag records build-007; it has not been moved to build-008. Original commit IDs identify historical trees; derivative file hashes identify exported bytes. See [checkpoint manifest](../source/CHECKPOINTS.json).

Current recorded APK: build-008, `dev.local.everydayfutures.museum`, arm64-v8a, 42,446,151 bytes, SHA-256 `b1a61d990c3efd15f8a17ec879cf385468bcbc5d2af7f13c2696e9a1484a3f26`. Its build receipt records two dirty lines; clean closeout `0efe2718e0eeb733e2a3b3b2cd300a1703025aa0` does not establish a clean build-time tree. The 128 passing app tests are retained results, not rerun for this package.

## Review and transfer

Review the [offline walkthrough](../demo/index.html), then choose one lesson and match its prompt to the source snapshot and [evidence index](../evidence/INDEX.md). For each claim, name its basis: source, host test, build receipt, correlated input, admitted/opened still, or reviewed motion. A render gate alone proves neither click delivery nor application FPS.

Editorial capstone: propose a nineteenth exhibit in a disposable local copy. Supply a registry record, asset provenance, failure state, scene-lifetime analysis and test cases. Do not operate the inherited museum target. Runtime assessment belongs to a separately authorized owner with a fresh storage/identity check and the existing single-attempt/hold rules intact.

Course completion here means the learner can explain and extend the evidenced design. It does not mean headset comfort, hand/controller input, literal card drag-out, account-free use, distribution clearance or fresh-machine reproduction has been established. Selected artifacts remain subject to the [rights record](../assets/PROVENANCE_AND_RIGHTS.md).

Sources: museum `docs/CORE_HANDOFF.md`, `docs/CURRICULUM.md`, `docs/NATIVE_DELIVERY_PLAN.md` and lesson records at the frozen closeout; individual hashes and source-status notes are in [source notes](../prompts/SOURCE_NOTES.md).

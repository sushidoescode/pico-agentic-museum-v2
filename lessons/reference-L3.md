# L3 — Reveal the miniature story

Build one reusable story system for the original three objects. Separate chapter state from presentation, preserve the visitor's study context, and make reduced motion reach the same composition. Learn why a static end frame cannot establish transition quality.

## States, prerequisites and supplied context

Start: `lesson-3-start` → `2855de92b824edf34696fdc475297e6568790b24` (accepted L2 and recorded story design). Complete: `lesson-3-complete` → `fa750a9a9b01d380868aa4cb192d40f16d597c19`. Use `source/snapshots/L3-start/` and `source/snapshots/L3-complete/` with the [checkpoint map](../source/CHECKPOINTS.json).

The full native sequence used build-002 (`a38072e6…`, source `19e8af3`). Review repairs were accepted on build-003, source `64cd831`, APK 33,369,569 bytes, SHA-256 `c3d2563ac7f1be645142c23e9ef278f654528ef965835ab4cd553d933d634456`. Retained build-003 result: 32 tests. L3 recorded 31 admitted frames, all opened, and five local recordings with differing review methods. These results were not rerun for delivery.

Read L2, the [architecture map](../curriculum/ARCHITECTURE_AND_GLOSSARY.md), museum `docs/native/CORE_L2_L3.md` and `docs/lessons/lesson-3/STORY_DESIGN.md` (summarized here; [source identities](../prompts/SOURCE_NOTES.md)). Historical PICO SDK/design/workflow guidance and the pinned polish kit supplied lifecycle/easing contracts. The exact coordinator start prompt stays private; review/repair wording below is reconstructed where a verbatim record is absent.

## Prompt sequence and expected output

1. [P11](../prompts/PROMPT_LIBRARY.md), reconstructed story implementation prompt: one reducer, one presenter and three data-defined compositions.
2. [P12](../prompts/PROMPT_LIBRARY.md), reconstructed Return repair: preserve changes made during the story.
3. [P13](../prompts/PROMPT_LIBRARY.md), reconstructed motion review: distinguish endpoint logic, sampled visual inspection and numerical frame analysis.

The companion becomes a story mode with Back, Next, Restart, Return and Close. The primary study retains its selected object and orientation. Repeated Reveal keeps the current chapter; a new selection starts that object's story at chapter 1. Close/reopen preserves chapter. In the later eighteen-object catalogue, selecting an item without a story ends story mode and offers no dead story action.

| Object | Chapters | Visual idea |
| --- | --- | --- |
| Handled tumbler (`quencher`) | Ritual → Signal → Afterlife | refill desk → social display → museum case |
| Robot vacuum (`vacuum`) | Routine → Boundary → Human trace | domestic route → furniture/threshold → route as a portrait |
| Smart speaker (`smartspeaker`) | Invitation → Connection → Silence | voice across a room → network → quiet room |

`StoryScenes` expresses props and one pose per chapter. `StoryPresenter` owns the companion scene's entities. Changes use a nominal 700 ms transition or an instant snap under Reduce motion. An interrupted transition retargets from the poses on screen. Story logical state does not depend on a motion preference.

## Checks, LOOK and motion

The recorded sequence covers all nine chapter states, Back/Next/Restart, repeated Reveal, both original mid-story selections, Return, saved-state agreement, comfort options and close/reopen. First frames led to a 1.25× miniature scale/lift correction and brass route marks for contrast. Build success had not revealed those visual problems.

The initial motion review opened sampled contact sheets and numerically measured every decoded frame; it did not play the recordings or inspect every frame. One animated transition measured 28 changing frames; a reduced-motion transition measured one. That supports a transition-versus-snap distinction, not wearer comfort or hardware smoothness. The build-003 reopen interval was later examined frame by frame: the first miniature frame already showed chapter 2. Full recordings are not silently included or cleared by this sheet.

## Failure, repair and verification boundary

The first Return reducer restored the entry pose. If the visitor had pressed Reset in the primary during the story, Return undid that deliberate change. The fix clears only story state. A host regression and the build-003 emulator sequence showed that Reset survives Return.

The review also fixed transition retargeting, mid-transition Reduce motion behavior, premature arrival logging and over-wide controls. **Interrupted transitions and switching Reduce motion on mid-transition are host-tested only.** The latter host check does not establish that Compose restarted the effect on device. Other changed paths were rechecked on the emulator; the two-row controls and chapter-2 reopen were opened.

L3's original vacuum path passed through furniture. A later demo-polish pass rearranged the room around its straight path and reviewed complete timestamped frame sequences. Do not describe that later fix as present in the L3 complete tag. It is present in the later L4/current lineage. The later loading/error treatment also added “Preparing the miniature…” and bounded retries for returned failures, without claiming to recover a native call that never returns.

## AI and human contributions

Agents translated chapter copy into compositions, wrote the state/presenter, inspected frames and repaired the review findings. No in-session human code correction is recorded for L3; user activity could cover the emulator, affecting capture validity. The design preserves human-authored product intent, while the Return correction reflects an agent-review finding rather than an invented human request.

## Learner exercise — editorial, newly written

Give one original object a new three-chapter motif in a disposable copy. Change chapter copy and diorama data without changing the reducer or controls. Supply tests for bounds and distinct compositions, plus a case where the visitor resets the study during a story then returns. Explain what additional frame sequence would be needed to assess motion. Pass when reduced and animated modes share endpoints and Return preserves the later study action.

Limits: the original three objects alone have stories; no claim of eighteen story productions. No hardware motion/comfort, hand/controller or account-free result. A reconstructed teaching prompt is not an exact reviewer transcript. Source snapshot/rights qualifications still apply.

Sources: `docs/lessons/lesson-3/{RUNBOOK,ARTIFACTS,WALKTHROUGH,STORY_DESIGN}.md`, `docs/native/CORE_L2_L3.md`, `evidence/l3-story-001/REVIEW.md`, `evidence/l3-story-001/motion/MOTION.md`, `evidence/demo-polish-001/README.md`; hashes in [source notes](../prompts/SOURCE_NOTES.md).

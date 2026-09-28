# First steps with an AI coding assistant

This guide is for people who have never built software with an AI coding assistant. An AI coding assistant is a program built on a large language model (LLM). You describe what you want in ordinary language, it reads the files you give it, and it proposes code or explanations. Any assistant of this kind will do; nothing here depends on a particular product.

You can do everything on this page with this repository alone: no internet, installation or headset is needed. Using an assistant for the exercise is optional.

## Three words to know first

- **Source code** is the human-readable text that describes an app. This app's source code is Kotlin, in [source/snapshots](../source/SNAPSHOTS.md). It comes as one folder for each lesson state.
- **A build** turns source code into an app you can install; on Android that file is an APK. A successful build means the code compiled. It does not mean the app looks or behaves right. The snapshots in this repository are missing some assets, so **do not try to build or install them**. See the [README](../README.md#what-is-included-and-what-is-not).
- **A screenshot** is a picture of what the app actually showed at one moment. It is the best evidence of how the app looked. It cannot show what happened between frames or whether a click landed.

A **check** (or test) is a small program that confirms one specific rule, such as “Reset keeps the selected object.” Checks are fast and repeatable. **A passing check alone does not prove the app looks right.** A layout can pass every check and still be too small to read. This course pairs checks with real screenshots for that reason.

## The working loop

1. **Pick a lesson.** Start with [Lesson 1](../lessons/L1.md) or [Lesson 2](../lessons/L2.md). Read its starting state and outcome first. The [course map](COURSE_MAP.md) lists all five lessons.
2. **Give the assistant context.** Attach or paste the lesson sheet and the specific source files involved. Say what “done” looks like and what must not change. An assistant only knows what you show it, so a missing file becomes a guess.
3. **Ask for one bounded change.** Ask for one behaviour, in named files, with a stated way to check it. “Improve the app” invites large, unreviewable edits.
4. **Check the proposed edits before accepting them.** Read what changed. Did it touch only the files you named? Does it call functions that really exist in the source you supplied? If you are unsure, ask the assistant to point to the exact line it relied on.
5. **Run the appropriate checks, if prerequisites are met.** You need a complete working copy and the tools in [setup and versions](SETUP_AND_VERSIONS.md). This repository alone does not meet them, so for now “checking” means reading the source and the existing test files.
6. **Examine the result.** Open the screenshot or output and compare it with what you asked for. Write down what the evidence shows and what it does not show.
7. **Save a checkpoint.** In your own copy, commit the change with a message that says what you checked. The lesson snapshots in this repository are checkpoints of this kind.
8. **Iterate.** If something is wrong, write a focused repair request (example below) rather than starting over.

## A teaching prompt to try

> **Teaching prompt, newly written for this guide (not from the original sessions).** Use it with Lesson 1's files.
>
> I am learning from Lesson 1 of the Everyday Futures course. I have attached `lessons/L1.md` and two files from `source/snapshots/L1-complete/native/app/src/`: `main/java/dev/local/everydayfutures/museum/state/MuseumState.kt` and `test/java/dev/local/everydayfutures/museum/state/MuseumStateTest.kt`.
>
> Without changing any code, explain what pressing Reset does. Name the function that handles it and quote the lines you rely on. Tell me which test checks that Reset keeps the selected object. If the answer is not in the files I gave you, say so instead of guessing.

You can check the answer yourself. Open the same two files and search for `reset`. A good answer names `MuseumReducer.reset` and the test `resetReturnsToFrontAndCataloguePreservingSelectionSavedAndOptions`. It quotes lines that really exist. An answer that invents a function name is a sign to supply more context or ask again.

## A focused repair request

> **Example repair request, newly written for this guide.** It is modelled on the Lesson 2 companion repair; it is not the original wording.
>
> The attached screenshot (`evidence/media/l2-before.png`) shows the companion window open, but its text is too small to read. Change only the companion window's size and text layout in `CompanionWindow.kt` so the object name and details are readable at the default distance. Do not change the shared state, the reducers or the catalogue window. List every line you changed. Then tell me which screenshot and which existing test would show that the fix worked without breaking selection.

This request names the visible problem, the evidence, the one file to change, what must stay the same and how success will be judged. To see how the real repair turned out, compare [the before image](../evidence/media/l2-before.png) with [the after image](../evidence/media/l2-after.png). Then read the “Failure and repair” section of [Lesson 2](../lessons/L2.md).

## A no-install exercise

1. Open [Lesson 2](../lessons/L2.md) and the two images above.
2. Compare the `L2-start` and `L2-complete` folders under `source/snapshots/`. Any file-comparison tool works, or read them side by side. Find the files that exist only in the completed state.
3. In a few sentences, explain what problem the companion window solved. Say what the “after” image shows, and one thing it cannot show on its own. The lesson's “Checks and LOOK” section helps here.
4. Optionally, give an assistant your notes and the lesson sheet and ask it to find a mistake in your explanation. Then check its claims against the files.

## Where next

- [Prompt library](../prompts/PROMPT_LIBRARY.md): the prompts used or reconstructed for each lesson, with labels saying which is which.
- [AI and human decisions](../process/AI_AND_HUMAN_DECISIONS.md): what the assistant produced, what failed and what people decided.
- [Vocabulary](ARCHITECTURE_AND_GLOSSARY.md#vocabulary) and [troubleshooting](TROUBLESHOOTING.md).

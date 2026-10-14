# How the app grew

The museum did not appear from one perfect prompt. It grew through small requests, mistakes, checks, and repairs.

People chose the idea and art direction. AI assistants wrote code, tests, modelling scripts, and much of the documentation. Reviews caught mistakes in that work. The project owner also signed in to the emulator and later restored it after a runtime problem. We should not describe those contributions as if one person or one model did everything.

This timeline describes the original September build. The [new Luna attempt](../reproduction/README.md) has its own record.

| When | What we tried | What changed after checking |
| --- | --- | --- |
| September 20–21 | Plan a museum around three ordinary objects. Make a browser design draft and early app code. | Incorrect code assumptions were found. This stage produced source files, not a running PICO app. |
| September 22 | Put the first app into the official PICO starter. | Build errors and theme setup needed fixes before an APK could be made. |
| September 23–25 | Select an object, turn it, and reset its view. | Text was too small, controls clipped, and some clicks missed. The layout and input checks were revised. |
| September 25 | Add a companion window. | Five placement attempts came before the final layout. Both windows needed to read the same selected object. |
| September 25 | Add three short stories. | The first scenes sat too low. A code review also found that Return could undo a Reset made during a story. |
| September 26–27 | Improve loading and reset controls, then improve the models. | A runtime failure interrupted testing. The owner restored the emulator; later checks continued. A generated cup replaced its earlier simple shape. |
| September 27 | Grow to eighteen objects and build the archive room. | Separate art and app work came together. Room labels, distances, and controls changed after inspection. |
| September 28 | Check search, window closing, and the final starting screen. | Search was checked with injected key events. The final build-008 starting screen passed its checks. A memory investigation remained inconclusive. |

## Three mistakes worth learning from

### The second window opened, but people could not read it

The first companion was a small strip. Its existence was easy to confirm; its usefulness was another matter. The repair made it larger and arranged the controls in two rows.

[Compare the pictures](../demo/index.html#p03). A useful next message would be: “The new window opens, but the labels are too small. Make them easier to read.” This is a new teaching example, not a recovered quote.

### Return brought back an old view

An early story design remembered the object's angle when the story opened. That sounds sensible until someone presses Reset during the story. Restoring the old angle would undo their latest action.

The fix was to close the story and leave the current object view alone. The lesson: test what happens when a visitor changes their mind, not just the expected sequence.

### A counter answered only part of the question

The room reported that its created objects were no longer valid after leaving. That did not prove all graphics memory had been released. The memory result stayed unresolved.

The lesson: say what a check measured. Avoid turning a small successful check into a claim about the whole app.

## Read the original detail

The [development record](DEVELOPMENT_RECORD.md) preserves the longer timeline and its source references. Four [actual saved user-message excerpts](../prompts/ACTUAL_USER_MESSAGES.md) are now available, with their context. These are part of the conversation, not the full build history. The [historical prompt record](../prompts/HISTORICAL_PROMPTS.md) labels exact excerpts, shortened briefs, and reconstructions. The new [short prompts](../prompts/PROMPT_LIBRARY.md) are teaching examples.

The rewrite is AI-assisted too. Shorter wording does not make it a human-authored transcript. Its purpose is to make the work easier to follow and the next step easier to try.

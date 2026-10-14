# What happened when we tried Luna

**Luna helped build a smaller museum prototype from a blank PICO starter.** It took 13 requests, including repairs. The final code built successfully, and 15 feature tests plus the starter's one test passed.

This is useful progress, but it does **not prove that Luna recreated the finished museum**. The prototype uses simple shapes instead of the museum's artwork. The extra fifteen catalogue objects are generic stand-ins, and the room is a set of simple objects on plinths. Its companion shows text and buttons; the original museum’s separate, independently rotating 3D inspector was not recreated. The prototype also gives extra objects generic placeholder stories. We did not open it in PICO to check how it looks or behaves.

We requested **GPT-5.6 Luna through Codex on an existing paid account**. This was not a trial inside free ChatGPT. A separate [guide](../curriculum/FREE_CHATGPT.md) explains free ChatGPT access and what students still need on their computers.

## One real request, then a repair

The first message was:

> Turn this empty PICO starter into a small museum with a handled cup, a robot vacuum and a smart speaker. Let me select an object, read a short description, and turn or reset its 3D view. Simple colored shapes are fine for this first version. Include small unit tests for selection and reset.

We also supplied the starter files and short notes about the PICO code library. **The short message was not the whole input.** An experienced operator prepared those notes, ran the build, inspected the code, and reported problems. Luna supplied all eleven final application/test files; the operator copied them without rewriting their implementation.

The first answer compiled, but left the 3D scene unchanged. The next answer tried to connect it and produced a compiler error. The follow-up was:

> Please repair the selected-object view using the attached compiler error. I also noticed in the code that the cup and speaker still sit far to either side; center whichever one I select.

That version compiled. Its appearance still needed a check in the running app. This is why the course asks students to look at the result before moving on.

## Follow the actual attempts

| Step | Requests | What the recorded checks found |
| --- | --- | --- |
| Three objects, turn, reset | [1](turns/turn-01/prompt.txt), [2](turns/turn-02/prompt.txt), [3](turns/turn-03/prompt.txt) | A static scene, then a compiler error, then a successful build. |
| Shared companion window | [4](turns/turn-04/prompt.txt)–[7](turns/turn-07/prompt.txt) | Missing files and an incorrect import. Later source checks found that one view still used its own copy of the selection. |
| Stories | [8](turns/turn-08/prompt.txt), [9](turns/turn-09/prompt.txt) | A Reset action mixed up the object and story; a follow-up repaired it. |
| Eighteen entries and a details companion | [10](turns/turn-10/prompt.txt) | Successful build and catalogue tests; extra objects use generic shapes. |
| Simple room | [11](turns/turn-11/prompt.txt)–[13](turns/turn-13/prompt.txt) | Two failed builds, then a successful build. Room entry, leaving, and layout remain untested in PICO. |

Each folder in [turns](turns/) contains the short prompt, the full input with supporting files, the returned answer, and its record. There were fourteen build/test checks including the untouched starter; five failed to compile. The failures are kept in [checks](checks/).

## Try this workflow yourself

1. Use a new folder and prepare the official PICO starter using [the recorded setup](TECHNICAL_RECORD.md#start-with-the-official-project). Build it unchanged first. You need PICO/Android tools; the shared packet does not include them.
2. Give ChatGPT your actual starter entry file, screen file, app Gradle file, and version list. The recorded project used `Main.kt`, `content/HomeVolume.kt`, `app/build.gradle.kts`, and `gradle/libs.versions.toml`.
3. Send one short request. Ask for complete changed files and where to save them. Our runner used a structured file list; ordinary named code blocks work for manual copying.
4. Save the files in your own project. Run the build and small tests. If it fails, send the exact error and the affected file before asking for more features.
5. Keep [short notes](../curriculum/MY_BUILD_NOTES.md). A successful build is one check; trying the app is another.

The [technical record](TECHNICAL_RECORD.md) gives exact versions, commands, every request link, and the detailed limits. To study the recorded endpoint, see [final-source](final-source/). Copying that endpoint into a starter replays our result; it is not a new independent test of the model.

No new museum artwork, audio, or native screenshots were produced. The original museum's APK and evidence remain separate and unchanged. The experimental APK stays outside this package; its size and hash are recorded in [EXPERIMENT.json](EXPERIMENT.json).

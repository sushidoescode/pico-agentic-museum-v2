# Luna starter-to-prototype experiment

This is an independent museum prototype built from the official PICO volumetric starter with short operator-authored teaching prompts. The first three objects use simple code-generated shapes; the other 15 catalogue entries use generic box/cylinder stand-ins. The room is an unviewed primitive plinth layout. It is not a recreation of the accepted museum’s art, layout, polish, or verified runtime behavior.

The experiment requested `gpt-5.6-luna` through Codex CLI 0.160.0 on an existing paid account. It was **not a verified Free ChatGPT session**. The requested model and CLI event records are retained; the event format does not attest the backend model. No fallback warning was observed. A text-only instruction prohibited model tools, and the recorded responses contain no tool events. The operator attached files/API declarations, copied returned files without editing their implementation, ran host checks, inspected source, and reported actual errors. This involved experienced assistance; short prompts alone were not the complete input.

The prompts are newly written teaching prompts, not quotations from historical user conversations. Each request was a fresh ephemeral CLI session with explicit attachments, not an ongoing ChatGPT conversation. Native rendering, gestures, window opening/reopening, room entry, visual fit, comfort and runtime performance were not tested. Build success is not runtime acceptance.

## Start with the official project

Use a PICO development environment with the Android SDK and PICO SDK repositories configured. This experiment used managed **pico-cli 0.5.0**, **Spatial SDK BOM 6.1.9**, **Gradle 8.13**, **AGP 8.13.2**, **Kotlin 2.1.20**, Android **compile/min/target 35**, and an **arm64-v8a** target. The host JDK was **Temurin 21.0.12.1+1**; Java/Kotlin bytecode target was **11**. Installed CLI versions and available templates can differ: inspect your command’s `--version` and `project create --help` first.

In a terminal, choose an empty project directory outside this sharing repository. The recorded scaffold command, with the host-specific executable/project paths replaced, was:

```sh
pico-cli project create --sdk-type spatial --template volumetric --name luna-museum --package dev.local.lunamuseum.reproduction --sdk 6.1.9 --dir /path/to/luna-museum -- --offline
```

The `--offline` scaffold and builds worked against an already prepared local SDK/dependency cache. This packet does not provide that cache or demonstrate a fresh offline setup. A fresh machine needs the normal official environment/dependency setup. Do not copy another machine’s `local.properties`; let your IDE/environment create it. Use the same package above when replaying the returned files; changing only the folder name is safe.

Open that generated directory in Android Studio. Before asking for a feature, provide these **starter files from your new project**:

- `app/src/main/java/dev/local/lunamuseum/reproduction/Main.kt`
- `app/src/main/java/dev/local/lunamuseum/reproduction/content/HomeVolume.kt` (initially empty)
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

The first recorded input also supplies a small SDK 6.1.9 declaration digest for `SpatialView`, entities, transforms and built-in SpatialUI controls. These are API facts, not a completed museum implementation. No accepted museum source, external polish module, supplied museum models or generated/reference models were attached. The official starter’s unused box/icon assets stayed local and are not included here.

## Use the text-and-copy loop

1. Read the next prompt and its attached context in the corresponding `turns/turn-NN/input.txt`. Each file clearly separates the short teaching prompt from supporting attachments. For a new recreation, attach **your current files** rather than pretending the recorded code is your model’s work.
2. Ask the model to return complete contents of changed/new files with project-relative names. This experiment used a JSON envelope with `summary`, `files` (`path` and `content`) and `limitations`; a ChatGPT student can use clearly named code blocks instead. Do not ask it to operate a headset or emulator.
3. Create or replace exactly those files inside the generated project. `turns/turn-NN/response.json` records what this run actually returned, including incomplete responses. Preserve your own response and previous version before replacing anything. Never treat a model’s summary as evidence that a file or test exists.
4. Check the project on the host, then send the actual compiler/test error back with the affected files. In the recorded environment, from the generated project directory, the command was:

   ```sh
   ./gradlew --offline --no-daemon --console=plain :app:testDebugUnitTest :app:assembleDebug
   ```

   Omit `--offline` only when intentionally resolving the normal declared dependencies in your own configured environment. Unit tests run on the host; the APK is a build artifact. These commands do not install or launch it.
5. Compare source behavior with the requested behavior even after tests pass. This run found a static scene, a missing companion implementation, an incorrect import, unshared state, and a mismatched reset through compiler output and source inspection. It allowed at most three repair prompts per slice.

To inspect the exact recorded endpoint without making a new model claim, apply the response files in numbered order to the official starter, or copy `final-source/` over it. That replays the recorded result; it is not a new independent model recreation. No APK, SDK binary, wrapper JAR, cache, signing key or account configuration is distributed by this experiment.

## Results and feature map

The final project compiled and its **15 feature tests plus the starter test passed**. It took 13 model requests and 14 host build/test checks, including the blank starter check. Five checks failed compilation; the failed outputs remain in this packet. These are the recorded results, not a prediction for a new model session. L0–L4 below map course feature goals, not identical accepted-museum checkpoints.

| Feature goal | Recorded endpoint | Observed host result | Remaining limit |
|---|---|---|---|
| L0: empty official foundation | starter | Build + 1 starter test | Existing prepared environment only |
| L1: three exhibits, select/turn/reset | turn 03 | Build + 3 feature tests | Scene/fit never rendered here |
| L2: companion shares selection | turn 07 | Build + 6 feature tests | Actual windows/recomposition/reopen untested |
| L3: three stories, reduced motion | turn 09 | Build + 12 feature tests | Timed discrete poses; no motion/comfort observation |
| L4: 18 catalogue entries + text companion | turn 10 | Build + 15 feature tests | Extra 15 entries use generic shapes |
| L4: primitive room | turn 13 | Build + same 15 feature tests | No room-specific test or native entry/exit proof; all room objects generic |

The room entry control is in the panel labelled “Companion inspector” in the generated source. Despite that label, it contains only text and buttons: no separate model view or independent inspector rotation was reproduced. Turning from this panel changes the shared main-view angle. The prototype also generates placeholder stories for the additional fifteen objects, unlike the original museum’s three-story-only behavior. The room code lays out 18 generic primitives and plinths; it has no exhibit picking, detailed architecture, wall environment or navigation. The main view owns the story timer, so companion-only playback after closing the main view remains unestablished. The main controls occur after the scrolling catalogue; actual comfort and reachability need a native review.

The final private APK is 32,777,233 bytes, SHA-256 `c51c62af78b415c6408daa48a0c74a4acfa9cb526ca5568e983a5c9a1b4c0898`. It is not distributed here. There are 11 model-authored Kotlin source/test files in [final-source](final-source/). The exact model returned each file; the copy harness did not edit its contents.

| Request | Short teaching ask | Observed check / follow-up |
|---|---|---|
| [01](turns/turn-01/prompt.txt) | Three-object museum | Compiled; model acknowledged static scene |
| [02](turns/turn-02/prompt.txt) | Connect selected 3D rotation | Failed nullable TransformComponent access |
| [03](turns/turn-03/prompt.txt) | Repair error and center shapes | Compiled |
| [04](turns/turn-04/prompt.txt) | Add shared companion | Failed: response omitted referenced files/callback/import |
| [05](turns/turn-05/prompt.txt) | Return missing companion files | Failed incorrect SpatialNavigator import; source had copied state |
| [06](turns/turn-06/prompt.txt) | Share updates and add controls | Compiled; main still copied the state |
| [07](turns/turn-07/prompt.txt) | Connect main to shared state | Compiled |
| [08](turns/turn-08/prompt.txt) | Stories and reduced motion | Compiled; source Reset mismatched exhibit/story |
| [09](turns/turn-09/prompt.txt) | Repair Reset; check zero motion | Compiled |
| [10](turns/turn-10/prompt.txt) | 18-entry catalogue and text companion | Compiled |
| [11](turns/turn-11/prompt.txt) | Enter/leave primitive room | Failed coroutine import; room lacked explicit sizing |
| [12](turns/turn-12/prompt.txt) | Repair room coroutine/layout | Failed nonexistent Immersion.Full and internal weight import |
| [13](turns/turn-13/prompt.txt) | Fix those compiler errors | Compiled; final 16 tests passed |

Each request has a separate short `prompt.txt`, full `input.txt` with SDK/file attachments, exact returned `response.json`, and sanitized `metadata.json`. [EXPERIMENT.json](EXPERIMENT.json) records counts, qualifications, token usage and build/APK hashes. [EXPORT_FILES.json](EXPORT_FILES.json) maps original and exported hashes and transformations. Sanitized logs are under [checks](checks/). Full CLI streams, private originals, local configuration and APKs are retained outside the sharing packet.

CLI-reported totals were 235,308 input tokens (including 38,144 cached input), 55,684 output tokens and 7,802 reasoning-output tokens. These include CLI context and are not evidence of Free ChatGPT quotas. No monetary charge was returned. Two operator preflight mistakes are recorded separately: an initial JDK path error before Gradle ran, and a mistaken build-log filename before a model request launched. They are not counted as model compile failures.

## Source and dependency notice

The generated starter and SDK are PICO components governed by their own terms. API declarations in the attachments are concise operator-written symbol summaries; SDK source/JARs and full API decompiles are not included. The model-authored application files depend on those APIs and do not bundle them. This packet introduces no blanket license for PICO or other third-party components. No external polish library, museum asset payload or reference model was used by this prototype. The 18 exhibit names were derived from the supplied asset register; the corresponding art was not supplied to Luna.

# Setup, exact versions and reproduction limits

This package is prepared for local source/teaching review. **It is not build-complete or offline runnable:** four exhibit families, template object and launcher PNGs are held, dependency caches/SDKs are not supplied, and no fresh build or native run was performed for this export. Instructions below describe a later authorized reproduction after those prerequisites are resolved; they are not a report of commands run for delivery.

| App input | Recorded value |
| --- | --- |
| Built-source commit | `c65959f66d164ee28933f3dd2c09bfe1fc760ff3` |
| Clean documentation closeout | `0efe2718e0eeb733e2a3b3b2cd300a1703025aa0` |
| Current artifact | `demo-hardening-001/build-008` |
| Application ID / version | `dev.local.everydayfutures.museum` / code 1, name 1.0 |
| APK | 42,446,151 bytes; SHA-256 `b1a61d990c3efd15f8a17ec879cf385468bcbc5d2af7f13c2696e9a1484a3f26`; held privately |
| ABI | `arm64-v8a` |
| Spatial BOM | 6.1.9 |
| Kotlin / Compose compiler plugin | 2.1.20 |
| Android Gradle Plugin / Gradle wrapper | 8.13.2 / 8.13 |
| Android compile / minimum / target SDK | 35 / 35 / 35 |
| Java and Kotlin bytecode target | 11 |
| External module | SpatialUI polish 0.3.4, commit `3b09ac5db919afef612558f3fdb5302b2c55e3eb` |

| Recorded host/tool input | Value and scope |
| --- | --- |
| Build JDK | OpenJDK 21.0.12.1, 2026-08-18 LTS, bundled with primer-cli |
| Host Spatial tools selection | 6.1; not a BOM version or hardware-compatibility claim |
| pico-cli | 0.5.0 public, Darwin arm64, Node v25.8.1; a separate older ambient 0.4.4 was present historically |
| primer-cli | 0.0.2-alpha.5, Build 20260909110037 |
| Volumetric template | 6.1.0 |
| Tools components | Editor 6.1.9, emulator 6.1.0, agent-vault 6.1.2 |
| Art toolchain | Blender 5.1.2 (`ec6e62d40fa9`), glTF exporter 5.1.20, bundled Python 3.13.9, ImageMagick 7.1.2-25 Q16-HDRI |

These are retained build/source identities, not a statement that PICO has approved a teaching-version baseline or that every host component was required to compile build-008. The build receipt retains `dirtyLines=2`; the later clean closeout does not prove a clean build-time worktree. L4's completion tag is build-007, with build-008 recorded separately.

The retained [compile dependency report](../source/dependency-freezes/deps-debugCompileClasspath.txt) and [runtime dependency report](../source/dependency-freezes/deps-debugRuntimeClasspath.txt) describe the resolved build-008 graphs. They are reports, not lockfiles, artifact caches or a guarantee of deterministic dependency retrieval. Individual declarations may resolve upward through constraints; for example, the catalog's core/annotation declarations should not be mistaken for the full resolved graph.

Before reproduction, the responsible developer needs authorized access to the SDK/template and dependency repositories, Android platform 35/tooling, the recorded JDK/Gradle toolchain or an explicitly assessed compatible replacement, and sufficient disk. Begin any environment-dependent PICO session with a read-only doctor. Retain the inherited 10 GB free-space prerequisite at the time of a build, plus the proposed output cost. Existing owner/hold/retry rules must be resolved explicitly; this document grants no native ownership and no new retry.

Resolve held assets and template recipient eligibility item by item using [the rights register](../assets/PROVENANCE_AND_RIGHTS.md). A later restore must use the expected paths and exact authorized identities, or create a separately identified derivative. Do not automatically generate substitutes. Missing launcher PNGs currently prevent complete Android resource resolution; missing models/thumbnails also prevent APK parity. Source review remains useful without restoring them.

From the extracted package root, these host-only path checks need no SDK or Gradle execution:

```sh
PACKAGE_ROOT="$PWD"
NATIVE_ROOT="$PACKAGE_ROOT/source/snapshots/build-008/native"
POLISH_ROOT="$PACKAGE_ROOT/third_party/spatial-ui-polish/resources/polish"
test -f "$NATIVE_ROOT/settings.gradle.kts"
test -f "$NATIVE_ROOT/gradle/libs.versions.toml"
test -f "$POLISH_ROOT/build.gradle.kts"
test -f "$POLISH_ROOT/src/main/kotlin/dev/local/picopolish/PolishTheme.kt"
```

For a later authorized build, copy the selected snapshot to a separate working directory, restore only cleared prerequisites, and create machine-local `local.properties` there with `sdk.dir` and `spatial.tools.dir` appropriate to that host. The recorded `spatial.tools.dir` convention uses the PICO tools root, not a versioned subdirectory. Keep that file private. Set `JAVA_HOME` to the chosen verified JDK. Do not modify the frozen original museum or the review candidate.

The portable module lives in this package. Resolve `PACKAGE_ROOT` before changing directories. In the separate restored working copy, the intended command is:

```sh
cd "$WORKING_NATIVE_ROOT"
./gradlew --no-daemon --console=plain \
  "-Ppico.polish.dir=$POLISH_ROOT" \
  :app:testDebugUnitTest :app:assembleDebug
./gradlew --no-daemon --console=plain \
  "-Ppico.polish.dir=$POLISH_ROOT" \
  :app:dependencies --configuration debugCompileClasspath
./gradlew --no-daemon --console=plain \
  "-Ppico.polish.dir=$POLISH_ROOT" \
  :app:dependencies --configuration debugRuntimeClasspath
```

Set `WORKING_NATIVE_ROOT` explicitly to that separate directory. The exported `gradle.properties` no longer supplies the original host's relative MCP-worktree default. The supported explicit `-P` property is essential. These commands may resolve dependencies over the network; no offline cache is included and no downloads were initiated for this package. Record new source/module hashes, original-versus-edited file differences, dependency reports, test results, APK size/hash/package/ABI and tool versions before making a new build claim. The retained 128 app tests passing is historical evidence, not this command's result.

L0-complete is an overlay, not a generated Gradle project. Its historical scaffold route was the following, issued from a separate empty working directory with a verified pico-cli 0.5.0 binary:

```sh
"$PICO_CLI_BIN" project create --sdk-type spatial --template volumetric \
  --name everyday-futures --package dev.local.everydayfutures.museum \
  --sdk 6.1.9 --dir native
```

The overlay and Gradle/module integration then need deliberate application using the later native snapshot as a reference. The export excludes the old host orchestration helpers, so this is a teaching procedure rather than an automatic reconstruction of L1 build inputs. Existing L1+ snapshots already contain the scaffold; do not rerun creation over them.

Deployment is a separate step for a root operator with an explicitly assigned, verified target. After a fresh compatible build and authorized clearance, use a verified installed CLI and the owned target, never an implicit device:

```sh
: "${PICO_CLI_BIN:?Set the verified pico-cli 0.5.0 executable path}"
: "${MUSEUM_DEVICE_ID:?Root must supply the explicitly owned verified target}"
"$PICO_CLI_BIN" app install "$WORKING_NATIVE_ROOT/app/build/outputs/apk/debug/app-debug.apk" \
  --device "$MUSEUM_DEVICE_ID"
"$PICO_CLI_BIN" app launch dev.local.everydayfutures.museum \
  --activity .platform.LaunchActivity --device "$MUSEUM_DEVICE_ID"
```

These command forms were read from the installed 0.5.0 workflow documentation; they were not executed in package preparation. Capture the selected target identity privately, installed APK identity and bounded app-only logs. Install/launch alone is not acceptance: obtain new identity-bound render/input evidence under the existing gate policy and open the exact frames. Preserve failure evidence and the spent retry budget. Do not restart/move/resize the emulator, change accounts/firmware/Wing, clear global logs or use another device as an implied continuation.

The existing successful scope is the tested signed-in emulator. Account-free operation, PICO hardware and wearer comfort are not established by the package. No continuous build-008 demonstration recording, SDK compatibility expansion or fresh-machine reproduction is claimed.

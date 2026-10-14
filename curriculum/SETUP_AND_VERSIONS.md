# Get your computer ready

This course is about asking an assistant to help build a PICO app. The easiest route uses a coding assistant that can open your project and run commands. A normal ChatGPT browser chat cannot do those jobs on your computer.

You can read the lessons first without installing anything.

## 1. Choose where to work

Create an empty folder called `my-museum`. Open it in your coding tool. Use a new folder so the original museum download stays unchanged.

If you are in a class, use the coding tool your instructor has prepared. Otherwise, PICO's [installation guide](https://developer.picoxr.com/document/pico-cli-installation-guide.md) lists supported tools and the setup steps.

## 2. Ask the assistant to prepare the tools

Give your coding assistant the installation-guide link above, then ask:

> Help me get this folder ready to build a PICO app. Check what is already installed and explain anything you need me to do.

The assistant should follow the official guide, explain missing tools, and confirm the folder and coding tool before making setup changes. PICO calls its command tool **PICO CLI**. You do not need to memorise its commands.

After setup, start a new assistant session in the same folder so it can load the new tools. This step is part of [PICO's setup guide](https://developer.picoxr.com/document/pico-cli-installation-guide.md).

## 3. Start with an empty app

Ask:

> Create a basic PICO starter app in this folder, ready for a small 3D object beside a list. Build it before we add the museum.

The assistant should use the official starter and keep its generated settings together. PICO's [first-app guide](https://developer.picoxr.com/document/pico-cli/quickstart-spatial/) explains this route.

If it reports an error, ask it to fix that error first. Do not add the museum features to a starter that does not build.

## 4. Open the starter

Use your own PICO emulator or the one assigned by your instructor. An emulator runs the app on your computer so you can look at it without a headset. Agree with the operator before using a shared one.

Ask your coding assistant to install and open the starter on that assigned setup. **You should see the starter app, not just a message saying the build passed.** If no suitable setup is available, you can continue reading; the visual checks will need to wait.

You are ready for [Lesson 1](../lessons/L0.md) when you have a working folder, a successful build, and an app you can open.

## If you are using ChatGPT in a browser

You will save files and run the tools yourself, or work with an instructor who can help. Follow PICO's [Android Studio setup guide](https://developer-cn.picoxr.com/en/document/spatial-sdk/set-up-development-environment/) to install the development tools, then open your starter project there.

Open the starter's `Main.kt` and its screen file (called `HomeVolume.kt` in the recorded starter). Paste their contents into ChatGPT, with each filename above it. Also provide `app/build.gradle.kts` and `gradle/libs.versions.toml` when it needs the project's settings. If your starter uses other names, ask which files to send rather than creating empty files with these names.

After saving an edit, open Android Studio's Terminal tab in the project folder. On macOS or Linux, enter:

```sh
./gradlew :app:assembleDebug
```

On Windows, enter `gradlew.bat :app:assembleDebug`. Wait for `BUILD SUCCESSFUL`; if it fails, copy the first error into the chat. This makes an APK, the Android installation file. Use PICO's [first-app instructions](https://developer.picoxr.com/document/pico-cli/quickstart-spatial/) to install and open it on your assigned setup, or ask your instructor to help with that step. The browser chat cannot install it for you.

See [the browser-chat guide](FREE_CHATGPT.md) for the copy-and-paste loop.

The old museum folders are reference copies with missing assets, not an installable student starter. Developers rebuilding that exact app can use the separate [original version and build record](RECORDED_SETUP.md). This rewrite did not test a fresh computer setup.

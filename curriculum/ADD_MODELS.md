# Add the first three objects

Your new starter does not contain the museum models. This step puts the files in place before you ask the assistant to show them.

## Download the models

Open the [exhibit-model folder in Drive](https://drive.google.com/drive/folders/1ZHyUQuv7WBgXgxFQfopJNDOczROMhnFr). Use these files:

| Object | File | Where to find it |
| --- | --- | --- |
| Cup / tumbler | `vessel_study.glb` | In the model folder itself. |
| Robot vacuum | `vacuum.glb` | In its `exhibits` subfolder. |
| Smart speaker | `smartspeaker.glb` | In the same `exhibits` subfolder. |

A GLB is a 3D model file. Download the files to your computer. Opening their Drive page is not enough to put them in your app.

Use the supplied art only where your class or project has permission. The cup has separate provider-related sharing conditions in the [asset notes](../assets/PROVENANCE_AND_RIGHTS.md). You can practise with simple shapes while waiting for cleared models, but that does not finish the recognizable-object check.

## Put them in your working project

With a coding assistant, tell it where you downloaded the three files and ask:

> Here are the three model files I downloaded. Put them in my project and tell me where they are.

Check that it copied the actual files and reported their locations. Keep the original downloads.

If you are doing the copying yourself, open your working project's `app/src/main` folder. Create an `assets` folder if it does not exist, then create a `models` folder inside it. Put all three files there. This gives paths such as `app/src/main/assets/models/vacuum.glb`.

Give ChatGPT those paths with your current screen code. They are the locations in **your new project**; you do not need to recreate the old museum's folder layout.

Now return to [Lesson 2: show the objects](../lessons/L1.md#add-the-objects). The first check is whether selecting the vacuum shows the vacuum, not just whether the project builds.

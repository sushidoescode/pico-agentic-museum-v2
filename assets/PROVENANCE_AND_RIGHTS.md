# Where the art came from

The finished museum has eighteen exhibit models. They came from three different sources, so they do not all have the same sharing conditions.

| Group | What it contains | Where to find it |
| --- | --- | --- |
| Fourteen original Blender models | Vacuum, speaker, coffee pod, delivery box, QR stand, mask, phone, earbuds, watch, crypto wallet, remote, router, doorbell, and rocket | Runtime files in the source snapshots; scripts and textures in [payloads](payloads/README.md). |
| Three models adapted from supplied references | Fidget toy, scooter, and gas can | Separate Drive supplement. |
| One generated model | The Tripo cup, also called the tumbler or quencher | Separate Drive supplement. |

AI assistants wrote the Blender scripts for the original models. People chose the direction and constraints. “Original” here describes the source of the artwork; it does not mean someone modelled every shape by hand.

The archive room was also authored in Blender for this project. Its exported model, layout, scripts, and baked textures are included. The editable Blender file is in [Drive](../ASSET_SUPPLEMENT.md), outside Git.

## What you can learn from these files

Look at how a model is built from simple shapes, given a material, exported, and placed in the app. The fourteen original models use generic designs. The QR-like card does not encode a link, and the delivery label does not contain an address.

The runtime files in this package are the existing exports. We did not regenerate or retouch them for the documentation rewrite. Some texture baking can vary between runs, so rerunning a script is not a promise of identical file bytes.

## Check before sharing the art further

This project has **no blanket public reuse license**. The owner's Drive link makes files available for review; it does not settle every permission question.

- **Reference models:** the owner reported permission to use the supplied set. File-level redistribution rights and required attribution still need confirmation.
- **Tripo cup:** the generation is recorded, but the account plan and ownership of its API credits were not verified for redistribution.
- **PICO template files:** the app contains launcher art and a template object. Access and redistribution depend on the applicable PICO terms and recipient eligibility.
- **Blender file:** the saved workspace contains old path fragments and an unexplained image name. Its full metadata and attribution review is unfinished. The exported room model is a separate file.
- **Screenshots and APK:** these can include the same qualified assets, so the conditions apply to them too.

No new provider generation or narration was needed. An earlier ElevenLabs audio candidate was never accepted as part of this package.

## Detailed records

[ASSET_REGISTER.json](ASSET_REGISTER.json) lists all eighteen objects and their file fingerprints. [ROOM_REGISTER.json](ROOM_REGISTER.json) describes the room. The [preserved rights record](RIGHTS_RECORD.md) has the full per-file notes and technical details; its “held privately” wording describes the earlier Git export. Some of those files are now in the separate owner-supplied Drive folder.

The [Drive guide](../ASSET_SUPPLEMENT.md) explains what is in each part. [Source notices](../source/NOTICES.md) cover code, templates, and the included UI module.

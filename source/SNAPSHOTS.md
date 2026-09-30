# Source snapshots for local review

This is the Museum team's teaching package. Ten allowlisted file snapshots preserve the named lesson states without exporting the original museum Git history. They are **incomplete relative to the admitted APK** and have not been compiled, deployed or reproduced on a fresh machine. Source inspection and path validation are the checks performed for this package.

[CHECKPOINTS.json](CHECKPOINTS.json) records every original peeled Git revision separately from the sanitized snapshot aggregate. [EXPORT_FILES.json](EXPORT_FILES.json) records original/output bytes and SHA-256, source revision, transformation and evidence class for each exported input. These are new export identities; no original tag was created, moved or recreated.

| Lesson | Teaching start | Completed export | Qualification |
| --- | --- | --- | --- |
| L0 | [L0-start](snapshots/L0-start/SNAPSHOT_NOTICE.md) | [L0-complete](snapshots/L0-complete/SNAPSHOT_NOTICE.md) | Browser source and context first; completed state includes staged, unbuilt L1 Kotlin. |
| L1 | L0-complete | [L1-complete](snapshots/L1-complete/SNAPSHOT_NOTICE.md) | Editorial start mapping: no original `lesson-1-start` tag exists. The completed tag's frame count was corrected forward to 12 admitted, 9 opened. |
| L2 | [L2-start](snapshots/L2-start/SNAPSHOT_NOTICE.md) | [L2-complete](snapshots/L2-complete/SNAPSHOT_NOTICE.md) | Shared museum state and companion window. |
| L3 | [L3-start](snapshots/L3-start/SNAPSHOT_NOTICE.md) | [L3-complete](snapshots/L3-complete/SNAPSHOT_NOTICE.md) | Stories for the original three objects. |
| L4 | [L4-start](snapshots/L4-start/SNAPSHOT_NOTICE.md) | [L4-complete](snapshots/L4-complete/SNAPSHOT_NOTICE.md) | The completed tag is build-007. |
| Hardening | L4-complete | [build-008](snapshots/build-008/SNAPSHOT_NOTICE.md) | Later built-source revision `c65959f66d164ee28933f3dd2c09bfe1fc760ff3`; retained build receipt reports `dirtyLines=2`. |

Each snapshot contains the historical `prototype.js`, `prototype.css`, `preview-shell.html` and design document. The bundled Web preview and vendor bundle are held. The shell alone does not reconstruct the offline browser preview. The design document is historical and does not describe current app acceptance.

L0-complete contains the staged native overlay. Later snapshots contain `native/` rather than duplicating the synchronized staging directory. The allowlist includes Kotlin and tests, manifests, Gradle configuration/wrapper and text resources. Eligible original exhibit models, thumbnails and archive assets appear only when they existed at that source revision. Nothing from a later checkpoint is inserted into an earlier state.

Held models/depictions and six template launcher images are absent. Some resource references therefore cannot resolve in this export; it is not currently build-complete. The four held exhibit families are the Tripo tumbler and reference-derived fidget, scooter and gascan. The template `box.usdz` is also absent. [HELD_ITEMS.json](HELD_ITEMS.json) lists exact identities and reasons; [asset provenance](../assets/PROVENANCE_AND_RIGHTS.md) explains the clearance needed. No stand-ins were substituted and no app cleanup/rebuild was performed.

The derivative removes the stale host-specific `pico.polish.dir` default from exported `native/gradle.properties`. Use the existing explicit Gradle property with the package-local external module as shown in [setup](../curriculum/SETUP_AND_VERSIONS.md). Executable permissions for wrapper scripts are retained. The module code is unchanged; its documentation has recorded privacy and link transformations. This arrangement is a portability preparation, not native acceptance of modified source.

No `.git`, SDK artifact, Gradle cache, `local.properties`, credential, keystore, provider task JSON, reference manifest, account/ownership log, raw recording, complete evidence tree or confidential coordinator document is included. Asset-authoring scripts and eligible source textures live once under [assets/payloads](../assets/payloads/README.md). Read [notices](NOTICES.md) before proposing any recipient distribution.

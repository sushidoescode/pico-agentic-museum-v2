# Package validation

September 30, 2026. **Private team repository edition of the September 28 review candidate. Correspondence remains unsent.** These checks concern documentation, exported file identities, navigation and package integrity. They are not a new application acceptance result or distribution clearance.

## Check your copy

From the repository folder (or the extracted archive folder), run:

```sh
python3 review/verify_checksums.py
```

It needs Python 3.8 or later and no extra packages. It only reads files: it checks every file in `SHA256SUMS`, confirms that `MANIFEST.json` lists the same files, and names any file that is not part of the package. Alternatively, run `shasum -a 256 -c SHA256SUMS` on macOS or `sha256sum --check SHA256SUMS` with GNU coreutils. These are integrity checks, not source builds.

`MANIFEST.json` lists every payload with path, byte count, SHA-256, original identity, transformation, evidence class, inclusion status and reason. It excludes itself and `SHA256SUMS`. `SHA256SUMS` covers payloads plus `MANIFEST.json` and excludes itself. The payloads are exactly the files tracked in this repository; Git's own `.git` folder is never listed. `heldItems` identifies excluded bytes, not missing files. The `.gitattributes` file turns off line-ending conversion so that checksums also match on Windows.

## Team repository edition, 2026-09-30

The user authorized placement in the private XRBootcamp team repository and supplied the Drive folder link. This edition updates delivery documentation, navigation, availability statements, the inventory and hashes. The original course, lesson prompts, source snapshots, assets, evidence and offline walkthrough are unchanged. The original museum Git history is absent; this repository has its own short delivery history.

The producer reruns the host-only integrity validator and the recipient checksum verifier before publication. Remote commit and tree identity are checked after the push. See [team handoff](TEAM_HANDOFF.md) for the edition boundaries, Drive verification and reproduction limits. No new archive, application build, install, native test or capture is part of this transfer.

## Repository successor, 2026-09-28 (historical)

This repository grew from the original review package. It adds the [README](../README.md), the [program ingredient index](../PROGRAM_INGREDIENTS.md), a [beginner guide](../curriculum/FIRST_STEPS_WITH_AI.md), the [asset-supplement explanation](../ASSET_SUPPLEMENT.md), the checksum verifier and `.gitattributes`. It also shortens `START_HERE.md` and updates the navigation and sharing wording in this review folder and `DELIVERABLES.json`. The walkthrough changes one link and one sentence about where the held build-008 image and APK are. No application source, snapshot, image, evidence export or prompt changed.

The producer's host-only checks are:

- the tracked file set equals the manifest's file set, with every hash in both lists;
- the 606 export hashes and ten checkpoint aggregates are recomputed;
- every JSON file parses;
- Markdown and HTML links outside the source snapshots resolve inside the repository, including Markdown heading anchors;
- a privacy pattern scan runs.

The same checks run again on a fresh local clone, where the walkthrough is also opened offline. The successor archive is checked separately. Those results and receipts are recorded outside this repository, beside the archive, so the payload never has to hash itself. No build, install, native test or new capture was made.

## Original package validation (historical)

The original review package was validated as follows before this repository existed. Its archive and its original `START_HERE.md` are unchanged.

| Check | Result and scope |
| --- | --- |
| Frozen museum | Clean closeout `0efe2718e0eeb733e2a3b3b2cd300a1703025aa0`, all nine original lesson refs unchanged; no `lesson-1-start` exists. |
| APK preservation | Preserved build-008 file rehashed and copied privately: 42,446,151 bytes, `b1a61d99…`; full digest in [setup and versions](../curriculum/SETUP_AND_VERSIONS.md) and MANIFEST. Original app/evidence bytes untouched. |
| Source exports | 606 input/output records checked; ten sanitized checkpoint aggregates recomputed. Focused reviewer independently read every original mapped Git object and checked both hashes and byte counts. 581 byte-identical-relocation claims matched. |
| Built-source correspondence | All 92 included build-008 app-source files checked against the retained receipt. Held files mean incomplete APK parity, despite those matches. |
| Portable module | Exact original 27-path kit identity pinned; LICENSE/NOTICE unchanged, module resource subtree unchanged. Documentation transforms and removed host-default property recorded. Package-local paths exist. No Gradle configuration, compilation or fresh-machine reproduction ran. |
| Prompt provenance | Five retained exact excerpts verified as unchanged contiguous text; 46 whole-file source citations matched the frozen revision. Other blocks labelled sanitized, condensed or reconstructed. |
| Visual review | Root and focused reviewer opened all six included stills. Root also opened both selected held images. Included pixels showed no unrelated windows, personal paths, account names or serials. Actual build/evidence classes retained. |
| Offline walkthrough | Browser loaded nine panels and all six images locally. Root inspected entry and rendered companion/archive panels. No horizontal overflow at 1440-pixel viewport. No remote font/script/media dependency. Final change was a speaker-note wording correction. |
| Assets | Eighteen entries mapped; included GLB JSON inspected for external image/private-path references. Source worker opened original exhibit sheet and all fourteen included source texture images. No new geometry render, rebake or generation. |
| Privacy/navigation | Automated payload pattern scan plus targeted source/pixel review; JSON parsed and reviewer links resolved. Bounded scan, not exhaustive secret or legal clearance. Historical snapshot runtime links to omitted dependencies remain documented omissions. |
| Focused review | One independent review, no material findings. One minor Return-state sentence corrected once in HTML/Markdown and generator. See [resolution](REVIEW_RESOLUTION.md). |

Original capture/gate, source transformation and prompt audit maps remain private. Raw gate receipts were not exported. Build-008's visitor-start summary retains its 17 passing checks, original hashes and bounded scope; build-007/earlier feature evidence is not relabelled build-008. The 128 app tests are retained prior execution, not newly run.

## Readiness limits

Snapshots omit held assets/template graphics and are not build-complete or offline runnable. This delivery adds the user-authorized GitHub placement and links to existing Drive files. No fresh build/install/native test, new capture, provider generation, accepted audio, correspondence or history rewrite is part of it. Recipient eligibility, supported teaching versions, rights/attribution and runtime/hardware limits remain in [open items](OPEN_ITEMS.md).

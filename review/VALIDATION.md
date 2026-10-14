# Check the files in this package

This is the October 14 delivery, carrying the revised lessons and the finished build-010 review records. File checks confirm that a download is complete and unchanged; they do not prove the app works on a device.

From this documentation folder, run:

```sh
python3 review/verify_checksums.py
```

You need Python 3.8 or later and no extra packages. The script reads the files, compares their fingerprints with `SHA256SUMS` and `MANIFEST.json`, and makes no edits. The complete Drive delivery has its own outer manifest and `verify_package.py`, covering the assets and APK too.

A **hash** is a file fingerprint. `MANIFEST.json` lists each payload's size, hash, origin and sharing changes. `SHA256SUMS` covers the payloads and manifest. Git history is excluded. `heldItems` identifies files omitted from this documentation/source export; some are supplied separately in the qualified Drive supplement.

The producer checks links, JSON, source hashes, preserved lessons and evidence, and APK identity. The [October 14 check record](OCTOBER_14_PACKAGE_CHECKS.md) states the local scope. The [Drive download check](DRIVE_DELIVERY.md) records the later upload verification. The [October 13 revision](FEEDBACK_REVIEW.md), [October 7 review](READABILITY_REVIEW.md) and [older package history](PACKAGE_HISTORY.md) retain their original dates.

Keep these results separate:

1. **Package checks:** file integrity, navigation, selected source exports and existing-picture review.
2. **Original museum:** historical feature captures and build-008's final starting-screen check in [evidence](../evidence/INDEX.md).
3. **Feedback app:** build-009 interactions and build-010's narrower checks in [current results](BUILD_010_RESULTS.md).
4. **Luna prototype:** separate code and recorded checks in [reproduction](../reproduction/README.md).

No app build, install, native capture or headset test was run for packaging. No complete fresh-computer setup, beginner completion or free-account reconstruction is established by these file checks.

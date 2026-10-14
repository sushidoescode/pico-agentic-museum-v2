# The included UI module

The museum uses a small extra module called **SpatialUI polish**. It supplies shared UI code. It is included here so reviewers do not need access to the original private development repository.

Use version **0.3.4**, from commit `3b09ac5db919afef612558f3fdb5302b2c55e3eb`. Its code, tests, configuration, Apache [LICENSE](spatial-ui-polish/LICENSE), and [NOTICE](spatial-ui-polish/NOTICE) are unchanged. Some documentation had private details removed and links adjusted for this package.

When building an original museum snapshot, tell Gradle where the module is:

```sh
-Ppico.polish.dir=/absolute/path/to/third_party/spatial-ui-polish/resources/polish
```

Replace the example path with the actual location on your computer. The [setup guide](../curriculum/SETUP_AND_VERSIONS.md) shows how to create that path without typing it out.

For exact file checks, use [POLISH_IDENTITY.json](../source/POLISH_IDENTITY.json) and [EXPORT_FILES.json](../source/EXPORT_FILES.json). The [preserved module record](MODULE_RECORD.md) explains the 27 original tracked paths, file fingerprints, required Gradle aliases, and the difference between this module and older package archives.

The module's historical test notes describe earlier work. Including them does not establish a new museum build or headset test.

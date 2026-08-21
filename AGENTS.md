# AGENTS.md

## What this is

Yomihon — Android manga reader (Kotlin, Jetpack Compose, Gradle multi-module). Community fork of [Mihon](https://github.com/mihonapp/mihon) (Tachiyomi lineage).

**Package names do not match the branding.** Code lives under `eu.kanade.tachiyomi.*` (upstream code), `mihon.*` (newer features), and `tachiyomi.*` (library modules); only `applicationId` is `app.yomihon`. Follow each module's existing namespace — never invent `app.yomihon.*` packages.

## Commands

CI (`.github/workflows/build.yml`) is the source of truth and runs in this order:

```bash
./gradlew spotlessCheck              # ktlint gate; fix with ./gradlew spotlessApply
./gradlew testDebugUnitTest          # unit tests
./gradlew verifySqlDelightMigration  # required after any DB schema change
./gradlew assembleRelease -Pinclude-telemetry -Penable-updater
```

- Single test: `./gradlew :app:testDebugUnitTest --tests "SomeClass.method"`
- Use `testDebugUnitTest` (not `test`): these are **build types** (`debug`, `release`, `foss`, `preview`, `benchmark`), not product flavors.
- Root `spotlessApply`/`spotlessCheck`/`clean` also delegate into the included build `gradle/build-logic`.

## Setup gotchas

- JDK 21 (`.github/.java-version`) + Android SDK (`local.properties`).
- **ML models are gitignored and absent from a fresh clone**; OCR / panel-detection silently breaks without them:
  - `app/src/main/assets/ocr/{encoder.tflite,decoder.tflite,embeddings.bin}` and `app/src/main/assets/ocr_fast/{encoder,decoder}.tflite`
  - `data/src/main/assets/panel_detector/model.tflite`
  - Pinned URLs + sha256: "Download ML models" step in `.github/workflows/build.yml`; manual steps in CONTRIBUTING.md.

## Architecture

- Convention plugins and the `mihonx` version catalog live in included build `gradle/build-logic`; dependency versions in `gradle/libs.versions.toml`. Typesafe project accessors are enabled (`projects.data`).
- Modules: `:app` (Compose UI/features; entrypoints `App.kt`, `MainActivity` under `app/src/main/java/eu/kanade/tachiyomi/`), `:domain` (repositories/interactors), `:data` (implementations + SQLDelight), `:source-api`/`:source-local` (catalogue sources), `:core:common`, `:core:archive`, `:core-metadata`, `:i18n`, `:presentation-core`, `:presentation-widget`, `:telemetry`, `:baseline-profile`.
- DI is **Injekt**, not Hilt/Koin/Dagger.
- Database: two SQLDelight schemas in `:data` — main `Database` (`.sq` files + numbered `.sqm` migrations under `data/src/main/sqldelight/tachiyomi/`) and `OcrCacheDatabase` (`sqldelight-ocr/`). Schema changes need a new `.sqm` migration, then run `verifySqlDelightMigration`.
- Strings: moko-resources in `:i18n`. Edit only `src/commonMain/moko-resources/base/`; other locales come from Weblate — never hand-edit them.
- `-Pinclude-telemetry` opts into Firebase (google-services/crashlytics); without it telemetry is compiled out. `-Penable-updater` enables the update checker.

## Misc

- Wireless ADB helper: `./scripts/adb-wireless pair|connect|devices`.
- Containerized toolchain: `.devcontainer/README.md` (its "Java 17" note is stale — CI uses 21).

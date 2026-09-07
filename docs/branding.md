# Yomitsu — Branding Decision Record

> Single source of truth for the Yomihon → Yomitsu product rebrand (executed
> 2026-09-07). Any future agent touching names/branding MUST read this first.
> Approved visual assets: `docs/Logo/` (note the capital L).

## Brand identity

- **Product name**: Yomitsu (public identity everywhere the user looks).
- **Approved logo source**: `docs/Logo/Yomitsu-logo` (PNG 1254×1254, black
  background, white 読-style mark + lavender accent ≈ `#B090E0`).
- **Approved cover/banner source**: `docs/Logo/Yomitsu-cover-page`
  (PNG 1672×941 — GitHub social-preview ratio, dark + lavender).
- **Visual direction**: Japanese-inspired, minimal, reader-oriented, black/white
  base with the approved lavender accent. The lavender accent is LOGO
  branding only — the app's Material 3 theme system (13 color schemes,
  dynamic color) is unchanged. Do NOT turn the app UI purple.
- **No transparent logo variants exist**; generated derivatives (README logo,
  launcher icons) are produced from the two approved files only. Do not
  redesign the artwork; only mechanical format conversion is allowed.
- **Wordmark**: plain text "Yomitsu" (no custom font exists).

## Category rules (audit classification)

| Category | Rule |
|---|---|
| A. Public branding | CHANGE to Yomitsu |
| B. Technical identity | KEEP (package/namespace/Gradle coordinates) |
| C. Upstream/historical | PRESERVE (lineage, history, upstream URLs) |
| D. Legal/attribution | PRESERVE (Apache-2.0 text, copyright lines) |
| E. Compatibility | PRESERVE (schemes, backup format, persisted names) |
| F. Generated/derived | regenerate only via approved assets |
| G. Dead/unused | leave or note |
| H. Unknown | investigate before touching |

## Technical identifiers that MUST remain unchanged

- `applicationId = "app.yomihon"` (app/build.gradle.kts) — changing it breaks
  Android upgrade identity, installed data, providers, and backups.
- All code namespaces: `eu.kanade.tachiyomi.*`, `mihon.*`, `tachiyomi.*`.
  Never invent `app.yomihon.*` packages (docs/rules.md §3).
- Intent schemes `tachiyomi://` and `mihon://` (extension-store deep links,
  tracker OAuth redirect URIs `mihon://*-auth`).
- Backup file format `*.tachibk` and filename pattern
  `${APPLICATION_ID}_<date>.tachibk` (BackupCreator).
- Provider authorities `${applicationId}.provider` / `.shizuku`.
- Telemetry gate: `app.yomihon` / `app.yomihon.debug` package names +
  release-certificate fingerprint (TelemetryConfig.kt).
- `google-services.json` package names (`app.yomihon` + build-type suffixes).
- Maven coordinates in `gradle/libs.versions.toml`:
  `com.github.yomihon:{Furiganable,hoshidicts,image-decoder}` — these resolve
  against the upstream Yomihon GitHub org, not this repo's name.
- AnkiDroid integration persisted names (user data on device):
  default deck "Yomihon", model "Yomihon Card", `YOMIHON_*` model field/CSS
  constants, media filenames `yomihon-*` / `yomihon-audio-*`. The model is
  looked up BY NAME in the user's AnkiDroid collection — renaming the
  constants' defaults is safe only for NEW installs; existing users' decks
  keep their stored names (repo honors `AnkiDroidPreferences` overrides).
  Constants therefore KEEP their names.
- DictionaryTermCard default Anki tag `"yomihon"` (appears on created cards —
  changing would alter users' existing card tags).
- `rootProject.name = "Yomihon"` → **changed to "Yomitsu"** (build-path only,
  no code depends on it — verified via grep; settings dir names unaffected).
- Upstream docs URLs `https://yomihon.github.io/...` — links point at LIVE
  upstream documentation that still serves this codebase's guides; PRESERVE.
- Upstream Discord invite (`Constants.URL_DISCORD`, CODE_OF_CONDUCT) —
  upstream community; PRESERVE.
- `.github/FUNDING.yml` (`patreon: mihon`) — upstream maintainers; PRESERVE.
- Upstream `yomihon/yomihon` refs in `release.yml` if-gates — those gates make
  release jobs SKIP on this fork (by design; releases are built locally).
  Renamed artifact names → Yomitsu (see changed list).

## What was changed (Category A → Yomitsu)

- `i18n` base `strings.xml`: `app_name` = "Yomitsu" (launcher label,
  notification titles, onboarding, Settings header — all read
  `MR.strings.app_name`; single source of truth).
- Launcher icons: `mipmap-*/ic_launcher*.webp` + adaptive `drawable/ic_launcher_*`
  + `@color/ic_launcher_background` — regenerated from `docs/Logo/Yomitsu-logo`.
- Splash + About logo: `drawable/ic_mihon.xml` (vector mark) redrawn to
  Yomitsu mark; `LogoHeader` and notification small icons reference it
  unchanged (`R.drawable.ic_mihon` name kept — resource-name-only, zero
  code churn).
- README rewritten: Yomitsu primary product, lineage section preserved.
- `.github/assets/logo.png` → Yomitsu logo (README logo + social preview).
- Issue templates, CONTRIBUTING: product-name refs → Yomitsu (upstream links
  preserved).
- `release.yml` APK artifact names → `yomitsu-*` (release gate ifs keep
  upstream comparison = skip-on-fork, unchanged).
- `AppUpdateChecker.GITHUB_REPO` / AboutScreen GitHub link → point at this
  fork's repo (`Nikhil0921/yomitsu` after repo rename).
- `settings.gradle.kts` `rootProject.name` = "Yomitsu".
- `docs/*.md` title lines + `docs/prd.md` §1.1 wording → Yomitsu (technical
  facts like applicationId untouched).
- GitHub repo renamed `yomihon` → `yomitsu` + description updated (gh CLI).
  GitHub serves redirects from the old name; local `origin` remote updated.

## What was intentionally NOT changed

- LICENSE text + copyright lines (Apache-2.0, upstream notices).
- CHANGELOG history entries (historical record; compare-links to upstream tags).
- Upstream credit sections in README (Yomihon → Mihon → Tachiyomi lineage).
- `gradle/mihon.versions.toml`, `mihonx` catalog, `Theme.Tachiyomi` styles,
  `ic_mihon` resource NAMES, `tachiyomi.db`/`ocr_cache.db` names.
- All Category B/E/D/C items listed above.
- Root `AGENTS.md`, `architect.md`, `architect-2.md` (user-owned planning docs;
  their "Yomihon" titles are historical session documents).
- `docs/memory.md` historical blocks (append-only record).

## Repo naming

- GitHub repo: `yomitsu` (renamed via `gh repo rename` — redirects from old
  URL remain active). Release/tag URLs auto-follow the redirect.
- Update-checker + About GitHub link point at `Nikhil0921/yomitsu`.

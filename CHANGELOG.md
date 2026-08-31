# Changelog

All notable changes to this project will be documented in this file.

The format is a modified version of [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
- `Added` - for new features.
- `Changed` - for changes in existing functionality.
- `Improved` - for enhancement or optimization in existing functionality.
- `Removed` - for now removed features.
- `Fixed` - for any bug fixes.
- `Other` - for technical stuff.

## [v0.5.1] - 2026-08-31

### Added
- Add advanced TTS voice configuration (Settings → Read aloud and voice): TTS engine
  picker, language and locale pickers, and a searchable voice picker with
  quality/latency/network indicators
- Add voice preview ("Play sample") and reset-to-default voice configuration
- Add deep-link from the reader's Read-Aloud tab to the full voice settings screen

### Changed
- Redesign the in-reader TTS playback bar as a floating pill: rounded, elevated,
  width-constrained, and positioned to avoid overlapping the reader's bottom tray

## [v0.5.0] - 2026-08-30

First release of this fork. This repository is a fork of [Yomihon](https://github.com/yomihon/yomihon)
(and through it, Mihon); it is not the official Yomihon project. This release contains
significant development on top of the upstream Yomihon v0.4.0 base.

### Added
- Add Text-to-Speech (Read-Aloud) mode: reads manga aloud via the system TTS engine with sentence-by-sentence playback, next/previous sentence stepping, pause/resume, and automatic page/chapter progression
- Add Read-Aloud settings (speech rate, pitch, auto page turn, auto next chapter, keep screen on)
- Add region-level auto-scroll on webtoon/long-strip pages: the reader scrolls to the text region being spoken
- Add OCR sentence cache and N+1 page prefetch for smooth Read-Aloud playback

### Improved
- OCR on tall webtoon strips: pages are now tiled with overlap (and seam duplicates deduplicated) instead of downscaled, making text actually readable for recognition; tiles run concurrently for faster scans
- Fix English text reading order on non-Japanese pages
- Deduplicate concurrent OCR scans of the same page (single-flight) so rapid page swipes no longer trigger repeated scans

### Fixed
- Fix reader memory leak (~100 MB retained after reader exit, LeakCanary-verified) by detaching the TTS controller from the engine on teardown
- Fix background OCR prefetch failures killing a healthy playback session (e.g. on brief network loss)
- Fix advance-confirmation timeouts on webtoon pages (visible-item detection)
- Fix TTS not following manual page navigation in some pause/resume cases

### Other
- Remove generated agent/session state (`.opencode/`) from the tracked repository and ignore it going forward
- Improve `.gitignore` protection (release artifacts `*.apk`/`*.aab`, `.opencode/`)

## [v0.4.0] - 2026-07-28

### Added
- Add option to bold target word in sentence field for Anki card export

### Changed
- Upgraded upstream base to Mihon v0.20.1 (from v0.19.3)
  - You can see Mihon's full changelog [here](https://mihon.app/changelogs/)

### Improved
- Update LiteRT to v2.1.6 for armv7 support

## [v0.3.2] - 2026-06-02

### Added
- Add self-hosted OwOCR server support
- Add setting to enable/disable automatic OCR model fallback

### Changed
- Switch to yomihon/image-decoder
  - Pass crop coords from the native decoder

### Fixed
- Fix OCR failure on AVIF and other image types
- Fix inconsistent auto crop on AVIF and other image types
- Fix offset OCR coordinates on some images when using auto crop

## [v0.3.1] - 2026-04-14

### Improved
- Better performance for selecting a region

### Fixed
- Fix new region selection
  - Prefer loaded pages over refetching
  - Fix selection region for vertical reader
  - Allow selection across multiple pages for vertical reader

## [v0.3.0] - 2026-04-12

### Added
- Panel-by-panel reading mode (@leoxs22)
- Cropped image export for Anki cards
  - This can be enabled in the settings

### Improved
- Normalize punctuation for OCR speech bubble text
- Allow Japanese/Chinese dictionary searches across spaces

### Fixed
- Fix saved searches not deleting when they are held
- Fix shifted OCR results in crop mode
- Fix app crash on devices without LiteRT library

### Other
- Derive region crop from page source, rather than PixelCopy

## [v0.2.5] - 2026-04-05

### Added
- Dictionary results as a popup mode for pre-scanned chapters
  - Requires clearing the scanned chapter cache (sorry)
  - Allows for one-tap dictionary lookups of any word
- Setting toggle for darkening the reader when the dictionary is open
- Audio button for dictionary terms and Anki export

### Improved
- Improve ruby format for exported Anki cards

### Changed
- Default Anki card format no longer center aligns glossary
- Group dictionary results with the same expression

### Fixed
- Fix some incorrect dictionary styling
- Fix extra pitch and term info for dictionary searches

## [v0.2.4] - 2026-03-31

### Added
- Search filter presets
- Full-page OCR chapter scanning
- Dedicated OCR setting screen

### Improved
- Better Japanese deinflection for searches
- Near instant dictionary imports

### Changed
- Reposition the copy button next to OCR text (@leoxs22)
- Switch dictionary controls to use Hoshidicts
- Add top whitespace to images in exported Anki cards

### Other
- Added database automatic migration logic
- Added Android DevContainer development support (@leoxs22)

## [v0.2.2] - 2026-03-23

### Added
- Add more field values for frequency dictionaries

### Improved
- Alphabetically sort Anki field value list

### Fixed
- Fix WordSelector alignment when no dictionary results

## [v0.2.1] - 2026-03-15

### Added
- Add English dictionary search support
- "Online" OCR model option (GLens) - many languages

### Improved
- Automatic fallback to another model if OCR fails

### Changed
- Add and use word (furigana) for default Anki template
- Add checkmark icon for duplicate cards

### Fixed
- Fix dictionary highlight issue with romaji
- Fix pitch accent color on Anki light mode
- Fix reader crash on arm32 devices when running OCR

## [v0.2.0] - 2026-02-20

### Added
- Add one-click AnkiDroid card creation
- Add AnkiDroid settings screen and field mappings
- Download and import recommended dictionaries from in-app
- "Fast" OCR model option (experimental)

### Improved
- OCR selection can be done on immediate hold + drag (no finger lift required)

### Changed
- Dictionaries now import in the background
- Dictionary import status is shown in notifications

### Fixed
- Fix broken pitch accent graph display

## [v0.1.1] - 2026-01-25

### Changed
- Improve display of OCR results on large screens

### Fixed
- Fix crash on tablets when opening dictionary settings
- Fix missing app language options

## [v0.1.0] - 2026-01-23

### Added
- In-reader OCR using `manga-ocr-tflite` with C++ impl, GPU and CPU fallbacks, automatic re-initialization, and error handling.
- Integrated dictionary system: low-RAM dictionary import and search; it supports term, pitch, kanji, and frequency dicts, priority sorting, and shows import progress in the UI.
- OCR selection mode (long-tap to select regions) and added OCR result UI sheet.

### Changed
- Rebranded to **Yomihon** (app name, logos, links, Firebase and related assets).
- Added C++ (this'll probably be removed with the new model next update) for running OCR.

### Other
- Initial release of **Yomihon** (based on Mihon v0.19.1).
- The full changelog for Mihon releases is available in their [repository](https://github.com/mihonapp/mihon/blob/main/CHANGELOG.md).


[unreleased]: https://github.com/Nikhil0921/yomihon/compare/v0.5.0...main
[v0.5.0]: https://github.com/Nikhil0921/yomihon/compare/v0.4.0...v0.5.0
[v0.4.0]: https://github.com/Nikhil0921/yomihon/compare/v0.3.2...v0.4.0
[v0.3.2]: https://github.com/yomihon/yomihon/compare/v0.3.1...v0.3.2
[v0.3.1]: https://github.com/yomihon/yomihon/compare/v0.3.0...v0.3.1
[v0.3.0]: https://github.com/yomihon/yomihon/compare/v0.2.5...v0.3.0
[v0.2.5]: https://github.com/yomihon/yomihon/compare/v0.2.4...v0.2.5
[v0.2.4]: https://github.com/yomihon/yomihon/compare/v0.2.2...v0.2.4
[v0.2.2]: https://github.com/yomihon/yomihon/compare/v0.2.1...v0.2.2
[v0.2.1]: https://github.com/yomihon/yomihon/compare/v0.2.0...v0.2.1
[v0.2.0]: https://github.com/yomihon/yomihon/compare/v0.1.1...v0.2.0
[v0.1.1]: https://github.com/yomihon/yomihon/compare/v0.1.0...v0.1.1
[v0.1.0]: https://github.com/yomihon/yomihon/compare/c856f12...v0.1.0

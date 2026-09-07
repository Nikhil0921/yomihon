<div align="center">

<img src="./.github/assets/logo.png" alt="Yomitsu logo" width="128" height="128" />

# Yomitsu
### Independent manga & webtoon reader for Android — with Read-Aloud TTS and OCR
Discover and read manga, webtoons, comics, and more – easier than ever on your Android device.

[![CI](https://img.shields.io/github/actions/workflow/status/Nikhil0921/yomitsu/build.yml?label=CI&labelColor=27303D)](https://github.com/Nikhil0921/yomitsu/actions/workflows/build.yml)
[![GitHub release](https://img.shields.io/github/v/release/Nikhil0921/yomitsu?label=Stable&labelColor=06599d&color=043b69)](https://github.com/Nikhil0921/yomitsu/releases)
[![License: Apache-2.0](https://img.shields.io/github/license/mihonapp/mihon?labelColor=27303D&color=0877d2)](/LICENSE)

*Requires Android 8.0 or higher.*

---

</div>

## About

**Yomitsu** is a free, open-source (Apache-2.0) Android manga / webtoon reader
with integrated OCR, dictionary tooling, and Read-Aloud text-to-speech.

Yomitsu is an independent project built upon the
[Yomihon](https://github.com/yomihon/yomihon) ecosystem — Yomihon is itself a
community fork of [Mihon](https://github.com/mihonapp/mihon) (Tachiyomi
lineage). Full credit and thanks go to those projects; Yomitsu would not exist
without them. Yomitsu is not affiliated with Yomihon, Mihon, or Tachiyomi.

## Features

### Reading
* Manga, webtoons, manhwa/manhua: LTR/RTL/vertical pagers, continuous and
  paged webtoon mode, dual-page split, experimental panel-by-panel navigation.
* Local content: folders, CBZ/CBR archives, EPUB.
* Tracker support: [MyAnimeList](https://myanimelist.net/), [AniList](https://anilist.co/),
  [Kitsu](https://kitsu.app/), [MangaUpdates](https://mangaupdates.com),
  [Shikimori](https://shikimori.one), [Bangumi](https://bgm.tv/) and [Hikka](https://hikka.io/).
* Categories, scheduled library updates, backups, light/dark/AMOLED themes with
  13 color schemes + dynamic color.
* Feed tab: follow Popular/Latest listings across multiple sources in one screen.

### Language learning
* **Built-in Text Recognition (OCR)**: extract text from manga pages
  on-device; online models (GLENS) available for more languages.
* **Read Aloud (TTS)**: speaks a page's OCR text sentence-by-sentence using the
  system TTS engine — auto page turn, auto chapter advance, pause/resume,
  next/previous sentence, speech rate 50–300%, pitch, voice preview, and voice
  profiles.
* **Webtoon-aware speech**: long strips are tiled so OCR can actually read
  them, and the reader auto-scrolls to the region being spoken.
* **OCR exclusion rules**: suppress ads, watermarks, SFX, or scanlator credits
  from being spoken — by zone, word, phrase, manga, or source.
* **Speech cleanup**: skip punctuation-only regions and OCR garbage, normalize
  excessive punctuation, classify sound effects / expressions / foreign script.
* **Yomitan-style dictionary lookups** across multiple languages.
* **One-click Anki cards** while reading and looking up new words.

### Reliability
* Single-flight, cached OCR scans with speed-adaptive prefetch (up to 3 pages).
* Reader memory-leak hardening (LeakCanary-verified), debounced rapid swiping,
  best-effort background prefetch that never kills a playback session.

## Download

Releases are published on this repository's [Releases page](https://github.com/Nikhil0921/yomitsu/releases).

## 🤝 Contributing

Feature requests, bug reports, and pull requests are welcome. Before opening an
issue, please search [existing issues](https://github.com/Nikhil0921/yomitsu/issues)
and keep in mind which project your report belongs to (Yomitsu vs. upstream).

[Code of conduct](./CODE_OF_CONDUCT.md) · [Contributing guide](./CONTRIBUTING.md)

Model and dataset attribution for externally fetched ML assets is documented in [MODEL_ATTRIBUTION.md](./MODEL_ATTRIBUTION.md).

## Credits

Yomitsu is based on [Yomihon](https://github.com/yomihon/yomihon), a community-driven
fork of the [Mihon](https://github.com/mihonapp/mihon) project (Tachiyomi lineage).
Thank you to all the people who have contributed to those projects and to this one.

> Note: Yomitsu is unaffiliated with the official Yomihon and Mihon projects.

Community links (Discord, website) for Yomitsu do not exist yet; they will be
added here if and when they do.

---

### Disclaimer
The developer(s) of this application have no affiliation with the content providers available. This application hosts zero content. Users are responsible for the content they bring or access.

### License
Copyright © 2015 Javier Tomás  
Copyright © 2024 Mihon Open Source Project  
Copyright © 2025 Yomihon  
Copyright © 2026 Yomitsu contributors

Licensed under the Apache License, Version 2.0. See the [LICENSE](/LICENSE) file for more details.

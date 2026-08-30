<div align="center">

<img src="./.github/assets/logo.png" alt="Yomihon logo" width="128" height="128" />

# Yomihon (fork)
### Full-Featured Reader for Android — with Text-to-Speech and OCR enhancements
Discover and read manga, webtoons, comics, and more – easier than ever on your Android device.

[![CI](https://img.shields.io/github/actions/workflow/status/Nikhil0921/yomihon/build.yml?label=CI&labelColor=27303D)](https://github.com/Nikhil0921/yomihon/actions/workflows/build.yml)
[![GitHub release](https://img.shields.io/github/v/release/Nikhil0921/yomihon?label=Stable&labelColor=06599d&color=043b69)](https://github.com/Nikhil0921/yomihon/releases)
[![License: Apache-2.0](https://img.shields.io/github/license/mihonapp/mihon?labelColor=27303D&color=0877d2)](/LICENSE)

*Requires Android 8.0 or higher.*

---

</div>

## About this fork

This repository is a **fork of [Yomihon](https://github.com/yomihon/yomihon)**, an open-source
manga reader for Android that is itself a community fork of [Mihon](https://github.com/mihonapp/mihon)
(Tachiyomi lineage).

This fork is **not** the official Yomihon project, is not affiliated with it, and does not
replace it. It is an independent downstream project that builds on the excellent Yomihon
base and extends it with its own development — most notably a **Text-to-Speech (Read-Aloud)**
feature and **OCR pipeline improvements**. Upstream Yomihon continues its own development
separately; if you want the official project, use the links above.

For bugs and features specific to this fork (TTS, OCR changes), please report them here,
not to the upstream project.

## Features

Everything in the Yomihon base, plus this fork's additions:

### Added in this fork

| Feature | Description |
| :--- | :--- |
| 🔊 **Text-to-Speech (Read-Aloud)** | Reads manga aloud using the system TTS engine: sentence-by-sentence playback, auto page turn, auto chapter advance, pause/resume, next/previous sentence, and speech rate/pitch controls. No new permissions. |
| 📜 **Webtoon-aware speech** | On long-strip/webtoon pages the reader auto-scrolls to the text region being spoken, and tall strips are tiled so OCR can actually read them. |
| ⚡ **OCR pipeline improvements** | Tall webtoon strips are split into overlapping tiles (with duplicate-region dedup) before recognition — previously they were downscaled into unreadability. English reading order fixed for non-Japanese pages. Scan requests are deduplicated (single-flight) and cached, so swiping no longer triggers repeated OCR scans. |
| 🧠 **Reliability hardening** | Reader memory-leak fix (LeakCanary-verified), background prefetch can no longer kill a playback session, rapid page swipes are debounced. |

### Inherited from the Yomihon base

| Feature | Description |
| :--- | :--- |
| 🔍 **Built-in Text Recognition** | On-device OCR to extract text from images in real-time. Online models are also available for more languages. |
| 📖 **Yomitan Dictionary Support** | Seamless dictionary lookups for language learners across multiple languages. |
| **One-click Anki Cards** | Instantly create flashcards while reading and looking up new words. |

Core Reader Features:

* Experimental offline panel-by-panel reading.
* Local reading of content.
* A configurable reader with multiple viewers, reading directions and other settings.
* Tracker support: [MyAnimeList](https://myanimelist.net/), [AniList](https://anilist.co/), [Kitsu](https://kitsu.app/), [MangaUpdates](https://mangaupdates.com), [Shikimori](https://shikimori.one), [Bangumi](https://bgm.tv/) and [Hikka](https://hikka.io/) support.
* Categories to organize your library.
* Light and dark themes.
* Schedule updating your library for new chapters.
* Create backups locally to read offline or to your desired cloud service.
* Plus much more...

## Download

Releases are published on this repository's [Releases page](https://github.com/Nikhil0921/yomihon/releases).

## 🤝 Contributing

Feature requests, bug reports, and pull requests are welcome. Before opening an
issue, please search [existing issues](https://github.com/Nikhil0921/yomihon/issues)
and keep in mind which project your report belongs to (this fork vs. upstream).

[Code of conduct](./CODE_OF_CONDUCT.md) · [Contributing guide](./CONTRIBUTING.md)

Model and dataset attribution for externally fetched ML assets is documented in [MODEL_ATTRIBUTION.md](./MODEL_ATTRIBUTION.md).

## Credits

This fork is based on [Yomihon](https://github.com/yomihon/yomihon), which is a
community-driven fork of the [Mihon](https://github.com/mihonapp/mihon) project.
Thank you to all the people who have contributed to those projects and to this one.

> Note: this fork is unaffiliated with the official Yomihon and Mihon projects.

Community links (Discord, website) for this fork do not exist yet; they will be
added here if and when they do.

---

### Disclaimer
The developer(s) of this application have no affiliation with the content providers available. This application hosts zero content. Users are responsible for the content they bring or access.

### License
Copyright © 2015 Javier Tomás  
Copyright © 2024 Mihon Open Source Project  
Copyright © 2025 Yomihon  

Licensed under the Apache License, Version 2.0. See the [LICENSE](/LICENSE) file for more details.

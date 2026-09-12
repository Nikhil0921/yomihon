# Yomitsu — Phase 6 Master Plan (Post UI-Audit Batches 1–5)

> **STATUS: SUPERSEDED (2026-09-12).** For ALL next-task decisions, read
> **docs/implementation-roadmap.md** — the canonical execution register
> produced by the 2026-09-12 master audit. This file is retained as
> historical planning evidence (its Part A baseline and Part F decision
> closures are still valid history; Part G "Next = v0.5.3 release" is
> SHIPPED; Part B row 12 was disproved — ReaderActivity has ONE composition
> block in current code). Do NOT select work from this file.

> PLANNING DOCUMENT. Produced 2026-09-10 from the clean baseline
> (HEAD c02efca25, +docs 2a377ef12; Batches 1–5 committed 1b2c56b23).
> NO application source changes. NO design decisions silently resolved.
> Companion: docs/ui-implementation-map.md (register D-01..D-15),
> docs/phase.md, docs/memory.md.

---

## PART A — Verified current baseline

All items below are committed and device-verified unless marked otherwise.

| Area | State | Evidence |
|---|---|---|
| Yomitsu rebrand | COMPLETE (708a7182d) | docs/branding.md; aapt2 label verified |
| TTS v1 (Phases 1–9) | COMPLETE, shipped v0.5.x | phase.md; device scripts 1–15 + user-confirmed 2026-08-28 |
| Phase 9 perf/stability | COMPLETE | leakcanary 0 leaks, battery PASS, exit-idle <1s |
| Leak fixes (engine focus chain, WebtoonTransitionHolder) | COMPLETE, device-verified | 5c7d2cc2c; 2026-09-03 set |
| OCR exclusion system (ZONE/WORD/PHRASE/COMBINED, NFKC matching) | COMPLETE, shipped v0.5.2 | matcher 35 tests; device logs |
| Voice config Phase 10A | COMPLETE, device-verified 2026-08-31 | tts-10a-test.log |
| 3x rate + speed chip + adaptive prefetch | COMPLETE | v0.5.2 |
| Voice profiles | COMPLETE | v0.5.2 |
| Speech cleanup/classification pipeline | COMPLETE | v0.5.2 |
| Dictionary navigation cleanup (More-tab lookup screen) | COMPLETE | v0.5.2 |
| Feed tab v2 (paging, persistence, manage screen, customize) | COMPLETE + device-verified | 09-06/09-07 passes |
| Recent tab IA (Continue/History/Updates, 5-tab nav) | COMPLETE + device-verified | 09-07 pass |
| Settings/Search (grouped cards, search registration D-08) | COMPLETE | Batch 4 + Batch 5 matrix |
| MangaScreen error state (D-11) | COMPLETE (success path device; missing path unit-verified 4/4) | Batch 4/5 |
| Typography/spacing/surface modernization | COMPLETE | Batches 1–5 + 09-05..07 sets |
| Post-v0.5.2 stabilization (LEGACY removal −133MB, Main-thread I/O fix, GLENS retry, cache retention 5000, PermissionStep) | COMPLETE, device matrix A–J (F/J/onboarding opportunistic) | 09-08 pass |
| UI audit Batches 1–5 (D-01..D-13 dispositions) | COMPLETE, committed 1b2c56b23, post-commit baseline PASS | batch5-verify.log, 65.7MB, 0 crashes |
| Gates at committed HEAD | ALL GREEN (spotless 37s; tests+migration 2m59s; assembleDebug 2m38s) | memory.md 2026-09-10 |

**Freeze list (do not reopen without a regression report):** reader/TTS/OCR
pipeline + controller + engine; Feed listing/state model (Batches 1–2
device-verified); Recent structure; navigation + reselect semantics;
settings IA; design-system tokens/frost roles; grouped-card rhythm.

Baseline gaps (not regressions): 3 local commits unpushed; v0.5.2 tag
predates rebrand/stabilization/UI-audit work (10 commits unreleased);
device package was found disabled (pm enable run — flagged to user, cause
unknown).

---

## PART B — Remaining-work matrix

| Item | Source | Status | User impact | Risk | Priority | Decision |
|---|---|---|---|---|---|---|
| v0.5.3 release (10 commits unreleased incl. 133MB APK reduction, rebrand, UI audit fixes) | git log 0286d9081..HEAD | not cut | users get no updates via in-app updater | none (build+publish only) | **P1** | recommended NEXT batch |
| TtsPlaybackBar typography (code bodyMedium vs design.md §4 bodyLarge) | TtsPlaybackBar.kt:111,162,176,270 | RESOLVED 2026-09-11 micro-batch: code RATIFIED, design.md updated | none functional | LOW (visual or docs) | P2 | CLOSED — code ratified (bodyMedium) |
| TtsPlaybackBar padding (code 16/4 vs design.md §5 24/12) | TtsPlaybackBar.kt:90 | RESOLVED 2026-09-11 micro-batch: code RATIFIED, design.md §5 documents 16/4 pill-interior exception | none functional | LOW | P2 | CLOSED — code ratified (16/4) |
| FeedFilterBar arrow contentDescription = null | FeedScreen.kt:445 | RESOLVED 2026-09-11: decorative icon inside labeled FilterChip — correct a11y practice | none | none | INFO ONLY | CLOSED — NO ACTION (map §29 Q2 closed) |
| FeedHeader label vs combined one-line header | FeedScreen.kt:253-263 | RESOLVED 2026-09-11: KEEP two-line (ListGroupHeader + bodySmall listing label), device-verified rhythm | cosmetic density | none | P2 | CLOSED — keep two-line |
| "Create" tab referent | map §2: absent from source | user referenced; no such destination exists; referent never clarified | none until clarified | none (invention risk if guessed) | INFO ONLY | OPEN — DECISION REQUIRED (clarify, don't build) |
| D-09 global search 4 entry paths | map §21 | intentional post-revert | none | none | INFO ONLY | CLOSED — no action |
| D-14 Recent AppBar no actions | map §21 | by design (actions per-page) | none | none | INFO ONLY | CLOSED — no action |
| D-15 Library reselect affordance undiscoverable | map §21 | documented convention | minor discoverability | behavior change needs IA gate | FUTURE | FUTURE — NOT PART OF CURRENT WORK (only with IA approval) |
| D-04 negative path (source without supportsLatest) | Batch 1/5 | code-verified; not device-triggerable (all installed sources support Latest) | none | none | INFO ONLY | opportunistic verify if such a source appears |
| D-11 missing-path live trigger | Batch 4/5 | unit-verified 4/4; no safe live trigger | none | none | INFO ONLY | closed as untriggerable |
| GLENS transient retry live-verify (stabilization F) | memory 09-08 | code-verified; needs a natural 502/429 | none | none | INFO ONLY | opportunistic (log watch during any session) |
| OCR cache eviction boundary (stabilization J) | memory 09-08 | code-verified; needs 5000 cached pages | none | none | INFO ONLY | opportunistic |
| Onboarding PermissionStep device test (stabilization B7) | memory 09-08 | compile-verified; needs fresh install (forbidden to wipe user data) | onboarding correctness on fresh installs | none | P2 | run on next fresh-install/emulator window |
| Device DB stale COMBINED rules 34–46 (prefill-era zones) | memory 09-03 | user data; behave as COMBINED by design | user's own rules less effective as pure zones | none (user data, not code) | INFO ONLY | user deletes/re-creates blank if desired |
| Phase 10B backlog | prd §6.4, phase.md | NOT_STARTED, PRD-gated | future | per item | FUTURE | each needs PRD update + architecture review |
| Known issue #1 local Fast scan ordering | memory | mitigated (redirects → GLENS); port Glens ordering to local scan = 10B item | JP vertical text on local scans | medium (OCR engine) | FUTURE | 10B |
| Known issue #6 seam-bubble fragments (IoU 0.45 ceiling) | memory | accepted v1 ceiling | rare duplicate/fragment speech | low | FUTURE | cross-tile merge, 10B |
| Known issue #12 Glance widget LocalContext error | memory | non-fatal, pre-existing | minor | low | P2 | investigate only if widget complaints |
| Known issue #2 ReaderActivity dual setComposeContent | memory | RESOLVED 2026-09-12 audit (docs corrected in RM-01): ReaderActivity has ONE composition block (setComposeOverlay l.606); dual-block landmine no longer exists | none | none | RESOLVED | CLOSED — docs corrected, no code change needed |
| libLiteRtClGlAccelerator.so ~2.8MB/ABI unused GPU lib | memory 09-08 | still packaged; exclusion untested | APK size | lazy-dlopen risk if excluded | P2 | test exclusion in a release batch |
| ResizableSheet geometry unification (24dp variant) | map §21 LOW | deferred by 09-05 B-R ruling | cosmetic | low | FUTURE | LOW polish |

No P0 items exist. Nothing is broken on the baseline.

---

## PART C — Chimahon-inspired analysis

**Evidence constraint:** the repository contains NO Chimahon feature
inventory, screenshots, or spec — only the reference RULES (Prompt.md §16:
what may be borrowed) and one recorded "Tadami-inspired" idea chain
(phase.md deferred list, memory 2026-09-06). The matrix below classifies
every reference-inspired idea actually recorded in this project, per the
§16 categories. A richer matrix requires the user to supply Chimahon
material; nothing was invented to fill it.

| Idea (reference origin) | Verdict | Existing Yomitsu equivalent | Reference behavior | Benefit | Downside | Architectural impact | UI impact | Priority |
|---|---|---|---|---|---|---|---|---|
| Grouped settings cards (Chimahon/Tadami grouping language) | **ADOPT (already shipped)** | PreferenceGroupCard system | card-per-group settings | scannable settings | none observed | none — shipped | frozen | done |
| "Tadami-inspired" contextual reader tray (artwork-reactive chrome) | **ADAPT — IMPLEMENTED 2026-09-11** | frosted floating chrome (asFloatingChrome 0.85) now blends an artwork-derived tone (≤8%) pre-frost; debounced/IO/identity-fallback | subtle tray deriving from artwork behind | ambient immersion | bounded: tiny decode per settled page; neutral guards keep most pages unchanged | none — reused frost roles + stream + ImageDecoder; no viewer/arch change | reader chrome only (4 floating-chrome sites) | SHIPPED pending device verify (phase.md deferred #3 closed) |
| Reader toolbar reordering (drag/drop customization) | **DEFER** | visibility toggles only (reader settings) | full drag-drop ordering | power-user control | persistence + defaults + mandatory-action edge cases; touches reader settings structure only (NOT ReaderBottomBar behavior) | pref schema addition | settings + bottom bar config | FUTURE (phase.md deferred #1) |
| True backdrop blur | **REJECT (for now)** | real-alpha frost roles | glass blur everywhere | visual depth | Compose can't sample sibling artwork View; fullscreen RenderEffect rejected on perf; battery/AMOLED cost | blocked by rendering arch | none | stays rejected until feasibility proven |
| Neon/cyberpunk/glass-everywhere aesthetic | **REJECT** | M3 structured minimal | flashy skins | none for Yomitsu | contradicts design.md §1 identity | none | none | rejected |
| Anime-ecosystem integrations / trackers beyond existing | **REJECT** | 11 trackers + Komga/Kavita/Suwayomi | ecosystem hubs | out of product scope | scope + maintenance | large | large | rejected |
| Navigation customization (custom tabs) | **REJECT** | fixed intentional 5-tab IA | configurable nav | power users | breaks reselect semantics + frozen IA decisions; map §27 gate 7 | navigation layer | app shell | rejected (map already ruled NOT PROPOSED) |
| Standardized reselect (scroll-to-top everywhere) | **REJECT** | per-tab intentional semantics | uniform convention | predictability | destroys documented per-tab behavior (Library=settings sheet, Recent=resume, Feed=Manage, Browse=search, More=Settings) | HomeScreen + all tabs | all tabs | rejected (map §3 frozen) |
| Feed auto infinite scroll | **DEFER** | explicit Load-more (device-tested stable 09-06..09-10) | continuous scroll | frictionless | paging state complexity; premature before stability — stability NOW proven, so eligible | FeedScreenModel only | Feed footer | FUTURE (phase.md deferred #4, now eligible) |
| Search tab in Browse (Chimahon-style unified search surface) | **REJECT (already tried+reverted 2026-09-04)** | Browse reselect → GlobalSearch; Sources TravelExplore action | dedicated search tab | discoverability | broke Extensions search routing when shipped here; set-2 revert stands | BrowseTab routing | Browse | rejected |

Anti-clone rule enforced: nothing above copies Chimahon branding, visual
identity, or product direction; Yomitsu's M3/structured/editorial language
stays authoritative (design.md §1).

---

## PART D — Design-system stability check

| Layer | Verdict |
|---|---|
| Typography | **stable** — tokens + itemTitle/header roles; all exceptions documented (map §6) |
| Spacing | **stable** — 16dp inset, 4/8 gutters, frozen 12dp grouped-card rhythm, pill constants |
| Colors | **stable** — colorScheme-only, 15 schemes + Monet + AMOLED verified |
| Surfaces | **stable** — frost roles frozen (floating chrome / frosted modal / solid); no frost-on-frost |
| Grouped cards | **stable** — PreferenceGroupCard everywhere settings live; transparent-ListItem rule codified |
| Navigation | **frozen** — 5 tabs, per-tab reselect semantics intentional (map §3) |
| Headers | **stable** — ListGroupHeader one rank below screen titles; nested headers prohibited |
| Grids | **stable** — Library/Feed geometry verified identical (Batch 3 pixel-band) |
| Pills | **stable** — floating pill language; ONE open token question (TtsPlaybackBar 16/4 vs 24/12, Part F) |
| Accessibility | **stable, one gap** — 48dp + contentDescriptions verified; full large-font-scale + screen-reader ordering pass not re-run since Batch 5 (time-limited); schedule in next device session |
| Motion | **stable** — reuse-only patterns, no new animation systems |
| AMOLED | **stable** — device runs AMOLED; black-vs-hierarchy rule enforced |
| Reader chrome boundaries | **frozen/protected** — no new chrome, z-order contract comment in place |

**Verdict: design system is STABLE for new feature work.** Nothing needs
redesign. Only cleanup: design.md §4/§5/§8 TtsPlaybackBar rows conflict
with code (Part F decision), and map §29 open questions 2–3 need
dispositions.

---

## PART E — Architectural safety (per proposed future item)

| Item | Module | Screens/components | Domain/data | Migration risk | Prefs | Nav | Testing | Device verification |
|---|---|---|---|---|---|---|---|---|
| v0.5.3 release | build files, CHANGELOG, version | none | none | none | none | none | full gates + assembleRelease (both flags) | release-APK smoke install + launch |
| TtsPlaybackBar decision ratification (docs-only) | none (docs) | none | none | none | none | none | none | none |
| TtsPlaybackBar code alignment (if chosen) | :app presentation | **TtsPlaybackBar — PROTECTED** | none | none | none | none | spotless + compile | reader TTS session visual |
| Feed auto pagination | :app (FeedScreenModel/FeedScreen) | Feed footer | none (state only) | none | none | none | FeedScreenModelStateTest extension | Feed long-scroll session |
| Reader toolbar reordering | :app reader settings + prefs | reader settings dialog; ReaderBottomBar config | none | none | NEW pref keys (approval gate) | none | unit test on ordering model | reader toolbar session |
| Artwork-reactive tray (shipped 2026-09-11) | :app presentation + ReaderActivity (param-threaded only) | reader floating chrome (frost roles unchanged) | none | none | none | none | ReaderArtworkToneTest 10 cases | reader session: tone pages + neutral pages + Monochrome |
| 10B per-voice rate/pitch tuning | :domain prefs + :app ReadAloud SM/screen | SettingsReadAloudScreen | pref model extension | none | NEW keys | none | domain unit tests | voice-switch session |
| 10B JP/multilingual opt-in preflight | :domain + AndroidTtsEngine (**PROTECTED**) | ReadAloud pickers | none | none | new pref | none | domain tests | JP-content session |
| 10B Glens-ordering port to local Fast scan | :data OCR engine (**PROTECTED family**) | none | engine internals | OCR cache version bump consideration | none | none | engine ordering tests | local-source JP vertical page |
| 10B cloud/neural engines | :domain TtsEngine impl + Injekt swap | SettingsReadAloud pickers | new impl | none to reader | new prefs | none | engine contract tests | per-engine session |
| 10B FGS background playback | :app service + manifest (**needs permission + PRD**) | notification, MediaSession | none | high (lifecycle) | new | none | service lifecycle tests | background/lock-screen matrix |

Protected systems (no touch without explicit authorization): ReaderActivity,
TtsPlaybackBar, TtsPlaybackController, AndroidTtsEngine, OCR engines + pipeline,
reader navigation, existing TTS/OCR architecture. No proposed cleanup
refactors target them. (Known issue #2, the dual compose blocks once listed
here, was disproved by the 2026-09-12 audit — one composition block exists;
the "issue" was stale documentation, not code.)

---

## PART F — Open decision register (RESOLVED 2026-09-11 decision micro-batch)

1. **TtsPlaybackBar typography** — RESOLVED: ratify code. bodyMedium stands
   (pill is single-line ellipsized compact chrome; bodyLarge inflates height
   over artwork at large font scales; code is the device-verified state).
   design.md §4/§8 updated to bodyMedium. No source change.
2. **TtsPlaybackBar padding** — RESOLVED: ratify code. 16/4 stands as the
   playback-pill interior metric; design.md §5 now documents it as the
   documented exception to the 24/12 pill precedent. No source change.
3. **"Create" tab referent** — REMAINS OPEN. Absent from source (map §2);
   plausible referents (Recent "Continue" tab; Feed add-feed dialog) noted,
   never confirmed. User clarification required; build nothing until then.
4. **FeedFilterBar arrow contentDescription** — RESOLVED: ACCEPT, NO ACTION.
   Decorative glyph inside a labeled FilterChip; chip label is the accessible
   name; a separate description would be redundant. map §29 Q2 closed.
5. **FeedHeader label vs combined header** — RESOLVED: KEEP two-line
   (ListGroupHeader source name + bodySmall listing label). Listing is
   secondary metadata; combining overloads the section-header role; rhythm
   device-verified incl. D-05 divider removal.
6. **D-09 (global search 4 paths)** — RESOLVED: no action (intentional
   post-revert).
7. **D-14 (Recent AppBar no actions)** — RESOLVED: no action (by design;
   actions live per-page).
8. **D-15 (Library reselect affordance)** — FUTURE, NOT PART OF CURRENT
   WORK. Standardizing or signposting = IA behavior change; only revisit with
   IA approval if discoverability complaints arrive.

Seven of eight closed by the 2026-09-11 micro-batch; Create-tab remains
OPEN pending user clarification.

---

## PART G — Prioritized roadmap

### Next
**v0.5.3 release batch** (Part H). Everything is committed, green, and
device-verified; 10 commits of user-visible value (including a 133MB APK
reduction and the rebrand) sit unreleased while the in-app updater waits.

### After Next
1. **Decision-driven micro-batch** — execute Part F rulings (docs edit
   ratification path is ~zero-risk; code path only if user overrules).
   Includes closing map §29 Q2/Q3 dispositions.
2. **A11y completion pass** — large-font-scale + screen-reader ordering
   sweep on one device session (only gap Batch 5 left open); fold in
   opportunistic F/J/onboarding verifications if triggers appear.
3. **Phase 10B track** (if user wants a feature track) — smallest first:
   per-voice rate/pitch tuning or JP opt-in preflight; each gated by PRD
   update + architecture review per rules.md §10.
4. **libLiteRt GPU-lib packaging exclusion** — test + strip ~2.8MB/ABI in
   the next release batch window.

### Later
- Feed automatic near-end pagination (now eligible — Load-more stable).
- Reader toolbar reordering — SHIPPED 2026-09-11 (Batch 7, user-verified).
- Artwork-reactive tray — SHIPPED 2026-09-11 (blend-based; device verify pending).
- ResizableSheet geometry unification (LOW).
- Glance widget issue #12 (complaint-driven).
- OCR cache eviction boundary + GLENS retry live-verify (opportunistic).

### Deferred
- Phase 10B heavy items: cloud/neural TTS, downloadable AI voices,
  expressive speech, FGS+MediaSession background playback, on-image bbox
  highlight, audio caching, cross-tile seam merge, Glens-ordering port.

### Rejected
- Chimahon-clone aesthetics (neon/cyberpunk/glass-everywhere/wholesale identity).
- Navigation/tab customization; 6th tab or Browse search tab (reverted once).
- Standardized reselect semantics.
- Library page-level Continue section (reverted twice — per-item button is the approved form).
- True backdrop blur (until feasibility proven).
- Any refactor of protected systems for cleanliness. (Former entry
  "incl. Known issue #2" is moot: the 2026-09-12 audit proved there is only
  one composition block — there is nothing to refactor.)

---

## PART H — Recommended next implementation batch

**Batch name:** v0.5.3 release consolidation.

**Objective:** ship the verified baseline to users via the existing local
release process (CI releases are fork-gated and skip — same as v0.5.0..2).

**Exact problem solved:** 10 commits of shipped-quality work (rebrand,
−133MB APK, NetworkOnMainThread fix, GLENS retry, cache retention, UI audit
Batches 1–5, MangaScreen error state, settings-search registration) are
invisible to updater users; local branch is 3 commits ahead of origin.

**User-facing benefit:** update prompt to existing v0.5.2 installs; 58%
smaller APKs; brand-correct app name/icons; Feed listing selector that
actually filters; MangaScreen honest failure; settings search coverage.

**Files likely affected:** `app/build.gradle.kts` (versionCode 29,
versionName 0.5.3), `CHANGELOG.md`, release tag + GitHub release (gh),
optional `docs/memory.md`/`phase.md` status rows. Nothing else.

**Dependencies:** none. Uses existing docker release recipe
(-u vscode, both volumes, -Xmx4g, `-Pinclude-telemetry -Penable-updater`).

**Tests:** full CI-order gates on the release commit
(spotlessCheck → testDebugUnitTest → verifySqlDelightMigration →
assembleRelease with both flags), aapt2 badging check (versionCode/Name,
label, 6→3 model assets present: ocr_fast ×2 + panel_detector).

**Device verification:** install release arm64 APK on SM_M066B (wired USB),
smoke: launch → 5 tabs → reader → TTS play/pause → settings search →
0 FATAL. In-place `adb install -r` acceptable (vc bump, data preserved).

**Acceptance criteria:** gates green on tagged commit; APK contents
verified (no assets/ocr/, ocr_fast present); device smoke PASS; tag pushed;
release published with notes covering the section above; origin/main pushed.

**Rollback risk:** LOW — release-only; a bad release is recoverable by
deleting the tag/release and re-cutting. No code paths change.

**OUT OF SCOPE:** any Part F decision implementation, TtsPlaybackBar
changes, 10B work, a11y pass, navigation changes, unrelated cleanup,
v0.6 planning.

---

## PART I — Implementation gate

### READY FOR USER APPROVAL — for the v0.5.3 release batch only.

Exact decisions required from the user before ANY implementation:

1. Approve/decline v0.5.3 release batch as Next (Part H).
2. TtsPlaybackBar ruling — RESOLVED 2026-09-11: ratified code
   (bodyMedium + 16/4); design.md updated; no source change.
3. "Create" tab referent — clarify meaning; no build without a spec.
   STILL OPEN.
4. FeedHeader two-line vs combined — RESOLVED 2026-09-11: keep two-line.
5. Optional early calls (not blocking the release): choose the first
   Phase 10B item if a feature track is wanted now; approve/dismiss the
   a11y completion pass scheduling.

v0.5.3 released 2026-09-11; the 2026-09-11 decision micro-batch closed
Part F items 1, 2, 4, 5, 6, 7, 8 (Create-tab, item 3, remains open).
Batch 6 does not exist until the user defines it from the roadmap above.

# Yomihon UI Modernization — Design Audit Report (Phase 1)

> Read-only audit per docs/Prompt.md. NO code modified. Produced 2026-09-04
> from 4 parallel audits (theme/components, navigation/screens, overlays/
> layering, settings/OCR/TTS) + docs/design.md + docs/architecture.md.
> This document is the proposal for the REQUIRED USER CONFIRMATION GATE.

---

## 1. Current UI Analysis

### Strengths (keep)
- **Token-driven theme system**: `MaterialExpressiveTheme`, `colorScheme`
  tokens only; 13 selectable schemes + Monet dynamic color + AMOLED; every
  feature composable already routes through `TachiyomiTheme`.
- **Shared component core**: presentation-core `Pill`, `AdaptiveSheet`,
  `EmptyScreen` (kaomoji + actions), `LoadingScreen`, `SettingsItems`,
  `NavigationBar`/`NavigationRail`, `Scaffold` wrappers — high reuse.
- **Floating pill language established** (2026-09 sets): bottom nav pill
  (`RoundedCornerShape(28.dp)`, `surfaceContainer`), TTS playback pill —
  two chrome + reader overlay families already unified.
- **Content-first reader**: overlay-based, auto-hiding, page is the hero.
- **Searchable settings** with breadcrumb + highlight scroll.
- **Consistent outlined icon set** (material-icons extended).

### Weaknesses (the "flatness" — real causes)

**Hierarchy**
- **Four competing section-header styles**: `ListGroupHeader`
  (bodyMedium/onSurfaceVariant/semibold, 16/8), `PreferenceGroupHeader`
  (bodyMedium/**secondary**, no weight), `MoreScreen.GroupHeader`
  (private, titleSmall/**primary**), `HeadingItem` (bodyMedium/variant/
  semibold, 24/10). No rank system — sections don't read as hierarchy.
  Evidence: ListGroupHeader.kt, PreferenceGroupHeader.kt, MoreScreen.kt:216-224,
  SettingsItems.kt.
- **Two settings-widget dialects**: main settings `BasePreferenceWidget`
  (16dp pad, 56dp rows, titleLarge@16sp) vs reader dialog
  `presentation-core SettingsItems` (24dp/10dp, `header` style). Same app,
  two visual systems for the same concept.
- Manga detail info/chapters/actions use inline LazyColumn without
  surface separation (fine) but action row + description + chapter header
  all compete at same visual weight.

**Alignment/spacing**
- `MaterialTheme.padding` tokens exist (4/8/16/24/32) but hardcoded dp
  dominates; **12dp is an off-token value used ~10×** (spacedBy(12.dp),
  PreferenceScreen group spacer, reader pill inset).
- **Grid gutters differ**: Library/browse grids = 4dp spacers + 8dp content
  padding (`CommonMangaItemDefaults`); Feed grid = `spacedBy(8.dp)` + 16dp
  edges (FeedScreen.kt:105-107) — same grid items, 2× gutter.
- Screen horizontal padding mixes `padding.medium` and literal `16.dp`
  (~33 literal sites).
- Sheet dialog paddings are 24dp islands (`SettingsItemsPaddings`,
  `TabbedDialogPaddings`, `EmptyScreen`) inside a 16dp app.

**Component inconsistencies**
- **Two bottom-sheet dialects**: `AdaptiveSheet` (shapes.extraLarge 28,
  `surfaceContainerHigh`, no handle) vs `ResizableSheet` (24dp top radius,
  `surface`, tonalElevation 6, handle) — reader shows both back-to-back.
- **Card radii unregulated**: 8/12/16/28dp across Dictionary, Migration,
  TrackerSearch, SectionCard, CategoryListItem — no rule.
- **Font-size hacks bypass typography tokens**: `GridItemTitle`
  titleSmall+12sp/18sp override (CommonMangaItem.kt:276-286, duplicated
  MangaInfoHeader.kt:743), AppBar search 18sp (AppBar.kt:308,331),
  LibraryToolbar Pill 14sp, TabText 10sp, prefs `TitleFontSize=16.sp`
  constant, Dictionary 15sp/10sp.
- **Color literals outside schemes**: `ColorScheme.active` amber
  (Color.kt:8-12), tracking success `0xFF4CAF50`
  (TrackingPreferenceWidget.kt:73), cover-placeholder gray duplicated
  (MangaCover.kt:54 + BrowseIcons.kt:93), pillAlpha 0.12/0.08 triplicated
  (Tabs.kt:15, LibraryToolbar.kt:107, SourcesScreen.kt:150), reader scrims
  (intentional), page-indicator literals (dead code).
- **Feed screen = biggest inconsistency cluster** (only main tab on raw M3
  `TopAppBar` instead of presentation `AppBar`, custom text-only empty
  state, plain-`Text` loading/error rows, `" ✓"` string-concat selection
  markers in AddFeedDialog — FeedScreen.kt:66-84, 122-133, 236-248, 288-300).

**Navigation problems**
- **Reselect semantics inconsistent + undiscoverable**: Updates reselect →
  DownloadQueueScreen, Browse reselect → GlobalSearchScreen, More reselect →
  Settings, Library reselect → settings sheet, Feed reselect → nothing.
  No scroll-to-top anywhere. Zero affordance.
- **Global search reachable via 4 scattered paths**, none primary
  (Browse reselect, Sources TravelExplore action, Library empty-state row,
  SEARCH intent). Semi-hidden after set-2 revert.
- **Tab metadata bug**: `FeedTab.options.index=5`, `MoreTab.options.index=4`
  — swapped vs TABS list order (FeedTab.kt:27, MoreTab.kt:52,
  HomeScreen.kt:75-82). Latent Voyager ordering hazard.
- **Feed = only tab without animated icon** (static `Icons.Outlined.Feed`).
- **Hidden long-press actions** with no hint: add-to-library long-press =
  edit categories; source long-press = options; extension long-press =
  install; migrate long-press = copy ID.
- DownloadQueue dual entry (Updates reselect + More) vs OCR queue only in
  More — asymmetric.
- **MangaScreen has no error state** — deleted/bad manga = stuck
  LoadingScreen (MangaScreenModel: Loading|Success only).

**Settings/organization problems**
- Reader/Download/Advanced screens start with loose ungrouped rows (only
  screens with pre-group drift); inconsistent scanning rhythm.
- **Search coverage gaps**: AnkiDroid screen implements SearchableSettings
  but missing from `settingScreens` list (one-liner, likely bug);
  Dictionary, OCR exclusions, OCR queue screens not searchable at all.
- **OCR/TTS settings split across 3 surfaces** (root Settings, reader
  quick-settings dialog, More→Text Recognition) with no cross-links from
  two of them; OCR *engine* config lives in the queue screen under More,
  outside the Settings tree.
- **Two "Keep screen on" switches** (reader pref vs TTS pref) in different
  screens, similar labels.
- `ocrTextSelectionEnabled` pref name says text-selection but gates the
  OCR *button* visibility; `ttsSpeechScript` pref + 3 strings dead in UI.
- OCR exclusion match types (ZONE/WORD/PHRASE/COMBINED) poorly explained;
  reader manage sheet is a weaker sibling of the settings screen (no
  identity labels, no edit, no collapse) — two UX for same data.

**Overlay problems (safety)**
- **ReaderPageIndicator is dead code** — lives in outer composition tree
  that the inner `setComposeContent` call replaces (dual composition
  blocks in ReaderActivity, merge 0eab09ce4). Pref-gated feature silently
  lost.
- **OcrLoadingIndicator has zero inset handling** — flush to bottom edge,
  overlaps system nav-bar zone and draws ON TOP of the TTS pill (last
  inline child, ReaderActivity.kt:1043 vs pill :883).
- TTS pill floats above OCR drag-select scrim (undimmed, still
  interactive) — mixed modality signal.
- OcrResultPopup clamps to raw viewport, no status/cutout/nav inset
  awareness.
- Dead outer composition block = "dialog added there never shows" trap
  (same class as fixed 2026-08-28 z-order regression).

---

## 2. Existing Design System

- **Theme**: `TachiyomiTheme` → `BaseTachiyomiTheme` →
  `MaterialExpressiveTheme(colorScheme)` — **no typography, no Shapes
  override** → M3 defaults everywhere (shapes: 4/8/12/16/28).
  Root default text `bodySmall` (ViewExtensions.kt:31-34).
- **Color**: 15 `BaseColorScheme` impls; AMOLED flips background/surface
  black + forces surfaceContainer literals; Monet = dynamic /
  materialkolor Expressive. Semantic mapping per design.md §2 (no new
  colors rule).
- **Typography**: M3 defaults; one custom `Typography.header` extension
  (bodyMedium/onSurfaceVariant/SemiBold); roles per design.md §4.
- **Spacing tokens**: `MaterialTheme.padding` — extraSmall 4 / small 8 /
  medium 16 / large 24 / extraLarge 32 (Constants.kt:12-26) + alpha
  constants; underused in app code.
- **Shared components** (all presentation-core): NavigationBar (floating
  pill), NavigationRail, Scaffold (startBar slot, FAB inset math), Surface,
  AdaptiveSheet, ResizableSheet, Pill, Badge/BadgeGroup, ActionButton,
  EmptyScreen, LoadingScreen, InfoScreen, SectionCard, ListGroupHeader,
  SettingsItems (HeadingItem/TextItem/CheckboxItem/SliderItem/ChipRow/
  IconGrid), Scrollbar/FastScroll LazyColumn+Grid, CollapsibleBox,
  LabeledCheckbox, WheelPicker, LazyColumnWithAction, Tabs/TabText wrappers,
  PullRefresh, DropdownMenu wrapper. App-side second system:
  `more/settings/widget/*` (BasePreferenceWidget family) for settings
  screens.
- **Pill language** (current): nav pill (28dp, surfaceContainer, tonal 3,
  nav-bars inset INSIDE pill, 12/8 outer margins), TTS pill (28dp,
  surfaceColorAtElevation(3)+alpha .9/.95 + 6dp shadow, fillMaxWidth(0.92f),
  max 560dp, clearance = max(tray, navBars, cutout)+12dp), MangaCoverDialog
  ActionsPill. Full-width strip family: bottom action menus
  (shapes.large zero-bottom, surfaceContainerHigh), OcrLoadingIndicator,
  bottom tray.
- **Navigation model**: single Voyager Navigator → HomeScreen (phone pill
  NavigationBar / tablet NavigationRail) → TabNavigator with saveable per-tab
  state; AnimatedContent fade-through 200ms; DefaultNavigatorScreenTransition
  = materialSharedAxisX. Reader = explicit ReaderActivity.
- **Elevation convention**: tonal 3dp for bars/pills (via
  surfaceColorAtElevation), tonal 6dp sheets (ResizableSheet), shadow only
  on TTS pill (6dp) — mostly tonal, one shadow outlier.

---

## 3. Modernization Strategy

**"Structured minimalism" on existing tokens — no new design system.**

### Remains unchanged
- Theme architecture, all 13 schemes + Monet + AMOLED, dark/light
- 6-tab structure (Library/Updates/History/Browse/Feed/More) + floating
  pill nav + tablet rail; Voyager navigation
- Reader architecture, overlay-based nature, all reading modes
- ALL functionality: OCR, TTS, dictionary, tracking, downloads, extensions
- Business logic, data layer, APIs, prefs keys, DB

### Visually improved (presentation-only)
1. **Token discipline pass**: literal dp/shape/radius values that already
   equal tokens → tokens; decide 12dp fate (promote to token or eliminate);
   grid gutters unified on `CommonMangaItemDefaults` pattern.
2. **Header hierarchy**: one shared section-header system — `ListGroupHeader`
   for in-list sections, `PreferenceGroupHeader` re-pointed to
   `Typography.header` (color converges on onSurfaceVariant); delete
   MoreScreen's private duplicate. Screen titles stay titleLarge/
   titleMedium; sections one rank below.
3. **Typography honesty**: replace fontSize hacks with one `itemTitle`
   style in presentation-core Typography (12sp/18sp grid title, used by
   grid items + MangaInfoHeader); kill ad-hoc 18sp/14sp/10sp overrides
   where a token fits.
4. **Sheet unification**: ResizableSheet → shapes.extraLarge +
   surfaceContainerHigh (+ keep its handle; optionally shared handle).
   Fix TrackInfoDialogHome double-clip. Card radius rule: list-level
   content = flat rows (no cards); grouping cards = shapes.large (16);
   pills/sheets = extraLarge (28).
5. **Semantic color cleanup**: amber `active` → scheme-driven (tertiary or
   per-scheme accent); tracking green → `tertiary`; single
   CoverPlaceholderColor val; single PillAlpha constant in Constants.kt
   beside SECONDARY_ALPHA.
6. **State completeness**: shared EmptyScreen everywhere (Feed included);
   loading/error rows as small components not plain Text.
7. **Settings rhythm**: group loose rows in Reader/Download/Advanced;
   AnkiDroid root row subtitle; unify delete-icon style/tint across
   Dictionary/Exclusions/Sheet.

### Reorganized (visual structure only)
- FeedScreen header/empty/chips/add-dialog normalized to app patterns.
- OCR exclusion manage surfaces unified (one presentation pattern).
- More-tab groups keep names; headers use shared component.
- TTS/OCR cross-link rows so each of the 3 surfaces can reach the others
  (rows that push existing screens — no new logic).

### NOT changed
- Tab set/order semantics, screen count, reader behavior, OCR/TTS behavior,
  matcher, DB, domain/data code, build config, dependencies.
- Items flagged "needs approval" in §9 are excluded until approved.

---

## 4. Screen-by-Screen Plan

Format: CURRENT → PROPOSED → VISUAL → NAV.

### Bottom navigation (HomeScreen)
- C: 6 tabs, floating pill, M3 indicator, animated icons except Feed;
  index metadata swapped (Feed=5/More=4). → P: same pill, same tabs;
  fix TabOptions.index; add `anim_feed_enter` icon; verify active-indicator
  contrast across AMOLED/Monochrome. → V: token pass on pill constants. →
  N: reselect convention decision (see §5, flagged).

### Library
- C: SearchToolbar + count Pill; category PrimaryScrollableTabRow;
  PullRefresh pager; 4 grid modes; badges start/end; per-item resume button;
  selection action bar; TabbedDialog settings sheet; per-category empty
  variants with GlobalSearchItem. → P: same structure; grid gutter =
  CommonMangaItemDefaults everywhere (Feed aligned TO this, not reverse);
  itemTitle style token; badge/pill typography via tokens. → V: header
  hierarchy (toolbar title → category tabs → content); count Pill token
  color. → N: unchanged; reselect keeps settings sheet (documented).

### Updates
- C: AppBar + PullRefresh + FastScrollLazyColumn; ListGroupHeader dates;
  UpdatesUiItem rows; MangaBottomActionMenu on selection; EmptyScreen. →
  P: unchanged structure; shared headers stay; state components already
  shared. → V: row typography token pass. → N: reselect currently pushes
  DownloadQueue — flag decision (keep vs scroll-to-top).

### History
- C: SearchToolbar; date ListGroupHeaders; HistoryItem rows (cover, title,
  chapter+time, favorite, delete). → P: unchanged; HistoryItem 96dp fixed
  height → wraps content naturally (verify font-scale). → V: token pass. →
  N: reselect = scroll-to-top candidate.

### Feed
- C: raw M3 TopAppBar (only deviant); FeedFilterBar chips (no icons);
  LazyVerticalGrid 96dp adaptive with FeedHeader full-span (titleMedium +
  divider); custom text empty state; inline Text loading/error; AddFeedDialog
  with "✓" concat markers; ManageFeedsScreen (AppBar + rows + up/down/
  switch/delete + EmptyScreen). → P: presentation `AppBar` + scrollBehavior;
  shared EmptyScreen + Add action; loading/error = spinner/error items;
  AddFeedDialog selection = FilterChips/RadioButtons; chips get leading
  icons like BrowseSource; grid = library gutter pattern + pref-driven
  columns alignment. → V: biggest single-screen win; FeedHeader →
  ListGroupHeader. → N: Tune → ManageFeeds stays; AddFeedDialog opens from
  empty state too.

### Browse (Sources/Extensions/Migrate)
- C: TabbedScreen SearchToolbar + PrimaryTabRow + pager; search routed to
  Extensions only; Sources language headers (theme.header) + rows;
  Extensions grouped lists + stores; Migrate sticky sort bar. → P: same
  tabs; SourceHeader → Typography.header-based shared header; SourceOptions
  dialog rows stay. → V: chip/icon consistency with BrowseSource screen. →
  N: search-in-sources discoverability unchanged (reverted set-2 experiment
  stands).

### Source browse (BrowseSourceScreen)
- C: BrowseSourceToolbar + iconed filter chips; Paging grid; EmptyScreen
  with action sets; MissingSourceScreen for stubs. → P: reference screen —
  others align to it. → V: none beyond token pass. → N: none.

### Manga detail (MangaScreen)
- C: alpha-animated MangaToolbar; info box → action row → expandable
  description → chapter header → chapter rows; FAB; tablet TwoPanelBox;
  dialogs/sheets; **no error state**. → P: same layout; typography/spacing
  token pass; CHAPTER FLAG: add missing-manga state (screen-model change →
  needs explicit approval; presentation alternative = timeout fallback,
  also approval). → V: action row + description get clearer separation
  via spacing, not cards. → N: unchanged.

### Global search
- C: GlobalSearchToolbar + progress + chip filters; per-source result rows. →
  P: unchanged; chip styling aligned to BrowseSource. → N: entry points
  stay as-is (4 paths); optionally add visible search action on Browse
  toolbar if approved.

### Dictionary
- C: DictionaryLookupScreen (More) → AppBar + in-body SearchBar + results;
  SettingsDictionaryScreen (import cards + installed list). → P: SearchBar
  placement reviewed for consistency (keep distinct if intentional);
  delete-icon style/tint unified with exclusions. → V: token pass. → N:
  unchanged.

### More tab
- C: LogoHeader + groups General/Library/Settings with private GroupHeader. →
  P: same groups; private GroupHeader → shared ListGroupHeader (visual
  only); queue rows keep live subtitles. → V: one header style app-wide. →
  N: unchanged rows.

### Settings tree
- C: 13 root rows w/ icon+subtitle; sub-screens SearchableSettings; Reader/
  Download/Advanced have loose pre-group rows; search index misses Anki/
  Dictionary/OCR screens. → P: loose rows grouped (pure list reshuffle via
  PreferenceGroup); AnkiDroid row gets subtitle; Anki added to
  `settingScreens` (one line, flagged as bug-fix); Dictionary/OCR-
  exclusions/OCR-queue = documented unindexed OR synthetic entries
  (decision needed); destination-id order fixed. → V: settings screens keep
  their own widget family (16dp) — reader-dialog SettingsItems (24dp)
  stays the in-reader dialect, but heading COLORS converge. → N: unchanged
  screens; new cross-link rows only.

### Reader
- C: dual composition blocks (outer dead); ContentOverlay pattern; tray +
  top bar auto-hide; TTS pill with clearance; OcrLoadingIndicator no
  insets; dead page indicator; OCR selection scrim + popups/sheets. →
  P: see §6 overlay table — insets + z-order + dead-code fixes. → V: no new
  chrome; pill families stay. → N: none.

### Reader quick settings (ReaderSettingsDialog)
- C: 4 tabs [Reading mode, General, Custom filter, Read aloud]; dim-hack
  `currentPage == 2`. → P: same tabs; hack → tab-identity check (compare
  page title/content identity, not index) — small, behavior-preserving. →
  V: SettingsItems dialect stays in-dialog. → N: ReadAloudPage gains
  "Manage exclusion rules" row (pushes SettingsOcrExclusionsScreen) beside
  existing "Advanced voice settings" row.

### OCR exclusions (SettingsOcrExclusionsScreen + reader sheet)
- C: settings screen (hint + words/phrases/zones sections; RuleRow with
  identity label, collapse, edit, switch, delete) vs reader
  OcrExclusionZonesSheet (toggle/delete only). → P: reader sheet gets
  RuleRow pattern (identity, collapse) or is replaced by navigation to
  the screen; small type legend (one-liner per ZONE/WORD/PHRASE/COMBINED
  — 4 short strings); zones/combined sections visually separated. →
  V: type + scope as small Badge chips instead of concatenated text. →
  N: no CRUD logic changes.

### Read aloud settings
- C: SettingsReadAloudScreen (Text to speech / Voice calibration / Voice
  profiles / Advanced); searchable voice picker; preview; TtsPlaybackBar
  speed chip. → P: already the modern reference; only token pass +
  cross-link row to OCR exclusions ("related controls"). → N: unchanged.

---

## 5. Navigation Plan

**Current**: 6-tab pill nav + Voyager stack; per-screen search bars;
reselect = 4 different hidden behaviors; global search via 4 scattered
paths; back → Library tab.

**Problems**: inconsistent/hidden reselect semantics; no scroll-to-top;
search discoverability; metadata index swap; static Feed icon; hidden
long-press actions; asymmetric queue entries.

**Proposed (structure-preserving)**:
1. Keep 6 tabs, keep order, keep pill. Fix TabOptions.index (Feed=4,
   More=5 in Voyager metadata matching list order).
2. Feed gets animated enter icon (matches siblings).
3. Reselect convention (NEEDS APPROVAL — behavior change): standardize on
   **scroll-to-top** for list tabs (Library/Updates/History/Feed);
   Updates' DownloadQueue shortcut moves to overflow (still reachable);
   More reselect → Settings kept; Library reselect → settings sheet kept
   (documented). If not approved: only fix metadata, document current
   semantics in-app via nothing (status quo stands).
4. Global search: keep all 4 existing paths; no new tab (set-2 revert
   stands). Optionally (approval) add search icon action on Browse
   toolbar for a visible 5th path.
5. Long-press affordances: no new UI (YAGNI); keep as-is.
6. Transitions: existing shared-axis/fade-through stay.

**Why better**: predictable reselect, correct metadata, consistent icon
language — same structure, less surprise.

---

## 6. Overlay Safety Plan

### Reader inline z-order (single Box child order = contract)
| Layer | Content | Notes |
|---|---|---|
| 1 | Viewer pages (View layer, below Compose) | reader_activity.xml order |
| 2 | Navigation tap-zone hint (View) | fades on touch |
| 3 | Chapter-load progress (View) | removed on setChapters |
| 4 | ReaderContentOverlay (brightness/filter) | full-bleed intentional |
| 5 | ReaderAppBars (top bar + bottom tray + vertical navigator) | navBars inset inside tray |
| 6 | OcrSelectionOverlay (drag scrim) | full-screen; bars hidden during selection |
| 7 | DisplayRefreshHost (e-ink flash) | transient |
| 8 | TtsPlaybackBar (pill) | clearance = max(tray,navBars,cutout)+12dp |
| 9 | Dialogs: Loading / Settings / Mode / PageActions / ExclusionScope (all WINDOW dialogs — always above inline) | pill intentionally before dialog block |
| 10 | OcrResultOverlay INLINE (scrim + popup / ResizableSheet) | above pill by child order |
| 11 | OcrLoadingIndicator | topmost inline — FIX: give clearance, stop covering pill |

Windows (Dialog/Popup) always above inline content. Snackbars above
bottom bars (Scaffold slot math). Speed-dropdown popup = window, below
later-opened dialogs.

### Overlay table (required format)
| Overlay | Purpose | Anchor | Safe Area | Priority | Collision Behavior |
|---|---|---|---|---|---|
| Bottom nav pill | persistent nav | bottom center, inset 12/8 | navBars INSIDE pill | above content, below everything transient | hides on selection mode |
| Reader tray | reader chrome | bottom (or side vertical navigator) | navBars inside tray | below floating pills | auto-hide 150-200ms |
| TTS pill | transport | bottom center, offset clearance | max(tray,navBars,cutout)+12dp | above tray, below dialogs | yields to all dialogs (window) |
| OcrLoadingIndicator | scan progress | bottom center | CURRENTLY NONE → clearance | topmost inline | FIX: same clearance as pill; never covers pill/nav zone |
| OCR selection scrim | drag-select | full screen | full-bleed (bars hidden) | above app bars | pill FIX: hide/dim pill during selection mode |
| OcrResultPopup | dictionary card | 4-side placement near bubble | clamp 8dp → FIX: inset-aware viewport | above pill, below dialogs | fallback to sheet if <35-55% usable |
| OcrResultBottomSheet | dictionary results | bottom/end ResizableSheet | FIX: navBars padding | above popup family | drag-dismiss |
| AdaptiveSheet dialogs | settings/actions | bottom (phone) / center (tablet) | navBars+statusBars inside surface | window = above all inline | — |
| AlertDialogs | confirms/pickers | center | platform | window | — |
| DropdownMenus | overflow/speed | anchor | popup window | above inline, below dialogs | — |
| Snackbars | feedback | above bottom bar | Scaffold slot math | above nav pill | — |
| FABs | primary action | Scaffold slot | bottom offset math | above content | hidden in selection |
| ReaderPageIndicator | page number | bottom center | navBarsPadding | above content, below pills | FIX: currently dead — restore into live tree |

### Fixes (safety, no behavior change)
1. OcrLoadingIndicator: apply the existing `bottomClearancePx` formula (or
   minimum navBars padding) — never under system bars, never on TTS pill.
2. ReaderPageIndicator: re-add to live inner composition (restores
   pref-gated feature lost in merge 0eab09ce4).
3. Delete dead outer composition tree (ReaderActivity) — kills the
   "dialog added to dead branch" trap; structural but presentation-only;
   needs approval (touched file is activity wiring).
4. Codify z-order table as comment at inner Box.
5. TTS pill during OCR selection mode: hide with menus (selection already
   hides menu) — 1-line AnimatedVisibility condition.
6. OcrResultPopup: subtract insets from placement viewport (small input
   change to calculatePopupPlacement).
7. Sheet geometry unification (see §3).

---

## 7. Component System Plan

**Reuse as-is**: NavigationBar/Rail, Scaffold, Pill, Badge/BadgeGroup,
ActionButton, EmptyScreen, LoadingScreen, AdaptiveSheet, SettingsItems
(in-reader dialect), preference widget family (settings dialect),
MangaComfortableGridItem + siblings, DropdownMenu wrapper, PullRefresh,
FastScroll lists.

**Refine**:
- `ListGroupHeader` → the in-list section header (MoreScreen, Sources,
  Feed headers, History/Updates dates converge on it).
- `PreferenceGroupHeader` → re-point to `Typography.header` color/weight.
- `ResizableSheet` → AdaptiveSheet geometry tokens (+keep handle).
- `SettingsItems` grid title → new shared `itemTitle` typography role in
  presentation-core Typography.kt (kills 12sp hack duplication).
- `EmptyScreen` → Feed adopts it.
- `NavigationBar` constants → token-based.

**Standardize**:
- Delete MoreScreen private GroupHeader; delete TrackInfoDialogHome
  double-clip; unify delete icon (outlined, error-tinted) across
  Dictionary/Exclusions/sheet.
- Grid gutter pattern: one (CommonMangaItemDefaults + contentPadding 8).
- Pill alpha + placeholder color + active amber → single constants/
  scheme extensions.

**New presentation-only components (minimal)**:
- Loading/Error list items (spinner row / error row) for in-grid section
  states (Feed, GlobalSearch) — thin wrappers, presentation-core.
- Type/scope Badge chips on exclusion RuleRow (reuse Badge).
- Nothing else. No new libraries, no new layout system.

---

## 8. Visual Design Direction

- **Surface hierarchy**: background → primary content (flat lists/grids,
  no cards) → grouped content (AdaptiveSheet dialogs, SectionCard only
  where true grouping) → interactive elements (pills, chips, rows) →
  focal (FAB, primary buttons). Controls hierarchy via spacing +
  typography, not borders/cards.
- **Spacing**: 4/8/16/24/32 tokens; 12dp promoted to token `mediumSmall`?
  NO — decision: **eliminate 12dp** (snap to 8 or 16) except the
  deliberately documented nav-pill 12dp inset (freeze as pill constant).
  24dp islands (sheet/settings dialogs) stay as the in-dialog dialect.
- **Typography**: M3 roles; grid/item titles = new `itemTitle` (12sp/18);
  headers = header style; titles titleLarge/titleMedium; metadata
  bodySmall/labelSmall. No font-size overrides outside
  presentation-core typography definitions.
- **Shapes**: M3 tokens only. Rule: pills/sheets extraLarge(28); grouping
  cards large(16); small controls small(8); chips M3 default. No
  literal radii in feature code.
- **Elevation**: tonal 3dp for bars/pills; tonal 6dp sheets; shadow ONLY
  on floating reader pill (existing signature); app bars pre-blend via
  surfaceColorAtElevation (existing).
- **Colors**: colorScheme tokens only; semantic fixes (active/tertiary,
  placeholder, alpha constants); scrims stay black-literal where
  intentional (reader, cover art).
- **Dark mode/AMOLED/Monet**: all changes token-driven → automatic
  compliance; verification sweep across 13 schemes + AMOLED + Monet +
  Monochrome at the end (STEP 10).
- **Motion**: existing patterns only (fade/slide for bars, animateContentSize
  for expand); no new animation.

---

## 9. Implementation Impact

### Files expected to change (presentation only)
- presentation-core: material/NavigationBar.kt, components/{ListGroupHeader,
  AdaptiveSheet, ResizableSheet, EmptyScreen, SettingsItems, Pill}.kt,
  theme/{Typography, Color, Constants}.kt, screens/EmptyScreen.kt (Feed
  reuse — no edit likely), new Loading/Error list items (1 small file).
- app/presentation + ui screens: FeedScreen.kt, ManageFeedsScreen.kt,
  AddFeedDialog (inside FeedScreen), MoreScreen.kt, SourcesScreen.kt
  (header), GlobalSearchToolbar.kt (chips), LibraryToolbar.kt +
  Tabs.kt + SourcesScreen.kt (PillAlpha), MangaCover.kt +
  BrowseIcons.kt (placeholder), CommonMangaItem.kt + MangaInfoHeader.kt
  (itemTitle), grid files (gutter), TrackingPreferenceWidget.kt (color),
  MangaBottomActionMenu family (token pass), AppBar.kt (search title size),
  DictionaryComponents.kt (token pass).
- Settings: SettingsReaderScreen/SettingsDownloadScreen/
  SettingsAdvancedScreen.kt (group loose rows), SettingsMainScreen.kt
  (Anki subtitle), SettingsSearchScreen.kt (Anki registration — flagged),
  SettingsScreen.kt (destination ids).
- OCR/TTS UI: SettingsOcrExclusionsScreen.kt (+SM), OcrExclusionZoneDialogs.kt
  (sheet RuleRow port), ReadAloudPage.kt (cross-link row),
  ReaderSettingsDialog.kt (dim-hack identity check).
- Reader presentation: ReaderActivity.kt (dead-tree removal + z-order
  comment + indicator/pill fixes — STRUCTURAL, approval), OcrLoadingIndicator.kt,
  ReaderPageIndicator placement, OcrResultPopup.kt (insets), TtsPlaybackBar.kt
  (minor), ReaderAppBars.kt (token pass).
- i18n: base strings.xml only (feed empty/loading/error reuse mostly
  existing keys; exclusion type legend ~4 keys; Anki subtitle 1).
- HomeScreen/FeedTab/MoreTab: index fix + anim_feed_enter drawable.

### Files that MUST NOT change
- domain/**, data/** (no .sq/.sqm/repositories), source-api/**,
  source-local/**, core:*/** (except none), telemetry/**, build files,
  libs.versions.toml, AndroidManifest, proguard.
- ViewModels/ScreenModels logic — EXCEPT explicitly approved items below.
- All TTS/OCR engine + controller + matcher code.

### Items requiring approval beyond pure presentation (excluded until approved)
1. MangaScreen error/missing state (ScreenModel state machine change).
2. Reselect → scroll-to-top convention (behavior change).
3. ReaderActivity dead-composition removal (structural, activity wiring).
4. SettingsSearchScreen Anki registration (behavior-ish bug fix).
5. OcrQueueScreen engine-settings relocation or cross-link (navigation
  reorg beyond a simple row).
6. `ttsSpeechScript` dead pref / `ocrTextSelectionEnabled` naming — logic
  or i18n-semantics decisions, not visual.

### Implementation order (maps to Prompt.md STEP 1-10)
1. Foundation: tokens, headers, sheets, typography, color literals.
2. Navigation: index fix, Feed icon, (approved reselect convention).
3. Library + browsing screens: grid gutters, itemTitle, badges.
4. Feed + discovery: FeedScreen normalization, GlobalSearch chips.
5. Settings hierarchy: grouping, subtitles, search coverage, cross-links.
6. OCR/TTS settings UI: exclusion unification, type legend, dialog hack.
7. Reader overlays: insets, indicator, dead tree (if approved), z-order
  comment, pill-during-selection.
8. TTS visual integration: pill token pass only (already modern).
9. Overlay collision verification pass (§6 table as checklist).
10. Light/dark/AMOLED/Monet/Monochrome sweep + spot screenshots.

---

## CONFIRMATION GATE — awaiting explicit user approval

1. **What visually changes**: spacing/shape/typography tokens enforced;
   one header system; one sheet geometry; grid gutters unified; Feed
   screen normalized (app bar, empty, chips, add-dialog, states); OCR
   exclusion rules get identity/type clarity + legend; settings get
   grouped rhythm + search coverage; semantic color cleanup.
2. **Screens affected**: all main tabs (mostly token-level), Feed
   (largest), More, Settings tree + sub-screens, Reader dialogs, OCR/TTS
   settings screens. Reader reading surface unchanged.
3. **Navigation changes**: TabOptions metadata fix + Feed animated icon
   (cosmetic). OPTIONAL (needs approval): reselect=scroll-to-top
   convention; MangaScreen error state; search-index registrations;
   OcrQueue cross-link.
4. **Overlay safety**: OcrLoadingIndicator clearance (stops covering TTS
   pill/system bars), dead ReaderPageIndicator restored, pill hidden
   during OCR selection, popup inset-awareness, dead outer composition
   removal (approval), explicit z-order table codified.
5. **Files/layers touched**: :app presentation/ui screens + settings +
   reader presentation, :presentation-core components/theme, :i18n base
   strings only. NO domain/data/source/build changes.
6. **NOT changed**: business logic, OCR/TTS engines + pipeline, DB,
   prefs semantics, theme schemes, tab structure, reader functionality,
   architecture, dependencies.
7. **Business logic untouched**: confirmed — every item that would touch
   logic is listed in §9 "requires approval" and excluded until you
   approve it individually.

**Do you approve this UI modernization plan and want me to proceed with the
frontend-only implementation?**

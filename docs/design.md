# Yomihon — Design & UX Specification

> Describes HOW the product should look/feel. Grounded in the existing design
> language; TTS-specific specs extend it and never override it.
> Compose implementation rules live in `docs/rules.md` §4.

---

## 1. Design philosophy

Yomihon inherits Mihon's **Material 3** design language, rendered through
`MaterialExpressiveTheme` (Compose BOM 2026.06.01):

- Content-first: chrome recedes; the manga page is the hero.
- System-conformant: dynamic color (Monet) on Android 12+, light/dark/AMOLED,
  13 selectable color schemes.
- Consistent shared components from `presentation-core`
  (`tachiyomi.presentation.core.components.*`) — feature code composes these
  rather than inventing new primitives.
- Reader UI is overlay-based and auto-hiding (menus toggle, bottom pills slide in
  for transient state like `OcrLoadingIndicator`).

New TTS UI must feel native to this system: a compact bottom pill in the reader,
settings rows/sliders using existing settings item specs.

## 2. Color system

Colors are NOT hard-coded per feature. All UI uses `MaterialTheme.colorScheme`
tokens supplied by `TachiyomiTheme`
(`app/src/main/java/eu/kanade/presentation/theme/TachiyomiTheme.kt`) via
`BaseColorScheme` implementations:

- Default scheme `TachiyomiColorScheme` (e.g., dark primary `0xFFB0C6FF`,
  dark background/surface `0xFF1B1B1F`, surfaceContainer `0xFF211F26`;
  light primary `0xFF0058CA`) — see
  `app/src/main/java/eu/kanade/presentation/theme/colorscheme/`.
- Alternatives: Monet (dynamic), Catppuccin, Nord, Tako, Yotsuba, YinYang,
  Midnight Dusk, Strawberry, Green Apple, Lavender, TealTurqoise, Tidal Wave,
  Monochrome; AMOLED variant flips surfaces to pure black.

Token mapping for TTS components (no new colors unless a proven need appears):

| Role | Token |
|---|---|
| Primary / accent | `colorScheme.primary` |
| Secondary | `colorScheme.secondary` |
| Background | `colorScheme.background` |
| Surface (bars/pills) | `colorScheme.surfaceContainer` (matches `OcrLoadingIndicator`) |
| Text | `colorScheme.onSurface` |
| Muted text | `colorScheme.onSurfaceVariant` |
| Success | `colorScheme.primary` (checkmark affordance); avoid new greens |
| Warning | `colorScheme.error`-adjacent tertiary only if unavoidable |
| Error | `colorScheme.error` + `onError` |
| Playback state | Communicated by icon + text (non-color), tinted with `primary` when active |

## 3. Theme

- Light/dark follow the system (`isSystemInDarkTheme()`), overridable in
  appearance settings (`AppTheme`, dark theme preference, AMOLED flag).
- Dynamic colors via `MonetColorScheme(context)` when selected.
- All reader overlays must remain legible under every scheme including AMOLED and
  Monochrome — use tokens only.

## 4. Typography

No custom fonts are introduced. The app uses Material 3 defaults provided by
`MaterialExpressiveTheme`; usages set roles explicitly per component:

| Use | Role |
|---|---|
| Screen titles / top bars | `titleLarge` / `titleMedium` |
| Section headers | `titleSmall` (with `ListGroupHeader` component) |
| Body text | `bodyMedium` / `bodyLarge` |
| Labels/settings descriptions | `bodySmall`, `labelMedium` |
| Captions/meta | `labelSmall` |
| Reader playback bar sentence text | `bodyLarge` (largest readable role that fits one line, ellipsized) |
| Reader playback bar position "x/y" | `labelMedium` |

## 5. Spacing

Follow existing component metrics rather than a new scale:

- Screen horizontal padding: 16 dp standard.
- Compact bars/pills: `horizontal = 24.dp, vertical = 12.dp`
  (`OcrLoadingIndicator` precedent).
- Icon spacing inside pills: `Arrangement.spacedBy(12.dp)`.
- Settings rows use `SettingsItems` specs (their built-in paddings).

## 6. Shapes

Material 3 shape tokens from the theme:

- Cards/sheets: `AdaptiveSheet` (presentation-core) handles bottom-sheet radius;
  dialogs use M3 defaults.
- Buttons: M3 defaults (`IconButton` for bar actions).
- Pills/bars: rectangular fill of `surfaceContainer` spanning full width
  (reader bottom overlays are full-width bars, not floating rounded chips).
- No custom corner radii in feature composables.

## 7. Icons

- Source: `androidx.compose.material.icons` extended set (dependency
  `androidx-compose-materialIcons`). Outlined variants dominate reader controls
  (OCR button uses `Icons.Outlined.DocumentScanner`).
- Planned TTS icons (existing material icons only):
  - Entry point: `Icons.Outlined.RecordVoiceOver`
  - Play/Pause: standard `PlayArrow` / `Pause`
  - Stop: `Stop`; Previous/Next sentence: `SkipPrevious` / `SkipNext`
  - Error state: `ErrorOutline` + retry via `Refresh`
- Every icon-only action requires a content description string resource.

## 8. Reader TTS controls

Placement mirrors established reader patterns:

- **Entry**: bottom-bar icon button in `ReaderBottomBar`, threaded exactly like
  `onClickOcr`.
- **Playback pill** (`TtsPlaybackBar`): full-width bottom bar above the bottom
  bar area, `Alignment.BottomCenter`, AnimatedVisibility slide-up/fade — visually
  a sibling of `OcrLoadingIndicator`. Contents:
  - Current sentence text (single line, ellipsize, `bodyLarge`)
  - Position indicator "x/y" (`labelMedium`)
  - Controls row: previous | play/pause | next | stop (`IconButton`s)
  - Preparing/LoadingPage states show a small `CircularProgressIndicator`
    (20 dp, stroke 2 dp) instead of controls where applicable
  - Error state: message + retry action
- Unobtrusive rule: the pill never covers more vertical space than the OCR
  loading bar family; it hides with menus when appropriate and always yields to
  page interaction (controls are tap targets only).
- v1 has NO on-image highlighting (approved scope decision); current-sentence
  feedback is textual in the pill only.

## 9. Accessibility

- Minimum touch target 48 dp for all controls (`IconButton` default size or
  explicit minimum).
- Content descriptions on every icon button from i18n strings.
- Contrast: rely on scheme tokens (`onSurface` on `surfaceContainer` meets WCAG;
  precedent: `ReaderOcrOverlayRenderer` already computes WCAG-safe colors for
  in-page bubbles).
- Scalable text: sp-based typography only; pill layout must tolerate large font
  scales (sentence line ellipsizes; controls wrap gracefully).
- State is never color-only: playing/paused/error differ by icon AND label.
- Screen reader order: sentence text → position → controls.

## 10. Motion

- Reuse existing patterns; no bespoke animation systems:
  - Bars appear/disappear with `fadeIn()+slideInVertically` /
    `fadeOut()+slideOutVertically` (OcrLoadingIndicator precedent).
  - Standard M3 ripple/pressed states on controls.
- No looping animations while playing (progress is conveyed by text position);
  spinner only during Preparing/LoadingPage.
- Respect system animator duration scale implicitly (Compose defaults).

## 11. Responsive behavior

- Phone portrait: pill spans width above bottom bar; controls centered.
- Phone landscape / tablets: same full-width pill; reader already adapts app bars
  (`ChapterNavigator` rails); no separate tablet layout for the pill.
- Dual-page/split pages: pill reflects the primary visible page's queue; worst
  case a one-tick stall on InsertPage transitions (accepted in root architect.md).

## 12. TTS visual feedback (v1)

- Current sentence: shown as text in the pill ("mini-bar highlight").
- On-image bbox highlight: explicitly deferred (approved decision). If added
  later, it must reuse the cached `OcrRegion.boundingBox` data and the drawing
  approach of `ReaderOcrOverlayRenderer` (normalized coords → view coords,
  WCAG-safe strokes) so it stays consistent with tap-to-lookup visuals.

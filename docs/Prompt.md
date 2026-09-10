# YOMITSU — COMPLETE UI AUDIT, DESIGN MAPPING & IMPLEMENTATION BLUEPRINT

## ROLE

You are acting as a senior Android UI/UX architect, product designer,
information-architecture specialist, and implementation-planning engineer.

You are NOT being asked to immediately redesign or implement the UI.

Your first responsibility is to fully understand the CURRENT Yomitsu
application, audit it, map it, identify inconsistencies, and produce an
implementation-grade UI design blueprint.

The purpose of this task is to prevent speculative UI implementation,
incorrect feature placement, inconsistent typography/spacing, accidental
architecture changes, and another UI regression cycle.

DO NOT start by implementing glassmorphism, blur, new cards, new navigation,
new settings groups, or any other visual feature.

FIRST AUDIT.
THEN MAP.
THEN DESIGN.
THEN SPECIFY IMPLEMENTATION.
IMPLEMENTATION HAPPENS ONLY AFTER THE BLUEPRINT IS REVIEWED/APPROVED.

---

# 1. REQUIRED DOCUMENTATION CONTEXT

Before doing anything else, read the following documents completely:

- docs/prd.md
- docs/architecture.md
- docs/rules.md
- docs/phase.md
- docs/design.md
- docs/design-audit.md
- docs/memory.md
- docs/branding.md, if present

Also inspect the relevant source code, shared presentation components,
theme implementation, settings infrastructure, navigation implementation,
reader UI, Feed UI, Recent UI, More UI, and all currently relevant
screens.

Treat these documents as the existing source of truth.

Do not silently replace documented project decisions with generic Android
or Material recommendations.

If a conflict exists between documents and actual source code:

1. Identify the conflict.
2. Record it explicitly.
3. Determine whether the source or documentation represents the current
   implementation.
4. Do NOT silently modify either one during the audit.
5. Record the required resolution in the audit/map.

---

# 2. PRIMARY OBJECTIVE

Create a complete, implementation-grade UI/UX map of the CURRENT Yomitsu
application and the APPROVED FUTURE UI.

The result must function like a professional designer-to-developer handoff.

Think of this as:

DESIGN SYSTEM
+
SCREEN INVENTORY
+
FIGMA-STYLE SCREEN SPECIFICATION
+
INFORMATION ARCHITECTURE
+
COMPONENT MAP
+
FEATURE LOCATION MAP
+
RESPONSIVE SPECIFICATION
+
IMPLEMENTATION CONTRACT
+
REGRESSION SAFETY PLAN

The final documentation must make it possible for another agent to
implement the approved design without having to guess:

- where something belongs
- what component should be used
- what surface it belongs to
- what typography role it uses
- how much spacing it requires
- whether it should be grouped
- whether it should be frosted
- whether it should remain solid
- how it behaves on phone/tablet/landscape
- what existing component should be reused
- what source file owns the UI
- what business logic must remain untouched

---

# 3. ABSOLUTE PROCESS RULE

DO NOT IMPLEMENT THE REDESIGN DURING THE AUDIT.

The first pass is READ-ONLY.

Do not modify application source files while building the map.

Do not "fix something quickly" because it appears obvious.

Do not refactor unrelated code.

Do not create speculative UI components.

Do not introduce a new design system.

Do not replace existing Material 3 components without documenting why.

Do not assume that a previous device verification means the current
behavior is correct.

The current source code and current behavior must be rechecked.

---

# 4. AUDIT THE ENTIRE CURRENT UI

Build a complete screen inventory.

At minimum inspect:

## Primary navigation

- Library
- Recent
- Feed
- Browse
- More

Also inspect:

- bottom navigation
- selected/unselected states
- labels
- icons
- badges
- reselect behavior
- navigation transitions
- navigation spacing
- navigation inset handling

Do not assume the current five-tab implementation is visually perfect
just because the information architecture is intentional.

---

# 5. RECENT SCREEN AUDIT

The Recent screen requires special attention because it was introduced
and modified recently.

Audit:

- Recent AppBar
- Continue tab
- History tab
- Updates tab
- PrimaryTabRow
- selected indicator
- tab typography
- tab horizontal spacing
- tab height
- tab baseline alignment
- top inset
- distance between AppBar and tabs
- distance between tabs and content
- empty states
- filters
- row spacing
- history rows
- update rows
- badges
- swipe behavior
- tab-tap behavior
- title duplication
- nested headers

IMPORTANT:

There have already been previous fixes for Recent layout overlap and
tab positioning.

Do not assume those fixes guarantee perfect visual consistency.

Re-audit the current implementation.

Also investigate the user's reported problem with the recently introduced
tab/screen surfaces.

If the user-referred "Create" tab/screen exists in the current source,
identify it precisely and include it in the audit.

If no such destination exists, do not invent one. Record that the user's
reference could correspond to another recently-created screen.

---

# 6. TYPOGRAPHY AUDIT

This is a REQUIRED audit category.

Do not only inspect colors and cards.

Audit every important screen for typography consistency.

For each text element determine:

- actual typography role
- intended typography role
- font family
- font size
- font weight
- line height
- letter spacing if explicitly configured
- max lines
- ellipsis behavior
- baseline alignment
- text-to-icon alignment
- title/subtitle hierarchy
- section-header hierarchy
- caption/meta hierarchy

Look specifically for:

- hard-coded sp values
- unnecessary custom TextStyle values
- inconsistent Material typography roles
- inconsistent line spacing
- text appearing too compressed
- text appearing too loose
- different screens using different title sizes for equivalent roles
- section headers with inconsistent size/weight
- supporting text using the wrong hierarchy
- metadata that is visually stronger than primary content
- text that wraps differently because of inconsistent width constraints

Do NOT change typography during the audit.

Record the discrepancy.

Every typography discrepancy must be classified:

- PASS
- MINOR
- MEDIUM
- MAJOR

---

# 7. SPACING AUDIT

Perform a systematic spacing audit.

Inspect:

- screen horizontal margins
- screen vertical padding
- AppBar-to-content spacing
- section-to-section spacing
- card-to-card spacing
- row padding
- icon-to-text spacing
- text-to-text spacing
- chip spacing
- tab spacing
- bottom-navigation inset
- dialog spacing
- sheet spacing
- reader overlay spacing
- list item height
- grid gutters
- content-to-edge spacing

Use the existing design system as the baseline.

Do NOT invent a second spacing system.

Check whether the current implementation consistently follows the existing
documented metrics.

Look especially for:

- 8dp in one place and 12dp in an equivalent place
- 12dp versus 16dp inconsistencies
- unequal left/right margins
- visually uneven vertical rhythm
- content touching card edges
- unnecessary nested padding
- double padding
- missing padding
- inconsistent group gaps
- inconsistent chip gaps
- inconsistent indentation

---

# 8. INDENTATION AND ALIGNMENT AUDIT

Audit visual alignment independently from spacing.

Check:

- left edges of section titles
- left edges of rows
- icon alignment
- title alignment
- subtitle alignment
- trailing control alignment
- switch alignment
- checkbox alignment
- slider alignment
- chip alignment
- card content alignment
- nested setting indentation
- dialog content alignment
- AppBar action alignment
- bottom navigation alignment

Equivalent components should share equivalent alignment.

If two visually equivalent rows begin at different horizontal positions,
record it.

If an icon and its text are not vertically centered, record it.

If a section header does not align with the content below it, record it.

---

# 9. HEADER AUDIT

Audit every major screen and destination.

For every screen determine:

- Does it need an AppBar?
- Does it currently have one?
- Is the title duplicated elsewhere?
- Is the title missing?
- Is the title using the correct typography role?
- Are actions correctly placed?
- Are actions discoverable?
- Is there unnecessary nested AppBar/header content?
- Is there an internal section header where an AppBar title should be used?
- Is there an AppBar where the screen architecture requires a nested
  destination title?

Do not normalize all screens into one generic header.

Document the correct header pattern for each screen.

---

# 10. FEED AUDIT

The Feed screen requires BOTH a visual audit and a behavioral audit.

Current conceptual model includes:

- source selector
- listing selector
- All
- Popular
- Latest
- feed sections
- grid
- customization
- feed management
- pagination

Audit all of these.

## Source selector

Verify:

- placement
- alignment
- chip styling
- selected state
- dropdown behavior
- menu alignment
- source persistence
- All Sources behavior

## Listing selector

Verify specifically:

- All
- Popular
- Latest

CRITICAL BUG INVESTIGATION:

The user reports that selecting Popular or Latest does not correctly
replace All.

The user reports that All remains selected and/or both listings remain
visible.

Do not assume the previous implementation is correct.

Reproduce and inspect the current implementation.

Determine the exact root cause.

Possible areas to investigate include, but are not limited to:

- listingOverride state
- selected listing state
- FilterChip selected state
- FeedScreenModel filtering
- FeedPreferences default listing
- recomposition
- state restoration
- source/listing combination logic
- duplicated FeedItem entries
- initial-state reset
- persisted preference overriding user selection

Do not guess.

Record:

CURRENT BEHAVIOR
EXPECTED BEHAVIOR
ROOT CAUSE
AFFECTED FILES
PROPOSED FIX
REGRESSION RISK
VERIFICATION PLAN

The desired behavior is:

If Popular is selected:
→ only Popular listing should be selected/displayed for the active
source/filter context.

If Latest is selected:
→ only Latest listing should be selected/displayed.

If All is selected:
→ All configured listings should be shown.

The selected visual state must match the actual data state.

---

# 11. MORE SCREEN TWO-LEVEL INFORMATION ARCHITECTURE AUDIT

The first-level More grouping already exists.

Do NOT treat this as complete.

Audit:

More
├── General
├── Library
└── Settings

Then recursively audit the destinations opened from those groups.

For each destination determine:

- purpose
- related settings/features
- whether internal grouping is required
- existing internal grouping
- missing grouping
- unnecessary grouping
- duplicate grouping
- wrong grouping
- correct group name
- rows belonging to each group

Examples requiring explicit investigation include:

- Text Recognition
- Dictionary
- Dictionary settings
- OCR-related destinations
- other destinations launched from More

Do not force every screen into cards.

Only group conceptually related settings.

Do not create meaningless categories.

A group should exist because its rows form a coherent conceptual category,
not merely because there are multiple rows.

---

# 12. SETTINGS AUDIT

Audit the entire settings hierarchy.

Check:

- main settings
- Appearance & Interface
- Reader
- Read Aloud & Voice
- Browse
- Library
- Tracking
- Data/backup/storage
- OCR/Text Recognition
- Dictionary
- other existing settings destinations

For every screen map:

SCREEN
→ GROUP
→ ROW
→ SUBSETTING
→ RELATED FEATURE

Check for:

- missing internal groups
- inconsistent group naming
- duplicated groups
- groups with one unrelated item
- loose rows
- rows that should belong together
- incorrect placement
- inconsistent spacing
- inconsistent typography
- inconsistent surfaces

Preserve existing working preference keys unless the approved design
explicitly requires a change.

---

# 13. DESIGN SYSTEM AUDIT

Use the existing design.md as the baseline.

Do not invent a new design language.

Audit:

## Typography

Use documented Material 3 roles.

## Colors

Use MaterialTheme.colorScheme tokens.

No feature-specific hard-coded colors unless explicitly justified.

## Surfaces

Maintain the semantic surface hierarchy:

1. Solid surface
2. Floating chrome
3. Frosted modal

Do not turn every surface into glass.

## Grouped surfaces

One conceptual settings group = one PreferenceGroupCard.

Do not create a card around every row.

## Shapes

Reuse Material 3/shared shape tokens.

## Icons

Reuse existing Material icons/components.

## Motion

Reuse existing motion patterns.

## Responsive behavior

Preserve compact/expanded behavior and existing tablet/landscape logic.

---

# 14. GLASS / FROSTED UI AUDIT

This is particularly important.

NEVER write a generic instruction such as:

"Add glassmorphism."

Instead, define semantic surface roles.

For every candidate surface classify:

- SOLID
- FLOATING CHROME
- FROSTED MODAL
- NO SPECIAL SURFACE

For each frosted candidate document:

- exact screen
- exact component
- exact location
- reason
- backdrop availability
- expected translucency
- fallback when translucent UI is disabled
- performance considerations
- accessibility considerations

The following are NOT automatically glass:

- settings cards
- manga cards
- long-form text
- OCR result content
- About content
- ordinary list rows
- large content surfaces

Do not introduce true backdrop blur simply because it looks attractive.

The existing project documentation explicitly records that true backdrop blur
is deferred because of Compose rendering architecture and performance concerns.

Therefore:

DO NOT implement true backdrop blur during this mapping exercise.

---

# 15. FEATURE LOCATION MAP

Every feature must have exactly one canonical location.

Create a table:

| Feature | Current Location | Proposed Location | Reason | Related Settings | Implementation Owner |
|---|---|---|---|---|---|

Examples:

- Panorama Cover
- Cover-based theming
- Dynamic controls
- Reader controls
- OCR
- Read Aloud
- Voice profiles
- Speech rate
- Dictionary
- Text Recognition
- Feed customization
- Feed listing selector
- Feed source selector
- Storage Manager
- Backup & Restore
- Theme customization
- Wallpapers
- Navigation customization
- etc.

Do not copy features from AnymeX or Chimahon automatically.

Reference products may inspire organization and visual hierarchy, but
Yomitsu remains its own product.

---

# 16. ANYMEX / CHIMAHON REFERENCE RULE

When using reference applications or documentation:

BORROW:

- visual hierarchy
- information architecture ideas
- meaningful customization
- settings organization
- surface hierarchy
- responsive thinking
- discoverability
- reader customization concepts

DO NOT COPY:

- branding
- product identity
- unrelated navigation architecture
- anime ecosystem
- tracking ecosystem
- service integrations
- unrelated features
- visual identity wholesale

For every borrowed idea document:

REFERENCE
→ OBSERVATION
→ YOMITSU ADAPTATION
→ WHY IT FITS YOMITSU

---

# 17. SCREEN-BY-SCREEN DESIGN SPECIFICATION

For EVERY major screen produce a specification with:

## Screen identity

- Screen name
- Route/destination
- Purpose
- Entry points
- Exit/back behavior

## Layout

- AppBar/header
- content container
- sections
- bottom navigation
- floating elements
- sheets/dialogs

## Typography

- title role
- section header role
- body role
- supporting role
- metadata role

## Spacing

Document the intended spacing relationships.

## Surfaces

Document:

- background
- card/surface
- floating surface
- frosted surface
- prohibited surface treatments

## Components

Specify which existing component should be reused.

## Interaction

Document:

- tap
- long press
- swipe
- selection
- expansion
- navigation
- menus
- persistence

## States

Document:

- loading
- empty
- error
- selected
- disabled
- active
- unavailable

## Responsive behavior

Document:

- compact phone
- expanded phone
- landscape
- tablet

## Accessibility

Document:

- minimum touch target
- content descriptions
- text scaling
- contrast
- state communication

---

# 18. CODE OWNERSHIP MAP

For every screen/component identify the actual source file.

Example:

| UI Element | Current Source File | Shared Component | State Owner | Business Logic Owner |
|---|---|---|---|---|

This is mandatory.

The implementation map must prevent an agent from editing a random file
because it "looks like" the correct location.

---

# 19. IMPLEMENTATION BOUNDARY

For every proposed change classify it:

- PRESENTATION ONLY
- PRESENTATION + STATE
- PREFERENCE CHANGE
- NAVIGATION CHANGE
- DOMAIN CHANGE
- DATA CHANGE
- DATABASE CHANGE

Default assumption:

UI modernization should remain presentation-only unless the feature
cannot work otherwise.

Do not alter:

- database schema
- OCR pipeline
- TTS engine
- TTS playback controller
- reader business logic
- source/network logic
- backup format
- Mihon/Tachiyomi compatibility
- existing preference keys

unless the feature explicitly requires it.

If a feature appears to require a deeper architectural change, STOP and
document the dependency rather than improvising.

---

# 20. PROTECTED SYSTEMS

Create a dedicated "DO NOT TOUCH" section.

At minimum include:

- Reader playback behavior
- OCR acquisition pipeline
- OCR caching
- OCR exclusion matching
- TTS progression
- TTS arbitration
- bitmap lifecycle
- database/schema
- backup compatibility
- existing preference semantics
- source networking
- Feed data fetching unless required for the listing bug
- Mihon/Tachiyomi protocols
- application IDs/namespaces
- existing working navigation semantics

UI work must not accidentally modify these systems.

---

# 21. DISCREPANCY REGISTER

Create a table:

| ID | Screen | Category | Current Problem | Expected | Severity | Root Cause | Proposed Resolution |
|---|---|---|---|---|---|---|---|

Categories:

- Typography
- Spacing
- Alignment
- Indentation
- Header
- Navigation
- Surface
- Grouping
- Responsive
- Accessibility
- Interaction
- Functional
- State
- Discoverability

This register must include even small inconsistencies.

Do not hide minor discrepancies.

---

# 22. FUNCTIONAL UI AUDIT

Visual correctness is not enough.

Audit UI behavior for controls that appear correct but do not work.

At minimum verify:

- Feed All/Popular/Latest
- Feed source selector
- Feed source persistence
- Feed customization
- Recent tab switching
- Recent swipe
- selected tab state
- More navigation
- settings grouping navigation
- reader controls
- settings toggles
- dialogs
- sheets
- expandable rows
- filters
- sort controls
- persistence where expected

For every control:

VISIBLE STATE
must equal
ACTUAL STATE.

---

# 23. RECENT / FEED / MORE REGRESSION PRIORITY

These areas receive P0/P1 audit priority because they were recently changed.

Priority:

P0:
- Feed listing selector correctness
- Recent tab layout correctness
- More internal grouping correctness
- typography/spacing/alignment regressions in recently modified screens

P1:
- Feed customization
- source selector
- Recent empty states
- Recent row alignment
- More destination consistency

P2:
- broader visual consistency
- minor typography/spacing cleanup

---

# 24. DO NOT TRUST PREVIOUS PASS/FAIL RECORDS BLINDLY

Previous device verification records are evidence, not permanent truth.

If current source behavior contradicts a previous PASS:

- reproduce
- investigate
- document the discrepancy
- determine whether the previous verification was incomplete,
  stale, state-dependent, or testing a different behavior

Do not simply mark the new observation as invalid.

---

# 25. DESIGN IMPLEMENTATION MAP OUTPUT

Create:

docs/ui-implementation-map.md

This is the primary deliverable.

It must contain:

1. Document purpose
2. Current application UI inventory
3. Navigation map
4. Screen hierarchy
5. Design system reference
6. Typography specification
7. Spacing specification
8. Alignment specification
9. Header specification
10. Surface specification
11. Glass/frost rules
12. Responsive specification
13. Component inventory
14. Screen-by-screen implementation map
15. Feature placement map
16. Settings information architecture
17. More two-level grouping map
18. Recent implementation map
19. Feed implementation map
20. Feed listing behavior contract
21. Code ownership map
22. Implementation boundaries
23. Protected systems
24. Discrepancy register
25. Functional audit findings
26. Accessibility requirements
27. Motion requirements
28. Reference adaptation notes
29. Implementation sequence
30. Acceptance criteria
31. Regression checklist
32. Open questions / approval gates

---

# 26. DESIGN SPECIFICATION MUST BE IMPLEMENTATION-GRADE

Avoid vague statements such as:

- "make it modern"
- "improve spacing"
- "add glass"
- "make the tabs nicer"
- "make settings cleaner"
- "use better typography"

Instead write measurable/implementable instructions such as:

- use the existing Material typography role
- preserve the shared settings row metrics
- use 16dp screen inset
- use the established 12dp group gap
- use one PreferenceGroupCard per conceptual group
- use transparent ListItem rows inside tonal grouped surfaces
- use FilterChip for source selection
- keep listing selector as a single-select state
- use existing MaterialTheme.colorScheme tokens
- no true backdrop blur
- no frost on readable content
- etc.

If exact dimensions cannot be safely inferred from the current component
system, explicitly mark them as "requires approval" instead of inventing
values.

---

# 27. APPROVAL GATES

The blueprint must identify decisions that require user approval.

Examples:

- exact feature placement
- new settings category
- major navigation changes
- new component
- new surface role
- true blur
- new persistent preference
- database change
- architecture change

Do not make those decisions silently.

---

# 28. IMPLEMENTATION SEQUENCE

After completing the audit, propose an implementation sequence.

It should NOT be:

"implement everything."

Instead use controlled batches such as:

Batch 0:
Documentation + audit only

Batch 1:
Typography + spacing + alignment corrections

Batch 2:
Recent UI corrections

Batch 3:
Feed functional + visual corrections

Batch 4:
More internal grouping

Batch 5:
Shared surface/component refinement

Batch 6:
Approved new feature placement

Batch 7:
Glass/frosted treatment only where explicitly approved

Batch 8:
Responsive/accessibility pass

Batch 9:
Device verification

Each batch must have:

- scope
- files
- protected systems
- expected behavior
- visual acceptance criteria
- functional acceptance criteria
- regression tests

---

# 29. FINAL QUALITY CHECK

Before declaring the blueprint complete, verify:

[ ] Every bottom-nav screen is mapped
[ ] Every major destination is mapped
[ ] Every settings screen is mapped
[ ] More destinations have been recursively audited
[ ] Recent is mapped
[ ] Feed is mapped
[ ] Feed listing bug is investigated
[ ] Typography is audited
[ ] Line spacing is audited
[ ] Spacing is audited
[ ] Indentation is audited
[ ] Alignment is audited
[ ] Headers are audited
[ ] Surface hierarchy is audited
[ ] Glass placement is explicitly mapped
[ ] Responsive behavior is mapped
[ ] Accessibility is mapped
[ ] Feature placement is explicit
[ ] Code ownership is explicit
[ ] Protected systems are explicit
[ ] Implementation boundaries are explicit
[ ] Previous device-pass claims have been rechecked where necessary
[ ] No speculative implementation was performed
[ ] No application source files were modified during the mapping phase

---

# 30. STOP CONDITION

STOP after producing the documentation.

Do NOT begin implementing the UI redesign automatically.

The user will review the resulting:

docs/ui-implementation-map.md

and then provide the final approved feature list / implementation scope.

Only after approval should implementation begin.

The implementation agent must treat the approved
docs/ui-implementation-map.md as the UI equivalent of a Figma handoff.

No guessing.
No improvisation.
No "while I'm here" refactors.
No glass-everywhere.
No random spacing values.
No arbitrary typography.
No moving features without an explicit IA decision.
No functional changes hidden inside visual changes.

The goal is controlled, deterministic, reviewable UI modernization.
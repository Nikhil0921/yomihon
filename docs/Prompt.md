# YOMITSU / YOMIHON UI MODERNIZATION
## DESIGN AUDIT → DESIGN PLAN → USER APPROVAL → IMPLEMENTATION WORKFLOW

You are responsible for planning and, ONLY AFTER USER APPROVAL, implementing a complete frontend UI/UX modernization for the Yomitsu application.

This is a STRICTLY CONTROLLED DESIGN TASK.

The application already has an established identity, architecture, navigation structure, Material 3 design system, themes, screenshots, and existing UI components.

Your job is NOT to redesign the application from scratch.

Your job is to modernize the existing application while preserving its identity, functionality, structure, and core navigation logic.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## 🚨 ABSOLUTE RESTRICTION: DO NOT MODIFY CODE INITIALLY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

During the FIRST PHASE, you MUST NOT modify any application code.

DO NOT:

- Edit Kotlin files
- Edit Java files
- Edit business logic
- Edit repositories
- Edit ViewModels
- Edit domain logic
- Edit OCR logic
- Edit TTS logic
- Edit database code
- Edit networking
- Edit dependency injection
- Edit Gradle configuration
- Add dependencies
- Remove dependencies
- Change backend functionality
- Change APIs
- Change data models
- Change application behavior
- Change feature logic
- Change reader functionality
- Change settings functionality

The first phase is ANALYSIS AND DESIGN PLANNING ONLY.

You must first inspect the repository and create a complete modernization proposal.

NO IMPLEMENTATION UNTIL THE USER EXPLICITLY APPROVES THE PLAN.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# PRIMARY OBJECTIVE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Modernize the UI of Yomitsu while preserving the application's actual layout and identity.

The current application should NOT become:

❌ Cyberpunk
❌ Neon
❌ Futuristic sci-fi
❌ Glassmorphism-heavy
❌ Over-animated
❌ Overly rounded everywhere
❌ Gradient-heavy
❌ Dashboard-like
❌ Visually noisy
❌ Complicated
❌ A completely different manga application

Instead, create:

✓ Modern
✓ Minimal
✓ Structured
✓ Clean
✓ Premium
✓ Calm
✓ Content-focused
✓ Properly aligned
✓ Visually hierarchical
✓ Consistent
✓ Easy to navigate
✓ Native-feeling
✓ Material 3 compatible

The goal is:

"THE SAME APPLICATION, BUT WITH A MUCH MORE REFINED AND STRUCTURED VISUAL SYSTEM."

Do not destroy recognizability.

A user familiar with the current application should immediately understand that this is the same application.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# EXISTING DESIGN SYSTEM MUST BE RESPECTED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Before planning anything, inspect:

- docs/design.md
- docs/rules.md
- docs/architecture.md
- relevant theme files
- Material theme implementation
- shared presentation-core components
- existing navigation implementation
- screenshots/reference images provided by the user

The existing design system is based around Material 3 and existing theme tokens.

DO NOT invent a separate visual system unless absolutely necessary.

Preserve:

- Material 3 compatibility
- Dynamic color support
- Light mode
- Dark mode
- AMOLED support
- Existing color schemes
- Theme adaptability
- Existing typography system
- Existing accessibility principles
- Existing shared components where possible

Do not hard-code random colors.

Use the application's existing color system and semantic theme tokens.

The modernization should work consistently across:

- Default themes
- Dynamic colors
- Dark mode
- Light mode
- AMOLED
- Existing selectable color schemes

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# DESIGN PROBLEM TO SOLVE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

The current UI visually feels too flat in several areas.

The modernization should improve:

1. Visual hierarchy
2. Structural grouping
3. Alignment
4. Spacing consistency
5. Navigation clarity
6. Content organization
7. Screen composition
8. Surface hierarchy
9. Interaction discoverability
10. Empty-state presentation
11. List organization
12. Settings organization

IMPORTANT:

Do NOT solve "flatness" by adding excessive cards everywhere.

Do NOT put every component inside a floating container.

The goal is controlled hierarchy.

Use surfaces only where they create meaningful separation.

Think in terms of:

BACKGROUND
    ↓
PRIMARY CONTENT SURFACE
    ↓
GROUPED CONTENT
    ↓
INTERACTIVE ELEMENTS
    ↓
PRIMARY ACTION / FOCAL POINT

The user should visually understand the structure of a screen without excessive borders.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# CORE VISUAL DIRECTION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Create a design language that can be described as:

"STRUCTURED MINIMALISM"

Characteristics:

- Calm backgrounds
- Clear content zones
- Strong spacing discipline
- Subtle surface elevation
- Consistent alignment
- Minimal decoration
- Purposeful rounded shapes
- Clear section hierarchy
- Strong typography hierarchy
- Comfortable density
- Minimal visual noise

Avoid:

- Excessive shadows
- Glowing borders
- Neon colors
- Animated backgrounds
- Heavy gradients
- Glassmorphism
- Random decorative lines
- Too many cards
- Excessive pill components
- Huge corner radii everywhere

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# PRESERVE THE ACTUAL APPLICATION STRUCTURE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

You MUST inspect the actual existing screens before proposing changes.

Do NOT invent generic screens.

Preserve the real structure of the application.

Audit all major areas, including wherever they exist:

- Library
- Manga/series browsing
- Sources
- Browse/discovery
- Updates/feed
- History
- Search
- Categories
- Settings
- Reader
- Reader settings
- OCR interfaces
- Read Aloud/TTS interfaces
- Dialogs
- Bottom sheets
- Navigation components
- Context menus
- Empty states
- Loading states
- Error states

The existing functionality and information architecture must remain recognizable.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# NAVIGATION MODERNIZATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Analyze the existing navigation carefully.

The user wants a more modern way to navigate the application, but the navigation must NOT become confusing.

First determine:

- What navigation currently exists
- Which navigation patterns are redundant
- Which actions are difficult to discover
- Which screens require faster access
- Which navigation elements are overloaded

Then propose improvements.

Possible improvements may include:

- Better bottom navigation hierarchy
- Clearer active state
- Better separation between primary and secondary navigation
- Contextual navigation where appropriate
- Improved top app bar organization
- Better use of overflow menus
- Improved screen transitions
- More logical grouping of secondary destinations

BUT:

DO NOT merge major sections blindly.

DO NOT create a browser/feed hybrid unless the existing information architecture clearly benefits from it.

DO NOT remove functionality merely to make the UI look cleaner.

DO NOT hide important features behind multiple layers of navigation.

The navigation should feel:

Simple → Predictable → Fast → Contextual

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# OVERLAY AND LAYERING SYSTEM
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

This is extremely important.

ALL overlays must be audited.

The application currently contains or may contain:

- Bottom sheets
- Dialogs
- Floating controls
- Reader overlays
- Playback controls
- OCR controls
- Menus
- Snackbars
- Loading indicators
- Navigation bars

You MUST establish a clear layering and safe-area system.

STRICT RULE:

NO OVERLAY MAY ACCIDENTALLY COVER OR BLOCK ANOTHER IMPORTANT ICON OR CONTROL.

Examples:

❌ Floating playback control covering reader controls
❌ Bottom overlay covering bottom navigation actions
❌ Dialog content hiding important buttons
❌ Floating action overlapping navigation icons
❌ Snackbar covering critical interaction controls
❌ OCR controls colliding with TTS controls

Every overlay must have:

1. Defined z-order
2. Defined anchor
3. Defined safe area
4. Collision behavior
5. Priority behavior

Create an explicit overlay hierarchy such as:

Layer 1:
Base application content

Layer 2:
Persistent navigation

Layer 3:
Contextual screen controls

Layer 4:
Temporary interactive surfaces

Layer 5:
Bottom sheets/dialogs

Layer 6:
Critical system feedback

The actual hierarchy should be determined from the codebase.

For the Reader especially:

- Reader content remains the hero
- Overlays must remain unobtrusive
- Controls must not collide
- Floating surfaces must respect safe insets
- Multiple active overlays must stack intelligently
- Auto-hide behavior must remain understandable

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SCREEN STRUCTURE SYSTEM
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

For every major screen, establish a consistent structure.

Recommended analysis structure:

1. Screen identity
2. Primary purpose
3. Primary focal content
4. Secondary content
5. Navigation
6. Actions
7. Information hierarchy
8. Surface hierarchy
9. Empty state
10. Loading state
11. Error state

Each screen should have a clear visual composition.

Example conceptual structure:

TOP AREA
- Screen title
- Contextual actions

↓

PRIMARY CONTENT AREA
- Main content
- Primary information

↓

SECONDARY GROUPS
- Related filters
- Categories
- Supporting information

↓

PERSISTENT NAVIGATION
- Clearly separated from content
- Does not visually compete with content

Do not mechanically apply this structure.

Adapt it to each actual screen.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# LIBRARY DESIGN MODERNIZATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Audit the actual Library screen.

Preserve its current functionality and content structure.

Improve:

- Header hierarchy
- Category/filter discoverability
- Grid/list alignment
- Cover spacing
- Metadata hierarchy
- Empty state
- Sorting/filter controls
- Search access
- Section separation

The manga covers should remain visually important.

Do not bury manga covers inside excessive cards.

The content itself should provide visual richness.

The UI chrome should support the content, not compete with it.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# BROWSE / SOURCE / DISCOVERY DESIGN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Audit these screens individually.

DO NOT automatically combine them.

If the user has separate Browser, Source, and Feed structures, preserve that distinction unless there is a strong usability reason not to.

Improve:

- Source discovery
- Search hierarchy
- Filtering
- List structure
- Section grouping
- Result presentation
- Empty/loading/error states

Use clear hierarchy rather than excessive containers.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# FEED / UPDATES DESIGN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Modernize feed/update presentation without turning it into a social media interface.

The feed should prioritize:

- Readability
- Chronological clarity
- Manga identity
- Chapter/update information
- Quick actions

Use subtle grouping where appropriate.

Avoid excessive visual clutter.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SETTINGS MODERNIZATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

The Settings area should become significantly more structured.

The existing settings should remain accessible.

Improve:

- Category grouping
- Section hierarchy
- Descriptions
- Visual scanning
- Search discoverability
- Advanced settings separation

Recommended conceptual hierarchy:

APPLICATION
    General
    Appearance
    Navigation

READING
    Reader
    Display
    Behavior

INTELLIGENCE
    Text Recognition
    OCR Models
    Exclusions

AUDIO
    Read Aloud
    Voice
    Playback

DATA
    Library
    Backup
    Storage

ABOUT
    Application
    Version
    Licenses

This is only a conceptual example.

You MUST derive the final structure from the actual application.

Do not change functionality without approval.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# TEXT RECOGNITION SETTINGS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

The Text Recognition area must receive special attention.

The existing options and models must remain functionally unchanged.

Organize them visually so the user can clearly understand:

- What each OCR model is
- Which model is selected
- What its purpose is
- Advanced options
- Exclusion rules
- Related controls

Do NOT change OCR implementation.

Do NOT change model behavior.

This is a presentation and UX organization task only.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# READER DESIGN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

The Reader is content-first.

The manga/manhua/webtoon page must remain the hero.

Do NOT add permanent visual chrome that distracts from reading.

Improve only:

- Control organization
- Overlay positioning
- Visual consistency
- Safe overlay stacking
- Control discoverability
- Settings access

Preserve the established overlay-based nature of the reader.

The reader should feel:

Invisible when reading
Clear when interacting

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# READ ALOUD / TTS UI
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Respect the existing Read Aloud design specification.

Do not redesign TTS into an unrelated component system.

Maintain consistency with:

- Existing Material 3 system
- Floating controls
- Reader overlay behavior
- Existing settings components

Improve visual integration only where necessary.

The TTS controls must never:

- Cover critical reader controls
- Cover navigation
- Create overlapping interaction zones
- Consume excessive reading space

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# TYPOGRAPHY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Do not introduce random fonts.

Preserve the existing typography system.

Improve hierarchy through:

- Title sizing
- Weight
- Spacing
- Section labels
- Metadata contrast
- Supporting text

Typography should create structure.

Do not rely only on borders and cards.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# SPACING AND ALIGNMENT SYSTEM
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Perform a complete spacing audit.

Identify inconsistent:

- Horizontal padding
- Vertical spacing
- Icon alignment
- Text alignment
- Card/list spacing
- Section spacing
- Grid gaps
- Bottom navigation clearance

Create a consistent spacing strategy based primarily on existing application conventions.

Do not introduce arbitrary spacing values everywhere.

Every screen should feel aligned to the same invisible grid.

The UI should feel intentional.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# COMPONENT CONSISTENCY AUDIT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Audit repeated components.

Identify inconsistencies in:

- Buttons
- Icon buttons
- Search fields
- Cards
- List rows
- Switches
- Sliders
- Menus
- Bottom sheets
- Dialogs
- Empty states
- Loading indicators

Propose a unified design approach.

Prefer improving or reusing existing shared components.

Do NOT create dozens of new custom components unless necessary.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# ACCESSIBILITY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Every modernization decision must preserve or improve accessibility.

Audit:

- Touch target sizes
- Contrast
- Text scaling
- Screen reader labels
- Icon-only actions
- State communication
- Light/dark compatibility

Never communicate important state using color alone.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# MOTION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Keep motion subtle.

Use motion only to communicate:

- Navigation
- Hierarchy
- Appearance/disappearance
- Expansion/collapse
- State changes

Avoid:

- Excessive animations
- Continuous animations
- Decorative animations
- Futuristic effects
- Bouncy exaggerated transitions

The application should feel refined, not animated for entertainment.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# DESIGN AUDIT PROCESS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

PHASE 1 — REPOSITORY AUDIT

Before proposing implementation:

1. Read all relevant design documentation
2. Inspect theme architecture
3. Inspect navigation architecture
4. Inspect shared UI components
5. Inspect all major screens
6. Inspect screenshots/reference images
7. Identify current visual problems
8. Identify inconsistencies
9. Identify navigation problems
10. Identify overlay collisions
11. Identify opportunities for modernization

DO NOT MODIFY CODE.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# REQUIRED OUTPUT: DESIGN AUDIT REPORT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

After the audit, provide the user with a detailed report.

The report must include:

## 1. Current UI Analysis

Explain:

- Current design strengths
- Current design weaknesses
- Flatness problems
- Hierarchy problems
- Alignment problems
- Navigation problems
- Component inconsistencies

## 2. Existing Design System

Explain:

- Current theme system
- Existing Material system
- Shared components
- Color behavior
- Typography
- Shapes
- Navigation patterns

## 3. Modernization Strategy

Explain exactly:

- What will remain unchanged
- What will be visually improved
- What will be reorganized
- What will NOT be changed

## 4. Screen-by-Screen Plan

For every major screen:

CURRENT STRUCTURE
→
PROPOSED STRUCTURE
→
VISUAL IMPROVEMENTS
→
NAVIGATION IMPROVEMENTS

## 5. Navigation Plan

Clearly explain:

- Current navigation
- Problems
- Proposed navigation model
- Why the new structure is better

## 6. Overlay Safety Plan

Create a clear table:

Overlay
Purpose
Anchor
Safe Area
Priority
Collision Behavior

## 7. Component System Plan

List:

- Components to reuse
- Components to refine
- Components requiring visual standardization
- Any proposed new presentation-only components

## 8. Visual Design Direction

Explain:

- Surface hierarchy
- Spacing
- Typography
- Shapes
- Elevation
- Colors
- Dark mode behavior

## 9. Implementation Impact

Clearly state:

FILES EXPECTED TO CHANGE:
[design/presentation files only]

FILES THAT MUST NOT CHANGE:
[domain/backend/data/business logic files]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# REQUIRED USER CONFIRMATION GATE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

After producing the complete design proposal:

STOP.

DO NOT IMPLEMENT ANYTHING.

Ask the user for confirmation.

The confirmation message MUST clearly explain:

1. What will visually change
2. What screens will be affected
3. What navigation changes are proposed
4. What overlay safety improvements will happen
5. What files/layers will be touched
6. What will NOT be changed
7. Confirmation that business logic will remain untouched

Then ask:

"Do you approve this UI modernization plan and want me to proceed with the frontend-only implementation?"

Wait for an explicit answer.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# AFTER USER APPROVAL
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ONLY after explicit approval:

Implement the approved plan gradually.

Follow this order:

STEP 1
Foundation and shared visual consistency

STEP 2
Navigation improvements

STEP 3
Library and browsing screens

STEP 4
Feed and discovery screens

STEP 5
Settings hierarchy

STEP 6
Text Recognition and advanced settings UI

STEP 7
Reader overlay organization

STEP 8
Read Aloud/TTS visual integration

STEP 9
Overlay collision verification

STEP 10
Light/dark/theme verification

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# IMPLEMENTATION RESTRICTIONS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

During implementation:

✓ Modify frontend/presentation only
✓ Preserve existing behavior
✓ Preserve existing APIs
✓ Preserve existing architecture
✓ Preserve theme compatibility
✓ Reuse shared components where possible

DO NOT:

❌ Refactor unrelated code
❌ Change business logic
❌ Change OCR behavior
❌ Change TTS behavior
❌ Change database logic
❌ Change network logic
❌ Add random dependencies
❌ Rewrite architecture
❌ Rename unrelated files
❌ Perform large cleanup unrelated to UI

If a proposed UI change requires business logic changes:

STOP.

Explain the dependency to the user.

Ask for approval before touching that logic.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# FINAL QUALITY STANDARD
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

The finished application should feel:

✓ More structured
✓ More modern
✓ More intentional
✓ Cleaner
✓ Easier to navigate
✓ Better aligned
✓ More visually hierarchical
✓ Still recognizably Yomitsu
✓ Still content-first
✓ Still minimal

The user should NOT feel:

"This is a completely different application."

The user SHOULD feel:

"This is the same application, but it finally has a proper, polished visual structure."

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# IMPORTANT FINAL RULE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Do not make design decisions merely because they look trendy.

Every visual change must have a clear purpose:

- Improve hierarchy
- Improve navigation
- Improve readability
- Improve alignment
- Improve discoverability
- Improve consistency
- Improve interaction safety

If a visual element does not improve one of these things, strongly consider not adding it.

Minimalism is not emptiness.

Minimalism is deliberate structure.
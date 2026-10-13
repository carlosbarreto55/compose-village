## ADDED Requirements

### Requirement: Color tokens
The game's screens SHALL take every color from one named token set, the cozy palette: `ground`, `groundRaised`, `surface`, `outline`, `ink`, `inkMuted`, `wood`, `woodDark`, `primary`, `primaryShadow`, `error`, `errorContainer`, `onErrorContainer`, `glow`, `chip`, and the editor tokens `editorBg`, `editorDivider`, `editorText`, `lineNumber`, `syntaxKeyword`, `syntaxString`, `syntaxFunction`, `syntaxComment`, `syntaxAnnotation`. The values SHALL be those in `design-docs/cozy/README.md`. The parchment surfaces SHALL use the light palette whatever the system appearance is, and the code editor SHALL use the dark editor palette. Body text on its background SHALL meet a contrast ratio of at least 4.5:1.

#### Scenario: System in dark mode
- **WHEN** the game starts while macOS is in dark appearance
- **THEN** the app background is `ground` (#F3EADB) and the editor background is `editorBg` (#2A2320)

#### Scenario: Muted text contrast
- **WHEN** `inkMuted` text is drawn on `surface`
- **THEN** its contrast ratio is at least 4.5:1

### Requirement: Bundled typography
The game SHALL use three type families: Fredoka for display text, titles and buttons; Atkinson Hyperlegible for body and UI text; JetBrains Mono for code and the console, with code at a line height of 1.75. The font files SHALL ship inside the game with their license, and the game SHALL NOT download fonts at runtime.

#### Scenario: Offline start
- **WHEN** the game starts with no network connection
- **THEN** the Run button label is drawn in Fredoka and the villager's request in Atkinson Hyperlegible

#### Scenario: Fonts in the bundle
- **WHEN** the macOS distributable is built
- **THEN** it contains the six font files and the SIL Open Font License text

### Requirement: Shape and spacing
Cards SHALL use a corner radius of 22 dp, and the editor and terminal panels 18 dp. Buttons, chips and the view switch SHALL be fully rounded. Primary buttons SHALL be 48 dp tall (the completion button 52 dp) with a 3 dp solid bottom shadow in `primaryShadow`. Interactive elements SHALL have a hit area of at least 44 × 44 dp, even where they are drawn smaller.

#### Scenario: Run button
- **WHEN** the level screen is shown
- **THEN** the Run button is a fully rounded pill, 48 dp tall, filled with `primary` and with a 3 dp `primaryShadow` bottom edge

### Requirement: Villager profession icons
The game SHALL have one glyph for each villager profession: baker, florist, chef, teacher, boat captain, fishmonger, mail carrier and mayor. A villager badge SHALL show the glyph stroked in `woodDark`, on a circle filled with the profession's tint and ringed 3 dp in `wood`. The tints are the ones in the README's villager table. A locked badge SHALL keep the glyph, fill with #E2D6C2, ring with #A8927A, and draw at 55% opacity. The glyphs SHALL be built into the game as vectors, so they stay sharp at any size and need no image decoding.

#### Scenario: Tom's badge
- **WHEN** the villager card for Tom the florist is shown
- **THEN** the badge shows the florist glyph on #F7D3DA with a `wood` ring

### Requirement: Motion and reduced motion
The game's transitions SHALL follow the cozy motion storyboard's timings:
- **Run press:** 80 ms to 0.96 scale.
- **Result:** 200 ms cross-fade.
- **Checklist:** a 120 ms stagger per row, a 200 ms color change and a 180 ms check stroke.
- **Failure:** one 300 ms shake of ±6 dp, then a 220 ms expand of the reason.
- **Completion:** a 600 ms glow, then the completion card sliding up 24 dp and fading in over 300 ms.
- **Panel unlock:** a 250 ms expand and fade, then two 600 ms pulse rings.

When the operating system asks for reduced motion, the game SHALL skip the shake and the pulse rings, and keep the color changes and fades. A launch option SHALL be able to force reduced motion on or off.

#### Scenario: Reduced motion on macOS
- **WHEN** "Reduce motion" is on in macOS Accessibility settings and a requirement fails after a Run
- **THEN** the failed row turns red and its reason expands without shaking

#### Scenario: Forcing reduced motion
- **WHEN** the game is started with reduced motion forced on
- **THEN** opening a level that unlocks a panel shows the panel and its tip without pulse rings

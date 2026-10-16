## MODIFIED Requirements

### Requirement: Color tokens
Every city theme SHALL define one named token set: `ground`, `groundRaised`, `surface`, `outline`, `ink`, `inkMuted`, `accent`, `accentStrong`, `primary`, `onPrimary`, `primaryShadow`, `error`, `errorContainer`, `onErrorContainer`, `glow`, `chip`, and the editor tokens `editorBg`, `editorDivider`, `editorText`, `lineNumber`, `syntaxKeyword`, `syntaxString`, `syntaxFunction`, `syntaxComment`, `syntaxAnnotation`. It SHALL also define the in-between shades the screens use. The game's screens SHALL take every color from the current theme's tokens, scenery or badge colors. The code editor SHALL use the theme's editor tokens.

In the cozy theme, the values SHALL be those in `design-docs/cozy/README.md`. `accent` is the former `wood` (#7A4E2D), `accentStrong` the former `woodDark` (#5A3820), and `onPrimary` is #FFFFFF. The cozy theme is light, and its editor is dark.

#### Scenario: System in dark mode
- **WHEN** the game starts with the cozy theme while macOS is in dark appearance
- **THEN** the app background is `ground` (#F3EADB) and the editor background is `editorBg` (#2A2320)

#### Scenario: Run button label
- **WHEN** the level screen is shown with the cozy theme
- **THEN** the Run button's label is drawn in `onPrimary` (#FFFFFF) on `primary` (#2F6B45)

### Requirement: Bundled typography
Every theme SHALL use Atkinson Hyperlegible for body and UI text, and JetBrains Mono for code and the console, with code at a line height of 1.75. Each theme SHALL choose its own display family for display text, titles and buttons. The cozy theme's display family is Fredoka. Every font file a theme uses SHALL ship inside the game with its license, and the game SHALL NOT download fonts at runtime.

#### Scenario: Offline start
- **WHEN** the game starts with no network connection
- **THEN** the Run button label is drawn in Fredoka and the villager's request in Atkinson Hyperlegible

#### Scenario: Fonts in the bundle
- **WHEN** the macOS distributable is built
- **THEN** it contains the six font files and the SIL Open Font License text

### Requirement: Shape and spacing
Each theme SHALL define the shapes of cards, the editor and terminal panels, rows, buttons, chips and the view switch, and the primary button's shadow. In the cozy theme:
- cards use a corner radius of 22 dp, and the editor and terminal panels 18 dp;
- buttons, chips and the view switch are fully rounded;
- primary buttons have a 3 dp solid bottom shadow in `primaryShadow`.

In every theme, primary buttons SHALL be 48 dp tall (the completion button 52 dp). Interactive elements SHALL have a hit area of at least 44 × 44 dp, even where they are drawn smaller.

#### Scenario: Run button
- **WHEN** the level screen is shown with the cozy theme
- **THEN** the Run button is a fully rounded pill, 48 dp tall, filled with `primary` and with a 3 dp `primaryShadow` bottom edge

### Requirement: Villager profession icons
The game SHALL have one glyph for each villager profession: baker, florist, chef, teacher, boat captain, fishmonger, mail carrier and mayor. The glyphs SHALL be built into the game as vectors, so they stay sharp at any size and need no image decoding.

Each theme SHALL define its badge colors:
- a tint per profession;
- the ring;
- the glyph stroke;
- the locked fill, the locked ring and the locked opacity.

A villager badge SHALL show the glyph stroked in the theme's glyph color, on a circle filled with the profession's tint and ringed 3 dp in the theme's ring color. A locked badge SHALL keep the glyph and use the locked colors and opacity.

In the cozy theme:
- the tints are the ones in the README's villager table;
- the ring is `accent` and the glyph `accentStrong`;
- a locked badge fills with #E2D6C2, rings with #A8927A and draws at 55% opacity.

#### Scenario: Tom's badge
- **WHEN** the villager card for Tom the florist is shown with the cozy theme
- **THEN** the badge shows the florist glyph on #F7D3DA with an `accent` (#7A4E2D) ring

#### Scenario: Locked badge
- **WHEN** a Not yet shop is shown on the street with the cozy theme
- **THEN** its badge keeps the glyph on #E2D6C2 with an #A8927A ring, at 55% opacity

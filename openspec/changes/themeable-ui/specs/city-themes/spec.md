## ADDED Requirements

### Requirement: City themes
The game SHALL draw its screens from a city theme. A city theme SHALL provide:
- an id and a name;
- whether it is light or dark;
- the full color token set (`visual-style`), including the in-between shades the screens use;
- its display font family;
- its shapes and the primary button's shadow;
- its street scenery: the sky, the ground, the road, a shop front for each shop state, and the Sandbox front;
- its villager badge colors;
- its brand mark.

A theme SHALL NOT leave any of these out. The game SHALL apply one theme at a time to the home screen, the level screen, the Sandbox and the dialogues. The cozy theme (`cozy`) SHALL be the only theme offered to players, and with it every screen SHALL look as it did before themes existed.

#### Scenario: Launching the game
- **WHEN** the player launches the game
- **THEN** the home screen, levels, Sandbox and dialogues are drawn with the cozy theme

#### Scenario: Cozy looks unchanged
- **WHEN** the window check runs with the cozy theme
- **THEN** each state's screenshot matches the screenshot of the same state taken before themes existed

### Requirement: The theme decides light or dark
The game SHALL build its Material 3 color scheme from the current theme's tokens: a light scheme for a light theme and a dark scheme for a dark theme. The system appearance SHALL NOT change the game's colors.

#### Scenario: System in dark mode
- **WHEN** the game starts with the cozy theme while macOS is in dark appearance
- **THEN** the app background is the cozy `ground` (#F3EADB)

#### Scenario: A dark theme
- **WHEN** a screen is drawn with the tracer theme, which is dark
- **THEN** Material 3 components on it, such as the overlay checkbox, use a dark scheme built from the tracer tokens

### Requirement: Contrast in every theme
In every theme, each of these text and background pairs SHALL have a contrast ratio of at least 4.5:1:
- `ink` on `surface`, and `ink` on `ground`;
- `inkMuted` on `surface`, and `inkMuted` on `ground`;
- `onPrimary` on `primary`;
- `onErrorContainer` on `errorContainer`;
- `editorText` on `editorBg`.

An automated test SHALL check every theme the game defines, the tracer included.

#### Scenario: A theme with low contrast
- **WHEN** a theme's `inkMuted` on `surface` is below 4.5:1
- **THEN** the theme contrast test fails, naming the theme and the pair

#### Scenario: Cozy muted text
- **WHEN** the contrast test checks the cozy theme
- **THEN** `inkMuted` on `surface` passes at 4.5:1 or more

### Requirement: Colors only come from themes and the cast
The game's UI code SHALL NOT contain color literals outside the theme definitions and the villager cast. An automated test SHALL scan the app's UI sources and fail on any other color literal, naming its file and line.

#### Scenario: A stray color
- **WHEN** a UI file outside the theme definitions and the cast draws with `Color(0xFFF6E7CE)`
- **THEN** the color literal test fails, naming that file and line

### Requirement: Tracer theme
The game SHALL define a tracer theme for checking that every surface follows the theme. It SHALL be dark, use square corners, take JetBrains Mono as its display family, and use colors that differ visibly from every cozy token. The window check SHALL accept `--theme tracer` and save each state's screenshot drawn with it. The tracer theme SHALL NOT be reachable from the packaged game.

#### Scenario: Tracing the screens
- **WHEN** the window check runs with `--theme tracer`
- **THEN** it saves a screenshot of every state, drawn with the tracer theme, to its own folder

#### Scenario: Players never see the tracer
- **WHEN** the packaged game is launched with any options
- **THEN** it is drawn with the cozy theme

### Requirement: Cozy values in other specs
Where a requirement names a token, it SHALL mean that token in the current theme. Where a requirement names a literal color, a corner radius, a shadow or the Fredoka family for a surface the theme draws, it SHALL describe the cozy theme, and other themes SHALL use their own values for that surface. Sizes, spacing, layout, text and motion SHALL be the same in every theme.

#### Scenario: Dialogue name tab in another theme
- **WHEN** the dialogue box is drawn with the tracer theme
- **THEN** its name tab is filled with the tracer's `accent` and has the tracer's square corners, with the villager's name in the tracer's display family at 18 sp

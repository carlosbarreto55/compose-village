## Why

The game is going to grow from one street into several cities, each with a UI theme completely different from the last: the cozy parchment of Main Street, then (for example) a neon Night Market, a nautical Harbor Town, a pixel-art Arcade Row and an art-deco City Hall (see the "Compose Village — City Themes" canvas in Claude Design). Today the cozy look is welded into the code, so none of that is possible:
- the color tokens have one hard-coded instance;
- fonts and shapes are global objects;
- the street's sky, hill, road and shop fronts are private hex constants;
- badge tints and locked colors are fixed;
- Material 3 gets a light scheme only;
- the `visual-style` spec pins every value to `design-docs/cozy/README.md`.

Cities also bring many new villagers. Each level needs its own character, and nothing stops two levels from sharing one. A villager without a bust silently falls back to their profession badge, so a missing avatar would ship unnoticed.

This change makes the look swappable before any second city exists, so the cities change can be about content and navigation, not plumbing.

## What Changes

- **City themes.** A `CityTheme` bundles everything that makes a city look like itself:
  - color tokens;
  - display font;
  - shapes and the primary button's shadow;
  - light or dark;
  - the street scenery (sky, ground, road, shop fronts, the Sandbox front);
  - villager badge colors;
  - the brand mark.

  `VillageTheme` takes a `CityTheme` and provides it through composition locals. Cozy becomes the first and only shipped theme, `CityThemes.Cozy`.
- **No visual change.** With the cozy theme, every screen renders as it does today. The window check's screenshots are the proof: they are compared against a baseline captured before the refactor.
- **Fonts, shapes and colors come only from the theme.** `VillageShapes` and the display font move from global objects into the theme. Every color literal in the UI moves into the theme, its scenery or the villager cast, and a test keeps new ones out.
- **Role-based token names.** `wood` and `woodDark` describe cozy materials, not roles. They become `accent` and `accentStrong`. A new `onPrimary` token replaces the hard-coded white on primary buttons, because some themes need dark text on a bright primary.
- **Dark themes are allowed.** A theme declares whether it is dark, and the Material 3 scheme is built to match. The game still never follows the system appearance: the city decides.
- **Contrast per theme.** The 4.5:1 body text rule becomes a test that runs on every registered theme, for each text/background token pair.
- **Body and code type stay shared.** Atkinson Hyperlegible and JetBrains Mono are the same in every city. A theme only picks its display family, which must be bundled with its license.
- **A tracer theme for checking.** A deliberately loud, dark, square-cornered theme, used only by the window check (`--theme tracer`), shows any surface still drawn with cozy values. It is never offered to players.
- **One villager, one bust, per level.** Busts move into a cast registry, one entry per villager id, separate from the shared rig. Two new rules:
  - no two bundled levels may share a villager id, which loading rejects;
  - every bundled level's villager must have their own bust, which a test enforces.

  The badge fallback stays only for levels that aren't bundled, such as test fixtures and work in progress.

## Capabilities

### New Capabilities
- `city-themes`: what a city theme provides and how it is applied. Cozy is the first theme. Every theme supplies every token, its display font, shapes, scenery and badge colors. Contrast is checked for each theme, and the theme, not the system, decides light or dark. There is also the tracer theme for the window check, and the guard against color literals outside themes and the cast. Literal cozy values in other specs describe the cozy theme.

### Modified Capabilities
- `visual-style`:
  - Color tokens are defined per theme, with `wood`/`woodDark` renamed to `accent`/`accentStrong` and `onPrimary` added. The cozy values stay those in `design-docs/cozy/README.md`.
  - Typography: the display family comes from the theme, while body and code are shared.
  - Shape and spacing values come from the theme, with the cozy values unchanged.
  - Villager badges take their colors from the theme.
- `villager-dialogue`: busts come from a cast keyed by villager id. Every bundled level's villager must have their own bust. The badge fallback is only for villagers of levels that aren't bundled. The dialogue box uses `accent`/`accentStrong`.
- `level-definition`: loading rejects an index in which two levels share a villager id.
- `home-screen`: the street card's sky, clouds, hill, road and buildings are drawn by the theme's scenery, with the cozy values unchanged. Shop states use `accent` instead of `wood`.
- `level-screen`: the header's progress dots and the street footer use `accent` instead of `wood`. The footer scenario's count is updated to five levels.

## Impact

- **`app`:**
  - In `ui/theme/`, `Theme.kt` splits into `CityTheme`, the cozy theme, shapes and scenery.
  - Every UI file that reads `VillageShapes`, `VillageFonts`, `wood` or `woodDark`, or has a color literal: `Panels`, `Common`, `HomeCards`, `StreetCard`, `Fronts`, `Header`, `CodeEditor`, `Checklist`, `Villager`, `BrandMark`, `VillagerCard`, `Dialogue`, `SpikeScreen`.
  - `ui/villagers/Bust.kt` splits into the rig and a cast.
  - The window check gets `--theme` and a screenshot comparison.
  - New tests: theme contrast, a color-literal guard, one bust per bundled villager.
- **`levels`:** a duplicate villager check in `LevelRepository`, with a test.
- **`design-docs/cozy/README.md`:** token names are updated to `accent`/`accentStrong`/`onPrimary`. A note says the cozy look is the first city theme.
- **No change** to level data, saved progress, player code compilation or the preview runtime.
- **Dependencies:** `main-street-levels` is complete but not archived. It must be archived first, because this change's `home-screen` delta builds on its version of the Street card requirement.
- **Non-goals:**
  - cities in level data, a city picker or map, and city-aware text ("Main Street" stays);
  - any second shipped theme;
  - per-theme motion, sounds or layouts;
  - restyling busts per city;
  - changing how the player's own preview renders.

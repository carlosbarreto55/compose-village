## Context

The cozy look is the game's only look, and it is spread across the code:

```
                    VillageTheme { … }                     today
                           │
        ┌──────────────────┼────────────────────┐
        ▼                  ▼                    ▼
  LocalVillageColors   LocalVillageType     LocalReducedMotion
  VillageColors()      VillageType()          (unchanged)
  one default instance fonts fixed in defaults

  outside the theme:
  VillageShapes (object)    ~25 direct uses      corners fixed
  VillageFonts (object)     ~35 direct uses      fonts fixed
  StreetCard.kt, Fronts.kt  private hex values   scenery fixed
  Villager.kt               tints + locked hex   badges fixed
  BrandMark.kt              2 hex                brand fixed
  HomeCards.kt              Sandbox card hex
  Color.White               on primary buttons, checks, bubble (~12 uses)
  lightColorScheme only                          no dark cities
  Bust.kt                   five looks + rig in one file, 54 hex
```

The next step is cities, each with a completely different look. The "Compose Village — City Themes" canvas in Claude Design explores five:
- Main Street: cozy parchment.
- Night Market: dark neon with cut corners.
- Harbor Town: navy and sailcloth with slab type.
- Arcade Row: pixel art with hard shadows.
- City Hall: black-and-gold art deco.

Those boards show what a theme has to be able to change:
- the palette, including a dark palette with dark text on a bright primary;
- the display font;
- corners and the button's shadow;
- the badge treatment;
- the scenery drawn on the street, which differs most of all.

Each city also brings its own villagers, and every level needs a character of its own.

This change does the plumbing and keeps the screen exactly as it looks today. Cities in level data, navigation between them and a second shipped theme come later.

## Goals / Non-Goals

**Goals:**
- With the cozy theme, every window-check screenshot matches the one taken before the change.
- Everything a city's look depends on is reachable from one `CityTheme` value: colors, display font, shapes, button shadow, scenery, badges, brand mark, light or dark.
- A second theme can be added by writing a new `CityTheme`, without editing screen code. The tracer theme proves this.
- No two bundled levels share a villager, and every bundled villager has their own bust. Both are enforced by tests, not review.

**Non-Goals:**
- Cities in `index.json`, a city map or picker, city-aware text, locking cities.
- Shipping a second player-facing theme.
- Per-theme motion, sound, layout or spacing.
- Busts drawn in a city's art style (for example, pixel busts). The cast is organized so that this can come later.
- Theming the player's own preview or the target PNGs. They show the player's UI, not the game's.
- Restyling the headless spike reports.

## Decisions

### D1. One `CityTheme` value, one composition local
```kotlin
class CityTheme(
    val id: String, val name: String, val dark: Boolean,
    val colors: VillageColors,        // tokens + in-between shades
    val display: FontFamily,          // body and code stay shared (D4)
    val shapes: VillageShapes,        // now a class (D5)
    val scenery: Scenery,             // street drawing (D2)
    val badges: BadgeColors,          // tints per profession, ring, glyph, locked (D6)
    val brandMark: ImageVector,
)
```
`VillageTheme(theme: CityTheme = CityThemes.Cozy, reducedMotion: …)` provides it through a single `LocalCityTheme`. `Village.colors`, `Village.type`, `Village.shapes`, `Village.scenery` and `Village.badges` read from it, so most call sites change from `VillageShapes.card` to `Village.shapes.card` and nothing else. `VillageType` is built from the theme's display family once per theme, not per call.

*Alternative:* one composition local per part (colors, type, shapes, scenery). Rejected. The parts always change together, and the theme is the unit the cities change will select. Separate locals would allow mixed themes nobody wants. `LocalReducedMotion` stays separate, because it is a player setting, not part of a city.

### D2. Scenery is drawing code behind an interface, not parameters
```kotlin
interface Scenery {
    fun DrawScope.sky()                       // behind the title block
    fun DrawScope.ground()                    // behind the buildings (cozy: the hill)
    @Composable fun Road()
    @Composable fun ShopFront(state: ShopState, profession: Profession)
    @Composable fun SandboxFront()
}
```
`CozyScenery` is today's `StreetCard`/`Fronts` drawing, moved together with its private colors, which become the scenery's own. `StreetCard` keeps the layout (title block, `FlowRow`, buildings as buttons, statuses, dashed current outline) and asks the scenery to draw the parts.

*Alternative:* parametric scenery, where every city gets gable-roofed shops with its own colors. Rejected. The design boards show market stalls, harbour houses with a lighthouse, pixel buildings and a deco hall. Recoloring the bakery front would make every city look like Main Street in a costume, which is the opposite of the goal. The building's size (150 × 150 dp) and the street's layout stay shared, so text and hit areas don't move between cities.

### D3. Rename `wood`/`woodDark` to `accent`/`accentStrong`, and add `onPrimary`
These tokens are used for the sign frame, badge rings, the brand mark, dashed outlines, the dialogue border and headings. "Wood" is what they look like in the cozy theme, not what they do, and a neon city's `wood` would be cyan. The rename is mechanical, with the same values. `onPrimary` replaces `Color.White` on primary buttons and their glyphs, because the neon board's magenta primary needs dark text for 4.5:1.

The specs that name `wood` are updated in this change (the `visual-style`, `villager-dialogue`, `home-screen` and `level-screen` deltas), and so is the cozy README.

Other `Color.White` uses get named tokens as the guard (D8) finds them, for example the reaction bubble's fill and the selected view switch. Each new token gets its cozy value from what the code draws today.

*Alternative:* keep the names. Rejected because every future theme would carry a misleading name forever, and renaming is cheapest now, while only one theme exists.

### D4. Only the display family varies; body and code are shared
`VillageFonts.Atkinson` and `VillageFonts.JetBrainsMono` stay global. Every `VillageType` style that used Fredoka uses `theme.display` instead, and the Material 3 typography is built the same way. The players read requests, failure details and code closely, so those stay in the families chosen for legibility. Cities express themselves through display type, color, shape and scenery.

The tracer uses JetBrains Mono as its display family, so it needs no new font file and the bundle keeps exactly six fonts.

*Alternative:* themes choose all three families. Rejected for legibility. Pixel or deco faces are fine for a 24 sp title and poor for a three-line failure message.

### D5. `VillageShapes` becomes a class on the theme; spacing stays shared
`VillageShapes(card, editor, sign, row, button, chip, viewSwitch, buttonShadow: DpOffset)`. The cozy values are 22/18/18/14/pill/pill/pill and `DpOffset(0.dp, 3.dp)`. The shadow's color stays the `primaryShadow` token.

Page padding and gaps (`pagePaddingH`, `cardGap`, …) move to a shared `VillageSpacing` object, because layout is the same in every city.

Glows, cut corners and double frames from the design boards aren't modeled yet. Each will be added to the theme when the city that needs it arrives, to avoid designing for guesses.

### D6. Badges take their colors from the theme
`Profession.tint` becomes `BadgeColors.tint(profession)`, beside `ring`, `glyph`, `lockedFill`, `lockedRing` and `lockedAlpha`. `VillagerBadge` and `ProfessionPill` read `Village.badges`. The cozy values are the README table, with `accent` for the ring, `accentStrong` for the glyph, and #E2D6C2, #A8927A and 0.55 for locked badges. The glyph shapes stay shared, since they say what a villager does, not where they live.

### D7. Light or dark comes from the theme
`VillageTheme` builds `darkColorScheme(…)` when `theme.dark` is true, and `lightColorScheme(…)` otherwise, from the same token mapping as today plus `onPrimary`. Nothing reads `isSystemInDarkTheme()`.

### D8. A source scan keeps color literals in themes and the cast
An `app` unit test walks `app/src/main/kotlin/village/app/ui/**`. It fails on `Color(0x…)` and on `Color.White`/`Color.Black`, except in `ui/theme/**` and `ui/villagers/cast/**`, and reports file and line. `Color.Transparent` and `Color.Unspecified` are allowed. `spike/` is outside `ui/` and isn't scanned.

*Alternative:* a detekt rule. Rejected because it adds a tool and its configuration for one check. A 30-line test does the job.

### D9. The tracer theme proves the abstraction
`CityThemes.Tracer` is defined in the main source set next to Cozy, so the window check (part of `app`) can use it. Its properties:
- dark;
- square corners;
- JetBrains Mono as its display family;
- loud colors, different from every cozy token;
- a deliberately crude scenery: flat rectangles, so it's obvious which code drew it.

`CityThemes.shipped = listOf(Cozy)` is what the game uses, and `CityThemes.all = listOf(Cozy, Tracer)` is what the tests iterate. Nothing in `Main.kt` or the launch options can select a theme. Only the window check's `--theme <id>` can.

Seeing a cozy color in a tracer screenshot means a surface is still hard-coded. That catches what the source scan can't, such as a value computed from a cozy constant in a theme file but used on every theme.

### D10. Proving "no visual change" with a screenshot comparison
The window check gets two options:
- `--baseline <dir>` copies each state's PNG there;
- `--compare <dir>` reports how many pixels differ per state and writes a diff image for each state that differs.

The first task captures the baseline from untouched `main` into a folder outside `build/` that `clean` won't remove (`app/spike-baseline/`, git-ignored). Every later group of tasks ends with a comparison.

The window check renders with real clocks, and a few states may catch a caret or animation frame. So the target is zero differing pixels, and any state that differs has to be explained (for example "caret blink") in that task's commit message before moving on.

### D11. A cast, separate from the rig
`Bust.kt` splits into two parts:
- `ui/villagers/Rig.kt`: `Face`, `VillagerLook`, the drawing and animation of eyes, mouth, bob and blink, plus `VillagerBust`/`VillagerPortrait`.
- `ui/villagers/cast/`: one file per villager (`Rosa.kt`, `Tom.kt`, …) and `Cast.kt` with `Cast.forId(id)` and `Cast.ids`.

The character's colors live with the character, because Iara's navy coat is Iara's, not the city's. That is why the cast is exempt from the color scan.

A new city's characters are new files in `cast/`. Grouping them per city (`cast/harbor/…`) can wait until there is a second city.

### D12. Distinct villagers: loader for data, app test for busts
- **`levels`:** `LevelRepository` rejects an index in which two levels have the same villager id, naming both folders. The levels module knows nothing about busts, so this check is about data only.
- **`app`:** a test loads the bundled levels and checks that `Cast.forId(level.villager.id)` exists for each. It fails naming the level and the id.

Together, the two checks mean "every level has its own character, and that character has a face."

The badge fallback in `VillagerBust`/`VillagerPortrait` stays, so authors can play a new level before its bust is drawn. The test stops such a level from shipping.

## Risks / Trade-offs

- **[Large mechanical diff over every UI file]** → Tasks go one area at a time: theme core, then shapes, then fonts, colors, scenery, badges and cast. Each group ends with a screenshot comparison against the baseline, so a regression shows up in the step that caused it.
- **[`Scenery` is shaped by one example and may not fit the next city]** → The tracer gives a second, very different implementation. Changing the interface when the first real city arrives is expected and cheap, because there will only be two or three implementations.
- **[Screenshot noise from real clocks]** → Differences are reported per state with diff images, and must be explained, not ignored (D10). If noise turns out to be common, freezing the clock in the window check is a small follow-up.
- **[The source scan is textual]** → It can miss a color built another way (`Color(red = …)`). The tracer screenshots (D9) are the second net. Both together are enough for one team.
- **[Spec drift: many requirements still say Fredoka or quote cozy hex values]** → The `city-themes` requirement "Cozy values in other specs" defines how to read them, instead of rewriting a dozen requirements now.
- **[`main-street-levels` isn't archived]** → Archive it before this change, so that the `home-screen` delta (Street card) applies to the five-level text.

## Migration Plan

There is no data migration: saved progress, level files and launch options are untouched. To roll back, revert the branch. The theme is a compile-time default, and there is no stored setting.

## Open Questions

- When cities arrive, does the Sandbox take the theme of the last city visited, or a fixed theme? This doesn't block this change, because there is only cozy.
- Should a city also be able to restyle busts, for example pixel busts in Arcade Row? D11 leaves room for it, but the rig's line weights and face proportions would need to come from the theme.

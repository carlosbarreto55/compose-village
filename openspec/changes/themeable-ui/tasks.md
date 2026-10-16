## 1. Groundwork

- [x] 1.1 Archive `main-street-levels` (implemented, all tasks done, merged), so this change's `home-screen` and `level-screen` deltas apply to its versions; run `openspec validate themeable-ui`
- [x] 1.2 Window check options `--baseline <dir>` (copy each state's PNG there) and `--compare <dir>` (per state, the number of differing pixels, plus a diff PNG for each state that differs); git-ignore `app/spike-baseline/`
- [x] 1.3 Capture the baseline from the untouched code into `app/spike-baseline/`, then compare a second run against it and record any noisy states (caret, animation frames) in the commit message

## 2. Theme core

- [x] 2.1 Add `CityTheme` (id, name, dark, colors, display, shapes, scenery, badges, brand mark; start with today's values), `LocalCityTheme` and `CityThemes.Cozy`; `VillageTheme(theme = CityThemes.Cozy, reducedMotion)` provides it, and `Village.colors`/`Village.type` read from it; compare with the baseline
- [x] 2.2 Build the Material 3 scheme from the theme: `darkColorScheme` when `theme.dark`, `lightColorScheme` otherwise; add the `onPrimary` token (cozy #FFFFFF) and map it
- [x] 2.3 Rename the `wood` and `woodDark` tokens to `accent` and `accentStrong` across `app`; compare with the baseline

## 3. Shapes and type

- [x] 3.1 Turn `VillageShapes` into a class on the theme (card, editor, sign, row, button, chip, view switch, button shadow offset) with the cozy values, and move page padding and gaps into a shared `VillageSpacing`; replace direct `VillageShapes.` uses with `Village.shapes`; compare with the baseline
- [x] 3.2 Draw primary buttons' shadow from `Village.shapes.buttonShadow` in `primaryShadow` (Run, Start/Continue, Next, dialogue buttons); compare with the baseline
- [x] 3.3 Build `VillageType` and the Material 3 typography from the theme's display family, keeping Atkinson Hyperlegible and JetBrains Mono shared; replace direct `VillageFonts.Fredoka` uses; compare with the baseline

## 4. Colors from the theme

- [x] 4.1 Replace `Color.White` on primary buttons, their glyphs and the passed-check marks with `onPrimary`
- [x] 4.2 Add tokens for the remaining literal colors on screens (reaction bubble fill, selected view switch, "Matches sketch" badge, Sandbox card text and line), with cozy values equal to what is drawn today; compare with the baseline
- [x] 4.3 Add `BadgeColors` to the theme (tint per profession, ring, glyph, locked fill, locked ring, locked opacity) with the cozy values; `VillagerBadge` and `ProfessionPill` read `Village.badges`; remove `Profession.tint`; compare with the baseline
- [x] 4.4 Move the brand mark onto the theme (`CityThemes.Cozy.brandMark` is today's house); the header reads it from the theme

## 5. Scenery

- [x] 5.1 Add the `Scenery` interface (sky, ground, road, shop front per state, Sandbox front) and `CozyScenery`, moving the drawing and private colors from `StreetCard.kt` and `Fronts.kt`; `StreetCard` keeps the layout and asks `Village.scenery` to draw; compare with the baseline
- [x] 5.2 Move any street drawing left in the level screen's footer onto theme tokens or the scenery; compare with the baseline

## 6. One villager, one bust

- [ ] 6.1 Split `Bust.kt` into `ui/villagers/Rig.kt` (face, look, drawing, blink, bob, `VillagerBust`, `VillagerPortrait`) and `ui/villagers/cast/` (one file per villager, plus `Cast.kt` with `forId` and `ids`); compare with the baseline
- [ ] 6.2 In `LevelRepository`, reject an index in which two levels share a villager id, with an error naming both folders and the id; loader tests for two levels with `rosa` and for the five bundled levels loading
- [ ] 6.3 App test: every bundled level's villager has a bust in the cast (failing names the level and the id), and a non-bundled villager without a bust still gets their profession badge

## 7. Guards and the tracer theme

- [ ] 7.1 Color literal test: scan `app/src/main/kotlin/village/app/ui/**` outside `ui/theme/` and `ui/villagers/cast/` for `Color(0x…)`, `Color.White` and `Color.Black`, failing with file and line; fix anything it finds
- [ ] 7.2 Add `CityThemes.Tracer`: dark, square corners, JetBrains Mono as the display family, loud colors that differ from every cozy token, crude rectangle `TracerScenery` and its badge colors; `CityThemes.shipped = [Cozy]` and `CityThemes.all = [Cozy, Tracer]`
- [ ] 7.3 Theme contrast test over `CityThemes.all`: `ink` and `inkMuted` on `surface` and `ground`, `onPrimary` on `primary`, `onErrorContainer` on `errorContainer`, and `editorText` on `editorBg` are all at least 4.5:1; a failure names the theme and the pair
- [ ] 7.4 Window check `--theme <id>`, saving to `app/build/spike/window-<id>/`; run it with `tracer`, and fix every surface still drawn with cozy values until the tracer screenshots show none
- [ ] 7.5 Test that the game only uses shipped themes: `VillageTheme`'s default is Cozy, `CityThemes.shipped` holds only Cozy, and `Main.kt` passes no other theme

## 8. Docs and wrap-up

- [ ] 8.1 Update `design-docs/cozy/README.md`: the `accent`, `accentStrong` and `onPrimary` token names, and a note that cozy is the first city theme, drawn through `CityTheme`
- [ ] 8.2 README: mention `--theme tracer` and `--compare` next to the window check
- [ ] 8.3 Run `./gradlew build` and a final window-check comparison against the baseline (zero differing pixels, or every difference explained), then `openspec validate themeable-ui --strict`

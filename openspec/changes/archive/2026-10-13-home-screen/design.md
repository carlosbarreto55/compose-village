## Context

`Main.kt` builds one `SpikeController` and always shows `SpikeScreen`. The controller starts with `level = null`, so the game opens in the Sandbox with `Samples.BAKERY`. The only street is the 44 dp footer of the level screen. `village-progress` gives the controller a saved `Progress` (completion, tries, lines and draft per level id), and that is almost everything the home mockups need.

The mockups are `design-docs/cozy/screens/04-home-first-launch.html` and `05-home-welcome-back.html`, plus the README's "Home screen" section. Their buildings are a 150 × 150 facade. The level shops all share one shape: a gable roof, a sign band, an awning stripe in the villager's tint, a window and a door. The colors change by state: lit, or dark at `#EFE3CF` / `#B8A58C` / `#8A6A4E`. The Sandbox has a different, flat-roofed facade with code braces in the doorway.

## Goals / Non-Goals

**Goals:**
- The game opens on Main Street. Starting, continuing and visiting levels all start there.
- The home screen is derived only from the bundled levels, saved progress and a small placeholder list, so it is right after any Run, restart or level switch.
- One compiler and preview engine are shared across home, levels and Sandbox, so going home never makes the next Run cold.

**Non-Goals:**
- Talking villagers, busts and the level intro (`talking-villagers`). Home uses the existing `VillagerBadge` where the mockups show a face.
- The "hints" stat (`hints-and-editor-aids`).
- Locked levels. "Not open yet" shops stay clickable.
- Animating the street (lights turning on as you arrive). The lit state changes with the existing 300 ms color fade only.

## Decisions

### D1. A `screen` value in the controller, not a navigation library
`SpikeController` gains `var screen by mutableStateOf(Screen.HOME)`, with `enum class Screen { HOME, PLAY }`:
- `PLAY` uses the existing `level` (null is the Sandbox).
- `goHome()` saves the draft (like switching levels), stops showing the preview and sets `HOME`. It is ignored while a Run is in progress.
- `open(level)` sets `PLAY`.

`open()` currently returns early when `level == this.level`. That guard has to let "home → the same level" through: it switches the screen back without resetting the visit. It also has to let "home → Sandbox" through on first launch, where `level` is already null.

`Main.kt` shows `HomeScreen(controller)` or `SpikeScreen(controller)` in a 200 ms `Crossfade` on `screen`. Under reduced motion it switches instantly.

*Alternatives:*
- A nullable `Destination` sealed type replacing `level`. It is cleaner, but it touches every `controller.level` reader for no behavior gain.
- A navigation library. Two screens don't need one.

### D2. Home state is a pure function
`homeModel(levels, progress, upcoming): HomeModel` lives in `app/Home.kt`. It returns:
- `firstLaunch`: no level completed and no level with a draft;
- `buildings`: for each one, the shop, villager, "<n> · <title>", a `ShopState` (OPEN, CURRENT_START, CURRENT_NEXT, NOT_YET, COMING_SOON) and the level, or null for a placeholder;
- `current`: the first incomplete bundled level, or null when all are open;
- `openShops`: the completed levels, each with its saved lines and tries;
- `shopsOpen` and `shopsTotal`.

Composables only render it. This keeps every rule in the `home-screen` spec testable in `HomeTest` without a window, like `GameTextTest` and `ProgressTest`.

The "Welcome back" line also comes from the model: "<latest open shop> is open. <current villager> is still waiting for their sign." The latest open shop is the last completed one in play order, because the progress file has no timestamps.

### D3. Placeholder shops live in the app
An `Upcoming` list in `app/Home.kt` holds `UpcomingShop(villager, profession, shop, title)` entries for Marco (chef), Dona Lúcia (teacher) and Iara (boat captain).
- The model drops an entry when a bundled level's `villager.name` matches it.
- It numbers the remaining entries after the last bundled level.
- Placeholders don't count in `shopsTotal`. With two levels bundled, the home and the footer both say "1 of 2", and the mockup's "of 5" becomes true once five levels ship.

*Alternative:* stub entries in `levels/index.json`. That would put unplayable levels in level data, and level validation has to stay strict (`level-definition`).

### D4. Drawing the buildings
One `Building(state, tint, badge)` composable draws the shared facade with `Canvas` on a 150 × 150 dp box, using the mockup's coordinates. Two colors come from the villager:
- the badge on the roof, the existing `VillagerBadge` at 40 dp;
- the awning stripe, `Profession.tint`.

The lit state adds the 35% `glow` ellipse behind the facade and `glow` fills for the sign and window. `SandboxBuilding` draws its own flat facade. The button around a building:
- has a dashed 2 dp `wood` outline and a `groundRaised` background when CURRENT;
- has a 44 dp minimum hit target;
- has a `Role.Button` semantics label: "<shop>, <n> · <title>, <status>".

*Alternative:* importing the SVGs as `ImageVector` resources. That needs one asset per state × shop, and the tint is baked in.

### D5. Saving the requirement count
`LevelProgress` gains `val met: Int = 0`.
- `afterRun(id, passed, lines)` becomes `afterRun(id, passed, lines, met)`. Every counted Run records it, including Runs after completion, so the pick-up card is never stale. Replays don't call `afterRun`, so they don't change it.
- A Run that did not compile or crashed records 0.
- `startOver()` records 0.

The default value keeps old files loading with version 1. kotlinx.serialization already ignores missing fields that have defaults. "M" in "N of M wishes" is `level.criteria.size`, computed at render time, so it follows level content changes such as `customer-bot` adding a fourth criterion.

### D6. Layout
`HomeScreen` is a `Column`:
- `HomeHeader`: the brand, then the "N of M shops open" text once anything is saved, then the build status. It reuses `Header`'s status composable and has no Run button.
- A scrollable body, at most 1440 dp wide and centred.

The body holds:
- `StreetCard`: the title block, then the buildings in a `FlowRow` (centred, 14 dp gaps) so they wrap instead of scrolling, then the 44 dp road.
- Under it, a `FlowRow`-style split. At 1180 dp of content width and above (`TWO_COLUMNS_MIN_WIDTH`, reused), the pick-up or first-villager card (weight 999 in the mockup, so it takes the extra width) sits beside the 420 dp aside. Below it, they stack.

### D7. The breadcrumb link
In `Header`, "Main Street" becomes a `TextAction`-style link (`inkMuted`, underlined on hover, 44 dp target height) calling `controller.goHome()`. The Sandbox breadcrumb becomes "Main Street › Sandbox", so it has the same way home. `StreetFooter` doesn't change. Its "Main Street" label stays plain text, so there is one link home, not two.

## Risks / Trade-offs

- [`open()`'s early return hides the same-level case from home] → D1 handles it explicitly, and `WindowCheck` covers "edit Open Sign, go home, Continue" and "first launch → Open the Sandbox".
- [The preview engine keeps the last level loaded while home is shown] → `goHome()` doesn't unload it. Coming back to the same level shows it as it was, and opening another level resets the visit as usual. That costs nothing in frames, because the preview isn't composed on home.
- [The mockup says "of 5" but the game says "of 2"] → This is intentional (D3). Placeholders read "Coming soon", not "Not open yet", so players don't try to open them.
- [The latest open shop is approximated by play order] → That is correct as long as players finish levels in order. Adding a `completedAt` field later is additive.

## Migration Plan

Archive `cozy-level-screen` and then `village-progress` before applying this change: its deltas modify `Header` and `Saved progress`, which those changes add. Save files need no migration (D5).

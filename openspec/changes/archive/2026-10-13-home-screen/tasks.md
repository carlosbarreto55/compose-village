## 0. Before starting

- [x] 0.1 Archive `cozy-level-screen`, then `village-progress`, so `Header` and `Saved progress` are in the main specs.

## 1. Progress

- [x] 1.1 Add `met: Int = 0` to `LevelProgress`. `afterRun` takes `met` and records it on every counted Run, including after completion, while tries and lines still stop at the first completion. Add a pure `withStartOver(id)` that sets it to 0 (D5).
- [x] 1.2 `SpikeController`: pass the verdict's met count to `afterRun` (0 when it did not compile or crashed), and record 0 in `startOver()`.
- [x] 1.3 `ProgressTest`: met is recorded after completion without changing tries, a file without `met` loads with 0 and stays version 1, Start over resets it.

## 2. Home model

- [x] 2.1 Add `app/Home.kt` with `UpcomingShop`, the `Upcoming` list (Marco, Dona Lúcia, Iara) and `ShopState` (D3).
- [x] 2.2 Add `homeModel(levels, progress, upcoming)`: first launch, buildings with states and numbers, current level, open shops with lines and tries, shops open count, and the welcome back line (D2).
- [x] 2.3 Add the home texts to `GameText`: titles, captions, the step texts, statuses, "Your code is saved", "Starter code is ready", "N of M wishes", "Every shop on the street is open", "Or pick any shop on the street."
- [x] 2.4 `HomeTest`: nothing done (Rosa is "Start here", Tom "Not open yet"), first level done (Rosa open, Tom "Up next"), all done (no current), a draft without completion is not first launch, a placeholder is dropped when a level has its villager, placeholders are numbered after the levels and not counted.

## 3. Navigation

- [x] 3.1 Add `Screen { HOME, PLAY }` and `screen` to `SpikeController`, starting at HOME. Add `goHome()` (saves the draft, ignored while running). `open()` sets PLAY and lets home → same level and home → Sandbox through without resetting the visit (D1).
- [x] 3.2 `Main.kt`: `Crossfade` between `HomeScreen` and `SpikeScreen` on `screen`, instant under reduced motion.
- [x] 3.3 `Header`: "Main Street" becomes a link to `goHome()`, disabled while running. The Sandbox breadcrumb reads "Main Street › Sandbox" (D7).

## 4. Home screen

- [x] 4.1 `ui/home/Building.kt`: the level facade on a `Canvas` for the open and dark states, the roof badge, the tint stripe, and the glow. Also `SandboxBuilding` (D4).
- [x] 4.2 `ui/home/StreetCard.kt`: sky, clouds, hill, road with dashed line, the title block for both forms, and the buildings in a wrapping `FlowRow` as buttons with status pills, dashed current outline, 44 dp targets and semantics labels. Coming soon buildings are not clickable.
- [x] 4.3 `ui/home/HomeCards.kt`: Your first villager (badge, pill, request via `requestText`, chips, Start button, starter note), How a level works, Pick up where you left off (saved/starter note, wishes, Continue, or the all-open form), Open shops with Visit, and the Sandbox card.
- [x] 4.4 `ui/home/HomeScreen.kt`: header with shops open count and build status, scrollable body capped at 1440 dp, two columns at `TWO_COLUMNS_MIN_WIDTH` and above, stacked below (D6).

## 5. Verification

- [x] 5.1 `WindowCheck`:
  - launch shows home in the first-launch form;
  - Start opens `000-hello-main-street`;
  - completing it and clicking "Main Street" shows Rosa lit and "1 of 2 shops open";
  - editing Open Sign, going home and choosing Continue keeps the edit;
  - Visit opens the welcome back form;
  - first launch → Open the Sandbox opens the Sandbox;
  - a new controller on the same progress file shows the welcome back form;
  - at 900 dp wide no building is clipped.
- [x] 5.2 Update the README screenshots section if the launch screenshot is now the home screen.
- [x] 5.3 `./gradlew build` and `./gradlew :app:spike -Pargs="--only window"` pass, and `openspec validate home-screen --strict` passes.

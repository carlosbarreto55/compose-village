## 1. Level data: villager identity

- [x] 1.1 Add the `Profession` enum (json name and player label) to `levels`, plus `Villager(name, profession)`, and add `title`, `villager`, `shop`, `concepts` and `thanks` to `Level`. Remove `resident`.
- [x] 1.2 Update the loader and validation: the new required and optional fields, unknown professions (listing the valid names), blank or repeated concepts, a blank `thanks`.
- [x] 1.3 Update both `level.json` files: titles, villagers, shops, concepts, and Open Sign's thanks line ("Ding! There's my bell. Thank you — the shop is open!").
- [x] 1.4 Loader tests: minimal level defaults, old `resident` field rejected, unknown profession, repeated concept. `./gradlew :levels:test` passes, including the self-checking levels.

## 2. Visual foundation

- [x] 2.1 Vendor the static fonts into `app/src/main/resources/font/` with `OFL.txt`: Fredoka SemiBold and Bold, Atkinson Hyperlegible Regular and Bold, JetBrains Mono Regular and SemiBold.
- [x] 2.2 Add `ui/theme`: `VillageColors` tokens (README values) and `LocalVillageColors`, typography (display, body and code styles), shapes, and `VillageTheme {}`, which also provides a Material 3 light scheme built from the tokens.
- [x] 2.3 Add `LocalReducedMotion`: read `com.apple.universalaccess reduceMotion` on macOS with a timeout, and let `-Dvillage.reducedMotion` override it.
- [x] 2.4 Write `scripts/svg-to-imagevector.py` (strip metadata, circles to arcs). Generate and commit `ui/icons/VillagerIcons.kt` with the 8 profession glyphs and the brand mark.
- [x] 2.5 Add `VillagerBadge(profession, size, locked)` and `ProfessionPill(profession)`, with the profession-to-tint map from the README.
- [x] 2.6 `Main.kt`: wrap the window in `VillageTheme` (light only), title it "Compose Village", set a minimum size of 900 × 640 dp. Update `WindowCheck` to use `VillageTheme` as well.

## 3. Controller state

- [x] 3.1 Add `completed` (session set of level ids), `runsThisVisit`, `dismissedCompletion`, `overlaySketch` (on when a level opens, turned off by the first frame of the opening) and a `buildStatus` value (warming up, ready, building, built in N ms, didn't build).
- [x] 3.2 Replace the 8 s highlight timer with `noteInteraction()`, which ignores input until the pulse has finished. Run and "Got it" still dismiss the tip.
- [x] 3.3 Add `reactionFor(level, verdict)` and the `GameText` lines (did not compile, crashed, see checklist, generic thanks), plus the Lines stat helper (no blank, `import` or `package` lines). Unit tests for both.

## 4. Screen shell

- [x] 4.1 Replace the `SpikeScreen` layout with header, main area and footer. Use `BoxWithConstraints` to switch between two columns and one wrapped, scrolling column at 1180 dp.
- [x] 4.2 Header: brand mark and wordmark, breadcrumb with level number and title and progress dots (sandbox shows "Sandbox"), build status with dot and a hover breakdown, and a pill Run button ("Run" / "Run again", ⌘↵, 3 dp shadow, 0.96 press scale). Show a progress bar along the bottom edge while building.
- [x] 4.3 Street footer: shops (lit, current dashed, hover title), "N of M shops open", and a Sandbox entry. Clicking opens the level; nothing happens while a Run is in progress. Remove the FilterChips toolbar.

## 5. Left column

- [x] 5.1 Villager card: badge avatar, name, profession pill, request with quoted phrases in bold and quotes removed, concept chips before the first Run, and a reaction bubble after a Run.
- [x] 5.2 Editor card: dark surface, `PlayerScreen.kt` tab, JetBrains Mono 15 sp with a 1.75 line height, gutter colors, warm red error band.
- [x] 5.3 Kotlin highlighting `VisualTransformation` (keywords, strings, comments, annotations, numbers, call names) with identity offsets. Tests: token spans for the Open Sign solution, and that caret offsets don't change.
- [x] 5.4 Status line ("No problems" / "Compiles fine" / "N problems") and the problems list moved into the editor card, with click-to-line kept.

## 6. Right column

- [x] 6.1 Preview card: rounded "Your screen | <Name>'s sketch" switch, Overlay sketch checkbox (disabled while the sketch is shown), wooden sign frame (10 dp `wood`, inset `woodDark` ring, 18 dp radius) sized to the viewport.
- [x] 6.2 Overlay rendering: sketch at 40% over the frame or alone with the "This is <Name>'s sketch…" caption. Input passes through the overlay and is blocked only in sketch view. Use a 200 ms cross-fade between frames.
- [x] 6.3 Checklist card: "<Name>'s checklist" and "k of n", drawn marks, failed row expanded in `errorContainer` with the detail (and the message unless the bubble shows it), and a not-run explanation. When the level is complete, collapse it to filled marks.
- [x] 6.4 Completion card: `glow` border, "<shop> is open!", "All n of <Name>'s wishes came true.", Lines and Tries stats, "Next: n · title" (52 dp), Keep tinkering, the last-level text. Add the sign frame glow.
- [x] 6.5 Terminal panels: Console and Recompositions as dark 18 dp cards with capitalized titles and mono content. Empty Console text: "Anything your code prints shows up here after Run." Restyle the runtime error card to the cozy error container.
- [x] 6.6 Panel unlock: "New: <panel>" tip with explanation and Got it, expand and fade in, two pulse rings.

## 7. Motion

- [x] 7.1 Checklist resolve: stagger of 120 ms per row keyed on the Run counter, 200 ms color tween, 180 ms check stroke.
- [x] 7.2 Failure nudge: one 300 ms ±6 dp shake on the first failed row, 220 ms expand of its detail. No shake under reduced motion.
- [x] 7.3 Level complete: one `updateTransition` drives the glow (600 ms), the card slide and fade (300 ms) and the footer shop light.
- [x] 7.4 Check that reduced motion (forced through the system property) removes the shake and the pulse rings and keeps the fades.

## 8. Verification

- [x] 8.1 Update the `WindowCheck` harness for the new texts:
  - "Tom's sketch" instead of "Target";
  - the completion card and "Next: 2 · Open Sign" instead of "Level complete!" and "Continue";
  - the footer shops and "1 of 2 shops open";
  - the overlay on, then off after the first frame;
  - the unlock tip surviving the opening click;
  - "Built in" status, "Run again".
- [x] 8.2 Add a harness case at 1000 dp width checking that the right column wraps under the editor.
- [x] 8.3 Review the harness screenshots (`app/build/spike/window/`) side by side with `design-docs/cozy/screens/01–03` and fix differences that aren't caused by content.
- [x] 8.4 Run `./gradlew test :app:spike -Pargs="--only window"` and `--selftest` from the built distributable. Check that the fonts and icons are in the bundle.

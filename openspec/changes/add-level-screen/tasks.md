## 1. Level model: descriptions, panels, aliases (`levels` module)

- [x] 1.1 Add `description` (required) to every criterion and make `message` optional (non-blank if given); add the `Below` and `RightOf` criterion types (`below`, `rightOf`) with `a`, `b` and `toleranceDp`
- [x] 1.2 Add `panels` to `level.json` (`enum Panel { CONSOLE, RECOMPOSITIONS }`, lowercase in JSON, default empty); validate unknown names (listing the valid ones) and repeated names
- [x] 1.3 Add `LevelFiles.readBytes` and `Level.targetPng: ByteArray?`, loaded from `<id>/target.png` when present; add `LevelRepository.unlocks()` (each panel → the first indexed level that lists it)
- [x] 1.4 Update `LevelRepositoryTest` for the new rules (missing description, blank message, typo `descripton`, unknown/repeated panel, defaults, target bytes) and add unlock-derivation tests with fixture indexes
- [x] 1.5 Commit section 1 with a conventional commit and a descriptive body, e.g. `feat(levels): add criterion descriptions, order aliases, panels and targets to the level model`

## 2. Generated failure details (`levels` module)

- [x] 2.1 Make each check return a failure reason (`Missing`, `Hidden`, `Count`, `Present`, `Reversed`, `NotInLine`) instead of a boolean; evaluate `below`/`rightOf` by swapping, keeping the author's `a`/`b` for phrasing
- [x] 2.2 Implement selector noun phrases (text, role + text, role only, content description, `contains`) with a role-word table for the 9 roles, and the detail sentences for each reason
- [x] 2.3 Change `CriterionResult` to `(id, description, passed, detail, message)`
- [x] 2.4 Update `EvaluatorTest`: one test per reason and phrase from the `level-acceptance` spec, the aliases, and descriptions/messages in results
- [x] 2.5 Commit section 2, e.g. `feat(levels): explain failed criteria with generated details`

## 3. Build-rendered targets (`levels` module)

- [x] 3.1 Add a `targets` source set (main classes + `:compiler`) with `RenderTargets`: for each indexed level, compile and render `Solution.kt` at the level viewport and density 2 with one pipeline and one engine, write `build/generated/level-targets/levels/<id>/target.png`, and fail on compile errors or crashes
- [x] 3.2 Register a `renderTargets` `JavaExec` task with level data, solutions and classpath as inputs and the generated directory as output; make `processResources` include its output; check that there is no task cycle and that a second build is up to date
- [x] 3.3 Extend `LevelsTest`: every bundled level has a `target.png` of `viewport × 2` pixels
- [x] 3.4 Commit section 3, e.g. `build(levels): render level targets from reference solutions at build time`

## 4. Level content

- [x] 4.1 Rewrite `000-hello-main-street`: criteria `title` (exists) and `order` (`below`), each with a description; plain `Solution.kt`; starter unchanged
- [x] 4.2 Add `001-open-sign` (Tom the florist): `level.json` with three criteria and `"panels": ["console"]`, `Starter.kt`, test-only `Solution.kt`, and an index entry
- [x] 4.3 Update `LevelScenariosTest` for the new criteria and details (wrong order → reversed detail, side by side → not-in-line detail, starter → two missing details) and run `./gradlew :levels:test`
- [x] 4.4 Commit section 4, e.g. `feat(levels): two-requirement Hello Main Street and the Open Sign level`

## 5. Level screen (`app` module)

- [ ] 5.1 Controller: decode the level's target once when it opens; track the first frame since opening (for the ghost), the overlay toggle and the panels to highlight (from `unlocks()`); add `continueToNext()` and a `nextLevel` lookup
- [ ] 5.2 Preview area: target thumbnail next to the preview, ghost (alpha 0.25 + caption) until the first frame, overlay toggle (alpha 0.5, input still reaches the scene); widen the preview column to fit
- [ ] 5.3 Checklist panel: ○ descriptions before the first Run, ✓/✗ with detail and optional message after each Run, `NotRun` line; "Level complete" state with Continue, or "coming soon" on the last level
- [ ] 5.4 Panel disclosure: show Console/Recompositions only when the level lists them (all in the sandbox); unlock highlight with a pulsing border, explanation callout, "Got it", and auto-dismiss after ~8 s
- [ ] 5.5 Update the harness window section to play through the real window: target and ghost before Run with no Console or Recompositions in 000; starter → two ✗ "can't find"; wrong order → reversed detail; solution → Level complete; Continue → 001 with the Console visible and highlighted; screenshots for each state
- [ ] 5.6 Run `./gradlew build` and `./gradlew :app:spike -Pargs="--only window,outofprocess"`; check that the packaged app contains both `target.png` files and no `Solution.kt`
- [ ] 5.7 Commit section 5, e.g. `feat(app): level screen with target, checklist, level complete and panel unlocks`

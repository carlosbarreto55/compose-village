## 1. Semantics snapshot and viewport (`preview` module)

- [x] 1.1 Add the `SemanticsSnapshot` data model (viewport in dp, node tree with texts, content descriptions, role name and dp bounds as plain floats; no Compose types)
- [x] 1.2 Implement `PreviewEngine.snapshot()`: on the player thread, inside a guard slice, render pending invalidations, copy the merged tree (`rootSemanticsNode`) dividing bounds by density, and return null when there is no scene
- [x] 1.3 Add an optional `viewport` (dp) parameter to `PreviewEngine.load` that sizes the new scene as `viewport × density` pixels; make `toScene` and rendering use the current scene size
- [x] 1.4 Test: a snapshot of a two-`Text` column has both texts in order; a snapshot after a composition crash is null; a snapshot stays readable after unload and does not keep the attempt's class loader reachable; a 400 × 300 dp viewport at density 2 gives 800 × 600 px frames
- [x] 1.5 Commit section 1 with a conventional commit (`type(scope): summary`) and a descriptive body, e.g. `feat(preview): add merged semantics snapshot and per-attempt viewport`

## 2. Level model and loader (new `levels` module)

- [x] 2.1 Create the `levels` module (Kotlin/JVM, `api(project(":preview"))`), add the `kotlin-serialization` plugin (Kotlin version) and `kotlinx-serialization-json` to the version catalog, and include the module in `settings.gradle.kts`
- [x] 2.2 Define the `@Serializable` model: `Level` (formatVersion, id, resident, request, entryFunction, starter, viewport, criteria), `Selector` (text, role, contentDescription, ignoreCase, contains), and a sealed `Criterion` with discriminator `type` for `exists` (optional `count`), `absent`, `above` and `leftOf` (`a`, `b`, `toleranceDp` = 1), each with `id` and `message`
- [x] 2.3 Implement `LevelRepository`: read `levels/index.json` from resources, load each `level.json` strictly (unknown keys rejected) and read its starter file
- [x] 2.4 Implement validation that collects every error into a `LevelLoadException` naming the folder, criterion and field: id format and id = folder, unique criterion ids, non-blank request and messages, `formatVersion == 1`, non-empty selectors, known role names (listed in the error), positive viewport, starter file present
- [x] 2.5 Unit-test the loader with fixture levels for each validation rule in the `level-definition` spec (minimal level defaults, duplicate ids, folder/id mismatch, typo field, unknown role, format version 2)
- [x] 2.6 Commit section 2 with a conventional commit and a descriptive body, e.g. `feat(levels): add level data model and strict JSON loader`

## 3. Acceptance evaluator (`levels` module)

- [ ] 3.1 Implement text normalization (trim, collapse whitespace) and selector matching against snapshot nodes: AND of the given fields, any single text value or all values joined by spaces, role by name, `ignoreCase` and `contains`
- [ ] 3.2 Implement the visibility filter (non-zero size, non-zero overlap with the viewport) applied before every criterion
- [ ] 3.3 Implement the criteria: `exists`/`count`, `absent`, and `above`/`leftOf` over some pair of distinct visible matches, with a tolerance in dp
- [ ] 3.4 Implement `Verdict` (`Evaluated` with per-criterion results in level order and `passed`; `NotRun` with `DidNotCompile`/`Crashed`) and `Evaluator.evaluate(level, snapshot)`
- [ ] 3.5 Unit-test the evaluator with hand-built snapshots, one test per scenario in the `level-acceptance` spec (whitespace, case, ignoreCase, contains, role mismatch, zero size, off-viewport, exact count, absent, column vs row, tolerance, partial progress)
- [ ] 3.6 Commit section 3 with a conventional commit and a descriptive body, e.g. `feat(levels): evaluate acceptance criteria over semantics snapshots`

## 4. Test level and self-checking levels

- [ ] 4.1 Add `levels/src/main/resources/levels/index.json` and `000-hello-main-street/` (`level.json` with Rosa's request and the `title`, `subtitle` and `order` criteria with their messages, and a `Starter.kt` that compiles and shows an empty `PlayerScreen` with a hint comment)
- [ ] 4.2 Add `levels/src/test/resources/levels/000-hello-main-street/Solution.kt`
- [ ] 4.3 Implement `LevelsTest` (`testImplementation(project(":compiler"))`): for every indexed level, validate it, compile and load `Solution.kt` at the level's viewport and assert that the verdict passes, compile and load `Starter.kt` and assert that it fails at least one criterion, and check that the index matches the folders on disk exactly; share one pipeline and one engine across levels
- [ ] 4.4 Add pipeline-backed tests for the spec scenarios that need real Compose: Button merged with role + text, styled solution still passes, wrong order fails only `order`, crash/compile error gives `NotRun`
- [ ] 4.5 Confirm that `./gradlew :levels:test` fails, naming the level and criterion, when the solution is broken, and naming the level when the starter already passes (try both by hand, then revert)
- [ ] 4.6 Commit section 4 with a conventional commit and a descriptive body, e.g. `feat(levels): add hello-main-street test level and self-checking level tests`

## 5. Game integration (`app` module)

- [ ] 5.1 Carry the snapshot in the helper protocol: optional snapshot field in `Reply.Result`, taken by `CompileServer` after compile and click requests; add an encode/decode round-trip test
- [ ] 5.2 Add a "Level: Hello, Main Street" entry to the spike window that loads the level, puts its starter code in the editor, shows the resident and request above the editor, and uses the level's viewport and entry function on Run
- [ ] 5.3 After each Run, evaluate and show the verdict below the preview (✓/✗ per criterion, failed messages, "solved" when all pass; "didn't compile" or "crashed" for `NotRun`)
- [ ] 5.4 Check that `app` depends on `levels` main only and that the packaged app contains no `Solution.kt` (inspect the jars of the `createDistributable` output)
- [ ] 5.5 Run `./gradlew build`, then play the level by hand with `./gradlew :app:run`: starter fails, wrong order fails only `order`, solution passes
- [ ] 5.6 Commit section 5 with a conventional commit and a descriptive body, e.g. `feat(app): play the hello-main-street level with acceptance verdicts`

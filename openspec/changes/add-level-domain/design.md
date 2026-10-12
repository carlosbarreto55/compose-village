## Context

The spike (archived as `2026-10-12-spike-in-process-player-compilation`) left four modules:
- `host-api` contains `LevelEntry`, `Guard` and `Console`, and is shared with player code.
- `compiler` contains `PlayerPipeline`: compile, guard, load.
- `preview` contains `PreviewEngine`, an offscreen `ImageComposeScene` on a player thread.
- `app` contains the spike window, the harness and the out-of-process `CompileServer`.

There is no level concept yet. The spike window loads hard-coded samples.

`PreviewEngine` already walks the scene's semantics tree (`scene.semanticsOwners` → `rootSemanticsNode`) to implement `click(text)`, so reading semantics offscreen is proven. Compose's real layout tree (`LayoutNode`) is internal API. The public, stable view of layout is each semantics node's `boundsInRoot`.

The product decisions behind this design were made in the explore session and are recorded in the proposal:
- acceptance is judged on structure, never pixels;
- selectors run on the merged tree;
- text matching trims and collapses whitespace and is case-sensitive by default;
- invisible nodes are ignored;
- v1 has four criteria types;
- solutions are not shipped;
- the change ships one test level.

## Goals / Non-Goals

**Goals:**
- A level is pure data: authors add a folder and an index entry, with no code changes.
- Acceptance verdicts are robust to styling differences and easy to explain to the player.
- The evaluator is testable without Compose and works in both the in-process and out-of-process setups.
- Every level is verified automatically by the build: the solution passes and the starter fails.
- One playable level, `000-hello-main-street`, to exercise the whole path.

**Non-Goals:**
- Interaction criteria (click, then expect), text-style criteria and color checks. See "Future criteria" below.
- Level progression, unlocking, saved progress, hints and a level-select screen.
- A level editor or authoring tool beyond JSON files.
- Fill-in-the-blanks templates (the spike report recommends them for early levels; they will build on this domain).
- Replacing the spike window with the real game UI.

## Decisions

### D1. A new `levels` module, with no Compose UI
```
host-api ◀── compiler
    ▲
    └────── preview ◀── levels ◀── app
                (SemanticsSnapshot)   (Level, loader, evaluator, level data)
```
`levels` depends on `preview` only for the `SemanticsSnapshot` data classes, which contain no Compose types: bounds are four floats in dp. The model, loader and evaluator are plain Kotlin. Level data ships as resources of this module.

*Alternatives:*
- Put levels in `app`. Rejected: that mixes domain code with UI and the harness, and makes level tests depend on the window.
- Put the snapshot in `host-api`. Rejected: `host-api` is visible to player code, and the snapshot is host-only.

### D2. Evaluate a plain-data snapshot, not the live tree
```
PlayerPipeline ─▶ PreviewEngine.load(entry, viewport) ─▶ renderNow ─▶ snapshot()   [player thread]
                                                                         │ SemanticsSnapshot (immutable)
                                     Level.criteria ───▶ Evaluator.evaluate(level, snapshot) ─▶ Verdict
```
`PreviewEngine.snapshot(): CompletableFuture<SemanticsSnapshot?>` runs on the player thread inside a guard slice. It renders pending invalidations first, then copies the merged tree:
- `SemanticsProperties.Text` → `List<String>`;
- `ContentDescription` → `List<String>`;
- `Role` → its name;
- `boundsInRoot` → dp, by dividing by density.

It returns `null` when there is no scene.

This keeps the evaluator pure, leaves no reference to the player's class loader (the reload-without-leaks requirement still holds), and makes the snapshot serializable for the helper.

*Alternative:* evaluate on the player thread against the live `SemanticsNode`s. Rejected: that couples the evaluator to Compose, it can't be unit-tested, and it can't cross the process boundary.

Selectors work only on the **merged** tree (`rootSemanticsNode`, not `unmergedRootSemanticsNode`). This is the same default as `compose-ui-test`, and it means "a button that says Order" is one node.

### D3. JSON with `kotlinx.serialization`, strict
Level files are decoded with `kotlinx-serialization-json` (`Json { ignoreUnknownKeys = false }`) into `@Serializable` classes. Criteria are a sealed class hierarchy, with the class discriminator `type` (`exists`, `absent`, `above`, `leftOf`). After decoding, a validation pass checks:
- that the id matches the folder name;
- that criterion ids are unique;
- that the selectors are non-empty;
- that roles are known;
- that the viewport is positive;
- that the starter file exists;
- that `formatVersion == 1`.

Errors are collected into a `LevelLoadException` that names the folder, the criterion and the field.

The serialization plugin version equals the Kotlin version (2.4.21) and is added to the version catalog. The runtime library takes the release that matches it.

*Alternatives:*
- YAML or TOML, which handle multiline text better. Rejected for now: multiline code lives in `.kt` files anyway, and JSON has the best tooling.
- Hand-written parsing with `org.json`. Rejected: too much boilerplate.

### D4. Folder per level, with an ordered index
```
levels/src/main/resources/levels/
  index.json                         ["000-hello-main-street"]
  000-hello-main-street/
    level.json
    Starter.kt
levels/src/test/resources/levels/
  000-hello-main-street/
    Solution.kt                      ← test resources only: never on the game's classpath
```
Resources inside a jar can't be listed, so `index.json` is how the game discovers levels, and it also defines play order. The numeric prefix in ids is only a convention. Order comes from the index, and ids never change after release.

### D5. Matching rules
- **Normalize** a string by trimming it and replacing each `\s+` run with `" "`. Then compare with `==` or `contains`, optionally ignoring case.
- **Node text** matches if any single value matches, or if all values joined by `" "` match. This covers merged nodes that hold several texts.
- **Visible** means `width > 0 && height > 0` and an intersection with the viewport of non-zero area. Invisible nodes are filtered out before any criterion runs.
- **Order criteria** pass if *some pair* of distinct visible matches satisfies the inequality. They are ∃-quantified, so an extra unrelated "Rosa's Bakery" elsewhere doesn't cause a failure. The default `toleranceDp` is 1, which absorbs sub-dp rounding from fractional layouts.

### D6. When to evaluate
The game evaluates after `load` completes without an error, then calls `renderNow()` and `snapshot()`. Animations are not awaited. v1 levels are static, and the interaction criteria that come later will need explicit settling rules anyway.

A compile failure or runtime crash produces `Verdict.NotRun(reason)` instead of evaluating criteria, so the player never sees "criterion failed" for code that never ran.

```kotlin
sealed interface Verdict {
    data class Evaluated(val results: List<CriterionResult>) : Verdict { val passed: Boolean }
    data class NotRun(val reason: Reason) : Verdict  // DidNotCompile, Crashed
}
data class CriterionResult(val id: String, val passed: Boolean, val message: String?)
```

### D7. Per-level viewport through `load`
The scene is recreated on every `load`, so `PreviewEngine.load(entry, lines, loader, viewport: DpSize? = null)` creates it with `viewport × density` pixels. Without a viewport, it keeps the current `width`/`height`. `toScene` uses the current scene size. This adds no new lifecycle.

### D8. The helper protocol carries the snapshot
`Reply.Result` gets an optional snapshot field, encoded as a pre-order list of nodes with child counts. `CompileServer` takes the snapshot after compile and click requests. A round-trip test checks that encoding then decoding gives an equal snapshot. The evaluator then works the same on both sides.

### D9. The self-checking test lives in `levels`
`LevelsTest` (with `testImplementation(project(":compiler"))`) loads the index and, for each level:
1. validates the level;
2. runs `Solution.kt` through `PlayerPipeline` and `PreviewEngine` at the level's viewport, takes a snapshot, and asserts that the verdict passes;
3. does the same with `Starter.kt` and asserts that it compiles, loads without error, and fails at least one criterion.

The test also compares the index with the folders on disk, under `src/main/resources/levels/`, through the test's working directory. One `PlayerPipeline` and one `PreviewEngine` are reused across levels, to pay the compiler warm-up once.

Unit tests for the evaluator use hand-built snapshots and cover each matching and criterion rule in the specs.

### D10. Minimal game integration
The spike window gets a "Level" source: a dropdown entry next to the samples. It loads `000-hello-main-street`, puts `Starter.kt` in the editor, shows the resident and request above the editor, and shows the verdict below the preview after each Run. Passed criteria get ✓. Failed ones get ✗ with the level's message. This is deliberately throwaway UI, enough to play-test the domain by hand.

### Future criteria (not in v1, recorded so the format leaves room)
- **Interaction:** `{ "type": "afterClick", "click": <selector>, "expect": [<criteria>] }`. The engine already has `click`. This needs settling rules and a fresh scene per check.
- **Text style:** `SemanticsActions.GetTextLayoutResult` exposes the laid-out `TextStyle` (font size, weight and color) without pixels, e.g. `{ "type": "textStyle", "node": …, "fontWeight": "Bold" }`.
- **Size and alignment:** for example "fills the width" or "centered horizontally", with dp tolerances.

New criterion types are additive. `formatVersion` only changes for breaking changes.

## Risks / Trade-offs

- **Semantics alone can be satisfied by odd code.** For example, text drawn with alpha 0 has semantics but isn't visible. → The visibility check covers size and position. Alpha and color aren't in semantics, so we accept this for v1. Style checks later can tighten it.
- **Custom `Modifier.semantics` or `clearAndSetSemantics` could fake or hide nodes.** → This only matters if a player games their own puzzle. It isn't a security issue, and we accept it.
- **The merged tree may surprise authors.** For example, a `Card(onClick)` merges its texts into one node. → Text matching also accepts the joined text, and the self-check test catches authoring mistakes because the solution must pass.
- **Material3 components may change semantics between versions**, such as roles or merging. → The self-check test runs every level's solution on every build, so a dependency bump that breaks a level fails CI.
- **The self-check test is slow** because of the embedded compiler: about 2 s warm-up plus about 150 ms per attempt. → Instances are shared across levels. At hundreds of levels, the test could be split or tagged.
- **Strict unknown-key rejection makes old games reject newer files.** → This is intended. `formatVersion` is the compatibility switch, and levels ship with the game.

## Migration Plan

This change is additive. The spike window keeps its samples, and the level entry is new. There is no stored data to migrate. To roll back, revert the change. Nothing else depends on `levels`.

## Open Questions

- Should a player be able to see criteria before running, as a checklist, or only failures after a Run? This is a UX decision for the real game UI. v1 shows all criteria after a Run.
- Where will progress be stored once there is more than one level? That belongs to a later change. The `id` stability rule prepares for it.

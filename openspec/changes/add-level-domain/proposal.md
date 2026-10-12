## Why

The spike proved we can compile and run a player's Compose code in-process, but the game has no notion of a *level*: nothing tells the player what a villager wants, and nothing decides whether their UI satisfies the request. Levels are the core of the game, so the domain and its acceptance rules must exist before any real content. Judging attempts by pixel comparison would fail visually correct solutions over anti-aliasing, font or spacing differences, which is exactly the kind of false negative that makes a learning game frustrating. So acceptance must be based on the UI's structure (the semantics tree and the bounds of its nodes) instead.

## What Changes

- Introduce a **level** domain: a stable `id`, the villager (`resident`) and their `request` text shown to the player, the entry function, the starter code, an optional viewport and a list of acceptance criteria. Each criterion carries a player-facing message.
- Define levels as **data files**, one folder per level (`level.json` + `Starter.kt`), listed in an ordered `index.json`. Adding a level means writing files, not code. Reference solutions (`Solution.kt`) live only in test resources and are **not shipped** in the game.
- Add **semantics-based acceptance**: after an attempt renders, the preview engine takes a plain-data snapshot of the scene's **merged** semantics tree (text, role, content description, bounds). A pure evaluator checks the level's criteria against it and produces a per-criterion verdict.
- v1 criteria: `exists` (with optional exact `count`), `absent`, `above`, `leftOf` (geometric order with a tolerance in dp). v1 selectors: `text`, `role`, `contentDescription`.
- Make text matching forgiving: whitespace is trimmed and collapsed, matching is case-sensitive by default, and a selector can opt in to `ignoreCase` or `contains`. A matched node must also have a non-zero size and overlap the viewport, so hidden text doesn't count.
- Add a **self-checking level test**: for every level, `Solution.kt` must pass all criteria, `Starter.kt` must compile and fail at least one, ids must be unique and match folder names, and the index must list exactly the level folders.
- Ship exactly **one level**, `000-hello-main-street` (Rosa's bakery sign), for testing. It is playable from the existing game window.
- Explicitly out of scope for v1, noted as future criteria: interactions (click, then expect) and text-style checks via `SemanticsActions.GetTextLayoutResult` (font size, weight, color).

## Capabilities

### New Capabilities
- `level-definition`: the level data model, the on-disk layout (folder per level, `index.json`, starter and test-only solution files), loading and validation of level data, and the self-checking test every level must pass.
- `level-acceptance`: the semantics snapshot, node selectors and text normalization, the v1 criteria types and their tolerances, implicit visibility checks, and the per-criterion verdict with player-facing messages.

### Modified Capabilities
- `player-preview-runtime`: the preview engine must also produce a plain-data snapshot of the merged semantics tree of the running attempt (safe to keep after the attempt is unloaded, and serializable across the helper-process boundary), and must render a scene at the viewport size the level asks for.

## Impact

- **New module** `levels` (Kotlin/JVM, no Compose UI): level model, JSON loader (adds the `kotlinx-serialization` plugin and `kotlinx-serialization-json`), the criteria evaluator and the bundled level data as resources.
- **`preview`**: adds a `SemanticsSnapshot` data model and a `snapshot()` call on `PreviewEngine`, and lets `load` take the scene size.
- **`app`**: the window can load level `000-hello-main-street`, showing its request, its starter code and the verdict after each Run. The out-of-process helper protocol carries the snapshot in its compile/click reply, so the fallback keeps working.
- **Packaging**: `kotlinx-serialization-json` adds roughly 1–2 MB to the distributable. Solutions are excluded from it.
- No change to the compile pipeline, sandbox or guard.

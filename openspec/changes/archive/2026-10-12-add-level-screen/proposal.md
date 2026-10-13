## Why

The level domain works, but the level screen doesn't yet help the player play:
- The player reads Rosa's request but can't see what the result should look like.
- The verdict only appears after a Run, and it shows a bare id for passed checks.
- A failed check shows one fixed message, which can't tell "missing" apart from "in the wrong place".
- Solving a level leads nowhere.
- Every level shows the Console and Recompositions panels, even when they have nothing to do with the task.

This change turns the spike window's level mode into a first playable level screen.

## What Changes

- **Level target.** Each level's reference solution is rendered **at build time** into a `target.png` that ships with the level. `Solution.kt` itself still never ships, and the game never compiles it.
  - The screen shows the target as a faded "ghost" in the preview before the first Run.
  - A "Your screen | Target" switch above the preview shows the target at full size, in place of the player's screen.
  - The target is for display only. Acceptance still never compares pixels.
- **Requirements checklist.**
  - Each criterion gets a required player-facing `description` (e.g. "\"Fresh bread daily\" appears below \"Rosa's Bakery\""). The checklist below the preview always shows these: neutral before the first Run, then ✓ or ✗ after each Run.
  - For each failed criterion, the evaluator **generates** a short explanation from why it failed (missing, hidden, wrong order, side by side, wrong count), quoting only texts from the level data, never code.
  - `message` becomes an optional extra line in the resident's voice.
- **Order aliases.** Add `below` and `rightOf` criteria so level data reads like its descriptions.
- **Level complete.** When every requirement passes, the checklist turns into a clear "Level complete" state with a **Continue** button that opens the next level in `index.json`. The last level says that more of Main Street is coming soon.
- **Progressive panels.**
  - The preview, target, checklist and compile problems are always shown. A level lists the extra panels it uses in a new `panels` field (`console`, `recompositions`), and other panels are hidden. The sandbox shows everything.
  - The first level in play order that shows a panel **unlocks** it: the panel is highlighted with a short explanation of what it does. The unlock is worked out from the level order, with no saved state.
- **Level content.**
  - `000-hello-main-street` drops to two requirements: "Rosa's Bakery" is on screen, and "Fresh bread daily" appears below it. Its solution becomes plain, so the target shows nothing the checks don't require.
  - A second level, `001-open-sign` (Tom the florist: a name with an "Order" button to its right, with the Console unlocked), shows that a level with different requirements needs only data.
- **BREAKING (level format, unreleased):** criteria require `description`, and `message` becomes optional. Format version 1 is changed in place, because no level has been released.

## Capabilities

### New Capabilities
- `level-screen`: what the player sees while playing a level: the target ghost and the "Your screen | Target" switch; the requirements checklist with generated failure details; the "Level complete" state and Continue; per-level panel disclosure; and panel unlock highlights.

### Modified Capabilities
- `level-definition`:
  - the data model gains `panels` and per-criterion `description`, and `message` becomes optional;
  - validation covers panel names;
  - reference solutions stay test-only, but their build-rendered targets ship;
  - the bundled content becomes two levels.
- `level-acceptance`:
  - the verdict carries each criterion's description and a generated failure detail;
  - order criteria gain the `below` and `rightOf` aliases.

## Impact

- **`levels`**:
  - model, loader and evaluator: descriptions, generated details, aliases, panels, target bytes;
  - a new build step that renders each solution to `target.png` in generated resources, using the real pipeline and engine at 2× density;
  - level data for both levels.
- **`app`**: the level screen reworked: target ghost and view switch; the checklist and "Level complete" state; panel visibility and unlock highlights; Continue. Harness window checks are updated.
- **Build**: `processResources` in `levels` depends on the render step, which compiles every solution, adding a few seconds whenever level data changes. Targets render on the build machine, so they use that machine's fonts.
- **No change** to the compiler, sandbox or guard, or to the preview engine's public API.

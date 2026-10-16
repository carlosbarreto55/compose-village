## Why

Main Street has five shops, but only two are playable: Rosa's Bakery and Tom's Flowers. Marco's Kitchen, Dona Lúcia's School and Iara's Ferry are "Coming soon" placeholders, and the game ends right after the player's first `onClick`. The villagers, busts, icons and intro lines for all three already exist in the design docs and the app. What's missing is the levels themselves, and a difficulty curve that takes a beginner from a static sign to a small interactive screen.

## What Changes

- **Three new levels, each harder than the last.** Each builds on the one before and adds one new idea:
  - **3 · Restaurant Menu** (Marco, chef, "Marco's Kitchen"): show every dish from a list in the starter, with its price on the right. New ideas: lists, `forEach`, and a `Row` nested in a `Column`, spread with `fillMaxWidth` / `SpaceBetween`. Still a still screen, but driven by data.
  - **4 · Contest Scoreboard** (Dona Lúcia, teacher, "Dona Lúcia's School"): a scoreboard with two teams, "+1" buttons and Reset. The starter keeps the scores in a plain `var`, so the board never changes. The player fixes it with `remember { mutableStateOf(...) }`. The customer presses several buttons in a row, and criteria check the board after each press. This level unlocks the **Recompositions** panel, which no level uses yet.
  - **5 · Ferry Times** (Iara, boat captain, "Iara's Ferry"): a timetable with a "Where to?" text field. Typing filters the departures, ignoring case, and a message shows when no ferry matches. New ideas: `TextField`, state the player owns, `filter`, and `if` in a composable. The customer types two searches.
- **Grouped criteria.** A new `all` criterion bundles several checks into one checklist row ("Every dish is on the menu"), so lists and filters don't need one row per item. Its detail comes from the first check that fails.
- **Clearer details after a step.** A criterion checked after a customer step now says which step it was ("After a customer pressed "+1 Blue": I can't find "Blue: 1" on the screen."). When a step leaves the screen exactly as it was, the detail says so instead ("A customer pressed "+1 Blue", but the screen didn't change."). This is the most common state bug, and the plain "I can't find" doesn't help with it.
- **Level content gets its own spec.** The content of all five levels moves out of `level-definition` (which keeps the format) into a new `main-street` capability. That spec also records the difficulty curve and the rules the levels follow.
- **Main Street is complete.** The three placeholder shops are replaced by their levels, and the street shows five buildings and the Sandbox. On the last level, the completion card says that Main Street is complete, instead of "more of Main Street is coming soon".

## Capabilities

### New Capabilities
- `main-street`: the five Main Street levels in play order, each with its villager, request, concepts, panels, customer steps and criteria, plus the difficulty rules: one new idea per level, at most one panel unlocked per level, starter code that compiles and leaves the new idea for the player.

### Modified Capabilities
- `level-acceptance`: the `all` criterion, with its detail and its rules for `after`. Details for criteria checked after a step name the step, with the "screen didn't change" case.
- `level-definition`: validation of `all` (at least two checks; no ids, descriptions, messages or `after` inside; no nesting; no `prints` inside). "Initial level content" moves to `main-street`.
- `home-screen`: the placeholder list is empty once the three levels ship, and the street and its scenarios show five levels.
- `level-screen`: the last level's completion card says Main Street is complete.

## Impact

- **`levels`:**
  - `Criterion.All` in the model, loader validation and evaluator, with the after-step details and the unchanged-screen comparison;
  - three new level folders (`002-restaurant-menu`, `003-contest-scoreboard`, `004-ferry-times`) with `level.json` and `Starter.kt`, their `Solution.kt` under test resources, and `index.json`;
  - tests for each of these.
- **`app`:** `Upcoming` becomes empty, the last-level completion copy changes, and the window check plays all five levels.
- **`preview`:** no change expected. An early task checks that the customer can type into a Material 3 `TextField` found by its label, and that the Recompositions panel counts the player's composables in the scoreboard level.
- **Dependencies:** `customer-bot` is implemented but not archived. It must be archived first, so that this change's deltas apply to its version of `level-definition` and `level-acceptance`. `hints-and-editor-aids` is not needed here, but once it lands it should add hints for levels 3–5, where players are most likely to get stuck.
- **Non-goals:**
  - scrolling lists and a `scroll` customer step (every level fits its viewport);
  - new chapters (Market, Harbor, City Hall);
  - locking levels until the previous one is done;
  - checking how the player wrote the code (for example, that they used `forEach`). Acceptance stays structural.

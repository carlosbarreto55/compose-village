## Why

Three parts of the cozy mockups help a stuck player read their own code, and none exist yet:
- a **"Need a hint?"** button on the editor;
- **folded imports** ("› 6 imports"), so a beginner's file starts at `@Composable`;
- the **failing line glow** with a status line such as "Compiles fine — line 9 is where Tom's bell should ring".

The completion card also has a **Hints used** stat that `cozy-level-screen` leaves out. Together these keep beginners moving without handing them the answer.

*Roadmap stub: written alongside `cozy-level-screen`. Design, specs and tasks are still to be written.*

## What Changes

- **Hints.** A level can list ordered hints. Each is a short text in the villager's or narrator's voice, with no full solutions. "Need a hint?" shows the next one in a popover. The editor tab counts the hints shown, and the completion card shows **Hints used**. The count is saved by `village-progress`, if that change has landed.
- **Folded imports.** The editor hides the file's leading `import` block behind a "› N imports" toggle. Line numbers, diagnostics and the caret keep their real positions. A compile error inside the folded block unfolds it.
- **Pointing at a line.** A criterion can name an **anchor**, a small pattern the game looks for in the player's code (for example the `Button(` call whose label is "Order"). When that criterion fails and the code compiles:
  1. the matched line gets a warm highlight (`#4A2E25`), fading in at 520 ms after the shake (storyboard moment 3);
  2. the status line says which line to look at.

  Anchors are matched against the player's source text or the compiler's PSI, never the solution. They are optional, and with no match there is no highlight.
- **Status line wording.** "Compiles fine — line N is where <reason>" when an anchor matched, and "Compiles fine" otherwise.

## Capabilities

### New Capabilities
- `level-hints`: hint data, the order hints are shown in, the hint count, and how hints relate to completion stats.

### Modified Capabilities
- `level-definition`:
  - the `hints` field;
  - optional per-criterion `anchor` (with its message) and validation.
- `level-screen`:
  - the Need a hint? button and popover;
  - folded imports;
  - the failing-line highlight and status-line wording;
  - the Hints used stat.

## Impact

- **`levels`:** model, loader and validation; hint and anchor content for both levels.
- **`app`:** editor folding (a `VisualTransformation` hiding lines, or a split view; decided in design), hint popover, line highlight.
- **`compiler`:** possibly a PSI query for anchors, if text matching is too brittle.

Open question for the design: anchors as text patterns (simple, brittle) or as PSI queries (robust, needs a compiler API)?

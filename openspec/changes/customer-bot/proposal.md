## Why

The cozy Open Sign mockup has a fourth requirement, "Pressing 'Order' prints 'New order!'", and a Console that reads `customer › pressed [Order]`. Acceptance today only looks at a still snapshot, so a level can't check that a button *does* something. That is the core of `onClick` and of every interactive level after it. Without it, Tom's request ("print New order! so I hear the bell") can't be checked.

*Roadmap stub: written alongside `cozy-level-screen`. Design, specs and tasks are still to be written.*

## What Changes

- **Customer actions.** A level can list actions for a "customer" to perform after the first frame, starting with `press` on a selector. The game performs them through the preview engine (`PreviewEngine.click` exists), lets the scene settle after each one, and records what each action caused: the console lines printed and the snapshot after it.
- **Interaction criteria.** A new criterion `pressPrints { node, text }` checks that pressing the node prints the text. Its generated details cover:
  - the button is missing;
  - pressing printed nothing ("A customer pressed Order, but nothing was printed");
  - pressing printed something else.
- **Evaluator stays pure.** The evaluator gets an `Attempt` (the initial snapshot, plus each action's snapshot and console delta) instead of one snapshot. It still never touches the scene. The "pure function" requirement changes from "of the snapshot" to "of the attempt record".
- **Console voice.** Customer actions show in the Console as `customer › pressed [Order]`, with "(nothing printed)" when there was no output. Before the first Run the Console says "A customer will press your buttons here after Run." While the customer acts, the preview caption reads "A customer pressed Order".
- **Content.** `001-open-sign` gains its fourth requirement, "Pressing 'Order' prints 'New order!'", matching the mockup. Its message ("my bell stayed quiet…") moves to the reaction bubble.
- **Checklist motion.** Rows resolve after the customer finishes (storyboard moment 2: "After the customer bot finishes").

## Capabilities

### New Capabilities
- `customer-actions`: the actions a level can script, how the game performs them after the first frame, how long it waits for each, and what it records per action.

### Modified Capabilities
- `level-acceptance`:
  - the evaluator's input becomes the attempt record;
  - the `pressPrints` criterion and its generated details.
- `level-definition`:
  - the `actions` field (or actions implied by interaction criteria; to be decided in design);
  - Open Sign's fourth criterion.
- `level-screen`:
  - the Console's customer lines and its empty-state text;
  - the preview caption while the customer acts.
- `player-preview-runtime`: if it needs anything beyond `click`, such as waiting for idle after input or reading the console as a delta.

## Impact

- **`levels`:** model, loader, evaluator (`Attempt`), level data and tests.
- **`app`:** the Run flow (perform actions before judging), Console rendering, preview caption.
- **`preview`:** possibly a console-mark/delta API.

Open question for the design: do levels list actions explicitly, or does each interaction criterion bring its own action?

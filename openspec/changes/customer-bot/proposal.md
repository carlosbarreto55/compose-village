## Why

The cozy Open Sign mockup has a fourth requirement, "Pressing 'Order' prints 'New order!'", and a Console that reads `customer › pressed [Order]`. Acceptance today only looks at a still snapshot, so a level can't check that a button *does* something. That is the core of `onClick` and of every interactive level after it. Without it, Tom's request ("print New order! so I hear the bell") can't be checked.

## What Changes

- **Customer actions.** A level lists the steps a "customer" performs after the first frame: `press` a node, or `enterText` into one. The game performs them in order through the preview engine, lets the scene settle after each one, and records what each step caused: the text printed and the snapshot after it.
- **Interaction criteria.** Any criterion can name a step in `after`, to be checked against the screen right after it. A new criterion `prints { after, text }` checks what a step printed. Its generated details cover:
  - the customer couldn't find the node;
  - the step printed nothing ("A customer pressed \"Order\", but nothing was printed.");
  - the step printed something else.
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
  - `after` on criteria, the `prints` criterion and the customer's generated details.
- `level-definition`:
  - the `actions` field and its validation;
  - Open Sign's fourth criterion.
- `level-screen`:
  - the Console's customer lines and its empty-state text;
  - the preview caption while the customer acts.
- `player-preview-runtime`: node ids in the snapshot, and pressing or typing into a node by id, returning the next snapshot and what was printed.

## Impact

- **`levels`:** model, loader, evaluator (`Attempt`), level data and tests.
- **`app`:** the Run flow (perform actions before judging), Console rendering, preview caption.
- **`preview`:** node ids in snapshots, `press` and `enterText` by node id.

Decided in design D1: levels list the steps explicitly, so later levels can script sequences such as typing a name and then pressing Send.

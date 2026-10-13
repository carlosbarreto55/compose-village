## Context

Acceptance today is a pure function of one semantics snapshot, taken after the attempt's first frame (`level-acceptance`). That is enough for signs, but not for anything a button *does*. Tom's request ("print New order! so I hear the bell") can't be checked, so `001-open-sign` has three criteria where the mockup has four.

What exists already:
- `PreviewEngine.click(text)` presses the first node whose text contains `text`, with a real pointer press and release. The spike and self-test use it; the game doesn't.
- `Console` collects everything the attempt prints, per attempt, and `PreviewState.console` publishes it.
- `SpikeController.start` compiles, loads, takes a snapshot and calls `Evaluator.verdict(level, compiled, snapshot)`. `levels/src/test/.../Attempts.kt` does the same for the self-checking level tests.

The roadmap goes on to interactive streets (a market with counters, forms and scales), so whatever this change adds has to handle sequences and typing, not just one press.

## Goals / Non-Goals

**Goals:**
- A level lists what a customer does after the first frame, as ordered steps: press a node, or type text into a node.
- Criteria can be checked against the screen after any step, and a new criterion checks what a step printed.
- The evaluator stays a pure function, now of an attempt record, so it is still unit-tested with hand-built data.
- The Console and the preview tell the player what the customer did.
- Open Sign gets its fourth requirement.

**Non-Goals:**
- Scrolling, long press, drag, keyboard shortcuts, focus or waiting for a `delay`. They come with the streets that need them.
- Waiting for animations to finish. The scene is settled by rendering pending invalidations, as snapshots already are.
- Replaying the customer for the player as an animation. The screen after the last step is shown, with a caption.
- The out-of-process helper. It keeps its own `click` request and is not used by the game.

## Decisions

### D1. Levels list the customer's steps

A level gains an optional `actions` array. Each step has an `id`, a `type` and a target `node` selector:

```json
"actions": [
  { "id": "order", "type": "press", "node": { "role": "Button", "text": "Order" } }
],
```

```json
{ "id": "name", "type": "enterText", "node": { "text": "Your name" }, "text": "Rosa" }
```

Steps run in order, once per Run, after the first frame.

*Alternative: each criterion brings its own action (`pressPrints { node, text }`).* Simpler data for one press, but it can't express "press three times, then check the counter", or "type a name, then press Send". Those are the next street's levels. With listed steps, a level is a short script, and any criterion can look at any point in it.

`enterText` is included now because it costs little next to `press`, and the first Market level will need it. No shipped level uses it yet; unit and engine tests cover it.

### D2. Criteria pick a moment with `after`

Every criterion gains an optional `after`: the id of a step. Without it, the criterion is checked against the first frame, as today. With it, against the screen right after that step. One new criterion type, `prints`, checks what was printed during a moment:

```json
{ "type": "prints", "id": "bell", "after": "order", "text": "New order!",
  "description": "Pressing \"Order\" prints \"New order!\"",
  "message": "The sign looks lovely! But a customer pressed Order and my bell stayed quiet…" }
```

`prints` passes when one line printed during that moment equals `text`, after the usual trim and whitespace collapse (and `ignoreCase` / `contains` as for selectors). Without `after`, the moment is the first composition, so a level can also check what the screen prints when it appears.

*Alternative: a separate criterion type per action (`pressPrints`, `pressShows`, …).* That multiplies criterion types for every new action. `after` keeps the existing types and adds only one.

### D3. The attempt record

The evaluator's input becomes an `Attempt`:

```kotlin
data class Attempt(val first: Moment, val steps: List<Step>)
data class Moment(val snapshot: SemanticsSnapshot, val printed: String)
data class Step(val action: Action, val outcome: Outcome) // the action itself, so failure details can name its target
sealed interface Outcome {
    data class Done(val moment: Moment) : Outcome
    data class NotFound(val hidden: Boolean) : Outcome // hidden: matches, but none visible
    data object NotReached : Outcome
}
```

`Evaluator.verdict(level, compiled, attempt)` replaces the snapshot parameter. `attempt == null` still means the attempt crashed, including a crash during a step.

A step whose node can't be found is not performed, and every later step is `NotReached`: a customer who can't find the Order button doesn't go on to the next thing. A criterion `after` such a step fails with a customer detail (D5) instead of being checked against a stale screen.

### D4. Who performs the steps

A `Customer` in the `levels` module runs the steps against a `PreviewEngine`, because it needs `Selector` matching, which lives there. For each step it:
1. finds the target in the latest snapshot with the evaluator's matching: the first **visible** node that matches the selector;
2. asks the engine to act on that node;
3. receives the next moment (snapshot and printed text) from the engine.

The engine gains two calls, both on the player thread inside a guard slice:
- `press(nodeId)`: a pointer move, press and release at the node's centre, as `click` does today.
- `enterText(nodeId, text)`: invokes the node's `SemanticsActions.SetText`, which is what a text field exposes. Keyboard events would depend on focus and the IME, and `SetText` is deterministic.

Each call then lets the tasks it posted run (click handlers resume as coroutines on the player thread), renders pending invalidations and returns an `ActionResult`: the new snapshot, the text printed since the call started and where it starts in the console, and whether the node was found. A text field's editable text is part of its node's texts in the snapshot, so a criterion can see what was typed. A crash (in `onClick`, in `onValueChange` or while rendering) goes through the engine's existing crash path, and the result has no snapshot.

For this, `SnapshotNode` gains `id`: the merged semantics node's id. The engine finds that node again in the current tree. The id defaults to -1 so hand-built snapshots in tests don't change.

*Alternative: press by coordinates only.* `enterText` needs the node itself, so a node id is needed anyway, and pressing by node keeps both actions symmetric.

Both the game (`SpikeController.start`) and the level tests (`Attempts`) build the attempt through `Customer`, so the self-checking tests run the customer exactly as the game does.

### D5. Customer details

New generated details, in the same voice as today's, naming the node like the selector does:
- step target missing: "The customer couldn't find X to press." / "… X to type into."
- step target hidden: "The customer found X, but it has no size or is off the screen."
- not reached: the detail of the step that stopped the customer.
- `prints`, nothing printed: "A customer pressed X, but nothing was printed."
- `prints`, something else printed: "A customer pressed X and \"Y\" was printed, but I wanted \"Z\"." (the first line printed).
- `prints` with no `after`: "Nothing printed \"Z\" when your screen appeared."

For `enterText`, "pressed X" reads "typed \"T\" into X". For presses, X is the selector's text in quotes when it has one (`"Order"`), so the line matches the mockup's "A customer pressed Order"; otherwise the selector phrase (`a button`).

### D6. What the player sees

- **Console:** a customer line before each step's output: `customer › pressed [Order]` or `customer › typed "Rosa" into [Name]`, then what the step printed, or `(nothing printed)`. The game keeps the console offset at which each step started and renders the transcript from the console text and those marks, so `Console` in `host-api` stays a plain buffer.
- **Empty Console:** before the first Run, a level with `press` steps says "A customer will press your buttons here after Run." Other levels keep today's line.
- **Preview:** the preview shows the screen as the customer left it, which is the same scene, never reloaded. Its caption reads "A customer pressed Order" (the last step performed) until the player clicks the preview, edits or runs again.
- **Checklist:** rows still resolve one after another once the verdict exists, and the verdict exists only after the customer is done. That is storyboard moment 2.

### D7. Validation

Loading rejects:
- unknown step types;
- step ids that are blank, repeated or not `^[a-z0-9]+(-[a-z0-9]+)*$`;
- step selectors that set nothing;
- `enterText` without `text`;
- an `after` that names no step;
- a `prints` with a blank `text`.

`formatVersion` stays 1, because `actions` and `after` are optional and every existing level stays valid.

## Risks / Trade-offs

- [The customer changes the scene the player sees, e.g. a counter already reads 3.] → The caption says what the customer did, and this is how a real customer would leave the shop. Reloading the scene for the player would double the load and first-frame time of every Run.
- [`renderPending` renders up to three times, so effects that settle later (`LaunchedEffect` with `delay`, animations) are not seen.] → Non-goal for now. Levels that need them come with a "wait" step and a settling rule.
- [A merged node may not expose `SetText` when the player wraps a text field oddly.] → `enterText` reports NotFound with the hidden/missing detail. The first level that uses it will be written and tested against real player code.
- [Node ids are only stable within one composition.] → The customer always takes the id from the snapshot taken right before the step, never across Runs.
- [A step's output mixes with output from effects that happen to run at the same time.] → Accepted. `printed` is everything printed during the step, which is what the player sees in the Console too.

## Migration Plan

All additions are optional fields, so existing levels load unchanged. `Evaluator.verdict` changes signature; its three callers (`SpikeController`, `Attempts`, `EvaluatorTest`) move to `Attempt` in the same change. Rollback is reverting the change; no saved data depends on it.

## Open Questions

- Should the preview draw a small cursor on the pressed node, as the Run-fail mockup does? Left out for now; the caption carries the meaning.

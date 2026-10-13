## 1. Engine: node ids and actions

- [x] 1.1 Add `id` (default -1) to `SnapshotNode` and fill it from the merged `SemanticsNode.id` in `PreviewEngine.snapshot`; keep existing snapshot tests passing
- [x] 1.2 Add `ActionResult(found, snapshot, printed)` and `PreviewEngine.press(nodeId)`: find the node in the current merged tree, send move/press/release at its centre, render pending changes, return the snapshot and the console text printed since the call started
- [x] 1.3 Add `PreviewEngine.enterText(nodeId, text)` through `SemanticsActions.SetText`, inside a guard slice; not found when the node or its action is missing
- [x] 1.4 Route exceptions from both actions (and the render after them) through the crash path, so the result has no snapshot and the runtime error is published
- [x] 1.5 Engine tests: press a button by id (handler runs once, printed text returned), type into a `TextField` bound to state, a stale id, and an `onClick` that throws

## 2. Level data: steps, `after` and `prints`

- [x] 2.1 Add `Action` (sealed: `Press(id, node)`, `EnterText(id, node, text)`) and `Level.actions`, decoded from the optional `actions` field
- [x] 2.2 Add optional `after` to every criterion, and the `Prints(id, description, after, text, ignoreCase, contains, message)` criterion type
- [x] 2.3 Validate steps and references in `LevelRepository`: unknown step types (listing `press`, `enterText`), step id pattern, blank and repeated step ids, step selectors, `enterText` without `text`, unknown `after`, blank `prints.text`
- [x] 2.4 Loader tests for each new validation error, and for a level with a press step loading as expected

## 3. Evaluator: the attempt record

- [x] 3.1 Add `Attempt`, `Moment`, `Step` and `Outcome` (`Done`, `NotFound(hidden)`, `NotReached`) in `levels`
- [x] 3.2 Change `Evaluator.verdict(level, compiled, attempt)` and `evaluate` to take an `Attempt`; check each criterion against its moment (`after` or the first frame); keep a convenience for a first-frame-only attempt
- [x] 3.3 Implement `prints` matching (split into lines, normalize, `ignoreCase`/`contains`)
- [x] 3.4 Add the customer failure details (missing/hidden target, not reached, nothing printed, something else printed, nothing printed when the screen appeared), with the pressed node named by its quoted text or its phrase
- [x] 3.5 Move `EvaluatorTest` to `Attempt` and add tests: criterion `after` a step, `after` a missing step, `prints` passing, printed during the first frame only, extra output, typo, each detail's exact wording

## 4. The customer

- [x] 4.1 Add `Customer` in `levels`: from the first frame's snapshot and console, perform each step on a `PreviewEngine` (find the first visible match, press or type by id), record each outcome, stop at the first step not found and mark the rest not reached; return null when the attempt crashed
- [x] 4.2 Record the console offset at which each step started, for the Console transcript
- [x] 4.3 Use `Customer` in `levels/src/test/.../Attempts.kt`, so the self-checking level tests run the customer like the game
- [x] 4.4 Customer tests through the real pipeline: Open Sign's starter (not found), an empty `onClick` (nothing printed), the solution (passes), and an `onClick` that throws (crashed)

## 5. Open Sign content

- [x] 5.1 Add the `order` press step and the `bell` `prints` criterion, with Tom's message, to `001-open-sign/level.json`
- [x] 5.2 Check the solution passes all four criteria and the starter still fails (`./gradlew :levels:test`)

## 6. The game

- [x] 6.1 In `SpikeController.start`, run the customer after the first frame and before the verdict; keep the customer's steps and their console offsets for the screen
- [x] 6.2 Console panel: render the transcript with `customer › pressed [X]` / `customer › typed "T" into [X]` lines and `(nothing printed)`, and the empty-state line for levels with a press step
- [x] 6.3 Preview caption "A customer pressed X" after a Run with steps, cleared when the player clicks the preview, edits or runs again
- [x] 6.4 Window check: Open Sign with an empty `onClick` shows the silent-bell reaction, the failed `bell` row and `(nothing printed)`; the solution completes the level and the Console shows `New order!`

## 7. Wrap-up

- [x] 7.1 Update the README's "How a level plays" if the Console screenshot changed, and regenerate screenshots with the window check
- [x] 7.2 `./gradlew build` passes

## RENAMED Requirements

- FROM: `### Requirement: Evaluation is a pure function of the snapshot`
- TO: `### Requirement: Evaluation is a pure function of the attempt record`

## MODIFIED Requirements

### Requirement: Evaluation is a pure function of the attempt record
The evaluator SHALL compute the verdict from the level's criteria and the attempt record alone: the first frame's moment and, for each customer step, its outcome and, if performed, its moment (a semantics snapshot and the text printed), as defined by the `customer-actions` capability. It SHALL NOT access the Compose scene, the player's classes or the player's thread, so it can be unit-tested with hand-built records and run on any thread.

#### Scenario: Unit test without Compose
- **WHEN** a test builds a snapshot by hand with two text nodes and evaluates a level's criteria against it
- **THEN** the verdict is produced without creating a scene or compiling code

#### Scenario: Hand-built step
- **WHEN** a test builds a record whose press step printed "New order!" and evaluates a `prints` criterion after that step
- **THEN** the criterion passes without creating a scene

### Requirement: When an attempt is evaluated
After each Run, an attempt SHALL be evaluated once it has compiled, loaded and rendered its first frame with no pending invalidations, and once the customer has finished the level's steps, if it has any. If the attempt failed to compile or crashed at runtime, including while the customer acted, the game SHALL NOT evaluate criteria. The verdict SHALL instead say that the attempt did not compile or crashed, and the existing diagnostics or runtime error SHALL be shown.

#### Scenario: Compile error
- **WHEN** the player's code has a type error
- **THEN** no criteria are evaluated and the verdict says the code did not compile

#### Scenario: Runtime crash
- **WHEN** the player's composable throws during its first composition
- **THEN** no criteria are evaluated and the verdict says the attempt crashed

#### Scenario: Crash in a pressed button
- **WHEN** the customer presses a button whose `onClick` throws
- **THEN** no criteria are evaluated and the verdict says the attempt crashed

## ADDED Requirements

### Requirement: Checking a criterion after a step
Every criterion MAY set `after`: the id of one of the level's customer steps. A criterion without `after` SHALL be checked against the first frame's moment. A criterion with `after` SHALL be checked against the moment recorded right after that step. If that step was not found or not reached, the criterion SHALL fail with a customer detail instead of being checked.

#### Scenario: Screen after a press
- **WHEN** a level presses "+" in a step `add` and has `{ "type": "exists", "id": "one", "after": "add", "node": { "text": "Count: 1" } }`, and the player's counter starts at 0 and adds 1 per press
- **THEN** the criterion passes, while the same criterion without `after` fails

#### Scenario: Step that never happened
- **WHEN** a criterion is checked after a step whose button is missing
- **THEN** it fails with a detail saying the customer couldn't find the button

### Requirement: Prints criterion
The `prints` criterion SHALL pass when the text printed during its moment, split into lines, contains a line matching its `text`. Matching SHALL follow the forgiving text rules of selectors: trimmed, whitespace collapsed, case-sensitive equality unless the criterion sets `ignoreCase: true` or `contains: true`. Without `after`, its moment is the first frame, so it checks what was printed while the screen first appeared.

#### Scenario: Tom's bell
- **WHEN** the customer presses "Order" and the button runs `println("New order!")`
- **THEN** `{ "type": "prints", "after": "order", "text": "New order!" }` passes

#### Scenario: Printed during the first frame, not the press
- **WHEN** the player calls `println("New order!")` in the composable's body and the button's `onClick` is empty
- **THEN** the same criterion fails, because nothing was printed during the press

#### Scenario: Extra output
- **WHEN** the press prints "Ding!" and then "New order!"
- **THEN** the criterion passes

### Requirement: Customer failure details
The evaluator SHALL generate a player-facing detail for criteria that fail because of the customer, naming nodes as selectors do and never including code. A pressed node SHALL be named by its selector's text in quotes when it has one, e.g. `"Order"`, and by its selector phrase otherwise. The failure reasons and their details SHALL be:
- **target missing:** "The customer couldn't find X to press." or "The customer couldn't find X to type into."
- **target hidden:** "The customer found X, but it has no size or is off the screen."
- **not reached:** the detail of the step that stopped the customer.
- **nothing printed:** "A customer pressed X, but nothing was printed." or "A customer typed \"T\" into X, but nothing was printed."
- **something else printed:** "A customer pressed X and \"Y\" was printed, but I wanted \"Z\".", where Y is the first line printed.
- **nothing printed when the screen appeared** (`prints` without `after`): "Nothing printed \"Z\" when your screen appeared."

#### Scenario: Empty onClick
- **WHEN** the player writes `Button(onClick = { }) { Text("Order") }` and Open Sign's `bell` criterion is checked
- **THEN** its detail is "A customer pressed \"Order\", but nothing was printed."

#### Scenario: Typo in the printed text
- **WHEN** the press prints "New oder!"
- **THEN** the detail is "A customer pressed \"Order\" and \"New oder!\" was printed, but I wanted \"New order!\"."

#### Scenario: No button at all
- **WHEN** the starter code of Open Sign is run
- **THEN** the `bell` criterion's detail is "The customer couldn't find a button \"Order\" to press."

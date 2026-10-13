## ADDED Requirements

### Requirement: Customer steps
A level MAY list `actions`: the steps a customer performs on the player's screen, in order. Each step SHALL have an `id`, a `type` and a target `node` selector, as defined by the `level-acceptance` capability. The step types SHALL be:
- `press`: press the target, like a mouse click.
- `enterText`: type the step's `text` into the target, replacing what it holds.

After each Run whose attempt renders its first frame, the game SHALL perform the level's steps once, in order, before the attempt is evaluated. A level without `actions` SHALL be evaluated from its first frame alone, as before.

#### Scenario: Open Sign's customer
- **WHEN** the player runs a correct solution for `001-open-sign`, whose only step presses the button "Order"
- **THEN** the customer presses the button once, after the first frame and before the checklist resolves

#### Scenario: No steps
- **WHEN** the player runs `000-hello-main-street`, which lists no actions
- **THEN** no customer acts and the verdict comes from the first frame

#### Scenario: Steps in order
- **WHEN** a level lists `enterText` into "Your name" with "Rosa", then `press` on "Send"
- **THEN** the text field holds "Rosa" before "Send" is pressed

### Requirement: Finding a step's target
Before each step, the customer SHALL look for the target in the screen as the previous step left it (the first frame, for the first step), and SHALL choose the first **visible** node that matches the step's selector, with the matching and visibility rules of the `level-acceptance` capability. If no visible node matches, the step SHALL NOT be performed and is recorded as not found, noting whether a matching node exists but is not visible. Every later step SHALL then be recorded as not reached and not performed.

#### Scenario: Missing button
- **WHEN** the starter code of `001-open-sign` runs, which shows no "Order" button
- **THEN** the press step is recorded as not found, and nothing is pressed

#### Scenario: Stopped customer
- **WHEN** a level has two steps and the first step's target is missing
- **THEN** the second step is recorded as not reached

### Requirement: Pressing
A `press` step SHALL send a pointer move, press and release at the centre of the target node, through the same input path as the player's mouse, so the press reaches whatever the player's layout puts there.

#### Scenario: Button nested in a row
- **WHEN** the player writes `Button(onClick = { println("New order!") }) { Row { Text("Order") } }` and the level presses `{ "role": "Button", "text": "Order" }`
- **THEN** the button's `onClick` runs once

### Requirement: Typing
An `enterText` step SHALL replace the target's text with the step's `text` through the target's set-text semantics action, which text fields provide, so the player's `onValueChange` runs as it does for typing. If the target has no set-text action, the step SHALL be recorded as not found.

#### Scenario: Typing into a text field
- **WHEN** the player shows `TextField(value = name, onValueChange = { name = it }, label = { Text("Your name") })` and a step types "Rosa" into `{ "text": "Your name" }`
- **THEN** after the step the field shows "Rosa"

#### Scenario: Not a text field
- **WHEN** a step types into `{ "text": "Your name" }` and that text is a plain `Text`
- **THEN** the step is recorded as not found

### Requirement: What each step records
After each performed step, the game SHALL render the scene's pending changes and record a moment: the semantics snapshot of the screen after the step, and the text printed while the step ran. The first frame SHALL be recorded the same way, with the text printed during the first composition. These moments, with each step's outcome, form the attempt record that the evaluator receives.

#### Scenario: Printed by a press
- **WHEN** the press on "Order" runs `println("New order!")`
- **THEN** that step's moment records "New order!" as printed, and the first frame's moment records nothing

#### Scenario: Screen after a press
- **WHEN** a press on "+" increments a `mutableStateOf` counter shown as `Text("Count: $count")`
- **THEN** the step's snapshot shows "Count: 1"

### Requirement: A crash while the customer acts
If player code throws while a step runs or while the scene renders afterwards, the game SHALL treat the attempt as crashed: the remaining steps SHALL NOT run, no criteria are evaluated, and the runtime error SHALL be shown as for any crash.

#### Scenario: onClick throws
- **WHEN** the button's `onClick` throws `IllegalStateException` and the customer presses it
- **THEN** the verdict says the attempt crashed and the runtime error points at the `onClick` line

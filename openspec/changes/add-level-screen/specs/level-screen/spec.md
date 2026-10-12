## ADDED Requirements

### Requirement: Level target
While a level is open, the screen SHALL show the level's target, the image rendered from its reference solution, as a thumbnail next to the player's preview. The target SHALL be loaded with the level and SHALL NOT trigger any compilation in the game. The target SHALL be used only for display, never to judge the attempt.

#### Scenario: Target before the first Run
- **WHEN** the player opens "Hello Main Street" and has not pressed Run yet
- **THEN** the target thumbnail shows Rosa's sign with "Rosa's Bakery" above "Fresh bread daily"

#### Scenario: Running does not recompile the target
- **WHEN** the player presses Run five times in a level
- **THEN** the compiler runs five times, once per Run, and never for the target

### Requirement: Target ghost before the first Run
Until the player's first Run in an open level renders a frame, the preview SHALL show the target faded (at about 25% opacity) with a caption saying it is the goal. The player's first rendered frame SHALL replace the ghost.

#### Scenario: Ghost then player frame
- **WHEN** the player opens a level, sees the faded target in the preview, and then presses Run on code that renders
- **THEN** the preview shows the player's frame instead of the ghost

### Requirement: Target overlay toggle
The screen SHALL offer a toggle that overlays the target semi-transparently (at about 50% opacity) on top of the player's preview, aligned to the same viewport. The overlay SHALL NOT block mouse input to the player's preview.

#### Scenario: Comparing with the target
- **WHEN** the player's preview shows the texts in the wrong order and the player turns the overlay on
- **THEN** the target appears over the preview so the mismatch is visible, and clicks still reach the player's UI

### Requirement: Requirements checklist
While a level is open, the screen SHALL show a checklist below the preview with one line per criterion, in the level's order, using each criterion's `description`. Before the first Run, every line SHALL show a neutral marker. After each Run, each line SHALL show ✓ if the criterion passed or ✗ if it failed. A failed line SHALL show the evaluator's generated detail and, if the criterion has one, its `message`. If the attempt did not compile or crashed, the checklist SHALL say so instead of marking lines.

#### Scenario: Starter code
- **WHEN** the player runs the starter code of "Hello Main Street"
- **THEN** both requirements show ✗, each with a detail saying which text can't be found on the screen

#### Scenario: Updated on every Run
- **WHEN** the player fixes one requirement and runs again
- **THEN** that line changes to ✓ and the other keeps ✗

#### Scenario: Before the first Run
- **WHEN** the player opens a level
- **THEN** the checklist lists every requirement's description with a neutral marker

### Requirement: Level complete
When every criterion passes, the screen SHALL show a "Level complete" state that is clearly distinct from a partial result, with a Continue button. Continue SHALL open the next level in `index.json` order. On the last level, the state SHALL say that more levels are coming instead of offering Continue.

#### Scenario: Solving Hello Main Street
- **WHEN** the player runs a correct solution for "Hello Main Street"
- **THEN** both requirements show ✓, the "Level complete" state appears, and Continue opens `001-open-sign`

#### Scenario: Last level
- **WHEN** the player completes the last level in the index
- **THEN** the "Level complete" state appears without Continue and says more of Main Street is coming soon

### Requirement: Panels per level
While a level is open, the screen SHALL always show the preview, the target, the checklist and the compile problems list. The Console and Recompositions panels SHALL be shown only if the level lists them in its `panels` field. In the sandbox (no level open), every panel SHALL be shown.

#### Scenario: Hello Main Street hides extra panels
- **WHEN** the player opens "Hello Main Street", which lists no panels
- **THEN** neither the Console nor the Recompositions panel is visible

#### Scenario: A level that uses the Console
- **WHEN** the player opens `001-open-sign`, which lists `console`
- **THEN** the Console panel is visible and the Recompositions panel is not

### Requirement: Panel unlock
A panel SHALL be unlocked by the first level, in `index.json` order, whose `panels` field lists it. When that level is opened, the screen SHALL highlight the panel and show a short explanation of what it does, until the player dismisses it or about 8 seconds pass. Opening a later level that also lists the panel SHALL NOT highlight it again. The explanations SHALL be part of the game, one per panel, not part of level data.

#### Scenario: Console unlock
- **WHEN** the player opens `001-open-sign`, the first level that lists `console`
- **THEN** the Console panel is highlighted with an explanation that it shows what the player's code prints

#### Scenario: Not the first level with the panel
- **WHEN** a later level also lists `console` and the player opens it
- **THEN** the Console panel is shown without a highlight

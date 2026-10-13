## ADDED Requirements

### Requirement: The customer in the Console
When the customer performs a step, the Console SHALL show a customer line before that step's output, in the Console's comment color:
- `customer › pressed [X]` for a press, X being the target selector's text, or its role name when it has no text;
- `customer › typed "T" into [X]` for typing.

If the step printed nothing, the line `(nothing printed)` SHALL follow it in `editorErrorText`. Output printed before the first step, and output from the player's own clicks after the customer is done, SHALL appear without a customer line. Before the first Run, the Console of a level with a `press` step SHALL read "A customer will press your buttons here after Run."; other levels keep the Console's usual empty line.

#### Scenario: Tom's bell rings
- **WHEN** the player runs a correct solution for Open Sign
- **THEN** the Console shows `customer › pressed [Order]` followed by `New order!`

#### Scenario: Silent bell
- **WHEN** the button's `onClick` is empty
- **THEN** the Console shows `customer › pressed [Order]` followed by `(nothing printed)`

#### Scenario: Before the first Run
- **WHEN** the player opens Open Sign
- **THEN** the Console reads "A customer will press your buttons here after Run."

### Requirement: The customer on the preview
After a Run in which the customer performed at least one step, the preview SHALL show the player's screen as the customer left it, with the caption "A customer pressed X" (or "A customer typed into X") for the last step performed, using the same X as the Console. The caption SHALL disappear when the player clicks the preview, edits the code or runs again. The scene SHALL NOT be reloaded to undo the customer's steps.

#### Scenario: After the customer presses Order
- **WHEN** the player runs Open Sign with a button "Order"
- **THEN** the preview's caption reads "A customer pressed Order"

#### Scenario: The player takes over
- **WHEN** the player then clicks the button in the preview
- **THEN** the caption disappears and the click reaches the scene

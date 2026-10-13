## ADDED Requirements

### Requirement: Generated failure details
For each failed criterion, the evaluator SHALL generate a short player-facing detail explaining why it failed, from the failure reason alone. The detail SHALL describe nodes using the selector's fields (e.g. `"Rosa's Bakery"`, `a button "Order"`, `an image described as "logo"`) and SHALL NOT include code. The failure reasons and their details SHALL be:
- **missing**: no visible node matches → "I can't find X on the screen."
- **hidden**: a matching node exists but is not visible → "X is there, but it has no size or is off the screen."
- **wrong count**: `exists` with `count` → "I see N matches for X, but there should be M."
- **present**: `absent` → "X should not be on the screen."
- **reversed**: an order criterion where the two nodes are in the opposite order → for `below`, e.g. "X is above Y."
- **not in line**: an order criterion where the nodes are present but neither order holds, e.g. side by side for `above` → "X and Y are side by side; X should be under Y."

#### Scenario: Missing text
- **WHEN** `{ "type": "exists", "node": { "text": "Rosa's Bakery" } }` fails because no node has that text
- **THEN** its detail is "I can't find \"Rosa's Bakery\" on the screen."

#### Scenario: Reversed order
- **WHEN** a `below` criterion with `a` "Fresh bread daily" and `b` "Rosa's Bakery" fails because "Fresh bread daily" is on top
- **THEN** its detail says "Fresh bread daily" is above "Rosa's Bakery"

#### Scenario: Side by side
- **WHEN** the same criterion fails because both texts are in a `Row`
- **THEN** its detail says the two texts are side by side and "Fresh bread daily" should be under "Rosa's Bakery"

#### Scenario: Button phrase
- **WHEN** `{ "type": "exists", "node": { "role": "Button", "text": "Order" } }` fails because there is no button
- **THEN** its detail is "I can't find a button \"Order\" on the screen."

## MODIFIED Requirements

### Requirement: Geometric order criteria
The `above` criterion SHALL pass when some visible node A matching selector `a` and some different visible node B matching selector `b` satisfy `A.bottom ≤ B.top + tolerance`. The `leftOf` criterion SHALL pass under the same conditions with `A.right ≤ B.left + tolerance`. The `below` criterion SHALL pass exactly when `above` with `a` and `b` swapped passes, and the `rightOf` criterion exactly when `leftOf` with `a` and `b` swapped passes. Bounds and `toleranceDp` SHALL be measured in dp, and `toleranceDp` SHALL default to 1 dp. If either selector matches no visible node, the criterion SHALL fail. Only the relative order is checked, never absolute positions, sizes or spacing.

#### Scenario: Above in a column
- **WHEN** "Rosa's Bakery" and "Fresh bread daily" are in a `Column` in that order, at any spacing
- **THEN** `{ "type": "above", "a": { "text": "Rosa's Bakery" }, "b": { "text": "Fresh bread daily" } }` passes

#### Scenario: Below is above swapped
- **WHEN** the same column is checked with `{ "type": "below", "a": { "text": "Fresh bread daily" }, "b": { "text": "Rosa's Bakery" } }`
- **THEN** the criterion passes

#### Scenario: Side by side is not above
- **WHEN** both texts are in a `Row`
- **THEN** the same `above` criterion fails, and `leftOf` with the same selectors passes

#### Scenario: Right of
- **WHEN** a `Row` shows "Tom's Flowers" and then a button "Order"
- **THEN** `{ "type": "rightOf", "a": { "role": "Button", "text": "Order" }, "b": { "text": "Tom's Flowers" } }` passes

#### Scenario: Slight overlap within tolerance
- **WHEN** the bottom of A's bounds is 0.5 dp below the top of B's bounds, because of a negative spacing
- **THEN** the `above` criterion passes with the default tolerance

### Requirement: Verdict with player-facing messages
The evaluator SHALL return a verdict listing every criterion in the level's order, with its id, its `description`, and whether it passed. A failed criterion SHALL also carry its generated detail and, if it has one, its `message`. The verdict SHALL pass only when every criterion passes. The game SHALL show the verdict to the player after each Run, as defined by the `level-screen` capability.

#### Scenario: Partial progress
- **WHEN** an attempt at "Hello Main Street" shows only "Rosa's Bakery"
- **THEN** the verdict lists `title` as passed and `order` as failed with a detail saying "Fresh bread daily" can't be found, and the level is not passed

#### Scenario: All criteria pass
- **WHEN** every criterion passes
- **THEN** the verdict is passed and no criterion carries a detail

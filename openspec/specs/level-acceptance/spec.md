# level-acceptance Specification

## Purpose
Deciding whether an attempt satisfies a level from its structure, never its pixels: a snapshot of the merged semantics tree, node selectors with forgiving text matching, implicit visibility checks, the presence and geometric order criteria, and the per-criterion verdict with player-facing messages.
## Requirements
### Requirement: Structural acceptance, never pixels
The game SHALL decide whether an attempt satisfies a level only from a snapshot of the attempt's merged semantics tree and the bounds of its nodes. Acceptance SHALL NOT compare rendered pixels or images, so that two visually different but structurally correct solutions get the same verdict.

#### Scenario: Different styling, same structure
- **WHEN** two attempts both show "Rosa's Bakery" above "Fresh bread daily", one with default styling and one with a larger font, padding and a background color
- **THEN** both attempts pass `000-hello-main-street`

### Requirement: Evaluation is a pure function of the snapshot
The evaluator SHALL compute the verdict from the level's criteria and a semantics snapshot alone. It SHALL NOT access the Compose scene, the player's classes or the player's thread, so it can be unit-tested with hand-built snapshots and run on any thread.

#### Scenario: Unit test without Compose
- **WHEN** a test builds a snapshot by hand with two text nodes and evaluates a level's criteria against it
- **THEN** the verdict is produced without creating a scene or compiling code

### Requirement: When an attempt is evaluated
After each Run, an attempt SHALL be evaluated once it has compiled, loaded and rendered its first frame with no pending invalidations. If the attempt failed to compile or crashed at runtime, the game SHALL NOT evaluate criteria. The verdict SHALL instead say that the attempt did not compile or crashed, and the existing diagnostics or runtime error SHALL be shown.

#### Scenario: Compile error
- **WHEN** the player's code has a type error
- **THEN** no criteria are evaluated and the verdict says the code did not compile

#### Scenario: Runtime crash
- **WHEN** the player's composable throws during its first composition
- **THEN** no criteria are evaluated and the verdict says the attempt crashed

### Requirement: Node selectors
A selector SHALL match semantics nodes by any combination of `text`, `role` and `contentDescription`, and a node matches only if every field given in the selector matches. A selector SHALL set at least one of these fields.
- `text` matches if any one of the node's text values matches, or if all of its text values joined with single spaces match.
- `role` matches the node's semantics role by name: `Button`, `Checkbox`, `Switch`, `RadioButton`, `Tab`, `Image`, `DropdownList`, `ValuePicker` or `Carousel`.
- `contentDescription` matches like `text`, against the node's content descriptions.

Selectors SHALL match nodes of the **merged** semantics tree, so that a `Button` whose content is `Text("Order")` is one node with role `Button` and text "Order", however the player nests its content.

#### Scenario: Button by role and text
- **WHEN** the player writes `Button(onClick = {}) { Row { Text("Order") } }`
- **THEN** the selector `{ "role": "Button", "text": "Order" }` matches the button's node

#### Scenario: Role mismatch
- **WHEN** the player shows `Text("Order")` without a button
- **THEN** the selector `{ "role": "Button", "text": "Order" }` matches no node

### Requirement: Forgiving text matching
Before comparison, both the expected text and the node's text SHALL be trimmed, and every run of whitespace in them SHALL be collapsed into a single space. Matching SHALL be case-sensitive equality by default. A selector MAY set `ignoreCase: true` to compare case-insensitively and `contains: true` to accept a node whose text contains the expected text. The same rules apply to `contentDescription`.

#### Scenario: Extra whitespace
- **WHEN** the player writes `Text("  Rosa's   Bakery ")`
- **THEN** the selector `{ "text": "Rosa's Bakery" }` matches it

#### Scenario: Different case by default
- **WHEN** the player writes `Text("rosa's bakery")`
- **THEN** the selector `{ "text": "Rosa's Bakery" }` does not match it

#### Scenario: Opting in to case-insensitive matching
- **WHEN** the player writes `Text("ROSA'S BAKERY")`
- **THEN** the selector `{ "text": "Rosa's Bakery", "ignoreCase": true }` matches it

#### Scenario: Contains
- **WHEN** the player writes `Text("Welcome to Rosa's Bakery!")`
- **THEN** the selector `{ "text": "Rosa's Bakery", "contains": true }` matches it

### Requirement: Only visible nodes count
A node SHALL be considered by any criterion only if its bounds have a non-zero width and height and overlap the level's viewport with a non-zero area. Nodes that fail this check SHALL be treated as absent.

#### Scenario: Zero-size text
- **WHEN** the player shows `Text("Rosa's Bakery", Modifier.size(0.dp))`
- **THEN** an `exists` criterion for "Rosa's Bakery" fails

#### Scenario: Text outside the viewport
- **WHEN** the player shows "Rosa's Bakery" offset entirely below the bottom of the viewport
- **THEN** an `exists` criterion for "Rosa's Bakery" fails

### Requirement: Presence criteria
The `exists` criterion SHALL pass when at least one visible node matches its `node` selector. If it sets `count`, it SHALL pass only when exactly `count` visible nodes match. The `absent` criterion SHALL pass when no visible node matches its `node` selector.

#### Scenario: Exists
- **WHEN** a criterion is `{ "type": "exists", "node": { "text": "Fresh bread daily" } }` and the attempt shows that text
- **THEN** the criterion passes

#### Scenario: Exact count
- **WHEN** a criterion is `{ "type": "exists", "node": { "role": "Button" }, "count": 2 }` and the attempt shows three buttons
- **THEN** the criterion fails

#### Scenario: Absent
- **WHEN** a criterion is `{ "type": "absent", "node": { "text": "Closed" } }` and the attempt shows no such text
- **THEN** the criterion passes

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


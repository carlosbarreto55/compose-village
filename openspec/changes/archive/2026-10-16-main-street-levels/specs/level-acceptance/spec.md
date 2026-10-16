## ADDED Requirements

### Requirement: Grouped criteria
The `all` criterion SHALL group several checks into one checklist row. It has the usual `id`, `description`, optional `message` and optional `after`, and a `checks` list. Each check SHALL be an `exists`, `absent`, `above`, `below`, `leftOf` or `rightOf` criterion written without `id`, `description`, `message` or `after`.

The `all` criterion SHALL pass when every check passes against the criterion's moment: the first frame, or the moment after its `after` step. When it fails, its detail SHALL be the detail of the first failing check, in the order the checks are written. The verdict SHALL list only the `all` criterion, never its checks.

#### Scenario: Every dish
- **WHEN** an `all` criterion checks that "Spaghetti", "Lasagna", "Risotto" and "Tiramisu" exist, and all four are on screen
- **THEN** the criterion passes and the verdict lists it as one row

#### Scenario: First failing check
- **WHEN** the same criterion is checked and only "Spaghetti" and "Risotto" are on screen
- **THEN** it fails with the detail "I can't find \"Lasagna\" on the screen."

#### Scenario: Grouped checks after a step
- **WHEN** an `all` criterion with `after: "coral"` checks that "08:00" exists and "10:30" is absent
- **THEN** both checks are made against the screen right after the `coral` step

### Requirement: Details after a step
When a criterion that is checked after a customer step fails, and that step was performed, its detail SHALL say what the customer did. This SHALL NOT apply to `prints` criteria, which have their own customer details, or to steps that were not found or not reached.
- If the step's screen is the same as the screen before the step, the detail SHALL be "A customer pressed X, but the screen didn't change." or "A customer typed \"T\" into X, but the screen didn't change."
- Otherwise, the detail SHALL be "After a customer pressed X: " or "After a customer typed \"T\" into X: ", followed by the detail the criterion would have without `after`.

X SHALL name the step's node as customer failure details do. The screen before a step SHALL be the screen after the previous step, or the first frame for the first step. Two screens SHALL be the same when they have the same visible nodes, in tree order, with the same texts, content descriptions, roles and bounds. Node ids SHALL NOT count. The comparison SHALL use only the attempt record.

#### Scenario: A press that changes nothing
- **WHEN** the scores are plain variables, the customer presses "+1 Blue", and "Blue: 1" is checked after that step
- **THEN** the detail is "A customer pressed \"+1 Blue\", but the screen didn't change."

#### Scenario: A press that changes the wrong thing
- **WHEN** pressing "+1 Green" makes the board read "Blue: 3" and "Green: 3", and "Green: 1" is checked after that step
- **THEN** the detail is "After a customer pressed \"+1 Green\": I can't find \"Green: 1\" on the screen."

#### Scenario: Typing into an unbound field
- **WHEN** the customer types "coral" into a field whose value never changes, and a criterion is checked after that step
- **THEN** its detail is "A customer typed \"coral\" into \"Where to?\", but the screen didn't change."

#### Scenario: First-frame criteria keep their details
- **WHEN** a criterion without `after` fails
- **THEN** its detail does not mention the customer

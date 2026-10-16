## MODIFIED Requirements

### Requirement: Header
The header SHALL show:
- the brand mark and "Compose Village";
- a breadcrumb with the street ("Main Street"), the open level's number and title ("2 · Open Sign") and one progress dot per level, in play order;
- the build status;
- the Run button.

A dot is filled with `primary` for a completed level (saved progress, so also from earlier sessions), has an `accent` ring for the open level, and is muted for the others. In the sandbox, the breadcrumb SHALL read "Main Street › Sandbox" and show no dots.

"Main Street" in the breadcrumb SHALL be a link that opens the home screen, and SHALL do nothing while a Run is in progress.

#### Scenario: Breadcrumb on the second level
- **WHEN** the player opens `001-open-sign` after completing `000-hello-main-street`
- **THEN** the breadcrumb reads "Main Street › 2 · Open Sign", the first dot is filled and the second is ringed

#### Scenario: Going home from a level
- **WHEN** the player clicks "Main Street" in the breadcrumb of `001-open-sign`
- **THEN** the home screen opens

#### Scenario: Going home from the sandbox
- **WHEN** the player clicks "Main Street" in the Sandbox's breadcrumb
- **THEN** the home screen opens

### Requirement: Street footer
The footer SHALL show the street's name, one shop per level in play order, and "N of M shops open":
- a shop is lit (`glow`) once its level has been completed, in this session or an earlier one;
- the open level's shop has a dashed `accent` outline;
- hovering a shop shows its number and title;
- clicking a shop opens its level;
- a Sandbox entry at the end of the footer opens the sandbox.

While a Run is in progress, shops and the Sandbox entry SHALL do nothing.

#### Scenario: Completing a level lights its shop
- **WHEN** the player completes `000-hello-main-street`
- **THEN** the first shop is lit and the footer reads "1 of 5 shops open"

#### Scenario: Opening a level from the street
- **WHEN** the player clicks the second shop, having never edited `001-open-sign`
- **THEN** `001-open-sign` opens with its starter code

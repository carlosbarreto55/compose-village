## MODIFIED Requirements

### Requirement: Header
The header SHALL show:
- the brand mark and "Compose Village";
- a breadcrumb with the street ("Main Street"), the open level's number and title ("2 · Open Sign") and one progress dot per level, in play order;
- the build status;
- the Run button.

A dot is filled with `primary` for a completed level (saved progress, so also from earlier sessions), has a `wood` ring for the open level, and is muted for the others. In the sandbox, the breadcrumb SHALL read "Main Street › Sandbox" and show no dots.

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

## MODIFIED Requirements

### Requirement: Strict level validation
Loading SHALL reject level data that is malformed or ambiguous, with an error that names the level folder and the offending field. The checks SHALL cover:
- invalid JSON;
- unknown fields (to catch typos);
- missing required fields;
- an unsupported `formatVersion`;
- unknown criterion types;
- unknown role names;
- unknown professions;
- a villager id that doesn't match its pattern;
- selectors with no matching field set, in criteria, in grouped checks and in steps;
- a non-positive viewport;
- unknown or repeated panel names;
- repeated or blank concepts;
- an empty, blank or too long `intro` line, or more than four of them;
- a blank or too long `celebration` line, or more than three of them;
- a too long `thanks` line;
- unknown step types;
- step ids that are blank, repeated or don't match their pattern;
- an `enterText` step without `text`;
- an `after` that names no step of the level;
- a `prints` criterion with a blank `text`;
- an `all` criterion with fewer than two checks;
- a grouped check that has an `id`, `description`, `message` or `after`, or whose type is `all` or `prints`;
- a missing starter file.

Errors in a grouped check SHALL name the criterion and the check's position, e.g. `criterion 'dishes', check 2`.

A level that fails validation SHALL NOT be playable.

#### Scenario: Typo in a field name
- **WHEN** a criterion is written with `"descripton"` instead of `"description"`
- **THEN** loading fails, naming the level, the criterion and the unknown field `descripton`

#### Scenario: Unknown role
- **WHEN** a selector uses `"role": "Buton"`
- **THEN** loading fails with an error listing the valid role names

#### Scenario: Unsupported format version
- **WHEN** a `level.json` declares `"formatVersion": 2`
- **THEN** loading fails, saying that this game supports format version 1

#### Scenario: Unknown panel
- **WHEN** a level lists `"panels": ["consol"]`
- **THEN** loading fails with an error listing the valid panel names

#### Scenario: Unknown profession
- **WHEN** a level's villager has `"profession": "gardener"`
- **THEN** loading fails with an error listing the valid professions

#### Scenario: Too many intro lines
- **WHEN** a level has five `intro` lines
- **THEN** loading fails, naming the level and `intro`, and saying at most four lines are allowed

#### Scenario: Intro line too long
- **WHEN** an `intro` line has 141 characters
- **THEN** loading fails, naming the level, the line's position and the 140-character limit

#### Scenario: After an unknown step
- **WHEN** a criterion has `"after": "ordr"` and the level's only step is `order`
- **THEN** loading fails, naming the criterion and saying `ordr` is not one of the level's steps

#### Scenario: Unknown step type
- **WHEN** a step has `"type": "click"`
- **THEN** loading fails, naming the step and listing the valid step types `press` and `enterText`

#### Scenario: A group of one
- **WHEN** an `all` criterion `dishes` has a single check
- **THEN** loading fails, naming `dishes` and saying an `all` criterion needs at least two checks

#### Scenario: A check with its own step
- **WHEN** the second check of an `all` criterion `coral` has `"after": "coral"`
- **THEN** loading fails, naming `criterion 'coral', check 2` and saying grouped checks use the criterion's `after`

#### Scenario: Nested group
- **WHEN** a grouped check has `"type": "all"`
- **THEN** loading fails, naming the check and listing the types a grouped check can have

## REMOVED Requirements

### Requirement: Initial level content
**Reason**: The content of the five Main Street levels moves to the new `main-street` capability. `level-definition` keeps the format, the loader and the self-checking rules.
**Migration**: The Hello Main Street and Open Sign requirements, with their scenarios, are carried over unchanged to `main-street` ("Hello Main Street", "Open Sign"). The scenario "Second level is data only" becomes "A level is data only" under "Main Street levels".

# level-definition Specification

## Purpose
The level data model and its on-disk layout: one folder per level (`level.json` and starter code) listed in an ordered `index.json`, strict loading and validation of level data, test-only reference solutions, and the self-checking test every level must pass, so new levels are added by writing data files.
## Requirements
### Requirement: Level data model
A level SHALL be defined by these fields:
- `formatVersion` (required, integer, `1` in this version).
- `id` (required): a stable identifier matching `^[a-z0-9]+(-[a-z0-9]+)*$`, used as the key for player progress. It MUST NOT change once a level is released.
- `title` (required, non-blank): the level's short name, e.g. "Open Sign", shown in the breadcrumb, on the street and on the Next button.
- `villager` (required): the villager who asks, with:
  - an `id` matching `^[a-z]+(-[a-z]+)*$` (e.g. `tom`), which picks their bust;
  - a non-blank `name` (e.g. "Tom");
  - a `profession`, one of `baker`, `florist`, `chef`, `teacher`, `boat-captain`, `fishmonger`, `mail-carrier` or `mayor`.
- `shop` (required, non-blank): the name of the villager's shop, e.g. "Tom's Flowers", shown when the level is complete.
- `request` (required, non-blank): the villager's request, shown to the player as the level's task. Phrases in double quotes are the key texts of the request.
- `concepts` (optional, default none): the Compose concepts the level introduces, e.g. `["Row", "Button", "onClick"]`, each non-blank and listed at most once.
- `intro` (optional, default none): one to four lines the villager says when the level opens, each non-blank and at most 140 characters.
- `celebration` (optional, default none): up to three lines the villager says when the level is first completed, before the thanks line, each non-blank and at most 140 characters.
- `thanks` (optional): the villager's line when the level is complete. If given, it SHALL NOT be blank and SHALL be at most 140 characters.
- `entryFunction` (optional, default `PlayerScreen`): the composable the player must write.
- `starter` (optional, default `Starter.kt`): the starter code file in the level's folder.
- `viewport` (optional, default 360 × 640 dp): `widthDp` and `heightDp`, both positive.
- `panels` (optional, default none): the extra panels the level shows, from `console` and `recompositions`, each listed at most once.
- `actions` (optional, default none): the customer's steps, in order, as defined by the `customer-actions` capability. Each step has:
  - an `id` matching `^[a-z0-9]+(-[a-z0-9]+)*$`, unique within the level, which criteria name in `after`;
  - a `type`, `press` or `enterText`;
  - a `node` selector for its target;
  - for `enterText`, the `text` to type (it may be empty).
- `criteria` (required, at least one): the acceptance criteria defined by the `level-acceptance` capability.

Each criterion SHALL have an `id` that is unique within the level and a non-blank player-facing `description` of what it requires. A criterion MAY have a `message`, an extra line in the villager's voice shown when it fails. If a `message` is given, it SHALL NOT be blank. A criterion MAY have `after`, naming the step whose result it checks.

#### Scenario: Minimal level
- **WHEN** a `level.json` has `formatVersion`, `id`, `title`, `villager` (with `id`, `name` and `profession`), `shop`, `request` and one valid criterion with an `id` and a `description`
- **THEN** it loads with `entryFunction` `PlayerScreen`, starter `Starter.kt`, a 360 × 640 dp viewport, no concepts, no intro, no celebration, no thanks line, no extra panels and no customer steps

#### Scenario: Duplicate criterion ids
- **WHEN** two criteria of the same level have the id `title`
- **THEN** loading the level fails with an error naming the level and the duplicate id

#### Scenario: Missing description
- **WHEN** a criterion has a `message` but no `description`
- **THEN** loading the level fails with an error naming the criterion and the missing `description`

#### Scenario: Old resident field
- **WHEN** a `level.json` still has `"resident": "Tom, the florist"` instead of `villager`
- **THEN** loading fails, naming the unknown field `resident` and the missing field `villager`

#### Scenario: Villager without an id
- **WHEN** a level's villager is `{ "name": "Tom", "profession": "florist" }`
- **THEN** loading fails, naming the missing field `villager.id`

#### Scenario: A press step
- **WHEN** a level lists `"actions": [{ "id": "order", "type": "press", "node": { "role": "Button", "text": "Order" } }]`
- **THEN** it loads with one press step `order` targeting a button "Order"

### Requirement: Levels are data files
Each level SHALL live in its own folder named after its `id`, under the `levels/` resource root. The folder contains `level.json` and the starter code file. An `index.json` at the resource root SHALL list the level ids in play order. Adding a level SHALL require only new data files and an index entry, with no code changes.

#### Scenario: Adding a level
- **WHEN** an author adds a folder `levels/001-open-sign/` with a valid `level.json` and `Starter.kt`, and appends `001-open-sign` to `index.json`
- **THEN** the game loads the new level after `000-hello-main-street` without any code change

#### Scenario: Folder and id disagree
- **WHEN** `levels/001-open-sign/level.json` declares the id `001-open-shop`
- **THEN** loading the level fails with an error naming both the folder and the declared id

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

### Requirement: Starter code
When a level is opened, the editor SHALL contain the level's starter code. The starter code SHALL compile, and SHALL NOT already satisfy all of the level's criteria.

#### Scenario: Opening a level
- **WHEN** the player opens `000-hello-main-street`
- **THEN** the editor shows that level's `Starter.kt` and the request text is visible next to it

### Requirement: Reference solutions are test-only
Each level SHALL have a reference solution `Solution.kt` stored under test resources, in a folder named after the level id. Reference solutions SHALL NOT be included in the game's runtime classpath or its packaged distributable. Only the target image rendered from a solution at build time SHALL ship. A solution SHALL show nothing that the level's criteria do not check, so the target never asks for more than the checklist does.

#### Scenario: Solutions are not shipped
- **WHEN** the macOS distributable is built
- **THEN** no `Solution.kt` file appears in any jar inside the bundle

### Requirement: Self-checking levels
The build's test suite SHALL check every level listed in `index.json` through the real compile pipeline, preview engine and customer, exactly as the game runs an attempt:
- the level loads without validation errors;
- its `Solution.kt` compiles and passes every criterion;
- its `Starter.kt` compiles and fails at least one criterion;
- the index lists exactly the level folders present, with no missing, extra or duplicate ids.

A level that does not meet these checks SHALL fail the build.

#### Scenario: Broken solution
- **WHEN** an author edits a level's criteria so that its `Solution.kt` no longer satisfies them
- **THEN** `./gradlew test` fails, naming the level and the failing criterion ids

#### Scenario: Starter already solves the level
- **WHEN** a level's `Starter.kt` passes all of its criteria
- **THEN** `./gradlew test` fails, naming the level

#### Scenario: Folder missing from the index
- **WHEN** a level folder exists that `index.json` does not list
- **THEN** `./gradlew test` fails, naming the folder

### Requirement: Level targets
For every level, the build SHALL render the level's reference solution through the real compile pipeline and preview engine, at the level's viewport and 2× density, and package the result as `target.png` in the level's folder among the game's resources. The rendered target SHALL be produced as a generated resource that is never committed, so it cannot drift from the solution. The build SHALL fail if a solution does not compile or crashes while rendering. Loading a level SHALL provide its target image if present.

#### Scenario: Target in the bundle
- **WHEN** the macOS distributable is built
- **THEN** the levels jar contains `levels/000-hello-main-street/target.png`, an 800 × 600 pixel image, and no `Solution.kt`

#### Scenario: Solution changed
- **WHEN** an author changes a level's `Solution.kt` and rebuilds
- **THEN** the packaged `target.png` shows the new solution


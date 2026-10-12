# level-definition Specification

## Purpose
The level data model and its on-disk layout: one folder per level (`level.json` and starter code) listed in an ordered `index.json`, strict loading and validation of level data, test-only reference solutions, and the self-checking test every level must pass, so new levels are added by writing data files.

## Requirements
### Requirement: Level data model
A level SHALL be defined by these fields:
- `formatVersion` (required, integer, `1` in this version).
- `id` (required): a stable identifier matching `^[a-z0-9]+(-[a-z0-9]+)*$`, used as the key for player progress. It MUST NOT change once a level is released.
- `resident` (required): the villager who asks, e.g. "Rosa, the baker".
- `request` (required, non-blank): the villager's request, shown to the player as the level's task.
- `entryFunction` (optional, default `PlayerScreen`): the composable the player must write.
- `starter` (optional, default `Starter.kt`): the starter code file in the level's folder.
- `viewport` (optional, default 360 × 640 dp): `widthDp` and `heightDp`, both positive.
- `criteria` (required, at least one): the acceptance criteria defined by the `level-acceptance` capability.

Each criterion SHALL have an `id` that is unique within the level and a non-blank player-facing `message`.

#### Scenario: Minimal level
- **WHEN** a `level.json` has `formatVersion`, `id`, `resident`, `request` and one valid criterion
- **THEN** it loads with `entryFunction` `PlayerScreen`, starter `Starter.kt` and a 360 × 640 dp viewport

#### Scenario: Duplicate criterion ids
- **WHEN** two criteria of the same level have the id `title`
- **THEN** loading the level fails with an error naming the level and the duplicate id

### Requirement: Levels are data files
Each level SHALL live in its own folder named after its `id`, under the `levels/` resource root. The folder contains `level.json` and the starter code file. An `index.json` at the resource root SHALL list the level ids in play order. Adding a level SHALL require only new data files and an index entry, with no code changes.

#### Scenario: Adding a level
- **WHEN** an author adds a folder `levels/001-open-sign/` with a valid `level.json` and `Starter.kt`, and appends `001-open-sign` to `index.json`
- **THEN** the game loads the new level after `000-hello-main-street` without any code change

#### Scenario: Folder and id disagree
- **WHEN** `levels/001-open-sign/level.json` declares the id `001-open-shop`
- **THEN** loading the level fails with an error naming both the folder and the declared id

### Requirement: Strict level validation
Loading SHALL reject level data that is malformed or ambiguous, with an error that names the level folder and the offending field. The checks SHALL cover: invalid JSON, unknown fields (to catch typos), missing required fields, an unsupported `formatVersion`, unknown criterion types, unknown role names, selectors with no matching field set, a non-positive viewport and a missing starter file. A level that fails validation SHALL NOT be playable.

#### Scenario: Typo in a field name
- **WHEN** a criterion is written with `"mesage"` instead of `"message"`
- **THEN** loading fails, naming the level, the criterion and the unknown field `mesage`

#### Scenario: Unknown role
- **WHEN** a selector uses `"role": "Buton"`
- **THEN** loading fails with an error listing the valid role names

#### Scenario: Unsupported format version
- **WHEN** a `level.json` declares `"formatVersion": 2`
- **THEN** loading fails, saying that this game supports format version 1

### Requirement: Starter code
When a level is opened, the editor SHALL contain the level's starter code. The starter code SHALL compile, and SHALL NOT already satisfy all of the level's criteria.

#### Scenario: Opening a level
- **WHEN** the player opens `000-hello-main-street`
- **THEN** the editor shows that level's `Starter.kt` and the request text is visible next to it

### Requirement: Reference solutions are test-only
Each level SHALL have a reference solution `Solution.kt` stored under test resources, in a folder named after the level id. Reference solutions SHALL NOT be included in the game's runtime classpath or its packaged distributable.

#### Scenario: Solutions are not shipped
- **WHEN** the macOS distributable is built
- **THEN** no `Solution.kt` file appears in any jar inside the bundle

### Requirement: Self-checking levels
The build's test suite SHALL check every level listed in `index.json` through the real compile pipeline and preview engine:
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

### Requirement: Initial level content
The game SHALL ship exactly one level, `000-hello-main-street`, as the test level for the domain. In it, Rosa the baker asks for a sign with the text "Rosa's Bakery" on top and "Fresh bread daily" below it. Its criteria SHALL check that both texts exist and that "Rosa's Bakery" is above "Fresh bread daily".

#### Scenario: Solving the test level
- **WHEN** the player writes a `PlayerScreen` that shows `Text("Rosa's Bakery")` above `Text("Fresh bread daily")` in a `Column`
- **THEN** the verdict passes all three criteria

#### Scenario: Wrong order
- **WHEN** the player shows both texts in a `Column` with "Fresh bread daily" first
- **THEN** the verdict fails only the ordering criterion and shows its message, "Rosa wants her name on top."


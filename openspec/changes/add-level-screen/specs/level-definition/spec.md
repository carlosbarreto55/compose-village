## ADDED Requirements

### Requirement: Level targets
For every level, the build SHALL render the level's reference solution through the real compile pipeline and preview engine, at the level's viewport and 2× density, and package the result as `target.png` in the level's folder among the game's resources. The rendered target SHALL be produced as a generated resource that is never committed, so it cannot drift from the solution. The build SHALL fail if a solution does not compile or crashes while rendering. Loading a level SHALL provide its target image if present.

#### Scenario: Target in the bundle
- **WHEN** the macOS distributable is built
- **THEN** the levels jar contains `levels/000-hello-main-street/target.png`, an 800 × 600 pixel image, and no `Solution.kt`

#### Scenario: Solution changed
- **WHEN** an author changes a level's `Solution.kt` and rebuilds
- **THEN** the packaged `target.png` shows the new solution

## MODIFIED Requirements

### Requirement: Level data model
A level SHALL be defined by these fields:
- `formatVersion` (required, integer, `1` in this version).
- `id` (required): a stable identifier matching `^[a-z0-9]+(-[a-z0-9]+)*$`, used as the key for player progress. It MUST NOT change once a level is released.
- `resident` (required): the villager who asks, e.g. "Rosa, the baker".
- `request` (required, non-blank): the villager's request, shown to the player as the level's task.
- `entryFunction` (optional, default `PlayerScreen`): the composable the player must write.
- `starter` (optional, default `Starter.kt`): the starter code file in the level's folder.
- `viewport` (optional, default 360 × 640 dp): `widthDp` and `heightDp`, both positive.
- `panels` (optional, default none): the extra panels the level shows, from `console` and `recompositions`, each listed at most once.
- `criteria` (required, at least one): the acceptance criteria defined by the `level-acceptance` capability.

Each criterion SHALL have an `id` that is unique within the level and a non-blank player-facing `description` of what it requires. A criterion MAY have a `message`, an extra line in the resident's voice shown when it fails. If a `message` is given, it SHALL NOT be blank.

#### Scenario: Minimal level
- **WHEN** a `level.json` has `formatVersion`, `id`, `resident`, `request` and one valid criterion with an `id` and a `description`
- **THEN** it loads with `entryFunction` `PlayerScreen`, starter `Starter.kt`, a 360 × 640 dp viewport and no extra panels

#### Scenario: Duplicate criterion ids
- **WHEN** two criteria of the same level have the id `title`
- **THEN** loading the level fails with an error naming the level and the duplicate id

#### Scenario: Missing description
- **WHEN** a criterion has a `message` but no `description`
- **THEN** loading the level fails with an error naming the criterion and the missing `description`

### Requirement: Strict level validation
Loading SHALL reject level data that is malformed or ambiguous, with an error that names the level folder and the offending field. The checks SHALL cover: invalid JSON, unknown fields (to catch typos), missing required fields, an unsupported `formatVersion`, unknown criterion types, unknown role names, selectors with no matching field set, a non-positive viewport, unknown or repeated panel names and a missing starter file. A level that fails validation SHALL NOT be playable.

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

### Requirement: Reference solutions are test-only
Each level SHALL have a reference solution `Solution.kt` stored under test resources, in a folder named after the level id. Reference solutions SHALL NOT be included in the game's runtime classpath or its packaged distributable. Only the target image rendered from a solution at build time SHALL ship. A solution SHALL show nothing that the level's criteria do not check, so the target never asks for more than the checklist does.

#### Scenario: Solutions are not shipped
- **WHEN** the macOS distributable is built
- **THEN** no `Solution.kt` file appears in any jar inside the bundle

### Requirement: Initial level content
The game SHALL ship two levels, in this order:
- `000-hello-main-street`: Rosa the baker asks for a sign with "Rosa's Bakery" on top and "Fresh bread daily" below it. It has two criteria, "Rosa's Bakery" is on screen and "Fresh bread daily" appears below "Rosa's Bakery", and lists no panels.
- `001-open-sign`: Tom the florist asks for "Tom's Flowers" with an "Order" button to its right, and to print a line when the button is pressed. Its criteria check the text, the button (by role and text) and that the button is to the right of the text. It lists the `console` panel.

#### Scenario: Solving the first level
- **WHEN** the player writes a `PlayerScreen` that shows `Text("Rosa's Bakery")` above `Text("Fresh bread daily")` in a `Column`
- **THEN** the verdict passes both criteria

#### Scenario: Wrong order
- **WHEN** the player shows both texts in a `Column` with "Fresh bread daily" first
- **THEN** the verdict fails only the ordering criterion, with a detail saying "Fresh bread daily" is above "Rosa's Bakery"

#### Scenario: Second level is data only
- **WHEN** `001-open-sign` is added
- **THEN** it needs only its folder (`level.json`, `Starter.kt`), its `Solution.kt` and an index entry, and no change to the loader, the evaluator or the screen

## MODIFIED Requirements

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
- `criteria` (required, at least one): the acceptance criteria defined by the `level-acceptance` capability.

Each criterion SHALL have an `id` that is unique within the level and a non-blank player-facing `description` of what it requires. A criterion MAY have a `message`, an extra line in the villager's voice shown when it fails. If a `message` is given, it SHALL NOT be blank.

#### Scenario: Minimal level
- **WHEN** a `level.json` has `formatVersion`, `id`, `title`, `villager` (with `id`, `name` and `profession`), `shop`, `request` and one valid criterion with an `id` and a `description`
- **THEN** it loads with `entryFunction` `PlayerScreen`, starter `Starter.kt`, a 360 × 640 dp viewport, no concepts, no intro, no celebration, no thanks line and no extra panels

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
- selectors with no matching field set;
- a non-positive viewport;
- unknown or repeated panel names;
- repeated or blank concepts;
- an empty, blank or too long `intro` line, or more than four of them;
- a blank or too long `celebration` line, or more than three of them;
- a too long `thanks` line;
- a missing starter file.

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

### Requirement: Initial level content
The game SHALL ship two levels, in this order:
- `000-hello-main-street`, titled "Hello Main Street":
  - Rosa the baker (villager id `rosa`, shop "Rosa's Bakery") asks for a sign with "Rosa's Bakery" on top and "Fresh bread daily" below it.
  - It has two criteria: "Rosa's Bakery" is on screen, and "Fresh bread daily" appears below "Rosa's Bakery".
  - It lists the concepts `Text` and `Column`, and no panels.
  - Its intro has three lines: Rosa greets the player and smells the fresh bread, asks for the two texts, and says her sketch is on the sign board and to press Run whenever the player wants her to look.
  - It has one celebration line before its thanks line.
- `001-open-sign`, titled "Open Sign":
  - Tom the florist (villager id `tom`, shop "Tom's Flowers") asks for "Tom's Flowers" with an "Order" button to its right, and to print a line when the button is pressed.
  - Its criteria check the text, the button (by role and text) and that the button is to the right of the text.
  - It lists the concepts `Row`, `Button` and `onClick`, has a thanks line, and lists the `console` panel.
  - Its intro is the four lines of `06-level-intro-dialogue.html`: Tom introduces himself as the florist next to Rosa's bakery, asks for the sign and the button, asks for "New order!" to be printed so he hears the bell, and says his sketch is on the sign board.
  - It has one celebration line before its thanks line.

#### Scenario: Solving the test level
- **WHEN** the player writes a `PlayerScreen` that shows `Text("Rosa's Bakery")` above `Text("Fresh bread daily")` in a `Column`
- **THEN** the verdict passes both criteria

#### Scenario: Wrong order
- **WHEN** the player shows both texts in a `Column` with "Fresh bread daily" first
- **THEN** the verdict fails only the ordering criterion, with a detail saying "Fresh bread daily" is above "Rosa's Bakery"

#### Scenario: Second level is data only
- **WHEN** `001-open-sign` is added
- **THEN** it needs only its folder (`level.json`, `Starter.kt`), its `Solution.kt` and an index entry, and no change to the loader, the evaluator or the screen

#### Scenario: Tom's intro
- **WHEN** `001-open-sign` is loaded
- **THEN** it has four intro lines, the first starting "Oh, hi! You must be the new sign maker."

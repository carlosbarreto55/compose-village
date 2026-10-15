## ADDED Requirements

### Requirement: Main Street levels
The game SHALL ship the five Main Street levels, listed in `index.json` in this order:
1. `000-hello-main-street`
2. `001-open-sign`
3. `002-restaurant-menu`
4. `003-contest-scoreboard`
5. `004-ferry-times`

Each level SHALL be data only: its folder (`level.json`, `Starter.kt`), its `Solution.kt` under test resources, and its index entry.

#### Scenario: Five shops
- **WHEN** the levels are loaded
- **THEN** there are five, in the order above, with the villagers Rosa, Tom, Marco, Dona Lúcia and Iara

#### Scenario: A level is data only
- **WHEN** `004-ferry-times` is added
- **THEN** it needs only its folder, its `Solution.kt` and an index entry, and no change to the screen

### Requirement: Rising difficulty
Main Street's levels SHALL introduce Compose ideas one level at a time:
- No concept SHALL be listed by more than one Main Street level.
- Each level SHALL unlock at most one panel. A panel is unlocked by the first level, in play order, that lists it.
- Each level's starter code SHALL compile, SHALL contain the data the villager's request refers to, and SHALL fail at least one of the level's criteria that needs the level's new concepts.

The self-checking level tests SHALL check the first two rules.

#### Scenario: Concept listed twice
- **WHEN** an author adds `Row` to the concepts of `002-restaurant-menu`, while `001-open-sign` already lists it
- **THEN** `./gradlew test` fails, naming both levels and `Row`

#### Scenario: Two unlocks in one level
- **WHEN** an author makes the first level that lists `console` also the first that lists `recompositions`
- **THEN** `./gradlew test` fails, naming that level

### Requirement: Hello Main Street
The game SHALL ship `000-hello-main-street`, titled "Hello Main Street", with this content:
- Rosa the baker (villager id `rosa`, shop "Rosa's Bakery") asks for a sign with "Rosa's Bakery" on top and "Fresh bread daily" below it.
- It has two criteria: "Rosa's Bakery" is on screen, and "Fresh bread daily" appears below "Rosa's Bakery".
- It lists the concepts `Text` and `Column`, and no panels.
- Its intro has three lines: Rosa greets the player and smells the fresh bread, asks for the two texts, and says her sketch is on the sign board and to press Run whenever the player wants her to look.
- It has one celebration line before its thanks line.

#### Scenario: Solving the test level
- **WHEN** the player writes a `PlayerScreen` that shows `Text("Rosa's Bakery")` above `Text("Fresh bread daily")` in a `Column`
- **THEN** the verdict passes both criteria

#### Scenario: Wrong order
- **WHEN** the player shows both texts in a `Column` with "Fresh bread daily" first
- **THEN** the verdict fails only the ordering criterion, with a detail saying "Fresh bread daily" is above "Rosa's Bakery"

### Requirement: Open Sign
The game SHALL ship `001-open-sign`, titled "Open Sign", with this content:
- Tom the florist (villager id `tom`, shop "Tom's Flowers") asks for "Tom's Flowers" with an "Order" button to its right, and to print a line when the button is pressed.
- Its customer presses the button "Order" once (step `order`).
- Its criteria check:
  - the text;
  - the button, by role and text;
  - that the button is to the right of the text;
  - that pressing it prints "New order!" (criterion `bell`, after `order`, with the message "The sign looks lovely! But a customer pressed Order and my bell stayed quiet…").
- It lists the concepts `Row`, `Button` and `onClick`, has a thanks line, and lists the `console` panel.
- Its intro is the four lines of `06-level-intro-dialogue.html`: Tom introduces himself as the florist next to Rosa's bakery, asks for the sign and the button, asks for "New order!" to be printed so he hears the bell, and says his sketch is on the sign board.
- It has one celebration line before its thanks line.

#### Scenario: Tom's intro
- **WHEN** `001-open-sign` is loaded
- **THEN** it has four intro lines, the first starting "Oh, hi! You must be the new sign maker."

#### Scenario: Silent bell
- **WHEN** the player shows "Tom's Flowers" with an "Order" button to its right whose `onClick` is empty
- **THEN** the verdict passes three criteria and fails `bell`, and Tom's reaction is "The sign looks lovely! But a customer pressed Order and my bell stayed quiet…"

### Requirement: Restaurant Menu
The game SHALL ship `002-restaurant-menu`, titled "Restaurant Menu", with this content:
- Marco the chef (villager id `marco`, shop "Marco's Kitchen") asks for "Marco's Kitchen" on top, then every dish from his menu list, in order, with its price to the right.
- Its viewport is 400 × 360 dp. It lists the concepts `List` and `forEach`, no panels and no customer steps.
- Its starter code defines `data class Dish(val name: String, val price: String)` and `val menu` with:
  - Spaghetti "€12";
  - Lasagna "€14";
  - Risotto "€15";
  - Tiramisu "€7".

  It shows "Marco's Kitchen" and only "Spaghetti", typed by hand, in a `Column`.
- Its criteria, in order:
  - `name`: "Marco's Kitchen" is on screen;
  - `dishes`: `all` four dish names are on screen;
  - `order`: `all` of: "Spaghetti" below "Marco's Kitchen", "Lasagna" below "Spaghetti", "Risotto" below "Lasagna", "Tiramisu" below "Risotto";
  - `prices`: `all` of: each price is to the right of its dish.
- Its intro has four lines:
  1. "Benvenuto! My whole menu lives in my head. Can you help me put it up on the wall?"
  2. Marco asks for every dish of the `menu` list, in order, under "Marco's Kitchen".
  3. Marco asks for each price to the right of its dish.
  4. Marco says his sketch is on the sign board.
- It has one celebration line before its thanks line.

#### Scenario: Only the first dish
- **WHEN** the player runs the starter code
- **THEN** `name` passes, `dishes` fails with "I can't find \"Lasagna\" on the screen.", and the level is not passed

#### Scenario: Every dish with a loop
- **WHEN** the player shows "Marco's Kitchen" and then, for each dish in `menu`, a `Row` with the dish's name and its price
- **THEN** every criterion passes

#### Scenario: Prices under the dishes
- **WHEN** the player shows each price on its own line under its dish
- **THEN** `prices` fails with a detail saying "€12" and "Spaghetti" are stacked on top of each other

### Requirement: Contest Scoreboard
The game SHALL ship `003-contest-scoreboard`, titled "Contest Scoreboard", with this content:
- Dona Lúcia the teacher (villager id `lucia`, shop "Dona Lúcia's School") asks for a scoreboard:
  - it shows "Blue: 0" and "Green: 0";
  - a "+1 Blue" and a "+1 Green" button each add a point to their team;
  - a "Reset" button puts both scores back to 0.
- Its viewport is 400 × 300 dp. It lists the concepts `remember` and `mutableStateOf`, and the panels `console` and `recompositions`.
- Its starter code shows "Contest Scoreboard", the two scores and the two "+1" buttons. The scores are kept in plain local `var`s, the buttons increment them, and there is no "Reset" button.
- Its customer, in order:
  - presses "+1 Blue" (`blue-1`);
  - presses "+1 Blue" again (`blue-2`);
  - presses "+1 Green" (`green-1`);
  - presses "Reset" (`reset`).
- Its criteria, in order:
  - `board`: `all` of "Blue: 0" and "Green: 0" on screen;
  - `blue-point`: after `blue-1`, "Blue: 1" is on screen;
  - `counts-up`: after `blue-2`, "Blue: 2" is on screen;
  - `teams`: after `green-1`, `all` of "Green: 1" and "Blue: 2" on screen;
  - `reset`: after `reset`, `all` of "Blue: 0" and "Green: 0" on screen.
- Its intro has four lines:
  1. "Good afternoon, dear. The school contest is tomorrow, and our scoreboard is still blank."
  2. Dona Lúcia asks for the scores, the "+1" buttons and Reset.
  3. She warns that last year's board never changed when it was pressed.
  4. She says her sketch is on the sign board.
- It has one celebration line before its thanks line.

#### Scenario: Plain variables
- **WHEN** the player runs the starter code
- **THEN** `board` passes, and `blue-point` fails with "A customer pressed \"+1 Blue\", but the screen didn't change."

#### Scenario: Remembered scores
- **WHEN** the player keeps each score in `remember { mutableStateOf(0) }` and adds a "Reset" button that sets both to 0
- **THEN** every criterion passes

#### Scenario: One shared score
- **WHEN** both "+1" buttons add to the same state
- **THEN** `teams` fails with a detail starting "After a customer pressed \"+1 Green\":"

#### Scenario: Recompositions unlock
- **WHEN** the player opens `003-contest-scoreboard` for the first time
- **THEN** the Recompositions panel is highlighted with its unlock tip, and the Console is shown without one

### Requirement: Ferry Times
The game SHALL ship `004-ferry-times`, titled "Ferry Times", with this content:
- Iara the boat captain (villager id `iara`, shop "Iara's Ferry") asks for a "Where to?" field. Under it, only the departures whose destination contains what was typed are listed, whatever the case. When nothing matches, the screen says "No ferry goes there".
- Its viewport is 400 × 420 dp. It lists the concepts `TextField`, `filter` and `if`, and the panels `console` and `recompositions`.
- Its starter code defines `data class Departure(val time: String, val destination: String)` and `val departures` with:
  - "08:00" to Coral Island;
  - "10:30" to Lighthouse Point;
  - "13:15" to Coral Island;
  - "16:45" to Old Harbor.

  It shows "Iara's Ferry" and every departure, listed with `forEach`, and has no field.
- Its customer types "coral" into the node with the text "Where to?" (`coral`), then "Moon Bay" into it (`moon`).
- Its criteria, in order:
  - `field`: "Where to?" is on screen;
  - `timetable`: `all` of the four times on screen, matched with `contains`;
  - `quiet`: "No ferry goes there" is not on screen;
  - `coral`: after `coral`, `all` of: "08:00" and "13:15" on screen, and "10:30" and "16:45" not on screen;
  - `nowhere`: after `moon`, `all` of: "No ferry goes there" on screen, and none of the four times on screen.
- Its intro has four lines:
  1. "Ahoy! The ferry always leaves on time. The trouble is, nobody knows when that is."
  2. Iara asks for the "Where to?" field and the filtered list.
  3. She asks for "No ferry goes there" when nothing matches, and says sailors never bother with capital letters.
  4. She says her sketch is on the sign board.
- It has one celebration line before its thanks line, and the thanks line says that Main Street is complete.

#### Scenario: No field yet
- **WHEN** the player runs the starter code
- **THEN** `timetable` and `quiet` pass, `field` fails, and `coral` fails with "The customer couldn't find \"Where to?\" to type into."

#### Scenario: Case-sensitive filter
- **WHEN** the player filters with `destination.contains(query)`, without ignoring case
- **THEN** `coral` fails, because typing "coral" lists no departure

#### Scenario: Field not bound to state
- **WHEN** the player writes `TextField(value = "", onValueChange = { }, label = { Text("Where to?") })`
- **THEN** `coral` fails with "A customer typed \"coral\" into \"Where to?\", but the screen didn't change."

#### Scenario: Message always shown
- **WHEN** the player always shows "No ferry goes there" under the list
- **THEN** `quiet` fails

#### Scenario: Iara's timetable
- **WHEN** the player keeps the query in `remember { mutableStateOf("") }`, binds a `TextField` labelled "Where to?" to it, lists `departures.filter { it.destination.contains(query, ignoreCase = true) }`, and shows "No ferry goes there" when that list is empty
- **THEN** every criterion passes

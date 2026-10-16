## MODIFIED Requirements

### Requirement: Street card
The street card SHALL show the sky (`#F6E7CE`) with two clouds, a hill (`#EDDCBF`) and a road (`#E3CDA9` with a dashed centre line). Above the buildings, it SHALL show a title and a short line of text:
- on first launch, the caption "A cozy game for learning Jetpack Compose", "Welcome to Compose Village", and a line about building signs with real Kotlin and Compose code;
- with saved progress, the caption "Main Street", "Welcome back", and a line naming the latest open shop and the villager who is still waiting, or saying every shop is open.

The street SHALL show one building per bundled level in play order, then one per placeholder shop, then the Sandbox building ("The Sandbox", "End of the street", "Free play"). Each building SHALL show:
- the villager's badge on its roof;
- the shop name;
- the level's "<number> · <title>";
- its status.

Clicking a level's building SHALL open that level. Clicking the Sandbox building SHALL open the Sandbox.

#### Scenario: First launch street
- **WHEN** the player launches the game with no saved progress
- **THEN** the street shows Rosa's Bakery, Tom's Flowers, Marco's Kitchen, Dona Lúcia's School, Iara's Ferry and The Sandbox, under "Welcome to Compose Village", with no Coming soon shop

#### Scenario: Opening a level from the street
- **WHEN** the player clicks Tom's Flowers on the home screen
- **THEN** `001-open-sign` opens with the player's saved code for it, or its starter code

#### Scenario: Opening a new shop
- **WHEN** the player clicks Iara's Ferry on the home screen
- **THEN** `004-ferry-times` opens with the player's saved code for it, or its starter code

### Requirement: Placeholder shops
The game SHALL keep an app-side list of upcoming shops, each with a villager name, profession, shop name and title, for shops whose levels are not bundled yet. Their numbers SHALL continue after the last bundled level. A placeholder SHALL NOT be shown when a bundled level has a villager with the same name. Placeholders SHALL NOT count toward "N of M shops open", and SHALL NOT be level data.

While the five Main Street levels are bundled and no later chapter has been announced, the list SHALL be empty.

#### Scenario: Main Street has no placeholders
- **WHEN** the five Main Street levels are bundled
- **THEN** the street shows the five level buildings and The Sandbox, and no building reads "Coming soon"

#### Scenario: Placeholders after the bundled levels
- **WHEN** the five Main Street levels are bundled and the upcoming list has two shops
- **THEN** the street shows them as "6 · <title>" and "7 · <title>", Coming soon, after the five level buildings

#### Scenario: A placeholder becomes a level
- **WHEN** the upcoming list has a shop for Marco and a bundled level's villager is Marco
- **THEN** the street shows that level's building and no Marco placeholder

### Requirement: Welcome back cards
When any level is completed or has saved code, the home screen SHALL show:
- **Pick up where you left off**, for the current level:
  - its "<number> · <title>";
  - a pill with the villager's name and profession;
  - its request;
  - "Your code is saved" if it has saved code, or "Starter code is ready" otherwise;
  - "N of M wishes", N being the requirements its latest Run met (0 if it was never run) and M the number of its requirements;
  - a primary **Continue: <title>** button that opens it, and "Or pick any shop on the street.";
- **Open shops**, listing each completed level in play order with its villager badge, shop name, "<lines> lines · <tries> tries" from saved progress, and **Visit**, which opens that level;
- the **Sandbox** card, with **Open**.

When every bundled level is completed, the pick-up card SHALL say that every shop on Main Street is open and offer **Open the Sandbox** instead of Continue. When no level is completed yet, Open shops SHALL NOT be shown.

#### Scenario: Coming back after the first level
- **WHEN** the player completed `000-hello-main-street` in 2 tries with 6 lines and launches the game again
- **THEN** the header reads "1 of 5 shops open", the pick-up card shows "2 · Open Sign", "0 of 4 wishes" and "Continue: Open Sign", and Open shops lists Rosa's Bakery with "6 lines · 2 tries"

#### Scenario: Up next after Open Sign
- **WHEN** the player completed the first two levels and launches the game again
- **THEN** the pick-up card shows "3 · Restaurant Menu" with Marco's pill and "Continue: Restaurant Menu"

#### Scenario: Saved code without a completion
- **WHEN** the player edited `000-hello-main-street` without completing it and launches the game again
- **THEN** the welcome back cards show "1 · Hello Main Street" with "Your code is saved", and no Open shops card

#### Scenario: Visiting an open shop
- **WHEN** the player chooses Visit on Rosa's Bakery
- **THEN** `000-hello-main-street` opens in its welcome back form

#### Scenario: Every shop open
- **WHEN** all five Main Street levels are completed
- **THEN** the header reads "5 of 5 shops open", the pick-up card says every shop on Main Street is open and offers Open the Sandbox, and Open shops lists all five shops

### Requirement: Shops open count
With saved progress, the home header SHALL read "N of M shops open", N being the completed bundled levels and M the bundled levels.

#### Scenario: Header count
- **WHEN** one of the five bundled levels is completed
- **THEN** the home header reads "1 of 5 shops open"

### Requirement: Returning home
The level screen and the Sandbox SHALL offer a way back to the home screen through the header's "Main Street" link. Returning home SHALL save the open level's code, the same as switching levels. While a Run is in progress, the link SHALL do nothing. The home screen SHALL reflect progress made since it was last shown.

#### Scenario: Back to the street after completing a level
- **WHEN** the player completes the first two levels and clicks "Main Street" in the header
- **THEN** the home screen shows Tom's Flowers lit, Marco's Kitchen as Up next, and "2 of 5 shops open"

#### Scenario: Code survives a trip home
- **WHEN** the player edits `001-open-sign`, goes home and chooses Continue
- **THEN** the editor shows the edited code

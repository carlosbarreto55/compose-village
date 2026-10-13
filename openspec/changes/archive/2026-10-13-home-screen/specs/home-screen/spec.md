## ADDED Requirements

### Requirement: Home is the first screen
The game SHALL open on the home screen. The home screen SHALL show:
- a header with the brand mark, "Compose Village" and the build status, without a Run button;
- the street card;
- below it, the first-launch cards or the welcome back cards.

Below 1180 dp of content width, the cards under the street SHALL stack in one column and the page SHALL scroll. The street's buildings SHALL wrap onto more rows instead of scrolling sideways.

#### Scenario: Launching the game
- **WHEN** the player launches the game
- **THEN** the home screen is shown, not the Sandbox or a level

#### Scenario: Narrow window
- **WHEN** the window is 900 dp wide on the home screen
- **THEN** the cards under the street are in one column, and every building is visible without scrolling sideways

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
- **THEN** the street shows Rosa's Bakery, Tom's Flowers, the placeholder shops and The Sandbox, under "Welcome to Compose Village"

#### Scenario: Opening a level from the street
- **WHEN** the player clicks Tom's Flowers on the home screen
- **THEN** `001-open-sign` opens with the player's saved code for it, or its starter code

### Requirement: Shop states
Each level's building SHALL be in one of these states:
- **Open**, when its level is completed: lit facade, `glow` window and sign with a soft glow behind it, status "✓ Open".
- **Current**, for the first level in play order that is not completed: dark facade with a dashed `wood` outline. Its status is a "Start here" pill if no level is completed, and an "Up next" pill otherwise.
- **Not yet**, for every other level that is not completed: dark facade, a locked (untinted) badge, status "Not open yet". It SHALL still open its level when clicked.

A placeholder shop SHALL show a dark facade, a locked badge and the status "Coming soon", and SHALL NOT respond to clicks.

#### Scenario: Nothing completed
- **WHEN** no level is completed
- **THEN** Rosa's Bakery is current with "Start here", and Tom's Flowers reads "Not open yet"

#### Scenario: First level completed
- **WHEN** `000-hello-main-street` is completed and `001-open-sign` is not
- **THEN** Rosa's Bakery is lit and reads "✓ Open", and Tom's Flowers is current with "Up next"

#### Scenario: Not yet is still playable
- **WHEN** no level is completed and the player clicks Tom's Flowers
- **THEN** `001-open-sign` opens

### Requirement: Placeholder shops
The game SHALL keep an app-side list of upcoming shops, each with a villager name, profession, shop name and title: Marco's Kitchen (3 · Restaurant Menu), Dona Lúcia's School (4 · Contest Scoreboard) and Iara's Ferry (5 · Ferry Times). Their numbers SHALL continue after the last bundled level. A placeholder SHALL NOT be shown when a bundled level has a villager with the same name. Placeholders SHALL NOT count toward "N of M shops open", and SHALL NOT be level data.

#### Scenario: Placeholders after the bundled levels
- **WHEN** two levels are bundled
- **THEN** the street shows "3 · Restaurant Menu", "4 · Contest Scoreboard" and "5 · Ferry Times" as Coming soon, after the two level buildings

#### Scenario: A placeholder becomes a level
- **WHEN** a bundled level's villager is Marco
- **THEN** the street shows that level's building and no Marco placeholder

### Requirement: First-launch cards
When no level is completed and no level has saved code, the home screen SHALL show:
- **Your first villager**: the first level's villager badge, name and profession pill, the request with its quoted phrases in bold, one "New: <concept>" chip per concept, a primary **Start: <number> · <title>** button that opens the first level, and "Starter code is ready for you. No setup needed.";
- **How a level works** in three numbered steps: "Read the wish", "Write the code", and "Run it" with the ⌘↵ shortcut;
- "Just want to experiment?" with **Open the Sandbox**.

#### Scenario: Starting the first level
- **WHEN** the player chooses "Start: 1 · Hello Main Street" on first launch
- **THEN** `000-hello-main-street` opens with its starter code

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

When every bundled level is completed, the pick-up card SHALL say that every shop on the street is open and offer **Open the Sandbox** instead of Continue. When no level is completed yet, Open shops SHALL NOT be shown.

#### Scenario: Coming back after the first level
- **WHEN** the player completed `000-hello-main-street` in 2 tries with 6 lines and launches the game again
- **THEN** the header reads "1 of 2 shops open", the pick-up card shows "2 · Open Sign", "0 of 3 wishes" and "Continue: Open Sign", and Open shops lists Rosa's Bakery with "6 lines · 2 tries"

#### Scenario: Saved code without a completion
- **WHEN** the player edited `000-hello-main-street` without completing it and launches the game again
- **THEN** the welcome back cards show "1 · Hello Main Street" with "Your code is saved", and no Open shops card

#### Scenario: Visiting an open shop
- **WHEN** the player chooses Visit on Rosa's Bakery
- **THEN** `000-hello-main-street` opens in its welcome back form

#### Scenario: Every shop open
- **WHEN** every bundled level is completed
- **THEN** the pick-up card says every shop is open and offers Open the Sandbox

### Requirement: Shops open count
With saved progress, the home header SHALL read "N of M shops open", N being the completed bundled levels and M the bundled levels.

#### Scenario: Header count
- **WHEN** one of two bundled levels is completed
- **THEN** the home header reads "1 of 2 shops open"

### Requirement: Returning home
The level screen and the Sandbox SHALL offer a way back to the home screen through the header's "Main Street" link. Returning home SHALL save the open level's code, the same as switching levels. While a Run is in progress, the link SHALL do nothing. The home screen SHALL reflect progress made since it was last shown.

#### Scenario: Back to the street after completing a level
- **WHEN** the player completes `001-open-sign` and clicks "Main Street" in the header
- **THEN** the home screen shows Tom's Flowers lit and "2 of 2 shops open"

#### Scenario: Code survives a trip home
- **WHEN** the player edits `001-open-sign`, goes home and chooses Continue
- **THEN** the editor shows the edited code

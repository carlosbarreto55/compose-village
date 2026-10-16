## MODIFIED Requirements

### Requirement: Street card
The street card SHALL be drawn with the current theme's scenery: its sky, ground and road behind the buildings, each level's building drawn with the theme's shop front for its state, and the Sandbox drawn with the theme's Sandbox front. In the cozy theme, that is the sky (`#F6E7CE`) with two clouds, a hill (`#EDDCBF`) and a road (`#E3CDA9` with a dashed centre line), with the gable-roofed shop fronts. Above the buildings, it SHALL show a title and a short line of text:
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

#### Scenario: Scenery follows the theme
- **WHEN** the window check draws the home screen with the tracer theme
- **THEN** the street's sky, ground, road and buildings are drawn by the tracer theme's scenery, not the cozy one

### Requirement: Shop states
Each level's building SHALL be in one of these states:
- **Open**, when its level is completed: lit facade, `glow` window and sign with a soft glow behind it, status "✓ Open".
- **Current**, for the first level in play order that is not completed: dark facade with a dashed `accent` outline. Its status is a "Start here" pill if no level is completed, and an "Up next" pill otherwise.
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

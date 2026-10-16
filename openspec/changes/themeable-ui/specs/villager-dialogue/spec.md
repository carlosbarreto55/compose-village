## MODIFIED Requirements

### Requirement: Villager busts
The game SHALL keep a cast of villager busts keyed by villager id, separate from the rig that animates them. The cast SHALL have a bust for each of Rosa, Tom, Marco, Dona Lúcia and Iara (`rosa`, `tom`, `marco`, `lucia`, `iara`), following `villager-cast.html`:
- **Rosa:** baker's hat, brown hair, white jacket, flour on her cheek.
- **Tom:** green cap with a pink flower, green shirt, apron.
- **Marco:** tall chef's hat, moustache, red neckerchief, double-breasted jacket.
- **Dona Lúcia:** grey hair in a bun, round glasses, green cardigan.
- **Iara:** captain's hat with an anchor badge, long black hair, navy coat.

All busts SHALL share one rig, in which only the eyes, the mouth and a vertical bob move. A bust SHALL have three expressions:
- **neutral:** open eyes, closed smile;
- **puzzled:** raised brows, a wavy mouth;
- **delighted:** happy closed eyes and a grin.

Every bundled level SHALL have its own villager, and that villager SHALL have their own bust in the cast, so that no two levels show the same character. An automated test SHALL fail when a bundled level's villager has no bust. A villager of a level that is not bundled, such as a test fixture, and that has no bust SHALL be shown with their profession badge wherever a bust would appear. Busts SHALL be part of the game, not level data.

#### Scenario: Every bundled villager has a bust
- **WHEN** the game loads the bundled levels
- **THEN** each level's villager is drawn with their own bust, not their badge, and no two levels show the same bust

#### Scenario: A bundled level without a bust
- **WHEN** a bundled level's villager id is `raimundo` and the cast has no bust for `raimundo`
- **THEN** the cast test fails, naming the level and the villager id

#### Scenario: A villager without a bust
- **WHEN** a level that is not bundled has the villager `{ "id": "pedro", "name": "Pedro", "profession": "fishmonger" }` and the cast has no bust for `pedro`
- **THEN** the villager card and dialogue show the fishmonger badge, and the level is playable

### Requirement: Dialogue box
The dialogue box SHALL have:
- a `surface` fill, a 4 dp `accent` border with an inner `outline` ring, a 22 dp corner radius and a 6 dp `accentStrong` bottom shadow;
- a name tab overlapping its top edge: an `accent` pill with the villager's name in Fredoka 18 and their profession in a chip;
- the line in Atkinson Hyperlegible 24 sp with a line height of 1.5, in an area tall enough for three lines, so the box keeps its size while typing;
- a footer with "<n> of <N>" on the left and, once the line has finished typing and it is not the last line, a blinking ▼ "Click to continue" on the right.

On the last line, the ▼ SHALL be replaced by the dialogue's closing button.

#### Scenario: Box size while typing
- **WHEN** a two-line sentence types out in the dialogue box
- **THEN** the box's height does not change from the first letter to the last

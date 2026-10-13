## ADDED Requirements

### Requirement: Level intro
When the player opens a level whose intro they have not seen, and the level has intro lines and is not completed, the big dialogue SHALL open with the villager's neutral bust and play the intro lines.
- **Skip intro**, just above the dialogue box at its right, SHALL close it at any time.
- On the last line, the ▼ SHALL be replaced by **Let's build it!**, which closes it.
- Closing the intro, either way, SHALL mark it as seen in saved progress.
- The level screen behind the scrim SHALL be in its before-the-first-Run state.

When the level has intro lines, the villager card SHALL offer **Talk to <name>**, which plays the intro again from its first line. Talk to SHALL do nothing while a Run is in progress.

#### Scenario: First time in Open Sign
- **WHEN** the player opens `001-open-sign` for the first time
- **THEN** the screen dims, Tom's bust and the dialogue box appear, and "Oh, hi! You must be the new sign maker." types out with "1 of 4"

#### Scenario: Skipping
- **WHEN** the player chooses Skip intro on the second line
- **THEN** the dialogue closes, the editor has focus, and reopening the level later does not play the intro

#### Scenario: Let's build it
- **WHEN** the player reaches the fourth line and chooses Let's build it!
- **THEN** the dialogue closes on the level screen with Tom's sketch in the preview

#### Scenario: Talking again
- **WHEN** the player has seen the intro and chooses Talk to Tom
- **THEN** the intro plays again from "1 of 4"

#### Scenario: Revisiting a completed level
- **WHEN** the player opens a level they completed in an earlier session, without having seen its intro
- **THEN** no intro plays, and the welcome back card is shown

## MODIFIED Requirements

### Requirement: Villager card
While a level is open, the villager card SHALL show:
- the villager's bust as the avatar, cropped to a round portrait, or their profession badge if the game has no bust for them;
- the villager's name;
- a pill with the profession's small glyph and label;
- **Talk to <name>**, when the level has intro lines;
- the request, with every phrase in double quotes shown in bold, without the quote marks.

Until the first Run, the card SHALL show one "New: <concept>" chip per concept the level lists. After a Run, a speech bubble with the villager's reaction SHALL take the chips' place. The avatar's expression SHALL follow the latest verdict:
- neutral before the first Run, and after a Run that did not compile or crashed;
- puzzled after a Run with failed requirements;
- delighted after a Run that passed.

The sandbox SHALL have no villager card.

#### Scenario: Opening Open Sign
- **WHEN** the player opens `001-open-sign` and closes the intro
- **THEN** the card shows Tom's bust, "Tom", a "Florist" pill, Talk to Tom, the request with **Tom's Flowers**, **Order** and **New order!** in bold, and the chips "New: Row", "New: Button" and "New: onClick"

#### Scenario: Puzzled after a failure
- **WHEN** a Run of `001-open-sign` fails one requirement
- **THEN** Tom's avatar has a puzzled face

### Requirement: Villager reaction
After each Run in a level, the reaction bubble SHALL speak for the villager, using the latest verdict:
- **Did not compile:** a game line saying the villager can't see anything yet.
- **Crashed:** a game line saying the sign fell down.
- **Some requirements failed:** the `message` of the first failed requirement that has one. If none of them has a message, a game line pointing to the checklist.
- **All requirements passed:** the level's `thanks` line, or a game thank-you line if it has none.

The reaction SHALL type into the bubble at the dialogue typing speed while the card's avatar talks. Clicking the bubble SHALL show the whole line. The bubble SHALL keep the size of the full line while it types, so the editor below it doesn't move. A new Run SHALL replace the line being typed. Game lines SHALL be part of the game, not level data.

#### Scenario: A failed requirement with a message
- **WHEN** the player runs Open Sign with the button on the left of the name
- **THEN** the bubble types out "Customers should read my name first, then see the button." while Tom's avatar talks, and the editor stays usable

#### Scenario: Completed with a thanks line
- **WHEN** the player completes Open Sign and closes the celebration
- **THEN** the bubble shows the level's `thanks` line

### Requirement: Level complete
When every criterion passes, the screen SHALL show a completion state that is clearly distinct from a partial result:
- the sign frame glows in `glow`;
- a completion card with a `glow` border appears above the checklist, reading "<shop> is open!" and "All n of <Name>'s wishes came true.", with two stats: **Lines** (the player's code without blank, `import` and `package` lines) and **Tries** (the player's Runs since the level was opened);
- the checklist collapses to its header and one filled mark per requirement;
- the level's shop in the footer lights up, and the completion is saved.

When a Run completes a level for the first time, the big dialogue SHALL open first, with:
- the villager's delighted bust, with sparkles around it;
- a glowing dialogue box;
- the level's celebration lines, then its thanks line (or the game's thank-you line).

Its last line SHALL offer **See <shop>** in place of the ▼, which closes it. The completion is saved when the Run finishes. The sign's glow, the completion card and the lit shop SHALL play once the dialogue closes. Later Runs that pass SHALL show the completion state without the big dialogue.

The completion card SHALL offer **Next: <number> · <title>**, which opens the next level in `index.json` order, and **Keep tinkering**, which hides the card and keeps the level open. On the last level, the card SHALL say that more of Main Street is coming soon instead of offering Next. When a completed level is reopened, the card SHALL take its welcome back form until the player's next Run (see Revisiting a completed level).

#### Scenario: Solving Hello Main Street
- **WHEN** the player runs a correct solution for "Hello Main Street" for the first time
- **THEN** Rosa's delighted bust says her celebration and thanks lines, and after See Rosa's Bakery, both requirements show a check, the completion card reads "Rosa's Bakery is open!", and "Next: 2 · Open Sign" opens `001-open-sign`

#### Scenario: Passing again
- **WHEN** the player chooses Play again on a completed level and runs a correct solution
- **THEN** the completion card appears without the big dialogue

#### Scenario: Last level
- **WHEN** the player completes the last level in the index
- **THEN** the completion card appears without Next and says more of Main Street is coming soon

#### Scenario: Keep tinkering
- **WHEN** the player chooses Keep tinkering and edits the code
- **THEN** the card is hidden, the level stays open, and its shop stays lit

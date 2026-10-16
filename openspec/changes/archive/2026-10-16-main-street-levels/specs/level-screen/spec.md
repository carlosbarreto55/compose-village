## MODIFIED Requirements

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

The completion card SHALL offer **Next: <number> · <title>**, which opens the next level in `index.json` order, and **Keep tinkering**, which hides the card and keeps the level open. On the last level, the card SHALL say "Main Street is complete! More of the village is coming soon." instead of offering Next. When a completed level is reopened, the card SHALL take its welcome back form until the player's next Run (see Revisiting a completed level).

#### Scenario: Solving Hello Main Street
- **WHEN** the player runs a correct solution for "Hello Main Street" for the first time
- **THEN** Rosa's delighted bust says her celebration and thanks lines, and after See Rosa's Bakery, both requirements show a check, the completion card reads "Rosa's Bakery is open!", and "Next: 2 · Open Sign" opens `001-open-sign`

#### Scenario: Passing again
- **WHEN** the player chooses Play again on a completed level and runs a correct solution
- **THEN** the completion card appears without the big dialogue

#### Scenario: Last level
- **WHEN** the player completes `004-ferry-times`, the last level in the index
- **THEN** the completion card appears without Next and says "Main Street is complete! More of the village is coming soon."

#### Scenario: Keep tinkering
- **WHEN** the player chooses Keep tinkering and edits the code
- **THEN** the card is hidden, the level stays open, and its shop stays lit

### Requirement: Revisiting a completed level
Opening a completed level SHALL restore the code that the player last had for it and run it once. That Run SHALL NOT count toward Tries. If that Run passes, the completion card SHALL appear in a welcome back form:
- it reads "<shop> is open!" and "You finished this in N tries.", N being the level's saved tries;
- its stats are the saved Lines and Tries of the first completion;
- it offers **Next: <number> · <title>** (or, on the last level, says "Main Street is complete! More of the village is coming soon.") and **Play again**, which does what Start over does.

#### Scenario: Coming back to a finished shop
- **WHEN** the player completes `000-hello-main-street`, opens `001-open-sign`, then clicks the first shop
- **THEN** the editor shows the code that completed it, the preview shows that code's sign, the checklist shows every requirement met, and the card reads "Rosa's Bakery is open!" with Play again

#### Scenario: Playing again
- **WHEN** the player chooses Play again on the welcome back card
- **THEN** the editor shows the starter code, the sketch ghost shows in the preview, and the first shop stays lit

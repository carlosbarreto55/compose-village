## ADDED Requirements

### Requirement: Saved code and Start over
Opening a level SHALL put the player's saved code for it in the editor, or its starter code if there is none. While a level is open and the editor differs from the starter code, the editor card's header SHALL offer **Start over**. Start over SHALL:
- put the starter code back in the editor;
- reset the checklist, the Runs of this visit, the preview and the sketch overlay as if the level had just been opened;
- keep the level completed if it was.

#### Scenario: Unfinished code comes back
- **WHEN** the player edits `001-open-sign` without completing it, opens the sandbox and opens `001-open-sign` again
- **THEN** the editor shows the player's edited code

#### Scenario: Starting over
- **WHEN** the player chooses Start over in `001-open-sign`
- **THEN** the editor shows the level's starter code, and the checklist shows neutral markers

### Requirement: Revisiting a completed level
Opening a completed level SHALL restore the code that the player last had for it and run it once. That Run SHALL NOT count toward Tries. If that Run passes, the completion card SHALL appear in a welcome back form:
- it reads "<shop> is open!" and "You finished this in N tries.", N being the level's saved tries;
- its stats are the saved Lines and Tries of the first completion;
- it offers **Next: <number> · <title>** (or says that more of Main Street is coming soon on the last level) and **Play again**, which does what Start over does.

#### Scenario: Coming back to a finished shop
- **WHEN** the player completes `000-hello-main-street`, opens `001-open-sign`, then clicks the first shop
- **THEN** the editor shows the code that completed it, the preview shows that code's sign, the checklist shows every requirement met, and the card reads "Rosa's Bakery is open!" with Play again

#### Scenario: Playing again
- **WHEN** the player chooses Play again on the welcome back card
- **THEN** the editor shows the starter code, the sketch ghost shows in the preview, and the first shop stays lit

## MODIFIED Requirements

### Requirement: Header
The header SHALL show:
- the brand mark and "Compose Village";
- a breadcrumb with the street ("Main Street"), the open level's number and title ("2 · Open Sign") and one progress dot per level, in play order;
- the build status;
- the Run button.

A dot is filled with `primary` for a completed level (saved progress, so also from earlier sessions), has a `wood` ring for the open level, and is muted for the others. In the sandbox, the breadcrumb SHALL read "Sandbox" and show no dots.

#### Scenario: Breadcrumb on the second level
- **WHEN** the player opens `001-open-sign` after completing `000-hello-main-street`
- **THEN** the breadcrumb reads "Main Street › 2 · Open Sign", the first dot is filled and the second is ringed

### Requirement: Street footer
The footer SHALL show the street's name, one shop per level in play order, and "N of M shops open":
- a shop is lit (`glow`) once its level has been completed, in this session or an earlier one;
- the open level's shop has a dashed `wood` outline;
- hovering a shop shows its number and title;
- clicking a shop opens its level;
- a Sandbox entry at the end of the footer opens the sandbox.

While a Run is in progress, shops and the Sandbox entry SHALL do nothing.

#### Scenario: Completing a level lights its shop
- **WHEN** the player completes `000-hello-main-street`
- **THEN** the first shop is lit and the footer reads "1 of 2 shops open"

#### Scenario: Opening a level from the street
- **WHEN** the player clicks the second shop, having never edited `001-open-sign`
- **THEN** `001-open-sign` opens with its starter code

### Requirement: Level complete
When every criterion passes, the screen SHALL show a completion state that is clearly distinct from a partial result:
- the sign frame glows in `glow`;
- a completion card with a `glow` border appears above the checklist, reading "<shop> is open!" and "All n of <Name>'s wishes came true.", with two stats: **Lines** (the player's code without blank, `import` and `package` lines) and **Tries** (the player's Runs since the level was opened);
- the checklist collapses to its header and one filled mark per requirement;
- the level's shop in the footer lights up, and the completion is saved.

The completion card SHALL offer **Next: <number> · <title>**, which opens the next level in `index.json` order, and **Keep tinkering**, which hides the card and keeps the level open. On the last level, the card SHALL say that more of Main Street is coming soon instead of offering Next. When a completed level is reopened, the card SHALL take its welcome back form until the player's next Run (see Revisiting a completed level).

#### Scenario: Solving Hello Main Street
- **WHEN** the player runs a correct solution for "Hello Main Street"
- **THEN** both requirements show a check, the completion card reads "Rosa's Bakery is open!", and "Next: 2 · Open Sign" opens `001-open-sign`

#### Scenario: Last level
- **WHEN** the player completes the last level in the index
- **THEN** the completion card appears without Next and says more of Main Street is coming soon

#### Scenario: Keep tinkering
- **WHEN** the player chooses Keep tinkering and edits the code
- **THEN** the card is hidden, the level stays open, and its shop stays lit

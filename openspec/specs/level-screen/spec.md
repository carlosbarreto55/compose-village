# level-screen Specification

## Purpose
TBD - created by archiving change add-level-screen. Update Purpose after archive.
## Requirements
### Requirement: Level target
While a level is open, the screen SHALL show the level's target, the image rendered from its reference solution, in the preview area: as a ghost before the first Run, and through a switch between the player's screen and the target. The target SHALL be loaded with the level and SHALL NOT trigger any compilation in the game. The target SHALL be used only for display, never to judge the attempt.

#### Scenario: Target before the first Run
- **WHEN** the player opens "Hello Main Street" and has not pressed Run yet
- **THEN** the preview shows Rosa's sign, faded, with "Rosa's Bakery" above "Fresh bread daily"

#### Scenario: Running does not recompile the target
- **WHEN** the player presses Run five times in a level
- **THEN** the compiler runs five times, once per Run, and never for the target

### Requirement: Target ghost before the first Run
When a level opens, the **Overlay sketch** option SHALL be on. While it is on and there is no player frame, the preview SHALL show the target at about 40% opacity in the sign frame, with the caption "This is <Name>'s sketch. Press Run to build yours on top of it." While it is on and there is a player frame, the target SHALL be drawn at about 40% opacity over the frame, and mouse input SHALL still reach the player's scene. The first Run in an opening that renders a frame SHALL turn the overlay off. After that, the overlay SHALL change only when the player toggles it.

#### Scenario: Ghost then player frame
- **WHEN** the player opens a level, sees the faded target in the preview, and then presses Run on code that renders
- **THEN** the preview shows the player's frame without the sketch over it, and Overlay sketch is unchecked

#### Scenario: Turning the overlay back on
- **WHEN** after the first frame the player checks Overlay sketch and runs again
- **THEN** the sketch stays drawn over the new frame, and clicks on the player's button still reach the scene

### Requirement: Target view switch
While a level with a target is open, the preview card SHALL offer a rounded switch above the sign frame with two options, "Your screen" (the default) and "<Name>'s sketch". Choosing the sketch SHALL show the target at full size and full opacity in the sign frame, at the level's viewport, and SHALL disable the Overlay sketch checkbox. While the target is shown, mouse input SHALL NOT be sent to the player's scene. Switching SHALL NOT recompile or reload anything. Opening a level and pressing Run SHALL both switch back to "Your screen". The sandbox SHALL have no switch.

#### Scenario: Looking at the target
- **WHEN** the player chooses "Tom's sketch"
- **THEN** the preview shows the target at full size, at the level's viewport

#### Scenario: No clicks reach the scene while Target is shown
- **WHEN** the sketch is shown and the player clicks where their own button would be
- **THEN** the click is not sent to the player's scene

#### Scenario: Run shows the result
- **WHEN** the sketch is shown and the player presses Run
- **THEN** the switch returns to "Your screen" and the preview shows the new attempt

### Requirement: Requirements checklist
While a level is open, the screen SHALL show a checklist card titled "<Name>'s checklist", with "k of n" passed requirements on the right and one line per criterion, in the level's order, using each criterion's `description`.
- **Before the first Run:** every line shows an empty circle.
- **After each Run:** a passed line shows a filled `primary` circle with a check, and a failed line shows a filled `error` circle with a cross and expands in `errorContainer` to show the evaluator's generated detail.
- **Messages:** a failed line SHALL also show its `message`, unless that message is the villager's reaction bubble.
- **Did not compile or crashed:** the checklist SHALL say so instead of marking lines.
- **Order:** after each Run, the lines SHALL resolve one after another in order.

#### Scenario: Starter code
- **WHEN** the player runs the starter code of "Hello Main Street"
- **THEN** both requirements show a cross, each with a detail saying which text can't be found on the screen, and the header reads "0 of 2"

#### Scenario: Updated on every Run
- **WHEN** the player fixes one requirement and runs again
- **THEN** that line changes to a check and the other keeps its cross

#### Scenario: Before the first Run
- **WHEN** the player opens a level
- **THEN** the checklist lists every requirement's description with an empty circle and "0 of n"

#### Scenario: Message shown once
- **WHEN** a Run fails one requirement whose message is in the reaction bubble
- **THEN** that line shows the generated detail but not the message

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

### Requirement: Panels per level
While a level is open, the screen SHALL always show the preview, the target, the checklist and the compile problems list. The Console and Recompositions panels SHALL be shown only if the level lists them in its `panels` field. In the sandbox (no level open), every panel SHALL be shown.

#### Scenario: Hello Main Street hides extra panels
- **WHEN** the player opens "Hello Main Street", which lists no panels
- **THEN** neither the Console nor the Recompositions panel is visible

#### Scenario: A level that uses the Console
- **WHEN** the player opens `001-open-sign`, which lists `console`
- **THEN** the Console panel is visible and the Recompositions panel is not

### Requirement: Panel unlock
A panel SHALL be unlocked by the first level, in `index.json` order, whose `panels` field lists it. When that level is opened, the panel SHALL expand into view, pulse twice, and show a "New: <panel>" tip with a short explanation of what it does. The tip SHALL stay until the player chooses "Got it", presses Run, or presses a key or the mouse after the pulse has finished. Opening a later level that also lists the panel SHALL NOT highlight it again. The explanations SHALL be part of the game, one per panel, not part of level data. The Console and Recompositions panels SHALL be drawn as dark cards with a capitalized title and monospaced content.

#### Scenario: Console unlock
- **WHEN** the player opens `001-open-sign`, the first level that lists `console`
- **THEN** the Console panel is highlighted with an explanation that it shows what the player's code prints

#### Scenario: Not the first level with the panel
- **WHEN** a later level also lists `console` and the player opens it
- **THEN** the Console panel is shown without a highlight

#### Scenario: The opening click does not dismiss the tip
- **WHEN** the player opens `001-open-sign` with "Next: 2 · Open Sign"
- **THEN** the "New: Console" tip is still shown after the pulse ends

### Requirement: Screen layout
The level screen SHALL be laid out as a header, a main area and a footer.
- The **main area** has a wide left column, with the villager card above the code editor, and a right column with the preview card, then the completion card when shown, then the checklist and the extra panels.
- When the window is too narrow for both columns (below 1180 dp), the right column SHALL move under the left one, and the main area SHALL scroll as one column.
- The window SHALL NOT be resizable below 900 × 640 dp.

#### Scenario: Default window
- **WHEN** the game opens at its default size of 1280 × 860 dp with "Open Sign" open
- **THEN** the villager card and editor are on the left, and the preview, checklist and Console are on the right

#### Scenario: Narrow window
- **WHEN** the player narrows the window to 1000 dp
- **THEN** the preview, checklist and Console appear below the editor, and the page scrolls

### Requirement: Header
The header SHALL show:
- the brand mark and "Compose Village";
- a breadcrumb with the street ("Main Street"), the open level's number and title ("2 · Open Sign") and one progress dot per level, in play order;
- the build status;
- the Run button.

A dot is filled with `primary` for a completed level (saved progress, so also from earlier sessions), has a `wood` ring for the open level, and is muted for the others. In the sandbox, the breadcrumb SHALL read "Main Street › Sandbox" and show no dots.

"Main Street" in the breadcrumb SHALL be a link that opens the home screen, and SHALL do nothing while a Run is in progress.

#### Scenario: Breadcrumb on the second level
- **WHEN** the player opens `001-open-sign` after completing `000-hello-main-street`
- **THEN** the breadcrumb reads "Main Street › 2 · Open Sign", the first dot is filled and the second is ringed

#### Scenario: Going home from a level
- **WHEN** the player clicks "Main Street" in the breadcrumb of `001-open-sign`
- **THEN** the home screen opens

#### Scenario: Going home from the sandbox
- **WHEN** the player clicks "Main Street" in the Sandbox's breadcrumb
- **THEN** the home screen opens

### Requirement: Build status and Run button
The header's build status SHALL read:
- "Compiler warming up…" until the compiler is warm;
- "Compiler ready" before the first Run;
- "Building…" during a Run, with a progress bar along the header's bottom edge;
- "Built in N ms" after a Run that compiled, N being compile, guard and load time together;
- "Didn't build" after a Run that did not compile.

Hovering the status SHALL show the compile, guard and load times separately. The Run button SHALL read "Run again" after a Run in the open level that did not complete it, and "Run" otherwise, and SHALL show the ⌘↵ shortcut. Pressing it SHALL scale it briefly to 0.96.

#### Scenario: After a partial Run
- **WHEN** the player runs code that compiles in 412 ms but fails one requirement
- **THEN** the status reads "Built in 412 ms" and the button reads "Run again"

#### Scenario: Compile error
- **WHEN** the player runs code with a syntax error
- **THEN** the status reads "Didn't build" next to an `error` dot

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

### Requirement: Code editor card
The code editor SHALL be a dark card with:
- a file tab named after the entry function (`PlayerScreen.kt`);
- line numbers;
- Kotlin syntax colors for keywords, strings, function names, comments and annotations;
- a status line at the bottom.

The status line SHALL say:
- "No problems" before the first Run;
- "Compiles fine" after a Run that compiled with no warnings;
- the number of problems otherwise.

When there are problems, the editor card SHALL list them under the code, and clicking one SHALL move the caret to its line and column. Lines with errors SHALL keep their highlight band. Syntax colors SHALL NOT change the text, the caret position or the columns that problems refer to.

#### Scenario: Syntax colors
- **WHEN** the editor shows `Text("Tom's Flowers")`
- **THEN** `Text` is drawn in `syntaxFunction` and `"Tom's Flowers"` in `syntaxString`

#### Scenario: Going to a problem
- **WHEN** a Run reports an error at line 9, column 9, and the player clicks it in the editor card
- **THEN** the caret moves to line 9, column 9

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


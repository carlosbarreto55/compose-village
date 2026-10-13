# villager-dialogue Specification

## Purpose
TBD - created by archiving change talking-villagers. Update Purpose after archive.
## Requirements
### Requirement: Villager busts
The game SHALL draw a bust for each of Rosa, Tom, Marco, Dona Lúcia and Iara, keyed by villager id (`rosa`, `tom`, `marco`, `lucia`, `iara`), following `villager-cast.html`:
- **Rosa:** baker's hat, brown hair, white jacket, flour on her cheek.
- **Tom:** green cap with a pink flower, green shirt, apron.
- **Marco:** tall chef's hat, moustache, red neckerchief, double-breasted jacket.
- **Dona Lúcia:** grey hair in a bun, round glasses, green cardigan.
- **Iara:** captain's hat with an anchor badge, long black hair, navy coat.

All busts SHALL share one rig, in which only the eyes, the mouth and a vertical bob move. A bust SHALL have three expressions:
- **neutral:** open eyes, closed smile;
- **puzzled:** raised brows, a wavy mouth;
- **delighted:** happy closed eyes and a grin.

A villager whose id has no bust SHALL be shown with their profession badge wherever a bust would appear. Busts SHALL be part of the game, not level data.

#### Scenario: Every bundled villager has a bust
- **WHEN** the game loads the bundled levels
- **THEN** Rosa and Tom are drawn with their busts, not their badges

#### Scenario: A villager without a bust
- **WHEN** a level's villager is `{ "id": "pedro", "name": "Pedro", "profession": "fishmonger" }` and the game has no bust for `pedro`
- **THEN** the villager card and dialogue show the fishmonger badge, and the level is playable

### Requirement: Talking motion
While a villager's line is typing, their bust SHALL talk:
- the mouth swaps between open and closed every 120 ms, and is closed as soon as the line is fully shown;
- the bust lifts 4 dp each time the mouth opens, easing out over 90 ms.

Every shown bust SHALL blink for 120 ms about every 3.6 s, with a different offset per villager so that two busts never blink together.

#### Scenario: Mouth stops with the text
- **WHEN** a line finishes typing
- **THEN** the mouth is closed and the bust is at rest

### Requirement: Dialogue box
The dialogue box SHALL have:
- a `surface` fill, a 4 dp `wood` border with an inner `outline` ring, a 22 dp corner radius and a 6 dp `woodDark` bottom shadow;
- a name tab overlapping its top edge: a `wood` pill with the villager's name in Fredoka 18 and their profession in a chip;
- the line in Atkinson Hyperlegible 24 sp with a line height of 1.5, in an area tall enough for three lines, so the box keeps its size while typing;
- a footer with "<n> of <N>" on the left and, once the line has finished typing and it is not the last line, a blinking ▼ "Click to continue" on the right.

On the last line, the ▼ SHALL be replaced by the dialogue's closing button.

#### Scenario: Box size while typing
- **WHEN** a two-line sentence types out in the dialogue box
- **THEN** the box's height does not change from the first letter to the last

### Requirement: Typing and advancing
Lines SHALL type out at 40 characters per second. Clicking the dialogue box, or pressing Space or Enter while the dialogue is open:
- while a line is typing, SHALL show the whole line at once;
- when a line is fully shown and it is not the last line, SHALL start typing the next line.

Pressing Escape SHALL do what the dialogue's Skip action does, if it has one.

#### Scenario: Click to finish, click to continue
- **WHEN** the player clicks the box while the first intro line is typing, then clicks it again
- **THEN** the first click shows the whole first line, and the second starts typing the second line with "2 of 4"

#### Scenario: Keys
- **WHEN** the intro is open and the player presses Space
- **THEN** the dialogue advances and the editor receives no input

### Requirement: Big dialogue
The big dialogue SHALL dim the screen with `ink` at 55%, and show the villager's bust large beside the dialogue box at the bottom of the window. The scrim SHALL fade in while the bust and box slide up. While it is open:
- clicks outside the box SHALL do nothing;
- the editor, Run and ⌘↵ SHALL be inactive;
- keyboard focus SHALL stay in the dialogue.

When it closes, it SHALL fade out and focus SHALL return to the editor.

#### Scenario: Run is blocked during the intro
- **WHEN** the intro is open and the player presses ⌘↵
- **THEN** no Run starts

### Requirement: Dialogue lines in reduced motion
When reduced motion is on:
- each line SHALL appear whole;
- busts SHALL NOT flap their mouth, bob or blink;
- the ▼ SHALL stay still;
- the big dialogue SHALL fade in and out without sliding.

#### Scenario: Reduced-motion intro
- **WHEN** reduced motion is on and the intro opens
- **THEN** the first line is shown whole at once, with the ▼ visible and still


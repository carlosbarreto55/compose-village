## Why

Villagers are the heart of the cozy design, but today they are a static profession badge and a bubble that fades in. The new mockups (`design-docs/cozy/screens/06-level-intro-dialogue.html`, `07-reactions-after-run.html`, `villager-cast.html`, README "Talking villagers") make them characters: they introduce themselves when a level opens, react as they talk, and celebrate when their shop opens. That gives the player a reason to care about each sign before writing any code.

## What Changes

- **Talking busts.** Each of the five villagers (Rosa, Tom, Marco, Dona Lúcia, Iara) gets a drawn bust. They all use one rig, in which only the eyes, mouth and a small bob move:
  - the mouth flaps every 120 ms while text types;
  - the bust bobs 4 dp when the mouth opens;
  - the eyes blink for 120 ms about every 3.6 s, offset per villager.
  - Expressions: neutral, puzzled (a failing Run) and delighted (level complete).
  - A villager without a bust keeps the profession badge.
- **Dialogue box.** A shared RPG-style box:
  - `surface` fill, 4 dp `wood` border with an inner `outline` ring, 22 dp radius, 6 dp `woodDark` bottom shadow;
  - a name tab with the profession;
  - text typed at 40 characters per second in a three-line area that never jumps;
  - "n of N" and a blinking ▼ "Click to continue".
  - Click, Space or Enter finishes the line, then moves to the next one.
- **Level intro.** The first time a level is opened, the screen dims (`ink` at 55%) and the villager's big bust and dialogue play the level's intro lines. **Skip intro** closes it at any time, and the last line offers **Let's build it!**. **Talk to <name>** on the villager card plays it again whenever the player wants.
- **Failing Run.** The reaction types into the villager card's bubble while the small avatar talks with a puzzled face. It never covers the editor.
- **Level complete.** On a level's first completion, the big dialogue comes back with the villager delighted, sparkles and a glowing box, playing the level's celebration lines and its thanks line. The last line offers **See <shop>**, which closes it on the lit sign and the completion card.
- **Level data.**
  - `villager.id` (e.g. `"tom"`) picks the bust. **BREAKING** for level files: it is required.
  - `intro`: 1–4 lines.
  - `celebration`: lines said before `thanks` in the big dialogue.
  - Both bundled levels gain them.
- **Progress.** Saved progress records whether a level's intro has been seen, so it plays automatically only once.
- **Reduced motion.** Each line appears whole. No mouth flaps, no bob, no blinking. The ▼ stays still, and the bust and box fade in without sliding.
- **Home cards.** Where `home-screen` shows a villager's badge on the first-villager and pick-up cards, it shows the bust's portrait instead.

Not in this change: the "customer pressed Order" reaction and Console lines (`customer-bot`). Any failing message types out the same way, so that change only adds content.

## Capabilities

### New Capabilities
- `villager-dialogue`: the bust rig and expressions, the dialogue box and its controls, the typing and its timing, and when the big dialogue opens and closes.

### Modified Capabilities
- `level-definition`:
  - Level data model: `villager.id`, `intro` and `celebration`;
  - Strict level validation: blank or too many lines, a bad villager id;
  - Initial level content: the intro and celebration lines of both levels.
- `level-screen`:
  - Villager card: the bust avatar and **Talk to <name>**;
  - Villager reaction: typed with a talking avatar;
  - Level complete: the big celebration before the completion card on the first completion.
- `player-progress`: Saved progress records the intro as seen.
- `visual-style`: Motion and reduced motion gains the dialogue timings and their reduced-motion form.

## Impact

- **`levels`:**
  - `Villager.id`, `Level.intro` and `Level.celebration` in the model and loader, with validation;
  - both `level.json` files;
  - loader tests.
- **`app`:**
  - a pure `Dialogue` state machine (typing, advance, skip);
  - `VillagerBust` and five looks drawn on `Canvas`;
  - `DialogueBox` and a `BigDialogue` overlay with focus and key handling;
  - a typed bubble in `VillagerCard`;
  - the completion sequence in `SpikeScreen`;
  - `introSeen` in `LevelProgress`;
  - `WindowCheck` dialogue checks.
- **Order:** after `home-screen` (it changes that change's home cards). Archive `cozy-level-screen` and `village-progress` first, as for `home-screen`.

## Context

The villager is drawn today by `VillagerBadge`, a profession glyph in a tinted circle. The reaction is a `Bubble` in `VillagerCard` that cross-fades with `AnimatedContent`. Completion is one `updateTransition(controller.complete)` in `SpikeScreen` that drives the sign's glow and the `CompletionCard`. `LocalReducedMotion` exists, but its rule is narrow: skip the shake and the pulse rings.

The mockups are:
- `06-level-intro-dialogue.html`: Tom's four-line intro;
- `07-reactions-after-run.html`: a failing Run, then the celebration;
- `villager-cast.html`: the five busts and the rig.

Each bust is about 15 SVG shapes on a 200 × 240 viewBox. The eyes (open, closed and happy), the brows, and the mouth (closed, open, puzzled and grin) are separate groups toggled by opacity. The mockups step at 40 ms ticks. That is a mockup detail; the timings in the README are the contract.

This change comes after `home-screen` and builds on its `Screen` navigation and its `LevelProgress.met`.

## Goals / Non-Goals

**Goals:**
- One bust rig and one dialogue box, used by the intro, the failing reaction and the celebration.
- All dialogue logic (what's shown, what a click does, when the mouth is open) is pure and clock-driven, so it can be unit tested without a window.
- Text never reflows while it types: neither the box nor the bubble changes size.
- Reduced motion gives a calm but complete experience: every line, every button, no movement.

**Non-Goals:**
- Voice or sound effects (no "blip" per letter).
- Branching dialogue or player choices beyond the closing button.
- Busts for professions without a villager yet (fishmonger, mail carrier, mayor). They keep the badge.
- The customer-bot reaction content.

## Decisions

### D1. A pure `Typewriter`, driven by elapsed time
`app/Dialogue.kt` holds:
- `class Typewriter(line: String, startedAt: Long)` with:
  - `shown(now)`: characters shown, `min(len, (now - startedAt) * 40 / 1000)`;
  - `typing(now)`;
  - `mouthOpen(now)`: typing and `((now - startedAt) / 120) % 2 == 0`;
  - `finish()`: shows the whole line now.
- `class DialogueState(lines, reducedMotion)` holding the page and the current `Typewriter`, with:
  - `advance(now)`: finish the line, or go to the next page, or report `AtEnd`;
  - `pageLabel`, `isLast`.

The composables read the clock with `withFrameMillis` in a `LaunchedEffect` keyed on the line, and stop once the line is out, so nothing animates at rest except the blink and the ▼. With reduced motion, `startedAt` is in the past, so lines are whole and the mouth stays closed.

*Alternative:* a counter that increments per tick, like the mockups. Rejected because it ties speed to frame rate and makes the timings untestable.

### D2. The controller decides when the big dialogue opens; the UI owns typing
`SpikeController` gains `var bigDialogue by mutableStateOf<BigDialogue?>(null)`, with `BigDialogue(kind: INTRO | CELEBRATION, villager, lines)`. It also gains:
- `talk()` (Talk to <name>);
- `closeDialogue()`, which records `introSeen` for an intro;
- `dialogueOpen`, which `run()` and the ⌘↵ shortcut check.

The open rules:
- `open(level)` sets an intro when the level has lines, isn't completed and `progress[id].introSeen` is false. Revisits never show one.
- A counted Run whose verdict passes on a level that wasn't completed before that Run sets a celebration (`celebration + (thanks ?: GameText.THANKS)`). Replays never do.

`DialogueState` is `remember`ed in the overlay, keyed on the `BigDialogue` instance. Pages and typing are presentation, so they don't survive a recomposition of the controller. That's fine.

### D3. Delaying the completion moment behind the celebration
`SpikeScreen`'s completion transition target becomes `controller.complete && controller.bigDialogue?.kind != CELEBRATION`. The street footer and the header dots light from `completed` minus the celebrating level for the same reason.

When See <shop> closes the dialogue, the existing 600 ms glow and card slide play exactly as before, so `cozy-level-screen`'s timings don't change. The completion is saved when the Run finishes (`record(afterRun)`), not when the dialogue closes, so quitting during the celebration loses nothing.

### D4. Busts drawn on `Canvas` from the cast mockup
`ui/villagers/Bust.kt`: `VillagerBust(id, expression, mouthOpen, blinking, bob, modifier)` draws in a 200 × 240 coordinate space, scaled to its size. `VillagerLook` is a sealed set of five objects:
- each draws its own back layers (hair, body, neck, hat), using the SVG paths from `villager-cast.html`;
- each gives the positions and colors the shared face needs (skin, eye y, mouth y, cheeks).

The rig then draws the shared eyes, brows, cheeks and mouth for the expression and the mouth state. Details that cover the mouth, like Marco's moustache, are a look's front layer.

`Busts.forId(id)` returns a look, or null for the badge fallback. `VillagerPortrait(id, size)` draws the same bust clipped to a circle around the head (centre 100,102, radius 64) for the villager card (88 dp) and the home cards.

*Alternative:* SVG files with `loadSvgPainter`. Rejected because a painter can't toggle groups, so it would need 5 looks × 3 eye states × 4 mouth states as files, and the bob would move the whole image instead of the head and body together.

### D5. Text that never reflows
The bubble and the dialogue box lay out the **whole** line, and give the not-yet-typed part `Color.Transparent` with an `AnnotatedString` span. The layout and wrapping are the same from the first letter to the last. That holds the README's "room for three lines so the box never jumps", and the bubble's height is fixed, so the editor under the villager card doesn't move while Tom talks.

Screen readers get the full line as the text's content description from the start, so they aren't read a partial word.

### D6. Overlay, focus and input
`BigDialogueOverlay` sits in a `Box` over the screen's content in `SpikeScreen`:
- a scrim (`ink` 55%) that consumes all pointer input;
- **Skip intro** as a pill just above the box at its right, for intros only;
- the bust (about 230 dp tall) and the box (up to 880 dp wide), bottom-centred, sliding up 24 dp in 300 ms.

The overlay is `focusable` and requests focus when shown. `onKeyEvent` handles:
- Space and Enter: `advance`;
- Escape: Skip, intros only.

The root `onPreviewKeyEvent` ignores ⌘↵ while `dialogueOpen`. On close, the editor's `FocusRequester` is asked for focus.

The celebration adds:
- four sparkle stars around the bust that twinkle on a 1.2 s loop (static under reduced motion);
- a `glow` halo on the box border.

### D7. Bubble typing in the villager card
`Bubble` keeps its `AnimatedContent` for the color and outline changes. It replaces its text with a `TypedText(line, onFinished)` that uses the same `Typewriter`.

The card's avatar becomes `VillagerPortrait`:
- its mouth follows the bubble's typewriter;
- its expression follows the verdict (D2 rules in the spec).

A new Run during typing swaps the line. A click on the bubble calls `finish()`.

### D8. Level data and content
In `levels`:
- `Villager` becomes `Villager(id, name, profession)`;
- `Level` gains `intro: List<String>` and `celebration: List<String>`;
- the loader validates the limits (1–4 intro lines, up to 3 celebration lines, at most 140 characters each, and `thanks` capped at 140 too). 140 characters is three lines of 24 sp text at the narrowest box, which is a 900 dp window.

Content:
- **Rosa** (`rosa`). Intro:
  1. "Good morning! Smell that? Fresh bread, right out of the oven. Now my bakery just needs a sign."
  2. "Could you put my shop's name, “Rosa's Bakery”, on top, and “Fresh bread daily” underneath?"
  3. "My sketch is up on the sign board. Press Run whenever you want me to take a look!"

  Celebration: "Oh! Look at that, it's beautiful!"
- **Tom** (`tom`). Intro: the four lines from `06-level-intro-dialogue.html`. Celebration: "Look at that Order button! My customers are going to love it." `customer-bot` can change it to the mockup's "Ding! Ding! There's my bell!" once pressing Order is actually checked.

Dialogue lines are plain text, with curly quotes and no bold, as in the mockups.

### D9. Progress
`LevelProgress` gains `introSeen: Boolean = false`, set by `withIntroSeen(id)`. Like `met` in `home-screen`, it is additive with a default, so the format version stays 1.

### D10. Test harness
The window checks must not get stuck behind an intro. `WindowCheck` uses a fresh progress file, so intros play. A `closeDialogue()` helper after each `open()` keeps the existing flows working, and new checks cover:
- the intro itself;
- Skip;
- Talk to;
- ⌘↵ blocked;
- the celebration before the completion card;
- the 900 dp fit of the longest line.

## Risks / Trade-offs

- [Hand-porting five busts' paths is fiddly] → Port them one look at a time and compare against `villager-cast.html` in a side-by-side screenshot check (task 4.4). The rig makes the face consistent across all five.
- [A continuous frame loop for blinking costs CPU on an idle screen] → Blink uses `delay`-based scheduling (one recomposition per blink), not `withFrameMillis`. Typing frames run only while a line types.
- [The celebration delays the payoff a player expects from the checklist] → It plays only on the first completion. The checklist already resolved behind the scrim, and See <shop> lands straight on the lit sign.
- [Making `villager.id` required breaks external level files] → Only the two bundled levels exist. The error names the missing field.
- [Focus can be lost to the editor if a click lands during the slide-in] → The scrim consumes pointer input from its first frame, before the slide finishes.

## Migration Plan

Apply after `home-screen`, and with `cozy-level-screen` and `village-progress` archived. Both bundled `level.json` files gain `villager.id`, `intro` and `celebration` in the same commit as the loader change. Old save files load with `introSeen = false`. A returning player whose levels are completed sees no intro, because completed levels never show one. They see the intro of their current level once.

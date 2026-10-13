## 0. Before starting

- [x] 0.1 Check that `home-screen` is applied, and that `cozy-level-screen` and `village-progress` are archived.

## 1. Level data

- [x] 1.1 `levels`: `Villager(id, name, profession)` with the id pattern, and `Level.intro` and `Level.celebration` in the model and loader. Validate: 1–4 intro lines, up to 3 celebration lines, non-blank, at most 140 characters each, and `thanks` at most 140 (D8).
- [x] 1.2 `000-hello-main-street/level.json` and `001-open-sign/level.json`: add `villager.id`, `intro` and `celebration` with the D8 lines.
- [x] 1.3 Loader tests: a missing villager id, a bad id pattern, five intro lines, a 141-character line, a blank celebration line, and both bundled levels loading with their intro counts (3 and 4).

## 2. Progress

- [x] 2.1 `LevelProgress.introSeen` with `withIntroSeen(id)`. Start over doesn't reset it (D9).
- [x] 2.2 `ProgressTest`: an old file loads with `introSeen = false`, the flag round-trips, Start over keeps it.

## 3. Dialogue logic

- [x] 3.1 `app/Dialogue.kt`: `Typewriter` (shown, typing, mouthOpen, finish), `DialogueState` (advance, page label, last line) and a reduced-motion mode that shows lines whole (D1).
- [x] 3.2 `DialogueTest` with a fake clock: 40 cps, mouth parity at 120 ms, closed mouth when done, finish then next page, `AtEnd` on the last page, reduced motion whole and silent.
- [x] 3.3 `SpikeController`: `bigDialogue`, `dialogueOpen`, `talk()` and `closeDialogue()` (records intro seen). Intros open from `open()`, and celebrations from the first counted passing Run. `run()` is ignored while a dialogue is open (D2).
- [x] 3.4 Avatar expression from the verdict: neutral, puzzled or delighted.

## 4. Busts

- [x] 4.1 `ui/villagers/Bust.kt`: the shared rig (eyes open, closed and happy, brows, cheeks, mouth closed, open, puzzled and grin) on a 200 × 240 canvas. Also `VillagerPortrait` (the circle crop) and the bob offset (D4).
- [x] 4.2 The five looks, ported from `villager-cast.html`: Rosa, Tom, Marco, Dona Lúcia, Iara. Then `Busts.forId` with the badge fallback.
- [x] 4.3 Blink: a `delay` loop, 120 ms about every 3.6 s, offset by villager. Off under reduced motion.
- [x] 4.4 A spike or self-test page that renders all five busts in all three expressions, for a visual comparison with the mockup.

## 5. Dialogue UI

- [x] 5.1 `ui/villagers/DialogueBox.kt`: the frame, the name tab, a three-line text area with transparent untyped text (D5), "n of N", the blinking ▼ "Click to continue", and a slot for the closing button.
- [x] 5.2 `BigDialogueOverlay`: the scrim that consumes input, Skip intro, the bust and box sliding up in 300 ms, focus on show and back to the editor on close, Space, Enter and Escape. Under reduced motion it fades only (D6).
- [x] 5.3 The celebration look: a delighted bust, twinkling sparkles (static under reduced motion), a glowing box, and See <shop> on the last line.
- [x] 5.4 `SpikeScreen`: host the overlay, gate ⌘↵ on `dialogueOpen`, and hold the completion transition and the lit shop until the celebration closes (D3).

## 6. Villager card and home

- [x] 6.1 `VillagerCard`: a `VillagerPortrait` avatar with the verdict's expression, a Talk to <name> action when the level has an intro, and a typed bubble that syncs the avatar's mouth and finishes on click (D7).
- [x] 6.2 The home screen's first-villager and pick-up cards use `VillagerPortrait` instead of `VillagerBadge`.

## 7. Verification

- [x] 7.1 `WindowCheck`: add a `closeDialogue()` helper to the existing flows, and new checks:
  - the first open of Open Sign shows "1 of 4";
  - Skip marks the intro seen and a reopen doesn't play it;
  - Talk to Tom replays it;
  - ⌘↵ during the intro starts no Run;
  - a first completion shows the celebration before the completion card;
  - Play again plus a pass shows no celebration;
  - the longest bundled line fits three lines at 900 dp (D10).
- [x] 7.2 A reduced-motion pass: `-Dvillage.reducedMotion=true` shows whole lines, still busts and a still ▼.
- [x] 7.3 `./gradlew build` and `./gradlew :app:spike -Pargs="--only window"` pass, and `openspec validate talking-villagers --strict` passes.

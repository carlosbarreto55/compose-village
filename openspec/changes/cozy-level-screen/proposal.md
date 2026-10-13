## Why

The level screen plays correctly, but it still looks like the compilation spike. It uses stock Material colors, a row of filter chips as navigation, and timing numbers in the toolbar. Nothing on screen says the player is helping someone in a village. The cozy design (`design-docs/cozy/`) defines the intended look and feel: parchment and wood, a villager who reacts, a wooden sign frame around the preview, and a street of shops that light up. This change applies that design to the level screen using only what the game can already do, so later changes (customer bot, saved progress, hints) add mechanics to a screen that already looks finished.

## What Changes

- **Visual style.** The cozy tokens (colors, shapes, spacing), three bundled fonts (Fredoka, Atkinson Hyperlegible, JetBrains Mono) and the villager profession icons become the app's theme. The theme is light-only, and the code editor uses the warm dark palette. Fonts and icons ship with the app, and nothing is fetched at runtime.
- **Screen layout.** The layout becomes a header, two columns and a footer, and the right column wraps under the left one on narrow windows.
  - **Header:** brand, breadcrumb ("Main Street › 2 · Open Sign") with progress dots, build status ("Compiler ready", "Built in 412 ms") and a pill Run button ("Run" / "Run again", ⌘↵).
  - **Footer:** a street of shops, one per level: lit when completed, dashed outline for the current level. Clicking a shop opens its level, and a Sandbox entry replaces the filter chips.
- **Villager card.** The villager card shows a profession badge as the avatar, the villager's name, a profession pill, and the request with quoted phrases in bold.
  - **Before the first Run:** "New: …" concept chips.
  - **After each Run:** a reaction bubble in the villager's voice.
- **Editor.** The warm dark palette, Kotlin syntax colors and a file tab (`PlayerScreen.kt`) are added. A status line sits under the code ("No problems", "Compiles fine", "2 problems"), and the compile problems list moves inside the editor card.
- **Preview.** The preview sits in a wooden sign frame. The switch reads "Your screen | Tom's sketch". A new **Overlay sketch** checkbox draws the sketch over the player's screen. It is on when a level opens, which gives the "ghost" before the first Run (now 40%), and the first rendered frame turns it off.
- **Checklist.**
  - It has a "Tom's checklist · 3 of 4" header and drawn status marks (empty, filled check, filled cross).
  - The failed row expands in the error container with the generated detail.
  - The criterion `message` moves to the reaction bubble. A failed row shows its message only when the bubble isn't already showing it.
- **Level complete.**
  - The sign frame glows, and a completion card shows "<Shop> is open!", "All N of <Name>'s wishes came true", the Lines and Tries stats, **Next: <n> · <title>** and **Keep tinkering**.
  - The checklist collapses to "N of N".
  - The level's shop lights up in the footer. Completion lasts for the session; saving it is a later change.
- **Panels.** Console and Recompositions become dark "terminal" cards. The unlock now pulses twice and shows a "New: Console" tip that stays until the player's first interaction, with no 8-second timer.
- **Motion.** Five storyboarded moments: the Run press and build bar, the checklist stagger, a failure nudge, the level-complete transition and the panel unlock. macOS reduced motion turns off the shake and the pulse rings.
- **Level content (BREAKING, level format unreleased, still version 1).**
  - `resident` is replaced by `villager { name, profession }`, with professions validated against the icon set.
  - New required `title` and `shop`, and optional `concepts` and `thanks`. Both bundled levels are updated.

## Capabilities

### New Capabilities
- `visual-style`: the game's visual language: color tokens, bundled typography, shapes and spacing, villager profession icons and badges, motion timings and reduced motion.

### Modified Capabilities
- `level-screen`:
  - **New requirements:** the header / two columns / footer layout and its narrow-window wrap, the villager card and reaction bubble, the build status and Run button, the street footer with level navigation and session completion, and the editor's status line and problems.
  - **Changed requirements:** the ghost (it becomes the overlay, at 40%), the view switch ("<Name>'s sketch" and the Overlay sketch checkbox), the checklist (count, marks, where messages go), Level complete (the completion card, stats, Next and Keep tinkering) and the panel unlock (pulse, tip until first interaction).
- `level-definition`:
  - **Data model:** `villager`, `title`, `shop`, `concepts` and `thanks` replace `resident`.
  - **Validation:** checks for unknown professions.
  - **Initial levels:** carry the new fields.

## Impact

- **`app`:**
  - a new `ui/theme` (tokens, typography, shapes, reduced-motion local) and `ui/icons` (8 profession glyphs and the brand mark as `ImageVector`s);
  - the level screen recomposed from new components (header, villager card, editor card, sign frame preview, checklist, completion card, terminal panels, street footer);
  - `SpikeController` gains run count, session completion and the overlay flag, and loses the 8 s highlight timer;
  - `Main.kt` drops the system dark scheme;
  - the window harness checks (`spike/WindowCheck.kt`) updated for the new texts.
- **`app` resources:** 6 font files (OFL) with their license.
- **`levels`:** model, loader and validation for the new fields; both `level.json` files; loader tests.
- **No change** to the compiler, sandbox, guard, preview engine or evaluator.
- **Later changes** (roadmap, not in this change):
  - `customer-bot`: a "press X prints Y" criterion and the customer lines in the Console;
  - `village-progress`: saved progress, locked shops, Tries across sessions;
  - `hints-and-editor-aids`: "Need a hint?", folded imports, the failing-line glow, the Hints stat.

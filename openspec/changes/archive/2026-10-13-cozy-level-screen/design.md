## Context

The reference is `design-docs/cozy/`: three static screens (before Run, a Run with one failure, level complete), a motion storyboard, the villager icon sheet and a README with tokens, typography, layout rules and timings. The mockups show Level 2 (Open Sign) with four requirements and five levels on Main Street. The game has two levels, and Open Sign has three requirements, because the "pressing Order prints New order!" check needs a customer bot that doesn't exist yet.

Today's screen (`app/ui/SpikeScreen.kt`, `Panels.kt`, `CodeEditor.kt`) is stock Material 3:
- it follows the system dark mode;
- FilterChips are the level navigation;
- the toolbar shows compile/guard/load timings;
- the editor is light;
- the checklist uses unicode ✓/✗.

The domain behind it (verdicts with generated details, target PNGs, panel unlocks from level order) is done and specified in `level-screen`, `level-definition` and `level-acceptance`.

The mockup has more parts than the game can drive. This change takes the parts the current domain can back:

```
mockup element                          this change   later change
──────────────────────────────────────  ────────────  ─────────────────────
tokens, fonts, icons, layout, motion        ✓
villager card, chips, reaction bubble       ✓          (level data fields)
sign frame, sketch switch, overlay          ✓
checklist look, completion card             ✓          Hints stat → hints
street footer, progress dots                ✓ session  persisted → village-progress
"press Order prints …" + customer lines                customer-bot
"Need a hint?", folded imports, line glow              hints-and-editor-aids
```

## Goals / Non-Goals

**Goals:**
- At the default window size (1280 × 860), the level screen matches the three mockups closely enough that a side-by-side review finds only content differences, such as three requirements instead of four.
- Every visual value comes from one token set, so later screens reuse it.
- Level authors give each villager a name, profession, title, shop and concepts as data. No villager-specific code.
- Motion follows the storyboard, and reduced motion is honored on macOS.

**Non-Goals:**
- Saved progress, locked levels, and Tries that last across restarts (`village-progress`).
- The interaction criterion and customer bot (`customer-bot`).
- Hints, import folding, highlighting the line a failure relates to (`hints-and-editor-aids`).
- Illustrated villager faces with moods. The profession badge is the avatar.
- A dark theme for the parchment surfaces.
- Restyling the headless spike reports.

## Decisions

### D1. Tokens live in a `VillageTheme` and also feed Material 3
`ui/theme/Tokens.kt` holds the README tokens as an immutable `VillageColors`, exposed through `LocalVillageColors`. `VillageTheme { }` provides them, the typography and shapes. It also builds a Material 3 `lightColorScheme` from them (`primary = primary`, `error = error`, `surface = surface`, `background = ground`, `outline = outline`, …), so any remaining M3 component (checkbox, tooltip, progress bar) inherits the palette without restyling.

*Alternative:* Material 3 only, mapping every token onto a color role. Rejected because `wood`, `glow`, `chip` and the editor palette have no M3 role, and forcing them into `tertiary` and friends makes the code unreadable.

The theme is **light only**. `Main.kt` stops reading `isSystemInDarkTheme()`. The design has no dark parchment, and the editor already provides a dark surface.

### D2. Fonts are vendored static TTFs on the classpath
Six files go in `app/src/main/resources/font/`, with `OFL.txt` alongside:
- Fredoka SemiBold and Bold;
- Atkinson Hyperlegible Regular and Bold;
- JetBrains Mono Regular and SemiBold.

They load with the desktop `Font(resource = "font/…", weight = …)`. The window harness (`ImageComposeScene`) uses the same classpath, so screenshots show the real fonts.

*Alternatives:*
- Variable fonts. Fewer files, but weight axes vary per family, and static files are predictable.
- The Compose resources library (`Res.font`). It adds a code-generation plugin to the app module for six files. Not worth it yet.

### D3. Icons become committed `ImageVector`s, generated once from the SVGs
The eight profession glyphs and the brand mark are simple: paths, circles and rects with `stroke="currentColor"`. A one-off script (`scripts/svg-to-imagevector.py`):
1. drops the embedded C2PA `<metadata>` block;
2. turns circles into two arcs;
3. emits `ui/icons/VillagerIcons.kt` with one `ImageVector` per glyph, using `PathParser`-compatible path data.

The vectors are tinted at the call site, `woodDark` in badges.

*Alternative:* loading the SVGs at runtime with `painterResource`/`loadSvgPainter`. Rejected: the API is deprecated in recent Compose Multiplatform, it parses on every load, and it ships the 20 KB metadata blob 8 times.

### D4. Level data gains villager identity. Tints and icons stay in the app
```jsonc
{
  "formatVersion": 1,
  "id": "001-open-sign",
  "title": "Open Sign",                       // breadcrumb, Next button, street tooltip
  "villager": { "name": "Tom", "profession": "florist" },
  "shop": "Tom's Flowers",                    // "<shop> is open!"
  "concepts": ["Row", "Button", "onClick"],   // optional: "New: Row" chips
  "thanks": "Ding! There's my bell. Thank you — the shop is open!", // optional: bubble on completion
  "request": "…", "viewport": { … }, "panels": ["console"], "criteria": [ … ]
}
```
- **`Profession`:** an enum in `levels`: `baker`, `florist`, `chef`, `teacher`, `boat-captain`, `fishmonger`, `mail-carrier`, `mayor`. Each has a player-facing label ("Boat captain").
- **Validation:** an unknown profession fails validation with the list of valid names, the same way roles do.
- **Mapping:** the app maps each `Profession` to its glyph and badge tint (README table). Tints are visual, so they stay out of level data.
- **`resident` removed:** "Tom, the florist" is now derived from the fields above.
- **No version bump:** format version 1 changes in place, as in `add-level-screen`, because no level is released.

### D5. Two columns, with a measured wrap point
`BoxWithConstraints` around the main area. From the README flex rules:
- **Wide (`maxWidth ≥ 1180 dp`):** the left column is weighted, and the right column is 460 dp, or the preview frame width when that is larger.
- **Narrow:** both columns stack inside one vertical scroll, the editor at a fixed 460 dp height, the right column under it.

The window gets a minimum size of 900 × 640 dp.

```
┌ header ─────────────────────────────────────────────────────────────┐
│ ⌂ Compose Village   Main Street › 2 · Open Sign ●◉○   ● Built 412ms [▶ Run again ⌘↵] │
├─────────────────────────────────────────────┬───────────────────────┤
│ villager card (badge, name, pill, request,  │ [Your screen|Tom's sketch] ☐ Overlay │
│   chips │ reaction bubble)                  │ ╔═ wooden sign frame ═╗│
│                                             │ ║   player frame      ║│
│ editor card (dark)                          │ ╚═════════════════════╝│
│   PlayerScreen.kt                           │ completion card (when done)          │
│   1 @Composable …                           │ Tom's checklist   3 of 4             │
│   …                                         │ CONSOLE (dark)        │
│   ─ status line ─ / problems                │ RECOMPOSITIONS (dark) │
├─────────────────────────────────────────────┴───────────────────────┤
│ Main Street  [lit][dashed][ ][ ]  1 of 2 shops open        Sandbox  │
└ footer ─────────────────────────────────────────────────────────────┘
```

### D6. The ghost becomes the overlay
Today the preview has a separate "ghost" (the target at 25% before the first frame) and a Target switch. In the mockups, the ghost before the first Run is the **Overlay sketch** checkbox, checked. Making it one mechanism removes a special case:

```
open level ──▶ overlay = ON, view = Your screen
                 │  (no frame yet: sketch at 40% + "This is Tom's sketch. Press Run to build yours on top of it.")
first Run that renders a frame ──▶ overlay = OFF (once per opening)
checkbox ──▶ overlay toggles; while ON the sketch is drawn at 40% above the frame,
             pointer input still reaches the scene (the overlay ignores input)
view = Tom's sketch ──▶ sketch at 100%, input blocked (unchanged), checkbox disabled
Run ──▶ view = Your screen (unchanged)
```
The automatic turn-off happens only once per opening, so a player who turns the overlay back on keeps it on across later Runs.

### D7. One function picks the villager's reaction
The bubble's text is chosen in one place (`reactionFor(level, verdict)`), from the latest verdict:

| Verdict | Bubble |
|---|---|
| none yet | no bubble, concept chips shown |
| `NotRun.DID_NOT_COMPILE` | game line: "I can't see anything yet. Something in the code needs fixing." |
| `NotRun.CRASHED` | game line: "Oh! The sign fell down while I was looking at it." |
| evaluated, some failed | the `message` of the first failed criterion that has one, else "Almost! Have a look at my checklist." |
| evaluated, all passed | `thanks`, else "Thank you! It's perfect." |

Game lines are in one `GameText` object, not level data, with the villager name filled in where needed. The checklist hides a failed row's `message` only when that exact message is the bubble's text. This keeps the "Tom's voice in the bubble, explanation in the row" split from the mockup without dropping a second failure's message.

### D8. Session completion and stats live in the controller
`SpikeController` gains:
- `completed: Set<String>`: level ids completed this session, used for the dots and the street;
- `runsThisVisit: Int`: Runs since the level was opened, used for the Tries stat;
- `dismissedCompletion: Boolean`: set by Keep tinkering, cleared on the next Run or open.

The **Lines** stat is the player's code without blank lines and without `import`/`package` lines. That matches the mockup's 13 with the imports folded away.

`completed` already has the shape that `village-progress` will persist. That change only needs to load it at startup and save it on change.

### D8b. Unlock tip waits for the player's next action
The storyboard says the tip lasts "until first interaction". If any click counted, the click on **Next** or a street shop that opened the level would dismiss its own unlock. So:
- the tip is dismissed by **Got it**, by Run, or by the first key or pointer press after the pulse ends (~1.2 s after opening);
- the window passes these events to `controller.noteInteraction()` through `onPreviewKeyEvent` and a pointer listener in the `Initial` pass;
- the 8-second timer is removed.

### D9. Build status is one number, with the breakdown in a tooltip
The header shows one of: "Compiler warming up…", "Compiler ready", "Building…", "Built in N ms" (compile + guard + load), "Didn't build" (compile errors). A dot appears beside it (`primary`, or `error` for "Didn't build"). Hovering shows the spike breakdown (compile / guard / load), which keeps the measurements available to us without showing them to players.

### D10. Syntax colors through a small lexer and `VisualTransformation`
A hand-written Kotlin tokenizer colors the code:
- keywords;
- strings (with `$` templates colored as text);
- line and block comments;
- annotations;
- numbers;
- call names (an identifier followed by `(` or `{`, or after `.`).

It runs in a `VisualTransformation` with identity offset mapping, so the caret and diagnostics columns stay exact. Player files are under a few hundred lines, so tokenizing the whole file on each change is fine. The red band on error lines stays, in a warm red that reads on the dark background.

*Alternative:* a highlighting library. None is small, JVM-only and Compose-friendly, and the token set is small.

### D11. Motion is local to each component, with one reduced-motion switch
`LocalReducedMotion` is set at startup:
- **macOS:** read `defaults read com.apple.universalaccess reduceMotion`, with a 500 ms timeout; failure means false;
- **any platform:** `-Dvillage.reducedMotion=true|false` overrides the reading.

Each storyboard moment lives in its component:
- **Run → build:** `animateFloatAsState` press scale, an indeterminate `LinearProgressIndicator` along the header's bottom edge while building, and `Crossfade` on the frame.
- **Checklist resolves:** `LaunchedEffect(runCounter)` stagger of 120 ms per row, a 200 ms color tween and a 180 ms check stroke (`Animatable` path trim). The effect is keyed on the run counter, so every Run replays it.
- **Failure nudge:** `Animatable` keyframes shake on the first failed row, then `AnimatedVisibility(expandVertically(220))` for its detail.
- **Level complete:** a single `updateTransition(complete)` drives the frame glow (600 ms), the card's `slideInVertically { it / 4 } + fadeIn` (300 ms, starting at 900 ms) and the footer shop's light.
- **Panel unlock:** `expandVertically(250) + fadeIn`, then two 600 ms ring pulses.

Reduced motion keeps the color and fade transitions and skips the shake and the ring pulses.

### D12. Navigation moves to the footer
The FilterChips row goes away:
- each street shop opens its level, and has a tooltip "2 · Open Sign";
- a **Sandbox** text button at the footer's end opens the sandbox;
- in the sandbox, the header breadcrumb reads "Sandbox", there is no villager card, and every panel shows (unchanged rule);
- all of these are disabled while a Run is in progress, which matches `open()` ignoring calls then.

## Risks / Trade-offs

- **[Only two levels, so the street looks empty next to the five-shop mockup]** → The street shows real levels only. Placeholder lots are an open question.
- **[Switching levels discards the player's edits, as today]** → Out of scope here. `village-progress` can keep a draft per level. The click is a footer click, not a stray chip click, so this is no worse than today.
- **[Reduced motion is detected on macOS only]** → The distributable targets macOS (DMG only), and the system property covers testing and other platforms.
- **[Fonts change text metrics, so harness checks that click by text may miss]** → Harness checks find nodes by text and semantics, not coordinates. The screenshots in `app/build/spike/window/` get a manual side-by-side review against the mockups.
- **[The 40% overlay can make a correct screen look doubled]** → The overlay turns off at the first frame. The player turns it back on only on purpose.
- **[Removing `resident` breaks any external level files]** → None exist. The format is unreleased, and both bundled levels are updated in the same change.
- **[Font licenses]** → All three families are under the SIL Open Font License 1.1, so `OFL.txt` ships next to the files.

## Migration Plan

This needs no data migration, because nothing is saved yet. The new level fields and the theme have to land in the same build because the screen reads them. Rollback is reverting the change.

## Open Questions

- Should the street show empty "coming soon" lots after the last real level? The mockup always shows five.
- Should a completed level that the player dismissed with **Keep tinkering** offer a way back to the card? An idea is a small "Open!" badge in the header that reopens it.
- Should the sandbox get its own look, for example a workshop instead of a villager?

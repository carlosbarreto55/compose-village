## Context

`cozy-level-screen` keeps progress in `SpikeController`:
- the `completed` set;
- `runsThisVisit`;
- the editor's code.

All three are lost or reset whenever a level is opened. A player who finishes "Hello Main Street", moves on and comes back finds the starter code again, with no sign that they ever finished it. If they relaunch, the street is dark again.

## Goals / Non-Goals

**Goals:**
- Completion, tries and the player's code for every level survive switching levels and restarting the game.
- Coming back to a finished level shows the finished shop, and the player chooses whether to play it again.
- A missing or broken save file never stops the game from starting.

**Non-Goals:**
- Locked levels. Every shop stays clickable.
- Saving the sandbox's code.
- Several save slots or player profiles.
- Syncing progress between machines.

## Decisions

### D1. One JSON file, versioned, written atomically
The file is `progress.json` in the platform's app-data directory:
- macOS: `~/Library/Application Support/Compose Village/`;
- Windows: `%APPDATA%\Compose Village\`;
- other systems: `$XDG_DATA_HOME/compose-village/`, or `~/.local/share/compose-village/` when it isn't set.

The `-Dvillage.progress=<file>` launch option overrides the location; the window harness and the tests use it. The format looks like this:

```json
{ "version": 1,
  "levels": { "000-hello-main-street": { "completed": true, "tries": 3, "lines": 6, "draft": "…" } } }
```

To save, the game writes `progress.json.tmp` and then moves it over `progress.json` with `ATOMIC_MOVE`, so a crash never leaves half a file.

It uses kotlinx.serialization, which the `levels` module already uses; `app` gains the plugin and the library.

Entries for level ids the game no longer ships are kept when saving, so a level that comes back later keeps its progress.

*Alternative:* the Java `Preferences` API. It is rejected because it's opaque to players and to support, and its size limits don't fit code drafts.

### D2. Recovering from a bad file
If the file can't be parsed, or its `version` is newer than the game knows, the game:
- renames it to `progress.json.bad`, replacing an older backup;
- logs the reason to stderr;
- starts with empty progress.

A missing file is simply empty progress. A failed write is logged, and the game keeps running with the progress it has in memory.

### D3. `Progress` is a pure value, and the controller owns the store
- `Progress` is an immutable data class with pure update functions, so its rules can be unit tested without a compiler or a window:
  - `withDraft(id, code, starter)`;
  - `afterRun(id, passed, lines)`.
- `ProgressStore` does the file I/O.
- `SpikeController` takes a `ProgressStore`, holds the current `Progress` as Compose state, and saves after each change.
- `completed` becomes a value derived from `progress`, so the header and footer don't change.

Saving happens after every Run, when a level is closed by opening another one, when the player starts over, and when the window closes. It does not happen on every keystroke: a crash can lose at most the edits made since the last Run or level switch.

### D4. Drafts
- `draft` is the editor's text when the player leaves the level, runs it, or closes the game.
- A draft equal to the starter code is stored as no draft at all, so a level that was only opened stays fresh.
- Opening a level loads its draft if it has one, and the starter code otherwise.

### D5. Tries and Lines
- `tries` counts the level's Runs until its first completion, across visits and restarts. It stops counting once the level is complete.
- `lines` is the Lines stat (`countCodeLines`) of the code that first completed the level.

The completion card still shows this visit's numbers after the player's own Run, as `level-screen` describes. On a revisit, before the player runs anything, it shows the saved first-completion numbers.

### D6. Revisiting a completed level
Opening a completed level:
1. restores its draft;
2. runs that draft once ("replay"), so the preview shows the player's shop and the checklist shows the result.

The replay:
- does not count toward Tries or `runsThisVisit`;
- does not save progress;
- turns off the overlay like any first frame.

If the replay passes, the completion card appears in its **welcome back** form:
- "<shop> is open!";
- "You finished this in N tries.";
- the stats Lines (the saved one) and Tries (the saved one);
- **Next** (or the "coming soon" line);
- **Play again**.

If the replay doesn't pass, for example because the level changed in an update, the checklist shows what fails. The shop stays lit, because completion is never taken back.

*Alternative:* show the card without running anything. It is rejected because the preview would show the target ghost and an unchecked checklist, which looks exactly like the bug being fixed.

### D7. Start over and Play again
- **Start over** is a text action in the editor card's header. It shows while a level is open and the editor differs from the starter code.
- **Play again** on the welcome back card does the same thing.

Both:
- reset the editor to the starter code, removing the draft;
- reset this visit's verdict, Runs, overlay and preview, as if the level had just been opened;
- keep the level completed and its saved Tries and Lines.

There is no confirmation step: the action is named for what it does. The replaced code is not kept.

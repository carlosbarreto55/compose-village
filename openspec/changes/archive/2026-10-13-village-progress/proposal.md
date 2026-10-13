## Why

The cozy design is built around the village filling up: a progress dot per level in the header, and a street whose shops light up and stay lit. `cozy-level-screen` lights them only for the current session, so every launch starts on a dark street, the Tries stat resets, and switching levels throws away the player's code. Progress has to survive restarts for the village to feel like the player's own.

Reported bug: completing a level and moving on loses it. Going back to a finished level brings back the starter code, the street forgets it, and there's no way to see the finished shop again or to choose to replay it.

## What Changes

- **Saved progress.** A small versioned JSON file in the user's app-data directory (`~/Library/Application Support/Compose Village/progress.json` on macOS) records, per level id:
  - completed;
  - tries until the first completion;
  - the Lines stat at the first completion;
  - the player's latest code draft.

  It is written atomically on change and read at startup. If the file is missing or unreadable, the game starts fresh and keeps a backup of the bad file.
- **Session state becomes saved state.** The controller's `completed` set (introduced by `cozy-level-screen`) is loaded from and saved to this file. The footer, dots and "N of M shops open" don't change.
- **Drafts.** Opening a level restores the player's last code for it instead of the starter code. A "Start over" action restores the starter code.
- **Revisiting a finished level.** Opening a completed level restores the player's code, runs it once so their shop is on screen, and shows a "welcome back" completion card with **Play again** (start over from the starter code) and Next.
- **Not in this change: locked levels.** Whether a shop stays locked until the previous level is complete is left for later; every shop stays clickable.
- **Panel unlocks stay derived** from level order, as `level-screen` requires, so the saved file never decides which panels show.
- **Tries across sessions.** The completion card's Tries counts every Run of the level until its first completion, across restarts.

## Capabilities

### New Capabilities
- `player-progress`: what is saved, where, the file format and version, atomic writes, recovery from a corrupt file, and draft restore.

### Modified Capabilities
- `level-screen`:
  - "this session" becomes "saved" for the street, the dots and completion;
  - drafts on open, and Start over;
  - revisiting a completed level.

## Impact

- **`app`:** a new progress store, controller wiring, a Start over action.
- **`levels`:** none. Level ids are already the stable progress key (`level-definition`).
- **Packaging:** none. The file lives outside the bundle.
- **Order:** archive `cozy-level-screen` first. This change modifies requirements that change adds.

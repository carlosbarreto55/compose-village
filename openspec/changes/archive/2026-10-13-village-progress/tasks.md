## 1. Progress model and store

- [x] 1.1 Add the kotlinx.serialization plugin and library to `app`.
- [x] 1.2 Add `Progress` and `LevelProgress`, immutable and serializable (`version`, `levels` map), with the pure updates `withDraft(id, code, starter)` and `afterRun(id, passed, lines)` (D3–D5).
- [x] 1.3 Add `ProgressStore`: the platform's app-data location, the `-Dvillage.progress` override and an in-memory variant, `load()` (missing → empty; unreadable or newer version → `.bad` backup), and an atomic `save()` that logs failures (D1, D2).
- [x] 1.4 Unit tests: tries stop at the first completion, drafts equal to the starter are dropped, unknown ids survive a round trip, a corrupt file is backed up, a newer version is backed up, saving leaves no temp file.

## 2. Controller

- [x] 2.1 `SpikeController` takes a `ProgressStore`, loads it, and derives `completed` from it.
- [x] 2.2 `open()` saves the outgoing level's draft and restores the incoming level's draft or starter code. For a completed level with saved code, it replays the code once without counting it (D6).
- [x] 2.3 `run()` saves the draft and records the result with `afterRun`; replays don't count. `close()` saves the draft.
- [x] 2.4 Add `revisiting` (the welcome back form until the player's own Run), `canStartOver` and `startOver()` (D7).

## 3. Screen

- [x] 3.1 `CompletionCard`: the welcome back form ("You finished this in N tries.", the saved Lines and Tries, Next or coming soon, Play again).
- [x] 3.2 `EditorCard`: a Start over action in the file tab row, shown when `canStartOver`.
- [x] 3.3 `Main.kt` uses the platform `ProgressStore`; `WindowCheck` uses a temporary file.

## 4. Verification

- [x] 4.1 `WindowCheck`:
  - a completed level reopens with its code, a passing checklist and the welcome back card;
  - Play again restores the starter code and keeps the shop lit;
  - an unfinished draft comes back after switching levels;
  - a new controller on the same file shows the street lit.
- [x] 4.2 `./gradlew build` and `./gradlew :app:spike -Pargs="--only window"` pass, and `openspec validate village-progress --strict` passes.

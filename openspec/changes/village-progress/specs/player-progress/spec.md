## ADDED Requirements

### Requirement: Saved progress
The game SHALL save the player's progress for every level to a `progress.json` file in the platform's app-data directory (`~/Library/Application Support/Compose Village/` on macOS), read it at startup, and save it again after every Run, when the player switches levels, when the player starts a level over and when the window closes. For each level id it SHALL record:
- whether the level is completed;
- the Runs until its first completion;
- the Lines stat of the code that first completed it;
- the player's latest code for it.

The file SHALL carry a format version. The launch option `-Dvillage.progress=<file>` SHALL override its location.

#### Scenario: Progress survives a restart
- **WHEN** the player completes `000-hello-main-street`, quits and launches the game again
- **THEN** the first shop is lit and the footer reads "1 of 2 shops open"

#### Scenario: Progress survives switching levels
- **WHEN** the player completes `000-hello-main-street`, opens `001-open-sign` and then opens `000-hello-main-street` again
- **THEN** the first level is still completed and its code is the code that completed it

### Requirement: Safe saving
The game SHALL write the file atomically, so it holds either the previous or the new progress, never a partial one. Entries for level ids that the game does not ship SHALL be kept when saving.

#### Scenario: Unknown level kept
- **WHEN** the file has an entry for a level id the game does not know and the player completes a level
- **THEN** the saved file still has the unknown entry

### Requirement: Recovery from a bad file
If the progress file is missing, the game SHALL start with no progress. If it cannot be read or has a newer format version than the game knows, the game SHALL keep it as `progress.json.bad`, start with no progress, and keep running. A failed save SHALL NOT stop the game.

#### Scenario: Corrupt file
- **WHEN** `progress.json` contains invalid JSON and the game starts
- **THEN** the game opens with every shop dark, and the old file is kept as `progress.json.bad`

### Requirement: Tries across visits
A level's saved tries SHALL count every Run of that level until its first completion, across visits and restarts, and SHALL stop counting once the level is completed.

#### Scenario: Tries over two visits
- **WHEN** the player runs `000-hello-main-street` twice, opens another level, comes back and completes it on the next Run
- **THEN** the level's saved tries is 3

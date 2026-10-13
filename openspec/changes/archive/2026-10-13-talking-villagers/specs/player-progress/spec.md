## MODIFIED Requirements

### Requirement: Saved progress
The game SHALL save the player's progress for every level to a `progress.json` file in the platform's app-data directory (`~/Library/Application Support/Compose Village/` on macOS), read it at startup, and save it again:
- after every Run;
- when the player switches levels;
- when the player goes to the home screen;
- when the player starts a level over;
- when the player closes a level's intro;
- when the window closes.

For each level id it SHALL record:
- whether the level is completed;
- the Runs until its first completion;
- the Lines stat of the code that first completed it;
- the player's latest code for it;
- the number of requirements the player's latest Run met. It is 0 when that Run did not compile or crashed, and Start over resets it to 0. Replaying a completed level's code does not change it;
- whether the player has seen the level's intro. Start over and Play again do not reset it.

The file SHALL carry a format version. A file saved before the requirement count or the intro flag existed SHALL load with a count of 0 and the intro not seen, without changing the format version. The launch option `-Dvillage.progress=<file>` SHALL override its location.

#### Scenario: Progress survives a restart
- **WHEN** the player completes `000-hello-main-street`, quits and launches the game again
- **THEN** the first shop is lit and the footer reads "1 of 2 shops open"

#### Scenario: Progress survives switching levels
- **WHEN** the player completes `000-hello-main-street`, opens `001-open-sign` and then opens `000-hello-main-street` again
- **THEN** the first level is still completed and its code is the code that completed it

#### Scenario: Wishes met survive a restart
- **WHEN** the player runs `001-open-sign` with two of its three requirements met, quits and launches the game again
- **THEN** the home screen's pick-up card reads "2 of 3 wishes"

#### Scenario: Older save file
- **WHEN** the game loads a version 1 file that has no requirement counts and no intro flags
- **THEN** every level's progress loads as before, with 0 requirements met and the intro not seen

#### Scenario: Intro seen survives a restart
- **WHEN** the player skips the intro of `001-open-sign`, quits and launches the game again, and opens `001-open-sign`
- **THEN** the intro does not play

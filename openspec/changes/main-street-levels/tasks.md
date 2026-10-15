## 1. Groundwork

- [x] 1.1 Archive `customer-bot` (implemented, all tasks done), so this change's `level-definition` and `level-acceptance` deltas apply to its versions; run `openspec validate main-street-levels`
- [x] 1.2 Spike in `preview` tests: an M3 `TextField` with `label = { Text("Where to?") }` keeps "Where to?" in its merged texts after `enterText("coral")`, and a field whose `onValueChange` ignores input leaves an identical snapshot
- [x] 1.3 Spike: a `remember`ed counter shows `PlayerScreen` recomposition counts rising after each press in `PreviewState.recompositions`, and a plain `var` counter does not

## 2. Grouped criteria (`all`)

- [x] 2.1 Add `Criterion.All(id, description, checks, message, after)` to `Level.kt`
- [x] 2.2 Decode `checks` in `LevelRepository`: each check is decoded as its criterion type with the id `<parent>.<n>` and the parent's description; reject `id`, `description`, `message` and `after` inside a check, `all` and `prints` as check types, and fewer than two checks; name errors `criterion '<id>', check <n>`
- [x] 2.3 Validate selectors inside checks like the selectors of top-level criteria
- [x] 2.4 Evaluate `all` in `Evaluator` against the parent's moment, with the first failing check's detail; the verdict lists only the parent
- [ ] 2.5 Loader tests for each new validation error (group of one, check with `after`, nested `all`, `prints` inside, bad selector in check 2) and for a valid `all` loading
- [ ] 2.6 Evaluator tests: every check passes; the first failing check's detail; checks after a step; the verdict has one row

## 3. Details after a step

- [ ] 3.1 Add a pure "same screen" comparison of two snapshots (visible nodes in tree order: texts, content descriptions, roles, bounds; ids ignored)
- [ ] 3.2 In `Evaluator`, for a failing non-`prints` criterion with `after` whose step was performed: use "A customer pressed X, but the screen didn't change." when the step's screen equals the one before it, otherwise prefix the usual detail with "After a customer pressed X: " (and the `typed "T" into X` forms)
- [ ] 3.3 Evaluator tests with hand-built attempts: unchanged press, unchanged typing, a changed screen with a missing text, `prints` and not-found details unchanged, first-frame criteria unchanged
- [ ] 3.4 Re-run `001-open-sign`'s tests to confirm Tom's details are unchanged

## 4. Level 3 · Restaurant Menu

- [ ] 4.1 Add `002-restaurant-menu/level.json`: Marco (`marco`, chef, "Marco's Kitchen"), request, concepts `List` and `forEach`, viewport 400 × 360, the four intro lines, the celebration and thanks lines, and the `name`, `dishes`, `order` and `prices` criteria
- [ ] 4.2 Add `Starter.kt` with `Dish`, the `menu` list (prices in €), "Marco's Kitchen" and a hand-typed "Spaghetti", and a `forEach` tip
- [ ] 4.3 Add `Solution.kt`: a `Column` with the name and `menu.forEach` rows, prices pushed right with `fillMaxWidth` and `Arrangement.SpaceBetween`
- [ ] 4.4 Add it to `index.json`; tests for the spec's scenarios (only the first dish, prices under the dishes)

## 5. Level 4 · Contest Scoreboard

- [ ] 5.1 Add `003-contest-scoreboard/level.json`: Dona Lúcia (`lucia`, teacher, "Dona Lúcia's School"), request, concepts `remember` and `mutableStateOf`, panels `console` and `recompositions`, viewport 400 × 420, the intro, celebration and thanks lines, the steps `blue-1`, `blue-2`, `green-1` and `reset`, and the `board`, `blue-point`, `counts-up`, `teams` and `reset` criteria
- [ ] 5.2 Add `Starter.kt`: the board with plain `var` scores, no Reset, and the comment and tip on watched state
- [ ] 5.3 Add `Solution.kt` with `remember { mutableStateOf(0) }` scores and a Reset button
- [ ] 5.4 Add it to `index.json`; tests for the plain-variable starter detail and the one-shared-score detail

## 6. Level 5 · Ferry Times

- [ ] 6.1 Add `004-ferry-times/level.json`: Iara (`iara`, boat-captain, "Iara's Ferry"), request, concepts `TextField`, `filter` and `if`, panels `console` and `recompositions`, viewport 400 × 300, the intro, celebration and thanks lines (the thanks says Main Street is complete), the steps `coral` and `moon`, and the `field`, `timetable`, `quiet`, `coral` and `nowhere` criteria
- [ ] 6.2 Add `Starter.kt`: `Departure`, the `departures` list, "Iara's Ferry" and a `forEach` list, with no field and a `TextField` tip
- [ ] 6.3 Add `Solution.kt`: a remembered query, a `TextField` labelled "Where to?", `filter` ignoring case, and "No ferry goes there" when the result is empty
- [ ] 6.4 Add it to `index.json`; tests for the starter, a case-sensitive filter, an unbound field and an always-shown message

## 7. Difficulty rules

- [ ] 7.1 In `LevelsTest`, fail when a concept is listed by more than one level, naming both levels and the concept
- [ ] 7.2 In `LevelsTest`, fail when one level is the first to list more than one panel, naming the level
- [ ] 7.3 Check that every intro, celebration and thanks line of the new levels fits the 140-character limit (the loader rejects them otherwise) and reads well in the dialogue box at three lines

## 8. The game

- [ ] 8.1 Empty `Upcoming` in `Home.kt`, keeping `UpcomingShop` and the Coming soon state
- [ ] 8.2 Last-level completion copy: "Main Street is complete! More of the village is coming soon." on the completion card and its welcome back form; "every shop on Main Street is open" on the pick-up card
- [ ] 8.3 Check the Recompositions unlock tip opens on `003-contest-scoreboard` and the Console shows no tip there
- [ ] 8.4 Check that the board, menu and timetable fit their viewports in the preview's sign frame, and that targets render at 800 × 720, 800 × 600 and 800 × 840 px

## 9. Window check and docs

- [ ] 9.1 Update `WindowCheck` for a street of five shops and "N of 5 shops open"
- [ ] 9.2 Window check: the scoreboard starter shows "the screen didn't change" in the reaction and the checklist; the solution completes the level
- [ ] 9.3 Window check: Ferry Times with a case-sensitive filter fails `coral`; the solution completes the level and the card says Main Street is complete
- [ ] 9.4 Window check: measure Run time for the four-press scoreboard and note it if it is over 3 s
- [ ] 9.5 README: mention the five levels and the `all` criterion under "How a level plays", and regenerate the home screenshots
- [ ] 9.6 Cozy README: remove "Shops 3–5 use placeholder names" from the Home screen section
- [ ] 9.7 `./gradlew build` passes

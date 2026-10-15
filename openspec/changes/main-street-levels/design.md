## Context

Main Street ships two levels:
- `000-hello-main-street`: Rosa asks for `Text` in a `Column`.
- `001-open-sign`: Tom asks for a `Row`, a `Button` and an `onClick` that prints.

The street shows three more shops as app-side placeholders (`Upcoming` in `app/.../Home.kt`). Everything else these shops need already exists:
- villager ids `marco`, `lucia` and `iara`;
- their busts in `Bust.kt`;
- their profession icons and badge tints;
- their first intro lines in `villager-cast.html`;
- their titles in the cozy README (3 · Restaurant Menu, 4 · Contest Scoreboard, 5 · Ferry Times).

The engine is also mostly ready:
- `customer-bot`, implemented but not yet archived, lets a level script `press` and `enterText` steps and check any criterion `after` a step;
- `ActionsTest` already shows that typing into a Material 3 `TextField` found by its label works, and that a press on a `remember`ed counter updates the next snapshot;
- the Recompositions panel exists, with its unlock explanation, but no level lists it.

Two gaps show up once the levels are written down:
1. **Checklist length.** Criteria check one selector each. Marco's menu would need 13 rows (4 names, 4 orders, 4 prices and the title), and Iara's filter would need one row per departure, per search.
2. **Details after a step.** When a state bug leaves the board unchanged, the player reads "I can't find "Blue: 1" on the screen." That's true, but it doesn't say the press did nothing, and that is the whole lesson of the level.

## Goals / Non-Goals

**Goals:**
- Three new levels whose difficulty climbs steadily, from a still screen to state to user input. Each level adds one new idea on top of the ones before.
- Keep levels as data: the three levels add folders, solutions and index entries. The only engine changes are general ones (`all`, after-step details) that later chapters will reuse.
- Every new level passes the self-checking level test through the real pipeline and customer.
- Main Street reads as finished: five lit shops are possible, and the last level says so.

**Non-Goals:**
- Hints, folded imports, and a highlight on the failing line (`hints-and-editor-aids`).
- Scrolling, `LazyColumn` and a `scroll` step. Every level fits its viewport.
- Checking *how* the code is written (for example, requiring `forEach`). Acceptance stays structural, per `level-acceptance`.
- Locking levels, and new chapters or their placeholders.

## Decisions

### D1. The difficulty curve

| # | Level | New idea | Builds on | Rows | Customer steps | What the starter gives |
|---|---|---|---|---|---|---|
| 1 | Hello Main Street | `Text`, `Column` | none | 2 | none | a `Column` with a placeholder text |
| 2 | Open Sign | `Row`, `Button`, `onClick` | 1 | 4 | 1 press | the `Row` with the shop's name |
| 3 | Restaurant Menu | `List`, `forEach` | 1, 2 (`Row` in `Column`) | 4 | none | the `menu` data and the first dish, typed by hand |
| 4 | Contest Scoreboard | `remember`, `mutableStateOf` | 2 (buttons), 3 (layout) | 5 | 4 presses | the board, built on a plain `var`, with no Reset |
| 5 | Ferry Times | `TextField`, `filter`, `if` | 3 (`forEach`), 4 (state) | 5 | 2 typed searches | the `departures` data, listed with `forEach`, and no field |

The difficulty climbs on three axes at once:
- **concepts:** one new idea per level, never introduced twice;
- **what the checks observe:** a still screen in levels 1 and 3, one event in level 2, a sequence of events with memory in level 4, and input-driven filtering in level 5;
- **starter scaffolding:** it shrinks until only the new idea is left.

Panels unlock one at a time: the Console with Tom, Recompositions with Dona Lúcia.

*Alternative considered:* a level 3 on modifiers and styling (padding, colors, fonts). It was rejected because acceptance is structural and can't check styling. A level whose wishes can't be checked would feel arbitrary.

### D2. Restaurant Menu (Marco)

- **Viewport and concepts:** 400 × 260 dp; concepts `List` and `forEach`; no panels; no steps.
- **Starter:**
  - `data class Dish(val name: String, val price: String)`;
  - `val menu = listOf(...)` with Spaghetti €12, Lasagna €14, Risotto €15 and Tiramisu €7;
  - a `Column` with "Marco's Kitchen" and one hand-typed `Text("Spaghetti")`;
  - a tip naming `menu.forEach { dish -> ... }`.
- **Rows:**
  - `name`: "Marco's Kitchen" exists.
  - `dishes`: `all` four names exist.
  - `order`: `all` of: Spaghetti below the name, Lasagna below Spaghetti, Risotto below Lasagna, Tiramisu below Risotto.
  - `prices`: `all` of: each price `rightOf` its dish.
- **Prices use €.** This fits Marco ("Benvenuto!"), and a `$` in a Kotlin string would start a template. A beginner copying "$12" would hit a confusing compile rule that has nothing to do with the lesson.
- **Where the prices sit is not checked.** The sketch shows them against the right edge (`Arrangement.SpaceBetween`), but `rightOf` passes wherever they are. The level doesn't list a concept it can't check.

### D3. Contest Scoreboard (Dona Lúcia)

- **Viewport, concepts and panels:** 400 × 300 dp; concepts `remember` and `mutableStateOf`; panels `console` and `recompositions` (the Recompositions unlock).
- **Starter:**
  - the board: "Contest Scoreboard", then a row each with "Blue: $blue" and **+1 Blue**, and "Green: $green" and **+1 Green**;
  - the scores in `var blue = 0` / `var green = 0`, so it compiles and passes the first row, but presses change nothing;
  - no Reset button;
  - a comment explaining that Compose only redraws for state it watches, with a tip naming `remember { mutableStateOf(0) }`.
- **Steps:** `blue-1` and `blue-2` press **+1 Blue**, `green-1` presses **+1 Green**, and `reset` presses **Reset**.
- **Rows:**
  - `board`: first frame, `all` of "Blue: 0" and "Green: 0".
  - `blue-point`: after `blue-1`, "Blue: 1".
  - `counts-up`: after `blue-2`, "Blue: 2".
  - `teams`: after `green-1`, `all` of "Green: 1" and "Blue: 2".
  - `reset`: after `reset`, `all` of "Blue: 0" and "Green: 0".
- **The starter fails on purpose.** Its `blue-point` fails with the new "screen didn't change" detail (D6), and the Recompositions panel shows `PlayerScreen` never ran again. That is the lesson.
- *Alternative considered:* a single counter. It was rejected because two teams also teach that each piece of state is independent (`teams`), and Reset teaches writing state from a different button.

### D4. Ferry Times (Iara)

- **Viewport, concepts and panels:** 400 × 300 dp; concepts `TextField`, `filter` and `if`; panels `console` and `recompositions`.
- **Starter:**
  - `data class Departure(val time: String, val destination: String)`;
  - `val departures` with 08:00 and 13:15 to Coral Island, 10:30 to Lighthouse Point, and 16:45 to Old Harbor;
  - a `Column` with "Iara's Ferry" and every departure listed with `forEach`, which reuses level 3;
  - no field, and a tip naming `TextField(value, onValueChange, label = { Text("Where to?") })`.
- **Steps:** `coral` types "coral" into the node with text "Where to?", then `moon` types "Moon Bay" into it.
- **Rows:**
  - `field`: "Where to?" exists.
  - `timetable`: first frame, `all` four times exist (`contains`, so "08:00 Coral Island" in a single `Text` is fine).
  - `quiet`: first frame, "No ferry goes there" is absent.
  - `coral`: after `coral`, `all` of: 08:00 and 13:15 exist, and 10:30 and 16:45 are absent.
  - `nowhere`: after `moon`, `all` of: "No ferry goes there" exists, and all four times are absent.
- **"coral" is lower case on purpose.** It only matches "Coral Island" with `contains(query, ignoreCase = true)`. The request and intro say capital letters don't matter.
- **The field is found by its label.** `ActionsTest.enterTextReplacesTheFieldsText` shows the label stays in the field's merged texts after typing.
- *Alternative considered:* a `Switch` that shows only morning ferries. It was rejected because typing is the newer skill, and `enterText` already exists without being used.

### D5. The `all` criterion: nested checks borrow the parent's identity

JSON:

```json
{ "type": "all", "id": "dishes", "description": "Every dish on the menu is on screen",
  "message": "…", "after": "…", "checks": [ { "type": "exists", "node": { "text": "Spaghetti" } }, … ] }
```

- **Nested checks.** Each check is an `exists`, `absent`, `above`, `below`, `leftOf` or `rightOf` written without `id`, `description`, `message` or `after`. The loader decodes it as that criterion, with the id `<parent>.<n>` (1-based) and the parent's description. The evaluator then reuses its per-criterion check unchanged and runs each check against the parent's moment.
- **Detail.** It is the first failing check's detail, in the order written, with the after-step wording of D6 applied once to the whole row.
- **What `all` cannot contain:**
  - another `all`, which keeps the checklist flat;
  - `prints`, which checks the moment's output and already has step-aware details.
- **Self-check output** names only top-level ids, because the checklist only shows those.
- *Alternatives considered:*
  - one row per item: 13 rows for Marco;
  - an `existsAll` with a list of selectors: it covers names, but not the order and price pairs;
  - making `id`/`description` optional on every criterion: it weakens validation for everyone.

### D6. After-step details and the unchanged screen

The rule applies to a failing criterion with `after`, other than `prints`, whose step was performed:
- if the step's screen is the same as the screen before it, the detail is "A customer pressed X, but the screen didn't change." (or "typed \"T\" into X");
- otherwise, the detail is "After a customer pressed X: " followed by the usual detail.

"The screen before it" is the previous performed step's screen, or the first frame. The customer stops at the first step it can't perform, so every earlier step was performed. "The same" means the same visible nodes, in tree order, with the same texts, content descriptions, roles and bounds. Node ids are ignored. The comparison stays a pure function of the attempt record.

Customer failure details (not found, hidden, not reached) and `prints` details are unchanged.

*Alternative considered:* a per-criterion `message` for the unchanged case. It was rejected because every state level would repeat it, and the generated detail is more accurate: it only appears when the screen really didn't move.

### D7. Level content moves to a `main-street` spec

`level-definition` keeps the format, loader and self-checking rules. The content of all five levels moves to a new `main-street` spec, one requirement per level, together with the D1 rules that tests can check:
- no concept is introduced twice;
- each level unlocks at most one panel.

`LevelsTest` gains these two checks.

### D8. Placeholders and the end of the street

`Upcoming` becomes an empty list, but the `UpcomingShop` mechanism stays: Seu Raimundo, Bia and Davi's chapters will need it. The completion card on the last level says "Main Street is complete! More of the village is coming soon." The welcome back form says the same.

## Risks / Trade-offs

- **Exact texts are strict.** A player who writes "Blue - 1" instead of "Blue: 1" fails. → Every key text is quoted in the request and shown in the sketch, and the detail quotes the expected text.
- **A label versus a placeholder.** A player who uses `placeholder` instead of `label` loses "Where to?" once they type, so the second search can't find the field. → The request says "a "Where to?" field"; the sketch shows a label; and the detail says the customer couldn't find "Where to?" to type into. Hints can cover this later.
- **Hard-coding instead of looping.** A player can type out all four dishes and pass Marco's level. → Accepted, since acceptance is structural. Level 5 starts with a `forEach`, so the idea comes back.
- **Longer Runs.** Four presses, each followed by a settle and a snapshot, add time after the first frame. → Measure in the window check. Each step already has a 10 s timeout, and the settle is per step.
- **Unchanged-screen false positives.** A press that only changes something without semantics (a color, say) reads as "didn't change". → Every level's criteria only observe semantics anyway. The wording describes what the checks can see.
- **Recompositions counts.** The panel has to show `PlayerScreen` rerunning after each press. → Check it in the window check before writing the unlock copy around it.
- **Ordering with `customer-bot`.** This change's deltas rewrite requirements that `customer-bot` also modifies. → Archive `customer-bot` first (task 1.1).

## Migration Plan

Existing saved progress is keyed by level id, so the new ids `002-restaurant-menu`, `003-contest-scoreboard` and `004-ferry-times` start empty. A player who finished Open Sign sees Marco's Kitchen as Up next. Nothing is renamed. Rollback means removing the three index entries; the placeholders come back by restoring `Upcoming`.

## Open Questions

- Should the first-launch "How a level works" card mention the customer, now that three of five levels use it? This change keeps it as is.

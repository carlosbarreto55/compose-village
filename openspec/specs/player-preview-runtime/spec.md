# player-preview-runtime Specification

## Purpose

Loading and running a compiled attempt: per-attempt class loader, isolated offscreen Compose scene on a dedicated player thread, crash isolation for composition/event/effect failures, reload without leaks, input forwarding, and recomposition counting.
## Requirements
### Requirement: Per-attempt class loader
Each attempt SHALL be loaded by a new child class loader that defines the attempt's classes from memory and whose parent is the host class loader, so the Compose runtime and `village.api` types are shared with the game. When a new attempt is loaded, the previous attempt's scene, entry and class loader SHALL be released.

#### Scenario: Attempts are isolated
- **WHEN** two successive attempts both declare a top-level class with the same name
- **THEN** both load successfully in separate class loaders, and the second attempt renders its own version

### Requirement: Isolated offscreen rendering on a player thread
The player's `LevelEntry.content` SHALL be composed only inside an offscreen `ImageComposeScene` owned by a preview engine. All scene operations (scene creation, composition, rendering, input dispatch, effect dispatch) SHALL run on a dedicated player thread, inside guard slices. Scene creation is included because `ImageComposeScene` composes its content in its constructor. Rendered frames SHALL be published as images and displayed inside the game window. Frames are rendered only when the scene has invalidations or animations are running.

#### Scenario: Player UI appears in the game window
- **WHEN** a successful attempt for a ~30-line composable is loaded
- **THEN** its rendered output is displayed in the game's preview pane

#### Scenario: Game UI stays responsive during player work
- **WHEN** player code is busy on the player thread
- **THEN** the game's own UI thread continues to process input and repaint

#### Scenario: Runaway first composition
- **WHEN** the player's composable loops forever during its first composition
- **THEN** the slice that creates the scene is aborted within the time budget and the game keeps running

### Requirement: Input forwarding
Pointer events on the preview pane SHALL be translated to scene coordinates and dispatched to the player's scene on the player thread, so that clickable player UI responds to the player's mouse. The scene's semantics tree SHALL be accessible to the host (this is the basis for future robot customers).

#### Scenario: Clicking a counter button
- **WHEN** the user clicks a button in the preview whose `onClick` increments a `mutableStateOf` counter
- **THEN** the next rendered frame shows the incremented value

#### Scenario: Synthetic click via semantics
- **WHEN** the host locates a node by its text in the scene's semantics tree and sends a press/release at its bounds
- **THEN** the node's click handler runs, exactly as for a real click

### Requirement: Crash isolation
Exceptions thrown by player code during composition, layout, drawing, input handling or coroutine effects SHALL NOT crash the game. The preview engine SHALL catch them, close the broken scene and report a friendly runtime error that includes the exception type, message and the first stack-frame line belonging to the player's file (ignoring frames whose line exceeds the file length, which come from inlined code).

#### Scenario: Exception during composition
- **WHEN** the player's composable throws `NullPointerException` on line 9 during composition
- **THEN** the game keeps running and shows a runtime error pointing at line 9

#### Scenario: Exception in click handler
- **WHEN** an `onClick` lambda throws on a click
- **THEN** the game keeps running and shows the runtime error with the lambda's line

#### Scenario: Exception in LaunchedEffect
- **WHEN** a `LaunchedEffect` coroutine throws after a `delay`
- **THEN** the exception is captured by the scene's coroutine exception handler and reported without crashing the game

#### Scenario: Recover by recompiling
- **WHEN** the player fixes the code after a runtime crash and runs again
- **THEN** a fresh scene renders the new attempt normally

### Requirement: Reload without leaks
Repeated compile → verify → load → render → close cycles SHALL NOT grow retained memory. After warm-up, used heap after full GC, Metaspace usage, loaded-class count minus unloaded-class count, and open file descriptors SHALL stay flat (within noise) across at least 150 cycles, and every discarded attempt class loader SHALL become unreachable.

#### Scenario: 150 reload cycles
- **WHEN** the same ~30-line composable is compiled, loaded, rendered for several frames and closed 150 times in one JVM
- **THEN** heap-after-GC, Metaspace and live class counts at cycle 150 are within a small tolerance of cycle 20, and weak references to at least 95% of discarded class loaders are cleared

### Requirement: Recomposition counting
The preview engine SHALL count how many times each player composable function executes, using the Compose runtime's public `Composer.setTracer` hook and the trace markers emitted by the Compose compiler plugin. It SHALL count only events from the player thread whose source information refers to the player's file. No custom compiler plugin SHALL be required for this.

#### Scenario: Counting recompositions of a counter app
- **WHEN** a counter screen with a header composable and a counter composable receives 5 synthetic clicks
- **THEN** the per-function counts show the counter composable recomposing on each click, and the counts are available to the host for a heatmap

### Requirement: Semantics snapshot
The preview engine SHALL produce, on request, a snapshot of the running attempt's **merged** semantics tree. For each node, the snapshot records its id in the merged tree, its text values, content descriptions, role name (if any), its bounds in the scene in dp, and its children. It also records the viewport size in dp. The snapshot SHALL be taken on the player thread after pending invalidations are rendered, inside a guard slice. It SHALL be plain immutable data holding no reference to Compose objects or to the player's classes or class loader, so it stays valid after the attempt is unloaded and can be serialized across the helper-process boundary. When there is no running scene (nothing loaded, or the attempt crashed), the engine SHALL report no snapshot.

#### Scenario: Snapshot of a column
- **WHEN** an attempt showing `Text("Rosa's Bakery")` above `Text("Fresh bread daily")` is loaded and a snapshot is requested
- **THEN** the snapshot contains two nodes with those texts, and the first node's bottom is not below the second node's top

#### Scenario: Snapshot survives unload
- **WHEN** a snapshot is taken and the attempt is then unloaded and its class loader collected
- **THEN** the snapshot is still readable and does not keep the attempt's class loader reachable

#### Scenario: No scene
- **WHEN** a snapshot is requested after the attempt crashed during composition
- **THEN** the engine reports that no snapshot is available

#### Scenario: Out-of-process helper
- **WHEN** the out-of-process helper handles a compile request
- **THEN** its reply includes the same snapshot the in-process engine would produce, decoded intact on the game's side

### Requirement: Scene size per attempt
Loading an attempt SHALL accept the viewport size in dp to render it at, so each level can choose its viewport. The scene's pixel size SHALL be the viewport size times the engine's density. When no size is given, the engine's default size SHALL be used.

#### Scenario: Level viewport
- **WHEN** an attempt is loaded with a 400 × 300 dp viewport at density 2
- **THEN** the scene renders 800 × 600 pixel frames, and the snapshot reports a 400 × 300 dp viewport

### Requirement: Acting on a snapshot node
The preview engine SHALL offer two actions on a node of the running attempt, named by the id the latest snapshot gave it, each run on the player thread inside a guard slice:
- **press:** a pointer move, press and release at the centre of the node's bounds, through the same path as forwarded mouse input;
- **enter text:** invoke the node's set-text semantics action with a given text.

After the action, the engine SHALL render pending invalidations and complete with the result: whether the node was found (and, for enter text, had a set-text action), the snapshot after the action, and the console text printed since the action started. If the action or the rendering after it throws, the engine SHALL handle it as a crash, and the result SHALL have no snapshot.

#### Scenario: Press a button by id
- **WHEN** the host takes a snapshot, finds the node of a button "Order" and presses it by id
- **THEN** the button's click handler runs once and the result's printed text is what the handler printed

#### Scenario: Enter text by id
- **WHEN** the host enters "Rosa" into the merged node of a `TextField` bound to a `mutableStateOf`
- **THEN** the result's snapshot shows a node with the text "Rosa"

#### Scenario: Stale id
- **WHEN** the host presses an id that is not in the current tree
- **THEN** the result says the node was not found and nothing is pressed


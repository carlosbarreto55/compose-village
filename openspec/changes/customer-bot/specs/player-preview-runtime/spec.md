## MODIFIED Requirements

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

## ADDED Requirements

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

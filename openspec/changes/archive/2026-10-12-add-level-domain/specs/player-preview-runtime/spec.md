## ADDED Requirements

### Requirement: Semantics snapshot
The preview engine SHALL produce, on request, a snapshot of the running attempt's **merged** semantics tree. For each node, the snapshot records its text values, content descriptions, role name (if any), its bounds in the scene in dp, and its children. It also records the viewport size in dp. The snapshot SHALL be taken on the player thread after pending invalidations are rendered, inside a guard slice. It SHALL be plain immutable data holding no reference to Compose objects or to the player's classes or class loader, so it stays valid after the attempt is unloaded and can be serialized across the helper-process boundary. When there is no running scene (nothing loaded, or the attempt crashed), the engine SHALL report no snapshot.

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

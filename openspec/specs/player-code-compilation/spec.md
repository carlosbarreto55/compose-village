# player-code-compilation Specification

## Purpose

Embedded Kotlin + Compose compilation of player source at runtime: version-aligned toolchain, warm compiler environment, generated `LevelEntry` wrapper, in-memory class output, and diagnostics mapped to the player's file.

## Requirements

### Requirement: Version-aligned embedded toolchain
The game SHALL embed `kotlin-compiler-embeddable` and `kotlin-compose-compiler-plugin-embeddable` at exactly the same Kotlin version as the Kotlin Gradle plugin and Compose compiler Gradle plugin used to build the game itself, and the versions SHALL be declared in one place (`gradle/libs.versions.toml`) together with the Compose Multiplatform runtime and material3 versions. The verified matrix for the spike is Kotlin 2.4.21, Compose Multiplatform 1.12.1, material3 1.9.0, JDK 17.

#### Scenario: Matrix declared in a single catalog
- **WHEN** a developer inspects `gradle/libs.versions.toml`
- **THEN** the embedded compiler, the embedded Compose plugin and the Kotlin Gradle plugins all reference the same `kotlin` version entry

#### Scenario: Host API and player code agree on composable lambda shape
- **WHEN** a player attempt's generated wrapper overrides `LevelEntry.content` of type `@Composable () -> Unit`
- **THEN** the compiled override is binary-compatible with the host's `LevelEntry` interface and the cast to `LevelEntry` succeeds at runtime

### Requirement: Compile player source in-process
The system SHALL compile a player's Kotlin source file that uses Jetpack Compose APIs into JVM bytecode inside the game's own JVM, using the Compose compiler plugin, against a compile classpath derived from the game's runtime classpath minus tooling jars. Compiled classes SHALL be returned in memory, and any temporary files written for the attempt SHALL be deleted after the attempt.

#### Scenario: Successful compilation
- **WHEN** a syntactically and semantically valid player file declaring `@Composable fun PlayerScreen()` is submitted
- **THEN** compilation succeeds and returns a non-empty map of JVM internal class names to class bytes

#### Scenario: No leftover files
- **WHEN** 100 compilations have completed
- **THEN** the compiler's scratch directory contains no per-attempt source or output directories

### Requirement: Warm compile latency
The compiler environment SHALL be kept alive between compilations (`kotlin.environment.keepalive`), and the game SHALL perform a background warm-up compilation at startup. A ~30-line player composable SHALL compile in under 2 seconds once the compiler is warm.

#### Scenario: Warm compile under target
- **WHEN** a ~30-line composable is compiled 30 times after warm-up
- **THEN** the median compile time is under 2000 ms, and the median and p95 are recorded in the spike report

#### Scenario: Cold compile is measured
- **WHEN** the first compilation in a fresh JVM runs
- **THEN** its duration is recorded separately as the cold compile time

### Requirement: Generated entry wrapper instead of reflection
For every attempt the system SHALL generate a separate source file containing an object that implements `village.api.LevelEntry` and whose `content` calls the level's expected entry composable, compile it alongside the player's file, and obtain the entry by casting that object's instance to `LevelEntry`. The system SHALL NOT invoke player composable functions via reflection.

#### Scenario: Entry obtained through the wrapper
- **WHEN** an attempt compiles successfully
- **THEN** the host obtains a `LevelEntry` instance from the generated wrapper object and renders `content` without reflective calls to player functions

#### Scenario: Wrapper name cannot clash with player code
- **WHEN** a player declares their own object named `PlayerEntry`
- **THEN** compilation is unaffected because the generated wrapper uses a reserved name that does not collide with typical player identifiers

### Requirement: Diagnostics mapped to the player's file
Compiler errors and warnings reported against the player's file SHALL be surfaced with severity, 1-based line and column (and end position when available) and message, with line numbers matching the editor exactly (no offset). Errors reported against the generated wrapper file SHALL be translated into a "contract" diagnostic that names the expected entry signature (e.g. "expected `@Composable fun BakeryScreen()`").

#### Scenario: Error at exact line and column
- **WHEN** a player file contains an unresolved reference on line 12, column 9
- **THEN** a diagnostic with severity ERROR, line 12, column 9 is returned for the player's file

#### Scenario: Missing entry function
- **WHEN** a player file does not declare the level's expected entry composable
- **THEN** a single contract diagnostic explains which `@Composable` function signature the level expects, instead of an error located in the hidden wrapper file

#### Scenario: Entry function is not composable
- **WHEN** the expected entry function exists but lacks `@Composable`
- **THEN** a contract diagnostic tells the player the function must be annotated `@Composable`

#### Scenario: Entry function has parameters
- **WHEN** the expected entry function is declared with parameters, e.g. `@Composable fun PlayerScreen(name: String)`
- **THEN** a single contract diagnostic at the declaration states the expected signature `@Composable fun PlayerScreen()`

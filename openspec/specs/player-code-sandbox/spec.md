# player-code-sandbox Specification

## Purpose

Protection of the game from player code: bytecode-level allow-list verification with line-attributed violations, class-loader allow-list, reserved packages, captured console output, and a runaway-code guard (time and allocation budgets, sticky abort).

## Requirements

### Requirement: Bytecode allow-list verification
Before loading, the system SHALL inspect every class compiled from player source and check every referenced type, field owner, method owner, `invokedynamic` bootstrap method and method-handle argument against an allow-list. Allowed by default: the attempt's own classes; `kotlin.*` (except file, thread, process and reflection APIs); core `kotlinx.coroutines` (excluding `runBlocking`, `Dispatchers.*`, `GlobalScope` and thread-pool builders); `androidx.compose.*` (excluding window, AWT, clipboard, URI-handler and resource-loading APIs); `androidx.collection`; a curated subset of `java.lang`, `java.util`, `java.math` and `java.time`; and `village.api` except the `Guard` and `PlayerAbort` classes. Everything else SHALL be rejected. Source-level import checks SHALL NOT be relied on, because fully qualified names bypass them.

#### Scenario: File system access rejected
- **WHEN** player code references `java.io.File`, `java.nio.file.*` or `kotlin.io` file helpers
- **THEN** the attempt is rejected before loading with a SANDBOX diagnostic explaining that files are not available in the village

#### Scenario: Network access rejected
- **WHEN** player code references `java.net.URL` or any `java.net` type
- **THEN** the attempt is rejected with a SANDBOX diagnostic about network access

#### Scenario: Reflection and process control rejected
- **WHEN** player code calls `Class.forName`, `java.lang.reflect.*`, `Runtime.exec`, `System.exit`, `exitProcess` or `Thread.sleep`
- **THEN** the attempt is rejected with a SANDBOX diagnostic for each violation

#### Scenario: Method reference to forbidden API rejected
- **WHEN** player code uses a callable reference such as `File::readText`, compiled to an `invokedynamic` handle
- **THEN** the forbidden handle target is detected and the attempt is rejected

#### Scenario: Ordinary Compose and Kotlin code accepted
- **WHEN** player code uses data classes, collections, string templates, lambdas, `remember`, `mutableStateOf`, `LaunchedEffect`, `delay`, Material3 components and `println`
- **THEN** verification passes with no SANDBOX diagnostics

### Requirement: Line-attributed sandbox violations
Each sandbox violation SHALL be reported as an editor diagnostic at the player-source line of the offending instruction, derived from the class's line number table. When the instruction comes from inlined library code (line beyond the file's length), the violation SHALL be attributed to the nearest preceding in-file line (the call site). When the instruction is in compiler-generated code that carries no line numbers (e.g. the class generated for a callable reference), the violation SHALL be attributed to the first in-file line that references that code.

#### Scenario: Violation reported at the call line
- **WHEN** line 7 of the player's file calls `java.io.File("x").readText()`
- **THEN** a SANDBOX diagnostic is reported at line 7

#### Scenario: Violation inside generated code
- **WHEN** line 5 of the player's file declares `val read: (java.io.File) -> String = java.io.File::readText`
- **THEN** every SANDBOX diagnostic for it is reported at line 5, and none is reported without a line

### Requirement: Class-loader allow-list
The per-attempt class loader SHALL resolve only the attempt's own classes and parent classes permitted by the class-level allow-list, SHALL refuse any other class with a sandbox error, and SHALL expose no resources to player code. This is defense in depth, independent of bytecode verification. Classes the JVM resolves through the player's loader to link allowed call sites (the `java.lang.invoke` bootstrap classes behind lambdas and string templates) and the guard classes called by instrumentation SHALL be permitted at the loader level only; player bytecode still cannot reference them.

#### Scenario: Forbidden class cannot be linked
- **WHEN** a player class that bypassed verification (e.g. in a test that disables it) attempts to link `java.io.File`
- **THEN** class loading fails with a sandbox error and the host remains running

### Requirement: Reserved packages
Player classes SHALL NOT be declared in packages that belong to the game or its libraries: `village`, `kotlin`, `kotlinx`, `androidx`, `java`, `javax`, `jdk`, `sun`, `com.sun` and `org.jetbrains`. Such classes SHALL be rejected with a SANDBOX diagnostic, and the per-attempt class loader SHALL never define them, so player code cannot shadow host classes such as `village.api.Guard`.

#### Scenario: Shadowing the guard is rejected
- **WHEN** a player file declares `package village.api` and an `object Guard` with an empty `tick()`
- **THEN** the attempt is rejected with a SANDBOX diagnostic at the declaration line, and instrumented ticks keep calling the host's `Guard`

### Requirement: Runaway-code guard
The system SHALL instrument player bytecode with a call to `village.api.Guard.tick()` at every method entry and before every backward jump. All player code SHALL execute inside host-opened slices on the player thread, each with a wall-clock budget (default 1 s) and an allocation budget (measured per thread). When a budget is exceeded or the host aborts, `tick()` SHALL throw a `PlayerAbort` error. The abort SHALL be sticky until the next slice begins, so player `catch (Throwable)` blocks cannot swallow it.

#### Scenario: Infinite loop stopped
- **WHEN** player composition executes `while (true) {}`
- **THEN** the slice is aborted within about 1.5× the time budget, the game stays responsive, and the player sees a friendly "infinite loop?" message

#### Scenario: Swallowing the abort does not help
- **WHEN** player code loops with `while (true) { try { while (true) {} } catch (t: Throwable) {} }`
- **THEN** the abort still escapes, because the next tick throws again

#### Scenario: Unbounded allocation stopped
- **WHEN** player code appends to a list forever
- **THEN** the allocation budget is exceeded and the slice is aborted before the game's heap is exhausted

#### Scenario: Deep recursion contained
- **WHEN** player code recurses without a base case
- **THEN** the resulting `StackOverflowError` is caught by the host and reported, and the game keeps running

#### Scenario: Wedged in uninstrumented code
- **WHEN** a slice exceeds a hard watchdog limit without reaching any tick
- **THEN** the host abandons the player thread, starts a fresh preview engine and tells the player the preview was restarted

### Requirement: Captured console output
The system SHALL redirect player `println`/`print` output (including `System.out` and `System.err` accesses produced by inlined stdlib functions) to an in-game console buffer instead of the host's standard output.

#### Scenario: println shows in game console
- **WHEN** player code calls `println("hello")`
- **THEN** "hello" appears in the attempt's console buffer and not on the host process's stdout

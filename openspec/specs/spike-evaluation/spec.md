# spike-evaluation Specification

## Purpose

The measurement harness, out-of-process comparison, packaging check, alternative-approach evaluation, and the final report with recommendation that settled how Compose Village compiles and runs player code (introduced by `spike-in-process-player-compilation`).

## Requirements

### Requirement: Headless spike harness
The project SHALL provide a headless Gradle task (`./gradlew :app:spike`) that runs every spike measurement and test without a display. It SHALL write a markdown report under `docs/spike/` with measured values, and a PASS/FAIL verdict for each success criterion: warm compile < 2 s; rendered output; correct error lines; survival of runtime exceptions; no memory growth over 100+ reloads.

#### Scenario: Report produced
- **WHEN** `./gradlew :app:spike` is run on the development Mac
- **THEN** a report file is written containing cold, first-after-warm-up, and warm median/p95 compile times, diagnostics checks, crash, runaway and sandbox test outcomes, memory series, and a verdict per success criterion

### Requirement: Latency variants measured
The harness SHALL measure compile latency for these variants: keepalive environment on vs off, and full host-derived compile classpath vs a trimmed Compose-only classpath. Each variant SHALL be measured over at least 30 warm runs.

#### Scenario: Variant table in report
- **WHEN** the latency benchmarks complete
- **THEN** the report contains a table with median and p95 per variant

### Requirement: Memory series recorded
The harness SHALL record, per reload cycle, the used heap after GC, the Metaspace usage, the loaded and unloaded class counts, the open file descriptors and the process RSS. It SHALL also record whether each discarded class loader was collected.

#### Scenario: Memory data available
- **WHEN** the 150-cycle reload test completes
- **THEN** the per-cycle series is written as CSV next to the report, with a summary of growth between cycle 20 and cycle 150

### Requirement: Out-of-process comparison
The spike SHALL implement a minimal helper JVM (`CompileServer`) that reuses the same pipeline. It talks to the game over stdin/stdout, compiles and renders an attempt, and returns diagnostics and a PNG frame. The harness SHALL measure helper spawn time, warm compile round-trip, per-frame render-and-transfer latency, and helper RSS, and put these side by side with the in-process numbers.

#### Scenario: Side-by-side comparison
- **WHEN** both the in-process and the out-of-process benchmarks have run
- **THEN** the report shows warm compile latency, frame latency, memory footprint and failure-isolation properties of both approaches in one table

### Requirement: Packaging check
The spike SHALL build a macOS distributable (`./gradlew :app:packageDmg` / `createDistributable`) that bundles the embedded compiler. The bundled app SHALL support a `--selftest` flag that compiles and renders a sample composable once and exits with status 0 on success. The report SHALL state the `.app` and DMG sizes and the size attributable to the compiler.

#### Scenario: Bundled self-test passes
- **WHEN** the packaged app is launched with `--selftest`
- **THEN** it compiles and renders the sample inside the bundle's Java runtime and exits with status 0

#### Scenario: Size impact reported
- **WHEN** packaging completes
- **THEN** the report lists the total bundle size and the compiler's share of it

### Requirement: Recommendation with alternatives
The final report SHALL document the exact working version matrix, and give a recommendation with trade-offs. It SHALL evaluate on paper, using the measured numbers, (1) out-of-process compilation, (2) server-side compilation, (3) an interpreted Compose-like DSL subset, and (4) blocks/fill-in-the-blanks. It SHALL also list known limitations of the recommended approach: in-process OOM cannot be fully capped, `Thread.stop` is unavailable on JDK 20+, and the sandbox allow-list needs a security audit. It SHALL state the next step: an architecture proposal and an MVP plan for 5 Main Street levels, produced only after the recommendation is accepted.

#### Scenario: Report concludes with a recommendation
- **WHEN** a reader opens the spike report
- **THEN** it ends with a recommended approach, its trade-offs, the known limitations, and the gated next step

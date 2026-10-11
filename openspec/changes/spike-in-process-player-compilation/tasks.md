## 1. Scaffold (already on disk, unverified)

- [x] 1.1 Review existing scaffold (`settings.gradle.kts`, `gradle/libs.versions.toml`, module build files, `host-api/LevelEntry.kt`, `host-api/Guard.kt`, `compiler/{Diagnostics,PlayerClasspath,PlayerCompiler}.kt`) against design D1–D7 and fix drift
- [x] 1.2 Make `./gradlew build` pass on JDK 17 (replace placeholder sources in `preview`/`app` as needed)
- [x] 1.3 Add `Console` (captured `out`/`err` streams + per-attempt buffer) to `host-api`
- [x] 1.4 Add a `docs/spike/` directory and a `VERSIONS.md` that mirrors the version matrix from `design.md`

## 2. Compilation pipeline (`compiler` module)

- [x] 2.1 Smoke test: compile a hello-world `@Composable` with `PlayerCompiler` from a throwaway main and confirm non-empty class output
- [x] 2.2 Implement wrapper generation (`__VillageEntry` object implementing `LevelEntry`, configurable entry function name) in `PlayerPipeline`
- [x] 2.3 Map raw diagnostics: player-file diagnostics pass through unchanged; wrapper-file errors become CONTRACT diagnostics (missing function, wrong parameters, not `@Composable`)
- [x] 2.4 Add the trimmed Compose-only classpath option alongside the host-derived classpath
- [x] 2.5 Add per-attempt timings (compile / guard / load) to the pipeline result

## 3. Sandbox (`compiler` + `host-api`)

- [ ] 3.1 Implement `SandboxPolicy`: package allow-lists, deny-lists with reasons, member-level rules (`System`, `Class`, `Object.wait`, `PrintStream`, coroutines builders/dispatchers, Compose window/AWT/clipboard/URI/res APIs), and own-class detection
- [ ] 3.2 Implement `BytecodeGuard` verification with ASM tree API: types, field/method owners, indy bootstrap + handle args, LDC types/handles, exception-table types; attribute violations to source lines (fallback to last in-file line for inlined code)
- [ ] 3.3 Implement instrumentation: `Guard.tick()` at method entry and before backward jumps; rewrite `System.out`/`System.err` reads to `Console`; write classes without frame recomputation and verify with `CheckClassAdapter`
- [ ] 3.4 Implement `PlayerClassLoader` (in-memory define, class-level allow-list delegation, no resources)
- [ ] 3.5 Finish `Guard` slices: time budget, allocation budget, sticky abort, external `abort()`; unit-test each in isolation

## 4. Preview runtime (`preview` module)

- [ ] 4.1 Create the player thread + coroutine dispatcher that wraps every task in a guard slice
- [ ] 4.2 Implement `PreviewEngine.load(entry)`: build `ImageComposeScene` with player dispatcher + `CoroutineExceptionHandler`, render first frame, publish `ImageBitmap`
- [ ] 4.3 Render loop: render only on `hasInvalidations()` or running animations; publish frames to a host-observable state
- [ ] 4.4 Crash handling: catch exceptions from `render`/`sendPointerEvent`/effects, close the scene, produce a `RuntimeError` with the first in-file `Player.kt` frame line
- [ ] 4.5 Hard watchdog: detect a slice stuck past the hard limit, abandon the thread, recreate the engine, report "preview restarted"
- [ ] 4.6 Pointer forwarding (preview coordinates → scene coordinates) and a semantics helper to find a node by text and synthesize a click
- [ ] 4.7 Recomposition counter via `Composer.setTracer`, filtered by player thread and player file; record the actual info-string format (design open question)
- [ ] 4.8 Release previous scene/entry/loader on reload; keep a `WeakReference` per loader for leak checks

## 5. Spike app window (`app` module)

- [ ] 5.1 Compose Desktop window: code editor (monospace, line numbers), Run button + Cmd+Enter, preview pane showing engine frames
- [ ] 5.2 Diagnostics list and red line markers; clicking a diagnostic moves the caret to line/column
- [ ] 5.3 Runtime-error panel, console output panel, compile timings, per-composable recomposition counts
- [ ] 5.4 Background warm-up compile at startup with a "compiler warming up" indicator
- [ ] 5.5 `--selftest` flag: compile + render a sample once headlessly, exit 0 on success / non-zero on failure

## 6. Headless spike harness (`./gradlew :app:spike`)

- [ ] 6.1 Harness skeleton writing `docs/spike/report.md` and CSVs, with PASS/FAIL per success criterion
- [ ] 6.2 Latency: cold compile (fresh JVM run), first-after-warm-up, 30+ warm runs median/p95 for a ~30-line composable; variants keepalive on/off and full/trimmed classpath
- [ ] 6.3 Diagnostics tests: known-bad sources assert exact line/column; contract diagnostics for missing/non-composable/parameterized entry
- [ ] 6.4 Crash tests: throw in composition, in `onClick` (synthetic click), in `LaunchedEffect` after `delay`; assert host survives and line is correct; recompile-after-crash recovers
- [ ] 6.5 Runaway tests: `while(true){}`, nested loop with `catch (Throwable)`, unbounded list growth, unbounded recursion; assert abort time and continued operation
- [ ] 6.6 Sandbox tests: `File`, `java.nio.file`, `URL`, `Class.forName`, `java.lang.reflect`, `Runtime.exec`, `System.exit`, `exitProcess`, `Thread.sleep`, `File::readText` reference; plus an allowed-code test (data classes, collections, state, effects, Material3, `println`)
- [ ] 6.7 Memory test: 150 compile→guard→load→render→close cycles; per-cycle heap-after-GC, Metaspace, loaded/unloaded classes, open FDs, RSS, loader-collected flag; growth summary cycle 20 vs 150
- [ ] 6.8 Heatmap probe: counter app with header + counter composables, 5 synthetic clicks, record per-function counts

## 7. Out-of-process comparison

- [ ] 7.1 Implement `CompileServer` main (same classpath/pipeline) with a length-prefixed stdin/stdout protocol returning diagnostics + PNG frame
- [ ] 7.2 Harness client: measure spawn time, warm compile round-trip, per-frame render+PNG transfer latency, helper RSS
- [ ] 7.3 Side-by-side table in the report: latency, frame cost, memory, failure isolation (OOM, wedged thread, `System.exit` survivability)

## 8. Packaging

- [ ] 8.1 Run `suggestRuntimeModules`; set `nativeDistributions.modules` accordingly
- [ ] 8.2 Build `createDistributable` and `packageDmg`; record `.app` and DMG sizes, plus size without compiler jars for the delta
- [ ] 8.3 Launch the bundled app with `--selftest` and record the result; verify the compiler uses the bundled runtime as JDK home

## 9. Report and recommendation

- [ ] 9.1 Write the final `docs/spike/report.md`: version matrix, all measurements, PASS/FAIL per success criterion
- [ ] 9.2 Paper evaluation of server-side compilation, interpreted Compose-like DSL subset, and blocks/fill-in-the-blanks using the measured numbers
- [ ] 9.3 Recommendation with trade-offs and known limitations (OOM, no `Thread.stop` on JDK 20+, sandbox audit needed), plus answers to the design's open questions
- [ ] 9.4 State the gated next step: architecture proposal + MVP plan for 5 Main Street levels after the recommendation is accepted

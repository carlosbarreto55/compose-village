# Spike report: compiling and running player code inside the game

**Change:** `spike-in-process-player-compilation` (OpenSpec) · **Date:** 2026-10-11 · **Platform measured:** macOS 26.6 on Apple M5 (arm64), JDK 17.0.19

**Recommendation:** build the game on the **in-process** pipeline proved here: embedded K2 compiler, ASM sandbox and
guard, and an offscreen Compose scene on a player thread. All five success criteria pass. Keep the out-of-process
helper from this spike as a tested fallback behind the same interface. Use fill-in-the-blanks for the first levels on
top of the same pipeline. Do not build server-side compilation or a custom DSL interpreter.

The numbers below come from the last harness run, [`results.md`](results.md) (`./gradlew :app:spike`, with raw CSVs
next to it), and the packaging check, [`packaging.md`](packaging.md) (`scripts/packaging-check.sh`). Both are
generated, so re-running them reproduces every figure. Run-to-run variation is about ±20 % for latencies.

## 1. The question

Every level of Compose Village compiles the player's Kotlin + Compose code at runtime and shows the UI it produces.
The spike had to show, with measurements, whether this can happen safely inside the game's own JVM, and what the
alternatives would cost.

## 2. Version matrix (verified)

| Component | Artifact | Version |
|---|---|---|
| Embedded compiler | `org.jetbrains.kotlin:kotlin-compiler-embeddable` | 2.4.21 |
| Embedded Compose plugin | `org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable` | 2.4.21 |
| Game build plugins | `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.plugin.compose` | 2.4.21 |
| Compose Multiplatform | `org.jetbrains.compose` (runtime/ui/foundation desktop) | 1.12.1 |
| Material3 | `org.jetbrains.compose.material3:material3` | 1.9.0 |
| Skiko | `org.jetbrains.skiko:skiko-awt-runtime-macos-arm64` | 0.150.1 |
| ASM | `org.ow2.asm:asm`, `asm-tree`, `asm-util` | 9.11 |
| JVM | JDK toolchain and bundled runtime | 17 (Microsoft OpenJDK 17.0.19) |
| Build | Gradle wrapper | 9.4.1 |

All Kotlin artifacts reference the single `kotlin` entry in `gradle/libs.versions.toml`. This matters because the
Compose plugin is not binary compatible across Kotlin versions, and `host-api`'s `LevelEntry` must have the same
lambda shape as the generated wrapper. Rules and the build environment: [`VERSIONS.md`](VERSIONS.md). Bundled runtime
modules: the Compose defaults plus `java.instrument`, `java.management`, `jdk.management` and `jdk.unsupported`.

## 3. Success criteria

| Criterion | Verdict | Evidence |
|---|---|---|
| Warm compile under 2 s for a ~30-line composable | **PASS** | median 146 ms, p95 221 ms over 30 warm runs (keepalive on, full classpath) |
| Player UI renders in the game window | **PASS** | first frame 2.2 ms, re-render after a click 0.2 ms; the spike window shows it in its preview pane (4/4 checks) |
| Errors point at the correct lines | **PASS** | 39/39 checks: compiler, contract, sandbox and runtime errors at exact line (and column) |
| The game survives runtime exceptions and runaway code | **PASS** | 16/16 checks: crashes in composition, click and effect; infinite loops, abort swallowing, allocation, recursion and a wedged thread |
| No memory growth over 100+ reloads | **PASS** | 150 cycles: heap after GC +0.2 MB, classes +26, file descriptors +0, Metaspace (C1 control) −0.1 MB, 150/150 class loaders collected |

The bundled `.app` also passes `--selftest` (compile, render, click), with the compiler reading JDK classes from the
bundled runtime.

## 4. Measurements

### 4.1 Compile latency

Workload: a ~30-line counter screen (state, `LaunchedEffect`, layout, Material3). Each variant ran in a fresh JVM.

| Variant | Cold (first in JVM) | First after warm-up | Warm median | Warm p95 |
|---|---|---|---|---|
| keepalive on, full classpath (47 jars), **default** | 1331 ms | 316 ms | 146 ms | 221 ms |
| keepalive on, trimmed classpath (21 jars) | 1426 ms | 389 ms | 145 ms | 280 ms |
| keepalive off, full classpath | 1479 ms | 387 ms | 161 ms | 248 ms |
| keepalive off, trimmed classpath | 1476 ms | 390 ms | 154 ms | 249 ms |

- Bytecode verification, instrumentation and class loading add 1–2 ms to a warm compile (about 15 ms for the first one).
- The cold compile (1.3–1.7 s) is the game's background warm-up at start-up, so the player never waits for it.
  The player's first Run after it takes about 0.3–0.4 s, and every Run after that about 150 ms.
- The trimmed classpath does not change warm latency at all. Keepalive saves at most about 10 ms when warm, which is
  within run-to-run noise. Warm speed comes from the JVM itself being warm (JIT, loaded classes), not from the
  compiler's kept-alive environment or from classpath indexing.

### 4.2 Diagnostics

Compiler errors reach the editor at the player's exact line and column, including Compose-plugin errors, syntax
errors, several errors in one file, files with a `package`, and warnings. Errors in the hidden wrapper become one
*contract* message naming the expected signature (missing, non-composable, parameterized or private entry, or another
entry name). Sandbox violations and runtime errors are also line-accurate, and the spike window moves the caret to a
clicked diagnostic. The report's diagnostics table lists every message as the player sees it.

### 4.3 Sandbox

Two independent layers: ASM verification of every type, member, `invokedynamic` and handle reference against an
allow-list, plus a class loader that refuses anything outside it, even when verification is skipped. Rejected at the
right line, each with a friendly reason: `java.io.File`, `java.nio.file`, `java.net.URL`, `Class.forName`,
`java.lang.reflect`, `Runtime.exec`, `System.exit`, inlined `exitProcess`, `Thread.sleep`, the `File::readText`
reference, `Object.wait`, `runBlocking`, `Dispatchers.IO`, `LocalUriHandler`, and a player class in `village.api`
shadowing the guard. Ordinary code (data classes, collections, state, effects, Material3, `println`) passes, runs,
and its output goes to the in-game console.

### 4.4 Crash isolation and runaway code

| Case | Result |
|---|---|
| `NullPointerException` in composition, exception in `onClick`, exception in `LaunchedEffect` after `delay` | reported at lines 7, 8 and 10; the next attempt renders |
| `while (true) {}` in composition / in an effect | stopped after 1001 / ~1015 ms (budget 1000 ms) |
| Loop that swallows the abort with `catch (Throwable)` | still stopped after 1001 ms (the abort is sticky) |
| Unbounded list growth | stopped by the 512 MB per-slice allocation budget after 319 ms |
| Unbounded recursion | `StackOverflowError` caught after 14 ms, reported at the recursive line |
| Wedged in `java.math` (uninstrumented, never ticks) | watchdog abandoned the thread and restarted the preview after 1.6 s (hard limit 1.5 s) |

A heartbeat thread in the host never stalled more than 47 ms during any of these, so the game's UI stays responsive.

### 4.5 Memory over 150 reloads

Each cycle compiles, verifies, loads, renders, clicks and closes an attempt with its own classes. From cycle 20 to 150:
heap after GC +0.2 MB, loaded classes +26, open file descriptors +0, Metaspace −0.1 MB in a C1-only control run, and
150/150 discarded class loaders collected. Total Metaspace under the default tiered JIT grows slowly (+8.5 MB, and
decelerating). That is profiling metadata for the host's own hot code; class space and the class count stay flat.

### 4.6 Recomposition counting

`Composer.setTracer` with the plugin's trace markers counts composable executions per function, filtered to the
player thread and file, with no custom compiler plugin. After 5 clicks on a counter, `Counter` ran 6 times and
`Header` once, which is exactly what a heatmap needs.

### 4.7 In-process vs out-of-process (measured)

The helper JVM (`CompileServer`) runs the identical pipeline and returns diagnostics and a PNG frame over a
length-prefixed stdin/stdout protocol.

| | In-process | Out-of-process |
|---|---|---|
| Start-up before the first compile | none | 88 ms to spawn the helper |
| Cold compile → first frame | 1739 ms | 1702 ms |
| Warm compile → first frame, median / p95 | 161 / 281 ms | 178 / 259 ms (process boundary 0.6 ms) |
| Click → new frame on the host, median / p95 | 3.3 / 5.8 ms | 7.7 / 12.4 ms (PNG encode 4.4 ms, decode 0.4 ms, 11 KB) |
| Memory | one JVM, ~1.1 GB RSS at `-Xmx1g` after the workload | the game plus a helper of about the same size (1.1 GB RSS after the same workload) |
| `System.exit` in player code | blocked by the sandbox; if it escaped, the game would exit | helper exits, detected in 0.4 s, new helper with a first frame in 2.3 s |
| Heap exhaustion | allocation budget per slice; a residual risk remains (§6.2) | helper exits on OOM, detected in 0.2 s, recovered in 2.3 s |
| Wedged thread | watchdog restarts the preview; the thread keeps burning a CPU core until exit | 3 s request timeout, helper killed, recovered in 2.3 s; nothing leaks |

The process boundary costs almost nothing per compile and about 5 ms per frame. Its real costs are a second JVM's
memory and a cold compile after every recovery. Its benefit is that exit, OOM and hangs cannot take the game down.
The helper runs with the same user's permissions, though. Without OS-level sandboxing it is **not** a security
boundary against a sandbox escape that reads files or opens sockets. RSS is mostly heap that G1 grew towards `-Xmx`
and has not been tuned yet. The helper faults were simulated, since the sandbox and guard block their real causes.

### 4.8 Packaging (macOS)

| | Size |
|---|---|
| `.app` on disk | 187.1 MB, of which compiler-only jars are 60.9 MB |
| DMG (`packageDmg`) | 123.6 MB |
| Compiler's share of a compressed DMG | 55.8 MB (124.6 MB vs 68.8 MB, compressed the same way) |
| Bundled `--selftest` in an empty environment | exit 0 in 2.6 s; compiler JDK home = bundled runtime |

`suggestRuntimeModules` also lists `java.scripting` and `jdk.compiler`. The compiler jar references them, but the
player pipeline never runs that code, so they are not shipped (saving 15 MB). `PlayerCompiler` passes `jdkHome`
explicitly, so a `JAVA_HOME` or system JDK can never change what player code compiles against.

## 5. Alternatives, evaluated with the measured numbers

| | In-process (built) | Out-of-process helper (built) | Server-side compilation | Interpreted Compose-like DSL | Blocks / fill-in-the-blanks |
|---|---|---|---|---|---|
| Feedback after Run | ~150 ms warm | ~180 ms warm | ~150 ms + network round trip; classes or frames must come back | likely < 10 ms (not built) | instant (fixed choices) to ~150 ms (compiled template) |
| Click → frame | 3 ms | 8 ms | 3 ms if classes run locally; RTT + 8 ms if frames are streamed | fast | as the underlying engine |
| Real Kotlin + Compose | yes | yes | yes | no: a subset that must re-implement state, `remember`, effects, lambdas, types | partial, by design |
| Error quality | kotlinc's own, exact positions (39/39) | same | same | must be written by hand | constrained, few errors possible |
| Protects the game from player code | sandbox + guard; exit/OOM risk remains in-process | + crash/OOM/hang isolation | player code still runs on the client unless frames are streamed | strong: no bytecode at all | strong |
| Offline play | yes | yes | no | yes | yes |
| Download | +56 MB DMG | +56 MB DMG | saves 56 MB | saves 56 MB | +0 / +56 MB |
| Running cost | none | none | servers with ~0.4–1 GB warm compiler per worker (~6 compiles/s each), accounts, ops, privacy of player code | none | none |
| Engineering effort | done (spike) | done (spike) + process supervision | backend + protocol + scaling | very high, and it never ends | moderate UI work on top of the engine |

- **Out-of-process** is a cheap switch thanks to the shared pipeline. It trades about 1 GB of extra RSS (untuned)
  and 2.3 s recoveries for isolation from exit, OOM and hangs. It is worth adopting if playtests show OOMs or wedged
  threads, or as the host for an OS-level sandbox if security requirements grow.
- **Server-side** removes only the download size and the start-up warm-up, which the player never waits for anyway.
  It adds latency, running costs and an online requirement. The client would still run player bytecode (needing the
  same sandbox) or stream frames (an RTT on every click). Not worth it for a desktop game.
- **An interpreted DSL** would be instant and safe, but it teaches something that is not Kotlin. It also throws away
  what the spike gets for free: the real compiler's type checking and exact diagnostics. Rejected.
- **Blocks / fill-in-the-blanks** complement the engine rather than replace it. Early levels can constrain input to
  blanks in a template compiled by the same pipeline, or to enumerated choices with precompiled variants, before
  players graduate to free-form code.

## 6. Recommendation

Adopt the in-process pipeline (`host-api`, `compiler`, `preview`) as the game's player-code runtime, behind an
interface that the measured `CompileServer` can also implement.

### 6.1 Trade-offs accepted

- **+56 MB of compressed download** (the compiler) on a 124 MB DMG.
- **About 1 GB RSS at `-Xmx1g`**, mostly heap the JVM grew into and keeps. Tuning heap size and GC is MVP work.
- **A 1.3–1.7 s cold compile** at every start, hidden by the background warm-up. A Run during warm-up waits for it.
- **Frames are CPU-rastered:** about 0.5 ms each for a phone-sized preview, which is fine.
- **Compiler internals pinned:** the K2 CLI entry point and its arguments can change between Kotlin versions.
  `./gradlew :app:spike` is the upgrade gate, and `kotlin-build-tools-api` is the migration path if the CLI breaks.

### 6.2 Known limitations

1. **In-process OOM cannot be fully capped.** The allocation budget is per slice and per thread. It stops runaway
   loops in player code (319 ms in the test), but it cannot catch a single huge allocation inside library code, or
   state that grows a little in every slice and stays reachable (e.g. a list in `remember` growing on every frame). Any
   of these can exhaust the shared heap and take the game down. Mitigations for the MVP: a retained-heap watchdog
   that aborts the attempt when used heap after GC crosses a threshold, and saving progress often. The full fix is
   the out-of-process helper (exits on OOM, recovers in 2.3 s).
2. **No `Thread.stop` on JDK 20+.** It throws `UnsupportedOperationException`, so code wedged in uninstrumented library
   code cannot be killed. The watchdog abandons the thread and restarts the preview (1.6 s), but the thread keeps a
   CPU core busy until the game exits, and repeated wedges add up. Mitigation: cap abandoned threads and ask for a
   restart. Fix: the helper (killed and respawned in 2.3 s). The spike targets JDK 17 but deliberately does not rely
   on `Thread.stop`.
3. **The sandbox allow-list needs a security audit.** It is hand-written and tested against 15 attack classes, not
   proven complete. `androidx.compose.*` and most of `kotlin.*` are allowed with specific exceptions, so something
   dangerous may still be reachable (e.g. other Compose platform `CompositionLocal`s). Audit before release, and
   consider a stricter allow-list for `androidx.compose.ui.platform`.
4. **Only macOS arm64 is verified.** Windows and Linux packaging, runtime modules and latency are not measured.
5. **Text input is unverified.** `TextField` levels in the offscreen scene have not been tried (see §7).

## 7. Answers to the design's open questions

1. **Does `ImageComposeScene` surface exceptions synchronously?** It depends on where the exception happens.
   - First composition: thrown synchronously from `setContent`.
   - Click handlers: thrown synchronously from `sendPointerEvent`.
   - Recomposition after a state change: does *not* come out of `render()`. It reaches the scene's
     `CoroutineExceptionHandler`, because the Recomposer runs in the scene's coroutine context.
   - `LaunchedEffect` failures: also reach the handler.

   The engine handles both routes, and the crash tests cover each one.
2. **What is the trace info-string format?** `"<function> (Player.kt:<line of the declaration>)"`, e.g.
   `PlayerScreen (Player.kt:16)`. Composable lambdas appear as `Counter.<anonymous>` or
   `ComposableSingletons$PlayerKt.lambda$<n>.<anonymous>`, so a heatmap should group or hide those. The strings and
   counts are identical with `sourceInformation=false`: trace markers do not depend on source information.
3. **Is text input good enough for `TextField` levels?** Not verified, as planned. The scene offers `sendKeyEvent` and
   the semantics `SetText` action. Try both in the first MVP level that uses a `TextField`.
4. **Is warm latency dominated by classpath indexing?** No. The trimmed 21-jar classpath compiles as fast as the full
   47-jar one (145 vs 146 ms median), and keepalive saves at most about 10 ms when warm, within noise. Keep the full
   host-derived classpath as the default: it guarantees binary compatibility with the runtime that renders the result.
5. **Should players also get Material (M2)?** This is a product decision, not a technical one. M2 is on the classpath
   (Compose Desktop brings it) and the sandbox currently allows it. The recommendation is Material3 only, to match what
   the curriculum teaches, enforced with a `SandboxPolicy` rule that denies `androidx.compose.material.*` outside
   material3's dependencies.

Found while writing this report: the line shown for an infinite loop inside a `LaunchedEffect` occasionally pointed
at line 1. The abort had been thrown inside a Kotlin synthetic `access$` accessor, whose line table says line 1.
`RuntimeError` now skips those frames, and a unit test pins the case.

## 8. Next step (gated)

Once this recommendation is accepted, and not before, open a follow-up OpenSpec change with:

1. **An architecture proposal:** promote `host-api`, `compiler` and `preview` into the game; define a player-runtime
   interface (in-process implementation, with `CompileServer` as the fallback); the level contract (entry function,
   checks); robot customers on the scene's semantics tree; the recomposition heatmap on the tracer; heap tuning and
   the retained-heap watchdog; and a sandbox audit plan.
2. **An MVP plan for 5 Main Street levels** built on that architecture, including where fill-in-the-blanks levels
   come before free-form code.

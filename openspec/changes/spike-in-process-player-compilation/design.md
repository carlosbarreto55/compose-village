## Context

Compose Village is a Compose Multiplatform Desktop game that teaches Kotlin and Jetpack Compose. Every level depends on compiling the player's code at runtime and rendering it inside the game. This design covers the technical spike that de-risks that pipeline (see `proposal.md`). The requirements are in `specs/player-code-compilation`, `specs/player-code-sandbox`, `specs/player-preview-runtime` and `specs/spike-evaluation`.

**Current state.** A Gradle 9.4.1 multi-module scaffold exists in `~/Downloads/project/compose-village`, with modules `host-api`, `compiler`, `preview` and `app`, and a version catalog. It also contains first drafts of `LevelEntry.kt`, `Guard.kt`, `Diagnostics.kt`, `PlayerClasspath.kt` and `PlayerCompiler.kt`. Dependencies resolve, but nothing has been compile-verified yet.

**Environment.** Apple M5, 16 GB, JDK 17.0.19 (Microsoft) and JBR 17.0.14, both with `jpackage`. The primary target is macOS arm64.

**Facts verified via `javap` against the resolved jars:**
- **Compiler entry point:** `K2JVMCompiler().exec(MessageCollector, Services.EMPTY, K2JVMCompilerArguments)`. The arguments expose `freeArgs`, `classpath`, `destination`, `noStdlib`, `noReflect`, `jvmTarget`, `moduleName`, `pluginClasspaths` and `pluginOptions`.
- **Diagnostics:** `MessageCollector.report(severity, message, CompilerMessageSourceLocation{path, line, column, lineEnd, columnEnd, lineContent})`.
- **Warm environment:** the system property `kotlin.environment.keepalive` (`CompilerSystemProperties`) keeps the compiler's application environment alive between runs.
- **Compose compiler plugin:** id `androidx.compose.compiler.plugins.kotlin`. Its options include `sourceInformation`, `traceMarkersEnabled`, `strongSkipping`, `stabilityConfigurationPath`, `metricsDestination` and `reportsDestination`. It registers through `META-INF/services` (`ComposePluginRegistrar`, `ComposeCommandLineProcessor`).
- **Offscreen scene:** `androidx.compose.ui.ImageComposeScene(width, height, density, coroutineContext, content)` offers `render(nanoTime): org.jetbrains.skia.Image`, `sendPointerEvent(...)`, `sendKeyEvent`, `hasInvalidations()`, `semanticsOwners` and `close()`.
- **Tracer hook:** `androidx.compose.runtime.Composer.Companion.setTracer(CompositionTracer)` is public in runtime 1.12.1, with `isTraceInProgress`, `traceEventStart(key, dirty1, dirty2, info)` and `traceEventEnd`. The runtime also has `ComposeStackTrace*` and `CompositionErrorContext` for composition-aware stack traces.
- **Runtime artifacts:** CMP 1.12.1 resolves `org.jetbrains.compose.runtime:runtime-desktop` to a 9-file shim. The real classes are in `androidx.compose.runtime:runtime-desktop:1.12.1`.
- **Jar sizes:** compiler-side jars total about 61 MB uncompressed (`kotlin-compiler-embeddable` 56.5 MB, `kotlin-reflect` 1.6.10 2.9 MB, Compose plugin 0.8 MB). Compose and Skiko together are about 38 MB.

**Version matrix:**

| Component | Artifact | Version |
|---|---|---|
| Embedded compiler | `org.jetbrains.kotlin:kotlin-compiler-embeddable` | 2.4.21 |
| Embedded Compose plugin | `org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable` | 2.4.21 |
| Game build plugins | `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.plugin.compose` | 2.4.21 |
| Compose Multiplatform | `org.jetbrains.compose` (runtime/ui/foundation desktop) | 1.12.1 |
| Material3 | `org.jetbrains.compose.material3:material3` | 1.9.0 (the version CMP 1.12.1 maps `compose.material3` to) |
| Skiko | `org.jetbrains.skiko:skiko-awt-runtime-macos-arm64` | 0.150.1 |
| ASM | `org.ow2.asm:asm`, `asm-tree` | 9.11 |
| JVM | JDK (toolchain + bundled runtime) | 17 |

## Goals / Non-Goals

**Goals:**
- Prove or disprove in-process compilation against the five success criteria: warm compile under 2 s for about 30 lines, rendering in the game window, correct error lines, surviving runtime exceptions, and no memory growth over 100+ reloads.
- Produce measured numbers for in-process vs. out-of-process, and evaluate the other alternatives on paper.
- Lock down an exact, documented version matrix and a packaging story for macOS.
- Leave behind reusable building blocks (`compiler`, `preview`, `host-api` modules) for the real game if the approach is accepted.

**Non-Goals:**
- Gameplay, villagers, levels, scoring, sketch matching, and production-quality editor UX.
- Robot customers and the heatmap UI. Only their technical hooks (semantics access, tracer counts) are proven.
- A security-audited sandbox. The spike establishes the mechanism and tests the main attack classes only.
- Windows and Linux verification.
- The architecture and MVP plan. These come after the recommendation is accepted.

## Decisions

### D1. Compile with the K2 CLI entry point (`K2JVMCompiler.exec`) in-process, with keepalive
The CLI entry point is the most stable embedding surface. It handles plugin loading via `pluginClasspaths`/`pluginOptions` and reports through `MessageCollector`. `kotlin.environment.keepalive=true` keeps the IntelliJ application environment (VFS, jar index, extension points) alive between runs. This is the same mechanism the Kotlin daemon uses, and it is the main warm-latency lever. A background warm-up compile runs at startup.
- *Alternatives:*
  - Low-level `KotlinCoreEnvironment` / `KotlinToJVMBytecodeCompiler` would allow in-memory output, but these are internal and change between Kotlin versions.
  - `kotlin-build-tools-api` is the officially supported embedding API (in-process or daemon). It is noted as a follow-up if the CLI path proves fragile.
  - Kotlin scripting (`BasicJvmScriptingHost`) adds script semantics we do not want.
  - The Kotlin daemon adds a process boundary. That boundary is covered by the out-of-process comparison instead.

### D2. Temporary source/output directory per attempt, read back into memory
Each attempt writes sources and classes into a unique scratch directory. The classes are read into a `Map<internalName, ByteArray>` and the directory is deleted. This keeps the compiler on its supported file-based path. The cost is a few milliseconds. The leak test checks for file-descriptor or VFS growth caused by unique paths.

### D3. Compile classpath derived from the host runtime classpath
The player compiles against the jars the game is running: `java.class.path` minus tooling jars (compiler, plugin, daemon, build-tools, ASM, app/compiler/preview modules). This guarantees binary compatibility with the Compose runtime that renders the result, and it also works inside a jpackage bundle. The Compose plugin jar is located through the code source of `ComposePluginRegistrar`. A trimmed Compose-only classpath is benchmarked as a latency variant.

### D4. Generated wrapper implementing `LevelEntry`; no reflection on composables
`host-api` defines `interface LevelEntry { val content: @Composable () -> Unit }` and is compiled with the same Compose plugin, so the lambda's JVM shape matches. Each attempt gets a generated file with a reserved-name object (e.g. `__VillageEntry`) whose `content` calls the level's entry composable. Because the wrapper is a separate file, the player's line numbers need no remapping. Wrapper-file diagnostics are rewritten into "contract" messages.
- *Alternative:* splicing the wrapper into the player file would shift line numbers and complicate error mapping.

### D5. Per-attempt child class loader with a class-level allow-list
`PlayerClassLoader(classes, parent = host loader)` defines classes from memory. It delegates to the parent only for allow-listed packages and classes, and it returns no resources. The previous attempt's loader is dropped when the next attempt loads. A weak reference per loader is used to prove collection.

### D6. Sandbox = ASM bytecode verification + instrumentation (not import checks, not SecurityManager)
`SecurityManager` is deprecated for removal, and import checks are bypassed by fully qualified names. After compiling, ASM scans all type, field, method, `invokedynamic` and handle references against `SandboxPolicy`. The policy is package allow-lists with member-level exceptions, e.g. `java.lang.System` allows only `nanoTime`, `currentTimeMillis` and `arraycopy`, and `java.lang.Class` allows only harmless members. Violations become SANDBOX diagnostics at the source line from the line number table.

The same pass instruments the code:
- `Guard.tick()` at each method entry and before each backward jump. These are stack-neutral insertions, so existing stack map frames stay valid and frames are not recomputed.
- `System.out`/`System.err` field reads are rewritten to `village.api.Console` streams.

### D7. Runaway control via cooperative ticks inside host slices
All player execution happens on one player thread inside `Guard.beginSlice()`/`endSlice()`. This covers composition, rendering, input and dispatched effect tasks, because the player dispatcher wraps each task in a slice. Every 1024 ticks, `tick()` checks wall time (default 1 s) and the thread's allocated bytes (`com.sun.management.ThreadMXBean`). A violation, or an external `Guard.abort`, throws `PlayerAbort`. `PlayerAbort` extends `Error` so that `catch (e: Exception)` does not swallow it, and the abort stays sticky until the next slice. A hard watchdog on the host side covers code wedged in uninstrumented library code: it abandons the thread and restarts the engine.
- *Alternative:* a watchdog thread plus `Thread.stop`. `Thread.stop` throws `UnsupportedOperationException` on JDK 20+.

### D8. Render in an offscreen `ImageComposeScene` on a dedicated thread, not a nested window or panel
Composing player content directly in the game's composition would let one exception kill the game's Recomposer, and Compose forbids `try`/`catch` around composable calls. A separate `ComposePanel` would run on the AWT EDT, shared with the game.

`ImageComposeScene` instead gives the host synchronous `render()` and `sendPointerEvent()` calls that it can wrap in `try`/`catch` on its own thread. The scene's coroutine context is the player dispatcher plus a `CoroutineExceptionHandler` that captures effect failures. Frames are converted to `ImageBitmap` and shown in the game window. The same scene provides `semanticsOwners`, which robot customers will use, and pixels, which sketch diffing will use.
- *Trade-off:* each frame costs a CPU raster render and a pixel copy. This is acceptable for small phone-sized previews and is measured in the spike.

### D9. Recomposition counting through `Composer.setTracer`
The Compose plugin already emits `traceEventStart` calls with source information when tracing is active. The preview engine installs a global tracer that returns `isTraceInProgress = true` and counts events from the player thread whose info string refers to the player's file. No custom compiler plugin is needed. A custom plugin, or extra ASM instrumentation of `$composer` methods, is kept as a fallback if the info strings prove insufficient.

### D10. Out-of-process comparison reuses the same pipeline
`CompileServer` is a second `main` on the same classpath. It speaks a length-prefixed protocol over stdin/stdout and returns diagnostics plus a PNG frame. Because it runs the identical pipeline, measured differences isolate the process-boundary cost: spawn time, IPC, PNG encoding, and a second JVM's RSS.

### D11. Packaging via the Compose Gradle plugin's jpackage pipeline
`nativeDistributions` builds a DMG with the runtime modules the compiler needs: `java.management`, `jdk.unsupported`, `java.instrument` and `jdk.management`. These are confirmed with `suggestRuntimeModules`. The compiler reads JDK classes from the bundled runtime's `jrt` image, so it uses the bundled runtime as its JDK home. A `--selftest` flag validates compile and render inside the bundle.

## Risks / Trade-offs

- **[Out-of-memory]** In-process code can exhaust the shared heap. → Mitigate with per-slice allocation budgets checked in `tick()`. The residual risk is documented, and out-of-process is the escape hatch if it shows up in practice.
- **[Wedged thread in uninstrumented code]** e.g. a huge stdlib sort, or a deadlock. → The hard watchdog abandons the thread and restarts the engine. CPU may leak until exit, which is documented.
- **[Sandbox completeness]** The allow-list is hand-written and may miss dangerous APIs reachable through allowed packages (e.g. Compose platform locals). → Two layers (bytecode + class loader), tests per attack class, and an explicit "needs audit" item in the report. A stricter allow-list (deny `androidx.compose.ui.platform` except vetted locals) is an option.
- **[Compiler internals churn]** `K2JVMCompilerArguments` and keepalive behaviour can change between Kotlin versions. → The version is pinned in one catalog and the plugin version is matched exactly. Re-running the spike harness is the upgrade gate. `kotlin-build-tools-api` is a possible migration path.
- **[Version skew]** The embedded plugin must match the embedded compiler, and the game's build plugin must match too (for `host-api`'s lambda shape). → A single `kotlin` version entry, and a harness test that casts the wrapper to `LevelEntry`.
- **[Plugin class loader per compile]** Passing `-Xplugin` may create a class loader or jar handle per compilation. → Measured in the 150-cycle test (file descriptors, class counts). Fallback: register the plugin programmatically.
- **[Keepalive memory]** The kept-alive compiler environment holds a fixed footprint of tens of MB. → This is accepted as the price of warm latency, and it is measured.
- **[Bundle size]** About +61 MB of jars. → The DMG delta is measured. Compose itself is already about 38 MB, so the total stays within normal desktop-app sizes.
- **[Render cost]** A CPU raster render per frame. → Render only on invalidation. The frame latency is measured, and a GPU-backed surface can be considered later.
- **[Global tracer]** The tracer is process-wide and also sees the host UI's events. → Filter by player thread and file name. The overhead is measured.

## Migration Plan

This is not applicable for production. The spike is additive and lives in its own modules. If the recommendation is rejected, `compiler` and `preview` are discarded and `host-api` is revisited. If it is accepted, those modules become the player-code pipeline of the real game, and the follow-up change proposes the architecture and the MVP for 5 Main Street levels.

## Open Questions

- Does `ImageComposeScene.render()` surface composition exceptions synchronously, or do some arrive only via the coroutine exception handler? The spike verifies this empirically and handles both.
- What is the exact `traceEventStart` info-string format in plugin 2.4.21 (e.g. `"PlayerScreen (Player.kt:12)"`)? Is it present without `sourceInformation=true`?
- Does `ImageComposeScene` support text input well enough for `TextField` levels, via key events or the semantics `SetText` action? This is only noted in the spike.
- Is warm latency dominated by classpath indexing? This decides whether a trimmed, curated classpath is the default.
- Should material (M2) also be available to players, or only material3?

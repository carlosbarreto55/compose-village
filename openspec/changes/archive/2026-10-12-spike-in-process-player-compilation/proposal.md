## Why

Compose Village's core loop (the player writes Kotlin + Compose code and instantly sees the UI it produces) depends on compiling and running untrusted player code at runtime inside a Compose Multiplatform Desktop game. That is the single biggest technical risk in the project and it decides the whole architecture (in-process vs. out-of-process vs. server vs. interpreter), so it must be settled with a measured prototype before any gameplay is built.

## What Changes

- Add a **technical spike prototype** in the existing `~/Downloads/project/compose-village` Gradle project (scaffold already on disk: `host-api`, `compiler`, `preview`, `app` modules, version catalog) that:
  - embeds the Kotlin K2 compiler and the Compose compiler plugin (version-aligned, Kotlin 2.4.21 / Compose Multiplatform 1.12.1) and compiles a player's `.kt` file in memory, warm, in under ~2 s;
  - invokes the player's UI through a generated wrapper implementing a host-side `LevelEntry` interface (no reflection on composables);
  - loads each attempt in a fresh, allow-listed child `ClassLoader` and renders it in an isolated offscreen `ImageComposeScene` shown inside the game window;
  - verifies and instruments player bytecode (sandbox allow-list, loop/method-entry watchdog ticks, captured `println`);
  - maps compiler and sandbox diagnostics to the player's file with exact line/column;
  - survives runtime exceptions and runaway code in player code without crashing the game;
  - counts recompositions per player composable via the public Compose runtime tracer hook.
- Add a **headless benchmark/report harness** (`./gradlew :app:spike`) that measures cold/warm compile latency, diagnostics accuracy, crash isolation, runaway handling, sandbox rejections, 150-cycle reload memory behaviour and recomposition counting.
- Add an **out-of-process comparison** (helper JVM compile/render server) and a **packaging check** (macOS DMG with the embedded compiler, `--selftest` launch, size delta).
- Produce a **spike report**: exact version matrix, measured comparison, recommendation with trade-offs, and a paper evaluation of server-side compilation, an interpreted Compose-like DSL, and blocks/fill-in-the-blanks.
- Out of scope: gameplay, villagers, levels, scoring, sketch matching, robot customers (only their feasibility is noted), and the architecture/MVP plan, which is gated on the spike's recommendation being accepted.

## Capabilities

### New Capabilities
- `player-code-compilation`: Embedded Kotlin + Compose compilation of player source at runtime: version-aligned toolchain, warm compiler environment, generated `LevelEntry` wrapper, in-memory class output, and diagnostics mapped to the player's file.
- `player-code-sandbox`: Protection of the game from player code: bytecode-level allow-list verification with line-attributed violations, class-loader allow-list, and a runaway-code guard (time and allocation budgets, sticky abort).
- `player-preview-runtime`: Loading and running a compiled attempt: per-attempt class loader, isolated offscreen Compose scene on a dedicated player thread, crash isolation for composition/event/effect failures, reload without leaks, input forwarding, and recomposition counting.
- `spike-evaluation`: The measurement harness, out-of-process comparison, packaging check, alternative-approach evaluation, and the final report with recommendation.

### Modified Capabilities
<!-- None: no existing specs in openspec/specs/. -->

## Impact

- **Code**: `host-api` (`LevelEntry`, `Guard`, `Console`), `compiler` (`PlayerCompiler`, `PlayerClasspath`, `SandboxPolicy`, `BytecodeGuard`, `PlayerClassLoader`, `PlayerPipeline`), `preview` (`PreviewEngine`), `app` (spike window, `spike` task, `CompileServer`, `--selftest`). Some scaffold files already exist and are unverified.
- **Dependencies**: `kotlin-compiler-embeddable` 2.4.21, `kotlin-compose-compiler-plugin-embeddable` 2.4.21, ASM 9.11 (`asm`, `asm-tree`, `asm-util`), Compose Multiplatform 1.12.1, material3 1.9.0. The Kotlin Gradle plugin that builds the game becomes pinned to the embedded compiler version.
- **Distribution**: about 61 MB of extra uncompressed jars (the compiler is 56.5 MB of it), on top of about 38 MB for Compose and Skiko. The bundled Java runtime must include the modules the compiler needs (e.g. `jdk.unsupported`, `java.management`).
- **Platform**: macOS arm64 first (JDK 17); Windows and Linux are not verified by this spike.

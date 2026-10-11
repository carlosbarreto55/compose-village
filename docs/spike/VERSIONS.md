# Version matrix

Mirrors the matrix in `openspec/changes/spike-in-process-player-compilation/design.md`.
The source of truth is `gradle/libs.versions.toml`. If the two disagree, the catalog wins and this file is stale.

| Component | Artifact | Version |
|---|---|---|
| Embedded compiler | `org.jetbrains.kotlin:kotlin-compiler-embeddable` | 2.4.21 |
| Embedded Compose plugin | `org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable` | 2.4.21 |
| Game build plugins | `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.plugin.compose` | 2.4.21 |
| Compose Multiplatform | `org.jetbrains.compose` (runtime/ui/foundation desktop) | 1.12.1 |
| Material3 | `org.jetbrains.compose.material3:material3` | 1.9.0 (the version CMP 1.12.1 maps `compose.material3` to) |
| Skiko | `org.jetbrains.skiko:skiko-awt-runtime-macos-arm64` | 0.150.1 |
| ASM | `org.ow2.asm:asm`, `asm-tree`, `asm-util` (`CheckClassAdapter`) | 9.11 |
| JVM | JDK (toolchain + bundled runtime) | 17 |

## Rules

- The embedded compiler, the embedded Compose plugin and the Kotlin Gradle plugins all use the single `kotlin` entry in the catalog. The Compose plugin is compiled against compiler internals, so it is not binary compatible across Kotlin versions.
- `host-api` is built with the same Compose plugin, so `LevelEntry.content` has the same JVM shape (`Function2<Composer, Int, Unit>`) as the generated wrapper's override.
- Upgrading any row means re-running `./gradlew :app:spike` before the upgrade is accepted.

## Build environment

| Item | Version |
|---|---|
| Gradle | 9.4.1 (wrapper) |
| Gradle daemon JVM | 17 (`gradle/gradle-daemon-jvm.properties`, needs a local JDK 17) |
| `kotlinx-coroutines` (catalog, Swing dispatcher for the app) | 1.10.2 |
| Development JDKs | Microsoft OpenJDK 17.0.19, JBR 17.0.14 (macOS arm64) |

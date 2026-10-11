package village.compiler

import java.io.File

/**
 * Picks the jars the player's code is compiled against. The game's own runtime classpath is the
 * source of truth: compiling against exactly the jars the host is running guarantees the player's
 * bytecode links against the Compose runtime instance that renders it.
 */
object PlayerClasspath {
    /**
     * Artifact names of jars that belong to the game's tooling, never to the player's world.
     * Matched against the jar's file name (`name.jar` or `name-<version or hash>.jar`), so this
     * works both for Gradle's cache layout and for a jpackage bundle where all jars sit flat in
     * one directory, possibly renamed with a hash suffix. Prefix matching is deliberately broad
     * (`asm-` also covers `asm-tree-`); no player-facing dependency currently starts with these.
     */
    private val excludedArtifacts = listOf(
        "kotlin-compiler-embeddable", "kotlin-compose-compiler-plugin-embeddable", "kotlin-daemon-embeddable",
        "kotlin-build-tools-api", "kotlin-script-runtime", "kotlin-reflect", "asm", "asm-tree",
        "jbr-api", "jspecify", "kotlinx-coroutines-swing",
        // The game's own modules (host-api is deliberately NOT here: player code links against it).
        "compiler", "preview", "app",
    )

    /** Class/resource directories of the game's own modules when running from a Gradle build. */
    private val excludedModuleDirs = listOf("/compiler/build/", "/preview/build/", "/app/build/")

    fun fromHost(hostClasspath: String = System.getProperty("java.class.path")): List<File> =
        hostClasspath.split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map(::File)
            .filter { f -> f.exists() && !isTooling(f) }
            .distinct()

    private fun isTooling(f: File): Boolean {
        if (f.isDirectory) {
            val path = f.absolutePath.replace('\\', '/') + "/"
            return excludedModuleDirs.any { path.contains(it) }
        }
        val name = f.name
        return name.endsWith(".jar") && excludedArtifacts.any { name == "$it.jar" || name.startsWith("$it-") }
    }

    /**
     * Artifacts a player's Compose code needs at compile time: the stdlib, coroutines, the Compose
     * runtime/ui/foundation/animation/material3 stack and `host-api`. A latency variant of
     * [fromHost] (design D3): fewer jars to index, same binaries, so the result still links
     * against the host's runtime.
     */
    private val trimmedArtifacts = listOf(
        "kotlin-stdlib", "kotlinx-coroutines-core-jvm", "host-api",
        "runtime-desktop", "runtime-saveable-desktop", "runtime-annotation-jvm",
        "ui-desktop", "ui-graphics-desktop", "ui-geometry-desktop", "ui-text-desktop", "ui-unit-desktop", "ui-util-desktop",
        "foundation-desktop", "foundation-layout-desktop", "animation-desktop", "animation-core-desktop",
        "material3-desktop", "material-ripple-desktop", "collection-jvm",
    )

    /** The Compose-only subset of [fromHost]; see [trimmedArtifacts]. */
    fun trimmed(hostClasspath: String = System.getProperty("java.class.path")): List<File> =
        fromHost(hostClasspath).filter { f -> trimmedArtifacts.any { f.name == "$it.jar" || f.name.startsWith("$it-") } }

    /** The Compose compiler plugin jar, located from the class the compiler will load from it. */
    fun composePluginJar(): File = jarOf("androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar")

    fun jarOf(className: String): File =
        File(Class.forName(className).protectionDomain.codeSource.location.toURI())
}

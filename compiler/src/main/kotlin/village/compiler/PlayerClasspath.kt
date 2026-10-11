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

    /** The Compose compiler plugin jar, located from the class the compiler will load from it. */
    fun composePluginJar(): File = jarOf("androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar")

    fun jarOf(className: String): File =
        File(Class.forName(className).protectionDomain.codeSource.location.toURI())
}

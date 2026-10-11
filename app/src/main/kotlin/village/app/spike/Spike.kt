package village.app.spike

import village.compiler.PlayerClasspath

// Placeholder until the harness (tasks 6.x): prints the player compile classpath derived from the host.
fun main() {
    PlayerClasspath.fromHost().forEach { println(it.name) }
    println("compose plugin: ${PlayerClasspath.composePluginJar().name}")
}

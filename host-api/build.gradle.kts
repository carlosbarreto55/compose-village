// Types shared by the game (parent ClassLoader) and the player's code (child ClassLoader).
// Must be compiled with the Compose plugin so `@Composable () -> Unit` has the same JVM shape
// on both sides.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
}

kotlin { jvmToolchain(17) }

dependencies {
    api(libs.compose.runtime)
    testImplementation(libs.kotlin.test)
}

tasks.test { useJUnitPlatform() }

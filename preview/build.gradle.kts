// Hosts a loaded player entry in an isolated, offscreen ComposeScene on a dedicated thread.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin { jvmToolchain(17) }

dependencies {
    api(project(":host-api"))
    api(compose.desktop.currentOs)
    api(libs.compose.material3)
}

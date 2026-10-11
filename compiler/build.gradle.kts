// Player-code pipeline: embedded kotlinc + Compose plugin, bytecode guard, class loading.
// Deliberately has NO Compose UI dependency (host-api brings only the Compose runtime).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(17) }

dependencies {
    api(project(":host-api"))
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.kotlin.compose.plugin.embeddable)
    implementation(libs.asm)
    implementation(libs.asm.tree)
}

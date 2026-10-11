// Player-code pipeline: embedded kotlinc + Compose plugin, bytecode guard, class loading.
// Deliberately has NO Compose UI dependency.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.kotlin.compose.plugin.embeddable)
    implementation(libs.asm)
    implementation(libs.asm.tree)
}

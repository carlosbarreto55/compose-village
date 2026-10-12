// The level domain (design D1): level data model, strict JSON loader, acceptance evaluator and the
// bundled level data. No Compose UI: it only uses preview's plain-data SemanticsSnapshot.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(17) }

dependencies {
    api(project(":preview"))
    implementation(libs.serialization.json)
    testImplementation(project(":compiler")) // self-checking levels run the real pipeline (design D9)
    testImplementation(libs.kotlin.test)
}

tasks.test { useJUnitPlatform() }

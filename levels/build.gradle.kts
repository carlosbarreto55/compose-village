// The level domain (design D1): level data model, strict JSON loader, acceptance evaluator and the
// bundled level data. No Compose UI: it only uses preview's plain-data SemanticsSnapshot.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(17) }

// Renders each level's reference solution to target.png at build time (add-level-screen design D1).
// Its own source set: it needs main's classes but not main's resources, which depend on its output.
val targets: SourceSet by sourceSets.creating
configurations[targets.implementationConfigurationName].extendsFrom(configurations.implementation.get())

dependencies {
    api(project(":preview"))
    implementation(libs.serialization.json)
    testImplementation(project(":compiler")) // self-checking levels run the real pipeline (design D9)
    testImplementation(libs.kotlin.test)
    targets.implementationConfigurationName(files(sourceSets.main.get().output.classesDirs))
    targets.implementationConfigurationName(project(":compiler"))
}

tasks.test { useJUnitPlatform() }

val targetsDir = layout.buildDirectory.dir("generated/level-targets")
val renderTargets by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Renders every level's Solution.kt to levels/<id>/target.png."
    val levelsDir = file("src/main/resources/levels")
    val solutionsDir = file("src/test/resources/levels")
    inputs.dir(levelsDir).withPropertyName("levels")
    inputs.dir(solutionsDir).withPropertyName("solutions")
    outputs.dir(targetsDir).withPropertyName("targets")
    classpath = targets.runtimeClasspath
    mainClass = "village.levels.RenderTargetsKt"
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }
    jvmArgs("-Xmx1g", "-Djava.awt.headless=true")
    args(levelsDir.absolutePath, solutionsDir.absolutePath, targetsDir.get().dir("levels").asFile.absolutePath)
}

tasks.processResources { from(renderTargets) }

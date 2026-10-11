import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":host-api"))
    implementation(project(":compiler"))
    implementation(project(":preview"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "village.app.MainKt"
        jvmArgs += listOf("-Xmx1g")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "ComposeVillage"
            packageVersion = "1.0.0"
            modules("java.management", "jdk.unsupported", "java.instrument", "jdk.management")
        }
    }
}

// Headless spike harness, writes docs/spike/report.md: ./gradlew :app:spike [-Pargs="--only latency,memory --runs 30 --cycles 150"]
tasks.register<JavaExec>("spike") {
    group = "spike"
    mainClass = "village.app.spike.SpikeKt"
    classpath = sourceSets["main"].runtimeClasspath
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }
    jvmArgs("-Xmx1g", "-Djava.awt.headless=true", "-XX:+UseG1GC")
    args((project.findProperty("args") as String?)?.split(" ")?.filter { it.isNotBlank() } ?: emptyList<String>())
    workingDir = rootProject.projectDir
}

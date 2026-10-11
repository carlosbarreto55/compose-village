package village.compiler

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

data class SourceFile(val name: String, val text: String)

/** A compiler message, still in terms of the file it was reported against. */
data class RawDiagnostic(
    val severity: Severity,
    val fileName: String?,
    val line: Int,
    val column: Int,
    val endLine: Int,
    val endColumn: Int,
    val message: String,
)

class CompileOutput(
    val success: Boolean,
    /** JVM internal name -> class bytes. Empty on failure. */
    val classes: Map<String, ByteArray>,
    val diagnostics: List<RawDiagnostic>,
    val millis: Long,
)

/**
 * Embedded Kotlin compiler (K2) + Compose compiler plugin, running in the game's own JVM.
 *
 * Sources are written to a scratch directory per attempt and the output directory is read back
 * into memory and deleted, so nothing accumulates on disk.
 *
 * Not thread-safe: the game serializes compilations (the player can only press "Run" once).
 */
class PlayerCompiler(
    private val classpath: List<File> = PlayerClasspath.fromHost(),
    private val composePluginJar: File = PlayerClasspath.composePluginJar(),
    private val scratchRoot: File = Files.createTempDirectory("village-compile").toFile(),
    keepEnvironmentAlive: Boolean = true,
    private val composeOptions: Map<String, String> = mapOf("sourceInformation" to "true"),
    /**
     * Where the compiler reads JDK classes from (its `jrt` image): the runtime the game runs on,
     * which inside a jpackage bundle is the bundled runtime. Explicit so that a `JAVA_HOME` or a
     * system JDK can never change what player code compiles against.
     */
    val jdkHome: File = File(System.getProperty("java.home")),
) {
    private val attemptCounter = AtomicInteger()

    init {
        // Keeps the compiler's IntelliJ "application environment" (VFS, jar index, extension
        // points) alive between compilations instead of rebuilding it each time. This is what the
        // Kotlin daemon does and is the main warm-compile win.
        System.setProperty("kotlin.environment.keepalive", keepEnvironmentAlive.toString())
        // Let the compiler's bundled IntelliJ core use plain NIO2 file APIs.
        System.setProperty("idea.io.use.nio2", "true")
        scratchRoot.deleteOnExit()
    }

    fun compile(sources: List<SourceFile>): CompileOutput {
        val start = System.nanoTime()
        val dir = File(scratchRoot, "a${attemptCounter.incrementAndGet()}")
        val srcDir = File(dir, "src").apply { mkdirs() }
        val outDir = File(dir, "out").apply { mkdirs() }
        try {
            val paths = sources.map { src -> File(srcDir, src.name).apply { writeText(src.text) } }
            val collector = Collector()
            val args = K2JVMCompilerArguments().apply {
                freeArgs = paths.map { it.absolutePath }
                destination = outDir.absolutePath
                classpath = this@PlayerCompiler.classpath.joinToString(File.pathSeparator) { it.absolutePath }
                noStdlib = true // the stdlib is on `classpath`, taken from the host
                noReflect = true
                jvmTarget = "17"
                jdkHome = this@PlayerCompiler.jdkHome.absolutePath
                moduleName = "player"
                pluginClasspaths = arrayOf(composePluginJar.absolutePath)
                pluginOptions = composeOptions.map { (k, v) ->
                    "plugin:androidx.compose.compiler.plugins.kotlin:$k=$v"
                }.toTypedArray()
            }
            val exit = K2JVMCompiler().exec(collector, Services.EMPTY, args)
            if (exit != ExitCode.OK && !collector.hasErrors()) {
                // Never fail silently: e.g. INTERNAL_ERROR without a reported message.
                collector.report(CompilerMessageSeverity.ERROR, "Compiler exited with $exit", null)
            }
            val ok = exit == ExitCode.OK && !collector.hasErrors()
            val classes = if (ok) readClasses(outDir) else emptyMap()
            return CompileOutput(ok, classes, collector.diagnostics, (System.nanoTime() - start) / 1_000_000)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun readClasses(outDir: File): Map<String, ByteArray> =
        outDir.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .associate { it.relativeTo(outDir).path.removeSuffix(".class").replace(File.separatorChar, '/') to it.readBytes() }

    private class Collector : MessageCollector {
        val diagnostics = mutableListOf<RawDiagnostic>()
        private var errors = false

        override fun clear() {
            diagnostics.clear()
            errors = false
        }
        override fun hasErrors() = errors

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            val sev = when (severity) {
                CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> Severity.ERROR
                CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> Severity.WARNING
                else -> return // LOGGING / OUTPUT / INFO chatter
            }
            if (sev == Severity.ERROR) errors = true
            diagnostics += RawDiagnostic(
                severity = sev,
                fileName = location?.path?.let { File(it).name },
                line = location?.line ?: 0,
                column = location?.column ?: 0,
                endLine = location?.lineEnd?.takeIf { it > 0 } ?: location?.line ?: 0,
                endColumn = location?.columnEnd?.takeIf { it > 0 } ?: location?.column ?: 0,
                message = message,
            )
        }
    }
}

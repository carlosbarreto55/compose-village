package village.compiler

/**
 * What player bytecode may reference (design D6). Names are JVM internal names (`java/io/File`).
 *
 * Type rules are matched by the longest matching key:
 * - `a/b/` is a package and everything below it,
 * - `a/b/C` is that class and its nested classes (`a/b/C$D`),
 * - `a/b/C*` is every class whose name starts with `a/b/C`.
 *
 * Anything no rule covers is rejected. A denied rule carries the reason shown to the player.
 * Member rules then narrow allowed types down to harmless fields and methods.
 *
 * This list is hand-written and NOT security-audited; the spike only covers the main attack
 * classes (files, network, reflection, processes, threads, windows/desktop, sandbox internals).
 */
object SandboxPolicy {
    const val FILES = "files are not available in the village"
    const val NETWORK = "the village has no network access"
    const val REFLECTION = "reflection is not available in the village"
    const val PROCESS = "controlling the game's process is not allowed"
    const val THREADS = "your code runs on the village's own thread; threads, blocking and other dispatchers are not available"
    const val DESKTOP = "windows, the clipboard, links and resource files belong to the game, not to your screen"
    const val INTERNALS = "this is part of the game's sandbox"
    const val NOT_AVAILABLE = "this is not available in the village"
    const val RESERVED = "this package is reserved for the game and its libraries"

    private val typeRules: Map<String, String?> = buildMap {
        fun allow(vararg keys: String) = keys.forEach { put(it, null) }
        fun deny(reason: String, vararg keys: String) = keys.forEach { put(it, reason) }

        // Kotlin standard library, minus files, threads, process control and full reflection.
        allow("kotlin/")
        deny(FILES, "kotlin/io/")
        allow("kotlin/io/ConsoleKt", "kotlin/io/CloseableKt", "kotlin/jdk7/AutoCloseableKt", "kotlin/io/encoding/")
        deny(PROCESS, "kotlin/system/ProcessKt")
        deny(THREADS, "kotlin/concurrent/")
        allow("kotlin/concurrent/atomics/")
        deny(REFLECTION, "kotlin/reflect/full/", "kotlin/reflect/jvm/")

        // Coroutines: structured concurrency on the scene's own dispatcher only.
        allow("kotlinx/coroutines/")
        deny(THREADS, "kotlinx/coroutines/Dispatchers", "kotlinx/coroutines/GlobalScope",
            "kotlinx/coroutines/ThreadPoolDispatcherKt", "kotlinx/coroutines/ExecutorsKt",
            "kotlinx/coroutines/ExecutorCoroutineDispatcher*", "kotlinx/coroutines/ThreadContextElementKt",
            "kotlinx/coroutines/scheduling/", "kotlinx/coroutines/swing/", "kotlinx/coroutines/javafx/",
            "kotlinx/coroutines/debug/")

        // Compose, minus windows, AWT interop, platform font/resource loading.
        allow("androidx/compose/", "androidx/collection/")
        deny(DESKTOP, "androidx/compose/ui/window/", "androidx/compose/ui/awt/", "androidx/compose/ui/res/",
            "androidx/compose/ui/text/platform/",
            "androidx/compose/ui/platform/ClipboardManager", "androidx/compose/ui/platform/Clipboard",
            "androidx/compose/ui/platform/NativeClipboard", "androidx/compose/ui/platform/AwtClipboard*",
            "androidx/compose/ui/platform/UriHandler", "androidx/compose/ui/platform/PlatformUriHandler*")
        allow("androidx/compose/ui/window/Popup*")

        // The game's API, minus the guard itself.
        allow("village/api/")
        deny(INTERNALS, "village/api/Guard", "village/api/PlayerAbort")

        // A curated java.lang.
        allow(
            "java/lang/Object", "java/lang/String", "java/lang/StringBuilder", "java/lang/CharSequence",
            "java/lang/Comparable", "java/lang/Iterable", "java/lang/Number", "java/lang/Integer", "java/lang/Long",
            "java/lang/Short", "java/lang/Byte", "java/lang/Double", "java/lang/Float", "java/lang/Boolean",
            "java/lang/Character", "java/lang/Math", "java/lang/StrictMath", "java/lang/Enum", "java/lang/Void",
            "java/lang/Runnable", "java/lang/AutoCloseable", "java/lang/Cloneable", "java/lang/Appendable",
            "java/lang/Record", "java/lang/Class", "java/lang/System",
            "java/lang/Throwable", "java/lang/Exception", "java/lang/RuntimeException", "java/lang/Error",
            "java/lang/IllegalArgumentException", "java/lang/IllegalStateException",
            "java/lang/IndexOutOfBoundsException", "java/lang/ArrayIndexOutOfBoundsException",
            "java/lang/StringIndexOutOfBoundsException", "java/lang/NullPointerException",
            "java/lang/ArithmeticException", "java/lang/UnsupportedOperationException",
            "java/lang/ClassCastException", "java/lang/NumberFormatException", "java/lang/NoSuchFieldError",
            "java/lang/AssertionError", "java/lang/StackOverflowError", "java/lang/CloneNotSupportedException",
            "java/lang/InterruptedException",
        )
        deny(REFLECTION, "java/lang/reflect/", "java/lang/invoke/", "java/lang/ClassLoader", "java/lang/Module*")
        deny(PROCESS, "java/lang/Runtime", "java/lang/Process*", "java/lang/Shutdown", "java/lang/SecurityManager")
        deny(THREADS, "java/lang/Thread*", "java/lang/InheritableThreadLocal", "java/lang/VirtualThread")

        // java.util collections and functional types, minus concurrency, services and archives.
        allow("java/util/")
        deny(THREADS, "java/util/concurrent/", "java/util/Timer*")
        allow("java/util/concurrent/atomic/", "java/util/concurrent/CancellationException",
            "java/util/concurrent/ConcurrentHashMap", "java/util/concurrent/ConcurrentLinkedQueue",
            "java/util/concurrent/CopyOnWriteArrayList", "java/util/concurrent/TimeUnit")
        deny(FILES, "java/util/jar/", "java/util/zip/", "java/util/prefs/", "java/util/logging/",
            "java/util/ResourceBundle*", "java/util/Scanner")
        deny(REFLECTION, "java/util/ServiceLoader*", "java/util/spi/")

        allow("java/math/", "java/time/")
        deny(NOT_AVAILABLE, "java/time/zone/ZoneRulesProvider")

        // Explicit reasons for the most common forbidden areas (everything else: NOT_AVAILABLE).
        deny(FILES, "java/io/", "java/nio/", "kotlin/io/path/")
        allow("java/io/Serializable", "java/io/PrintStream")
        deny(NETWORK, "java/net/", "javax/net/", "java/net/http/")
        deny(DESKTOP, "java/awt/", "javax/swing/", "org/jetbrains/skiko/", "org/jetbrains/compose/resources/")
    }

    /** Members of allowed types. `allowOnly` lists the only usable members; `deny` blocks single ones. */
    private class MemberRule(val allowOnly: Set<String>? = null, val deny: Map<String, String> = emptyMap(), val reason: String = NOT_AVAILABLE)

    private val memberRules: Map<String, MemberRule> = mapOf(
        "java/lang/System" to MemberRule(
            allowOnly = setOf("nanoTime", "currentTimeMillis", "arraycopy", "identityHashCode", "lineSeparator", "out", "err"),
            reason = PROCESS,
        ),
        "java/lang/Class" to MemberRule(
            allowOnly = setOf("getName", "getSimpleName", "isInstance", "cast", "desiredAssertionStatus", "hashCode", "equals", "toString"),
            reason = REFLECTION,
        ),
        // Only reachable through Console.out/err after instrumentation; no new streams to files.
        "java/io/PrintStream" to MemberRule(
            allowOnly = setOf("print", "println", "printf", "format", "append", "flush", "checkError"),
            reason = FILES,
        ),
        "kotlin/io/ConsoleKt" to MemberRule(allowOnly = setOf("print", "println"), reason = "the village has no keyboard input; use state instead"),
        "kotlinx/coroutines/BuildersKt" to MemberRule(deny = mapOf("runBlocking" to THREADS)),
        // CoroutineScope()/MainScope() would run player code on Dispatchers.Default / the game's UI thread.
        "kotlinx/coroutines/CoroutineScopeKt" to MemberRule(deny = mapOf("CoroutineScope" to THREADS, "MainScope" to THREADS)),
        "androidx/compose/ui/platform/CompositionLocalsKt" to MemberRule(
            deny = listOf("getLocalClipboardManager", "getLocalClipboard", "getLocalUriHandler").associateWith { DESKTOP },
        ),
        // Process-wide hooks: would see the game's own UI and run player code on game threads.
        "androidx/compose/runtime/Composer\$Companion" to MemberRule(deny = mapOf("setTracer" to INTERNALS)),
        "androidx/compose/runtime/snapshots/Snapshot\$Companion" to MemberRule(
            deny = listOf("registerGlobalWriteObserver", "registerApplyObserver").associateWith { THREADS },
        ),
    )

    /**
     * Members denied whatever the owner is, because the call names the receiver's static type
     * (`myObject.wait()` has the player's class as owner but runs `Object.wait`).
     */
    private val anyOwnerDeny: Map<String, String> = mapOf(
        "wait" to THREADS, "notify" to THREADS, "notifyAll" to THREADS,
        "printStackTrace" to "use println(e) to see an error in the console",
    )

    /** `parallelStream()`, `Arrays.parallelSort()`... would run player lambdas on a shared thread pool. */
    private fun isParallel(owner: String, name: String) = owner.startsWith("java/util/") && name.startsWith("parallel")

    /** Bootstrap methods the Kotlin compiler emits for lambdas, callable references and string templates. */
    private val bootstraps = setOf(
        "java/lang/invoke/LambdaMetafactory.metafactory", "java/lang/invoke/LambdaMetafactory.altMetafactory",
        "java/lang/invoke/StringConcatFactory.makeConcatWithConstants", "java/lang/invoke/StringConcatFactory.makeConcat",
    )

    /** Packages a player's own classes may not live in: they would shadow the game or its libraries. */
    private val reservedPackages = listOf("village/", "kotlin/", "kotlinx/", "androidx/", "java/", "javax/", "jdk/", "sun/", "com/sun/", "org/jetbrains/")

    /**
     * Classes the JVM itself resolves through the player's class loader while linking allowed
     * `invokedynamic` call sites, plus the guard the instrumentation calls. Player bytecode can't
     * name them (the bytecode check rejects that); only the loader lets them through.
     */
    private val linkageOnly = setOf(
        "java/lang/invoke/LambdaMetafactory", "java/lang/invoke/StringConcatFactory", "java/lang/invoke/MethodHandles\$Lookup",
        "java/lang/invoke/MethodHandles", "java/lang/invoke/MethodHandle", "java/lang/invoke/MethodType", "java/lang/invoke/CallSite",
        "village/api/Guard", "village/api/PlayerAbort",
    )

    /** Why [type] may not be referenced, or null if it may. Array types are checked by element type. */
    fun checkType(type: String, own: Set<String>): String? {
        val element = type.trimStart('[')
        if (element.length == 1) return null // primitive array
        val name = if (element.startsWith("L") && element.endsWith(";")) element.substring(1, element.length - 1) else element
        if (name in own) return null
        var best: String? = null
        var reason: String? = NOT_AVAILABLE
        for ((key, r) in typeRules) {
            if (matches(key, name) && (best == null || key.length > best.length)) { best = key; reason = r }
        }
        return reason
    }

    /** Why member [name] of [owner] may not be used, or null if it may. Assumes [owner] passed [checkType]. */
    fun checkMember(owner: String, memberName: String, own: Set<String>): String? {
        val name = memberName.removeSuffix("\$default") // Kotlin's bridge for default arguments
        anyOwnerDeny[name]?.let { return it }
        if (isParallel(owner, name)) return THREADS
        if (owner in own) return null
        val rule = memberRules[owner] ?: return null
        rule.deny[name]?.let { return it }
        return if (rule.allowOnly != null && name !in rule.allowOnly) rule.reason else null
    }

    fun checkBootstrap(owner: String, name: String): String? = if ("$owner.$name" in bootstraps) null else REFLECTION

    /** Why the player may not declare a class named [name], or null if they may. */
    fun checkOwnClassName(name: String): String? = if (reservedPackages.any { name.startsWith(it) }) RESERVED else null

    /** Class-level check for the per-attempt class loader (no member rules at this level). */
    fun isClassAllowed(name: String): Boolean = name in linkageOnly || checkType(name, emptySet()) == null

    private fun matches(key: String, name: String) = when {
        key.endsWith("/") -> name.startsWith(key)
        key.endsWith("*") -> name.startsWith(key.dropLast(1))
        else -> name == key || name.startsWith("$key$")
    }
}

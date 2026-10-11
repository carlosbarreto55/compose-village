package village.compiler

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.util.CheckClassAdapter
import java.io.PrintWriter
import java.io.StringWriter

/**
 * The bytecode half of the sandbox (design D6). Runs on the compiled classes of an attempt before
 * they are loaded:
 *
 * 1. [verify] checks every type, field/method owner, `invokedynamic` bootstrap and handle argument,
 *    `ldc` constant and exception type against [SandboxPolicy]. Violations become SANDBOX
 *    diagnostics at the player-source line of the offending instruction.
 * 2. [instrument] inserts `Guard.tick()` at every method entry and before every backward jump, and
 *    redirects `System.out`/`System.err` reads to `village.api.Console`. Both edits are
 *    stack-neutral and add no jump targets, so the existing stack map frames stay valid and are
 *    written back as-is (no frame recomputation, which would need to load classes).
 */
class BytecodeGuard(private val policy: SandboxPolicy = SandboxPolicy) {

    /**
     * @param playerFileLines line count of the player's file. Lines beyond it come from inlined
     *   library code and are attributed to the last in-file line before them (the call site).
     */
    fun verify(classes: Map<String, ByteArray>, playerFile: String, playerFileLines: Int): List<Diagnostic> {
        val own = classes.keys
        val nodes = classes.values.map { bytes -> ClassNode().also { ClassReader(bytes).accept(it, ClassReader.SKIP_FRAMES) } }
        val referencedAt = referenceLines(nodes, playerFileLines)
        val found = LinkedHashMap<Pair<Int, String>, Diagnostic>() // one diagnostic per (line, message)
        for (node in nodes) {
            val inPlayerFile = node.sourceFile == playerFile
            fun report(line: Int, target: String, reason: String) {
                val at = if (inPlayerFile) line else 0
                val message = "`$target` is not allowed: $reason."
                found.getOrPut(at to message) { Diagnostic(Severity.ERROR, at, 1, message, source = Diagnostic.Source.SANDBOX) }
            }
            val classLine = referencedAt[node.name]
                ?: node.methods.firstNotNullOfOrNull { m -> m.instructions.firstOrNull { it is LineNumberNode }?.let { (it as LineNumberNode).line } }
                    ?.takeIf { it <= playerFileLines }
                ?: 1

            policy.checkOwnClassName(node.name)?.let { report(classLine, display(node.name), it) }
            (listOfNotNull(node.superName) + node.interfaces).forEach { t ->
                policy.checkType(t, own)?.let { report(classLine, display(t), it) }
            }
            for (method in node.methods) {
                val startLine = referencedAt["${node.name}.${method.name}${method.desc}"] ?: classLine
                verifyMethod(method, own, playerFileLines, startLine, ::report)
            }
        }
        return found.values.sortedBy { it.line }
    }

    /**
     * Compiler-generated code often carries no line numbers before its first instruction: a
     * callable reference like `File::readText` becomes its own class whose metadata methods have
     * none. Such code is attributed to the first in-file line that references it: keys are
     * `owner.name+desc` for methods and `owner` for classes.
     */
    private fun referenceLines(nodes: List<ClassNode>, fileLines: Int): Map<String, Int> {
        val own = nodes.mapTo(HashSet()) { it.name }
        val result = HashMap<String, Int>()
        fun note(owner: String, member: String?, line: Int) {
            if (line == 0 || owner !in own) return
            result.putIfAbsent(owner, line)
            if (member != null) result.putIfAbsent("$owner.$member", line)
        }
        for (node in nodes) for (method in node.methods) {
            var line = 0
            for (insn in method.instructions) {
                when (insn) {
                    is LineNumberNode -> if (insn.line <= fileLines) line = insn.line
                    is MethodInsnNode -> note(insn.owner, insn.name + insn.desc, line)
                    is FieldInsnNode -> note(insn.owner, null, line)
                    is TypeInsnNode -> note(insn.desc, null, line)
                    is InvokeDynamicInsnNode -> insn.bsmArgs.filterIsInstance<Handle>().forEach { note(it.owner, it.name + it.desc, line) }
                }
            }
        }
        return result
    }

    private fun verifyMethod(method: MethodNode, own: Set<String>, fileLines: Int, startLine: Int, report: (Int, String, String) -> Unit) {
        val lineAt = HashMap<LabelNode, Int>()
        var line = startLine
        for (insn in method.instructions) {
            if (insn is LineNumberNode && insn.line <= fileLines) line = insn.line
            if (insn is LabelNode) lineAt[insn] = line
        }
        line = startLine

        fun type(t: String) { policy.checkType(t, own)?.let { report(line, display(t), it) } }
        fun descriptor(desc: String) {
            val types = if (desc.startsWith("(")) Type.getArgumentTypes(desc) + Type.getReturnType(desc) else arrayOf(Type.getType(desc))
            types.filter { it.sort == Type.OBJECT || it.sort == Type.ARRAY }.forEach { type(it.descriptor) }
        }
        fun member(owner: String, name: String, desc: String) {
            val ownerReason = policy.checkType(owner, own)
            if (ownerReason != null) { report(line, display(owner), ownerReason); return }
            policy.checkMember(owner, name, own)?.let { report(line, "${display(owner)}.$name", it) }
            descriptor(desc)
        }
        fun handle(h: Handle) = member(h.owner, h.name, h.desc)
        fun constant(c: Any?) {
            when (c) {
                is Type -> if (c.sort == Type.METHOD) descriptor(c.descriptor) else if (c.sort == Type.OBJECT || c.sort == Type.ARRAY) type(c.descriptor)
                is Handle -> handle(c)
                is ConstantDynamic -> report(line, "constant ${c.name}", SandboxPolicy.REFLECTION)
            }
        }

        for (insn in method.instructions) {
            when (insn) {
                is LineNumberNode -> if (insn.line <= fileLines) line = insn.line
                is TypeInsnNode -> type(insn.desc)
                is MultiANewArrayInsnNode -> type(insn.desc)
                is FieldInsnNode -> member(insn.owner, insn.name, insn.desc)
                is MethodInsnNode -> member(insn.owner, insn.name, insn.desc)
                is LdcInsnNode -> constant(insn.cst)
                is InvokeDynamicInsnNode -> {
                    val bsm = insn.bsm
                    policy.checkBootstrap(bsm.owner, bsm.name)?.let { report(line, "${display(bsm.owner)}.${bsm.name}", it) }
                    insn.bsmArgs.forEach(::constant)
                    descriptor(insn.desc)
                }
            }
        }
        for (block in method.tryCatchBlocks) {
            val t = block.type ?: continue // finally
            line = lineAt[block.handler] ?: startLine
            type(t)
        }
    }

    /**
     * Returns the instrumented classes. Call only on classes that passed [verify].
     *
     * @param hostSourceFile classes compiled from this file (the generated entry wrapper) are left
     *   as they are: they are the game's code, and they are initialized on a host thread when the
     *   attempt loads, outside any slice, where a tick could rethrow a stale sticky abort. Matched
     *   by the SourceFile attribute, which a player's classes cannot change.
     */
    fun instrument(classes: Map<String, ByteArray>, hostSourceFile: String? = null): Map<String, ByteArray> = classes.mapValues { (_, bytes) ->
        val node = ClassNode().also { ClassReader(bytes).accept(it, 0) }
        if (hostSourceFile != null && node.sourceFile == hostSourceFile) return@mapValues bytes
        for (method in node.methods) {
            if (method.instructions.size() == 0) continue // abstract / native
            instrumentMethod(method.instructions)
        }
        ClassWriter(0).also { node.accept(it) }.toByteArray()
    }

    private fun instrumentMethod(code: InsnList) {
        val seen = HashSet<LabelNode>()
        for (insn in code.toArray()) {
            when (insn) {
                is LabelNode -> seen += insn
                is JumpInsnNode -> if (insn.label in seen) code.insertBefore(insn, tick())
                is TableSwitchInsnNode -> if ((insn.labels + insn.dflt).any { it in seen }) code.insertBefore(insn, tick())
                is LookupSwitchInsnNode -> if ((insn.labels + insn.dflt).any { it in seen }) code.insertBefore(insn, tick())
                is FieldInsnNode -> if (insn.opcode == Opcodes.GETSTATIC && insn.owner == "java/lang/System" &&
                    (insn.name == "out" || insn.name == "err")) insn.owner = CONSOLE
            }
        }
        code.insert(tick()) // method entry
    }

    private fun tick(): AbstractInsnNode = MethodInsnNode(Opcodes.INVOKESTATIC, GUARD, "tick", "()V", false)

    /**
     * Data-flow verification of instrumented classes with ASM's [CheckClassAdapter], resolving
     * types through [loader] (which must see the attempt's classes). Slow; for tests and the
     * harness, not for every attempt. Returns problems, or an empty list.
     */
    fun check(classes: Map<String, ByteArray>, loader: ClassLoader): List<String> = classes.mapNotNull { (name, bytes) ->
        val out = StringWriter()
        CheckClassAdapter.verify(ClassReader(bytes), loader, false, PrintWriter(out))
        out.toString().takeIf { it.isNotBlank() }?.let { "$name: $it" }
    }

    private fun display(internalName: String): String {
        val element = internalName.trimStart('[')
        val name = if (element.startsWith("L") && element.endsWith(";")) element.substring(1, element.length - 1) else element
        return name.replace('/', '.').replace('$', '.')
    }

    companion object {
        const val GUARD = "village/api/Guard"
        const val CONSOLE = "village/api/Console"
    }
}

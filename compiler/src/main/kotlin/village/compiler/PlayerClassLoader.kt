package village.compiler

import java.net.URL
import java.util.Collections
import java.util.Enumeration

/**
 * One loader per attempt (design D5): defines the attempt's classes from memory, delegates to the
 * host loader only for classes [SandboxPolicy.isClassAllowed] permits (so Compose and `village.api`
 * types are shared with the game), refuses everything else, and exposes no resources. Dropping
 * the loader (with the attempt's scene and entry) lets its classes unload.
 *
 * Defense in depth: the bytecode check already rejects forbidden references, but this loader
 * holds even if a class slipped past it.
 */
class PlayerClassLoader(
    classes: Map<String, ByteArray>,
    parent: ClassLoader,
) : ClassLoader("player", parent) {
    /** Binary name (`a.b.C$D`) -> bytes. Classes in reserved packages are never defined here. */
    private val own: Map<String, ByteArray> = classes
        .filterKeys { SandboxPolicy.checkOwnClassName(it) == null }
        .mapKeys { (internalName, _) -> internalName.replace('/', '.') }

    override fun loadClass(name: String, resolve: Boolean): Class<*> = synchronized(getClassLoadingLock(name)) {
        val c = findLoadedClass(name) ?: when {
            name in own -> findClass(name)
            SandboxPolicy.isClassAllowed(name.replace('.', '/')) -> parent.loadClass(name)
            else -> throw ClassNotFoundException("Sandbox: $name is not available in the village")
        }
        if (resolve) resolveClass(c)
        c
    }

    override fun findClass(name: String): Class<*> {
        val bytes = own[name] ?: throw ClassNotFoundException(name)
        return defineClass(name, bytes, 0, bytes.size)
    }

    override fun getResource(name: String): URL? = null
    override fun getResources(name: String): Enumeration<URL> = Collections.emptyEnumeration()
}

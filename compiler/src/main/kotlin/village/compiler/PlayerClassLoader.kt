package village.compiler

import java.net.URL
import java.util.Collections
import java.util.Enumeration

/**
 * One loader per attempt (design D5): defines the attempt's classes from memory, delegates
 * everything else to the host loader so Compose and `village.api` types are shared, and exposes
 * no resources. Dropping the loader (with the attempt's scene and entry) lets its classes unload.
 *
 * TODO(task 3.4): delegate to the parent only for the class-level allow-list.
 */
class PlayerClassLoader(
    classes: Map<String, ByteArray>,
    parent: ClassLoader,
) : ClassLoader("player", parent) {
    /** Binary name (`a.b.C$D`) -> bytes. Entries are removed once defined. */
    private val pending = classes.mapKeys { (internalName, _) -> internalName.replace('/', '.') }.toMutableMap()

    override fun findClass(name: String): Class<*> {
        val bytes = synchronized(pending) { pending.remove(name) } ?: throw ClassNotFoundException(name)
        return defineClass(name, bytes, 0, bytes.size)
    }

    override fun getResource(name: String): URL? = null
    override fun getResources(name: String): Enumeration<URL> = Collections.emptyEnumeration()
}

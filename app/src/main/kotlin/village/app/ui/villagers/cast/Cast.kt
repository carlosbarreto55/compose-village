package village.app.ui.villagers.cast

/**
 * Every villager's bust, keyed by villager id (spec: Villager busts). Each bundled level has its own villager
 * with a look here; a new city's characters are new files in this folder.
 */
internal object Cast {
    private val looks: Map<String, VillagerLook> by lazy {
        mapOf("rosa" to Rosa, "tom" to Tom, "marco" to Marco, "lucia" to Lucia, "iara" to Iara)
    }

    /** The villager ids that have a bust. */
    val ids: Set<String> get() = looks.keys

    fun forId(id: String): VillagerLook? = looks[id]
}

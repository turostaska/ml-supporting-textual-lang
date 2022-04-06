package symtab

import symtab.Scope.Serial.serial

class Scope(
    val parent: Scope? = null,
    val children: MutableList<Scope> = mutableListOf(),
    val name: String = if (parent == null) "Global scope" else "Scope ${serial++}",
    private val map: MutableMap<String, Symbol> = hashMapOf(),
): MutableMap<String, Symbol> by map {
    init {
        parent?.children?.add(this)
    }

    override fun get(key: String): Symbol? = map[key] ?: this.parent?.get(key)

    override fun put(key: String, value: Symbol): Symbol? {
        if (map.containsKey(key))
            println("Name $key is already in scope.")

        return map.put(key, value)
    }

    val isGlobal
        get() = (parent == null)

    val isLeaf
        get() = children.isEmpty()

    override fun toString(): String = StringBuilder().let { sb ->
        sb.appendLine("---------------")
        sb.appendLine(name)
        sb.appendLine("---------------")
        map.values.forEach { sb.appendLine((it.toString())) }
        sb.appendLine("---------------")
        sb.appendLine()

        sb.toString()
    }

    object Serial { var serial = 0 }
}

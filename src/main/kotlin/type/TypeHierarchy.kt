package type

import type.TypeNames.ANY
import type.TypeNames.BOOLEAN
import type.TypeNames.INT
import type.TypeNames.NOTHING
import type.TypeNames.NUMBER
import type.TypeNames.STRING
import type.util.asType
import type.util.find

class TypeHierarchy {
    val anyN = Type(ANY, true)
    val any = Type(ANY, false, _parents = mutableSetOf(anyN))
    val nothingN = Type(NOTHING, true, _parents = mutableSetOf(anyN))
    val nothing = Type(NOTHING, false, _parents = mutableSetOf(any, nothingN))

    val root = anyN

    init {
        addType(STRING)
        addType(NUMBER)
        addType(BOOLEAN)
        addType(INT, baseClassNames = setOf(NUMBER))
    }

    fun addType(name: String, baseClassNames: Set<String> = setOf(ANY)) {
        if (baseClassNames.isEmpty()) {
            addType(name, setOf(ANY))
        } else {
            require(baseClassNames.none { "?" in it }) { "The names of the base classes should only contain the non-nullable variants" }

            baseClassNames.map { this.find("$it?") }.forEach { it.insertUnder("$name?".asType()) }
            baseClassNames.map { this.find(it) }.forEach { it.insertUnder(name.asType()) }

            find("$name?").addChild(find(name))
        }
    }

    private fun Type.insertUnder(type: Type) {
        type.addParent(this)

        val nothingChild = if (type.nullable) nothingN else nothing
        type.addChild(nothingChild)

        nothingChild.removeParent(this)
    }
}

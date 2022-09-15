package type

import type.TypeNames.ANY
import type.TypeNames.ANY_PY
import type.TypeNames.BOOLEAN
import type.TypeNames.BOOLEAN_PY
import type.TypeNames.INT
import type.TypeNames.INT_PY
import type.TypeNames.LIST
import type.TypeNames.LIST_PY
import type.TypeNames.NOTHING
import type.TypeNames.NUMBER
import type.TypeNames.STRING
import type.TypeNames.STRING_PY
import type.util.asType
import type.util.find

class TypeHierarchy {
    val anyN = Type(ANY, true, pythonName = ANY_PY)
    val any = Type(ANY, false, _parents = mutableSetOf(anyN), pythonName = ANY_PY)
    val nothingN = Type(NOTHING, true, _parents = mutableSetOf(anyN))
    val nothing = Type(NOTHING, false, _parents = mutableSetOf(any, nothingN))

    val root = anyN

    init {
        addType(STRING, pythonName = STRING_PY)
        addType(NUMBER)
        addType(BOOLEAN, pythonName = BOOLEAN_PY)
        addType(INT, baseClassNames = setOf(NUMBER), pythonName = INT_PY)
        addType(LIST, pythonName = LIST_PY)
    }

    fun addType(
        name: String,
        pythonName: String = name,
        baseClassNames: Set<String> = setOf(ANY),
    ) {
        if (baseClassNames.isEmpty()) {
            addType(name, pythonName, setOf(ANY))
        } else {
            require(baseClassNames.none { "?" in it }) { "The names of the base classes should only contain the non-nullable variants" }

            baseClassNames.map { this.find("$it?") }.forEach { it.insertUnder("$name?".asType(pythonName = pythonName)) }
            baseClassNames.map { this.find(it) }.forEach { it.insertUnder(name.asType(pythonName = pythonName)) }

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

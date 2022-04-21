package type

import type.util.asType
import type.util.find

class TypeHierarchy {
    // todo: Class? is a parent of Class
    val anyN = Type("Any", true)
    val any = Type("Any", false, _parents = mutableSetOf(anyN))
    val stringN = Type("String", true, _parents = mutableSetOf(anyN))
    val string = Type("String", false, _parents = mutableSetOf(any))
    val numberN = Type("Number", true, _parents = mutableSetOf(anyN))
    val number = Type("Number", false, _parents = mutableSetOf(any))
    val intN = Type("Int", true, _parents = mutableSetOf(numberN))
    val int = Type("Int", false, _parents = mutableSetOf(number))
    val booleanN = Type("Boolean", true, _parents = mutableSetOf(anyN))
    val boolean = Type("Boolean", false, _parents = mutableSetOf(any))
    val nothingN = Type("Nothing", true, _parents = mutableSetOf(stringN, intN, booleanN))
    val nothing = Type("Nothing", false, _parents = mutableSetOf(string, int, boolean))

    val root = anyN

    fun addType(name: String, baseClassNames: Set<String> = setOf("Any")) {
        if (baseClassNames.isEmpty()) {
            addType(name, setOf("Any"))
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

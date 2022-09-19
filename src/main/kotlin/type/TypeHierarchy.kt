package type

import symtab.SymtabBuilderVisitor
import type.BuiltInTypes.anyN
import type.BuiltInTypes.nothing
import type.BuiltInTypes.nothingN
import type.TypeNames.ANY
import type.util.asType

class TypeHierarchy(
    private val symtabBuilder: SymtabBuilderVisitor,
) {
    val root = anyN

    private val currentScope get() = this.symtabBuilder.currentScope

    /**
     * @return The non-nullable version of the added type
     */
    fun addType(
        name: String,
        pythonName: String = name,
        baseClassNames: Set<String> = setOf(ANY),
    ): Type {
        if (baseClassNames.isEmpty()) {
            return addType(name, pythonName, setOf(ANY))
        } else {
            require(baseClassNames.none { "?" in it }) { "The names of the base classes should only contain the non-nullable variants" }

            val nonNullableType = name.asType(pythonName = pythonName)
            val nullableType = "$name?".asType(pythonName = pythonName).apply {
                addChild(nonNullableType)
            }

            baseClassNames.map { baseClassName ->
                currentScope.resolveType("$baseClassName?")?.referencedType
                    ?: throw RuntimeException("Can't find type symbol of type '$baseClassName?'")
            }.forEach { it.insertUnder(nullableType) }
            baseClassNames.map { baseClassName ->
                currentScope.resolveType(baseClassName)?.referencedType
                    ?: throw RuntimeException("Can't find type symbol of type '$baseClassName'")
            }.forEach { it.insertUnder(nonNullableType) }

            return nonNullableType
        }
    }

    private fun Type.insertUnder(type: Type) {
        type.addParent(this)

        val nothingChild = if (type.nullable) nothingN else nothing
        type.addChild(nothingChild)

        nothingChild.removeParent(this)
    }
}

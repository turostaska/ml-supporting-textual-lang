package symtab

import type.Type
import type.TypeNames

val Boolean.asMutability get() = if (this) Mutability.VAR else Mutability.VAL

fun String.getAsMutability() = Mutability.valueOf(this.uppercase())

sealed interface Symbol {
    val name: String
    val type: String
}

// todo: visibility
class VariableSymbol(
    override val name: String,
    override val type: String,
    val mutability: Mutability,
) : Symbol {

    val isMutable = (mutability == Mutability.VAR)
    override fun toString() = "${mutability.name} $name: $type"
}

class MethodSymbol(
    override val name: String,
    val returnType: String?,
    val params: List<String>,
    val isInfix: Boolean = false,
) : Symbol {
    override val type: String
        get() = "(${params.joinToString()}) -> ${returnType ?: TypeNames.UNIT}"

    companion object {
        fun fun0(name: String, returnType: String? = null) = MethodSymbol(name, returnType, emptyList())
    }

    override fun toString() = "fun $name: $type"
}

open class TypeSymbol(
    override val name: String,
    val referencedType: Type,
) : Symbol {
    override val type: String = "{ Type: ${referencedType.name} }"

    override fun toString() = "class $name (user-defined)"
}

class BuiltInTypeSymbol(
    name: String,
    referencedType: Type,
) : TypeSymbol(name, referencedType) {
    override val type: String = "{ Built-in Type: ${referencedType.name} }"

    override fun toString() = "class $name (built-in)"
}

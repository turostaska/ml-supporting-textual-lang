package symtab

import type.TypeNames

val Boolean.asMutability get() = if (this) Mutability.VAR else Mutability.VAL

fun String.getAsMutability() = Mutability.valueOf(this.uppercase())

// todo: TypeSymbol
// todo: add built in types to global scope
// todo: add custom classes to their current scopes
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
}

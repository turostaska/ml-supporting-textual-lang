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
// todo: type should be TypeSymbol
class VariableSymbol(
    override val name: String,
    override val type: String,
    val mutability: Mutability,
) : Symbol {

    val isMutable = (mutability == Mutability.VAR)
    override fun toString() = "${mutability.name} $name: $type"
}

// todo: infix and property methods?
open class MethodSymbol(
    override val name: String,
    val returnType: String?, // todo: TypeSymbol
    val params: Map<String, TypeSymbol>,
    val isInfix: Boolean = false,
) : Symbol {
    override val type: String
        get() = "(${params.map { "${it.key}: ${it.value.name}" }.joinToString()}) -> $returnTypeName"

    val returnTypeName = returnType ?: TypeNames.UNIT

    companion object {
        fun fun0(name: String, returnType: String? = null) = MethodSymbol(name, returnType, emptyMap())
    }

    override fun toString() = "fun $name: $type"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodSymbol) return false

        if (name != other.name) return false
        if (params.values.toList() != other.params.values.toList()) return false
        if (isInfix != other.isInfix) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + params.values.hashCode()
        result = 31 * result + isInfix.hashCode()
        return result
    }


}

class ClassMethodSymbol(
    name: String,
    returnType: String?,
    params: Map<String, TypeSymbol>,
    val onType: TypeSymbol,
) : MethodSymbol(name, returnType, params) {
    init {
        onType.classMethods += this
    }

    override val type: String
        get() = "$onType.(${
            params.map { "${it.key}: ${it.value.name}" }.joinToString()
        }) -> ${returnType ?: TypeNames.UNIT}"

    override fun toString() = "fun $onType.$name: $type"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassMethodSymbol) return false
        if (!super.equals(other)) return false

        if (onType != other.onType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + onType.hashCode()
        return result
    }
}

open class TypeSymbol(
    override val name: String,
    val referencedType: Type,
) : Symbol {
    val classMethods: MutableSet<MethodSymbol> = mutableSetOf()

    override val type: String = "{ Type: ${referencedType.name} }"

    override fun toString() = "class $name (user-defined)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeSymbol) return false

        if (name != other.name) return false
        if (referencedType != other.referencedType) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + referencedType.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

class BuiltInTypeSymbol(
    name: String,
    referencedType: Type,
) : TypeSymbol(name, referencedType) {
    override val type: String = "{ Built-in Type: ${referencedType.name} }"

    override fun toString() = "class $name (built-in)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BuiltInTypeSymbol) return false
        if (!super.equals(other)) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

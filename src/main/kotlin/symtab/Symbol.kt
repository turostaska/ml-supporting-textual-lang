package symtab

import type.Type
import type.TypeNames

val Boolean.asMutability get() = if (this) Mutability.VAR else Mutability.VAL

fun String.getAsMutability() = Mutability.valueOf(this.uppercase())

sealed interface Symbol {
    val name: String
    val type: String
}

class ModuleSymbol(
    val moduleScope: ModuleScope,
) : Symbol {
    override val name: String = moduleScope.importAlias ?: moduleScope.moduleName
    override val type: String = "Module symbol of module '$name'"
}

// todo: visibility
class VariableSymbol(
    override val name: String,
    val typeSymbol: TypeSymbol,
    val mutability: Mutability,
    override val type: String = typeSymbol.name,
) : Symbol {

    val isMutable = (mutability == Mutability.VAR)
    override fun toString() = "${mutability.name} $name: $type"
}

// todo: infix and property methods?
open class MethodSymbol(
    override val name: String,
    val returnType: TypeSymbol?,
    val params: Map<String, List<TypeSymbol>>,
    val isInfix: Boolean = false,
) : Symbol {
    override val type: String
        get() = "(${params.map { "${it.key}: ${it.value.joinToString { type -> type.name }}" }.joinToString()}) -> $returnTypeName"

    val returnTypeName = returnType ?: TypeNames.UNIT

    companion object {
        fun fun0(name: String, returnType: TypeSymbol? = null) = MethodSymbol(name, returnType, emptyMap())
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
    returnType: TypeSymbol?,
    params: Map<String, List<TypeSymbol>>,
    val onType: TypeSymbol,
) : MethodSymbol(name, returnType, params) {
    override val type: String
        get() = "$onType.(${
            params.map { "${it.key}: ${it.value.first().name}" }.joinToString()
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
    var scope: ClassDeclarationScope? = null,
) : Symbol {
    val classMethods: Set<MethodSymbol> get() {
        return scope!!.classMethods
    }

    override val type: String = "{ Type: ${referencedType.name} }"

    override fun toString() = "class $name (user-defined)"

    constructor(name: String, referencedType: Type): this(name, referencedType, null)

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

// todo: reverse map as property
fun TypeSymbol.pythonName() = if (this is BuiltInTypeSymbol) this.referencedType.pythonName else name

class BuiltInTypeSymbol(
    name: String,
    referencedType: Type,
) : TypeSymbol(name, referencedType, null) {
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

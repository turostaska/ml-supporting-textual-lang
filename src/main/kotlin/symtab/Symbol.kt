package symtab

import type.Type
import type.TypeHierarchy
import type.util.find

val Boolean.asMutability get() = if (this) Symbol.Mutability.VAR else Symbol.Mutability.VAL

val String.asMutability get() = Symbol.Mutability.valueOf(this.uppercase())

// todo: visibility
class Symbol(
    val name: String,
    val type: Type,
    val mutability: Mutability,
) {
    enum class Mutability {
        VAR, VAL,
    }

    constructor(
        name: String,
        type: String,
        mutability: String,
        typeHierarchy: TypeHierarchy,
    ) : this(
        name = name,
        type = typeHierarchy.find(type),
        mutability = mutability.asMutability,
    )

    constructor(
        name: String,
        type: String,
        mutable: Boolean,
        typeHierarchy: TypeHierarchy,
    ) : this(
        name = name,
        type = typeHierarchy.find(type),
        mutability = mutable.asMutability,
    )

    override fun toString() = "${mutability.name} $name: $type"

    val isMutable = (mutability == Mutability.VAR)
}

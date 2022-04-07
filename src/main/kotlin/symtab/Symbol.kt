package symtab

// todo: visibility
// todo: nullability (in Type?)
class Symbol(
    val name: String,
    val type: String, // todo: Type class -> hierarchia
    val mutability: Mutability,
) {
    enum class Mutability {
        VAR, VAL,
    }

    constructor(
        name: String,
        type: String,
        mutability: String,
    ) : this(name = name, type = type, mutability = Mutability.valueOf(mutability.uppercase()))

    constructor(
        name: String,
        type: String,
        mutable: Boolean,
    ) : this(name = name, type = type, mutability = if (mutable) Mutability.VAR else Mutability.VAL)

    override fun toString() = "${mutability.name} $name: $type"

    val isMutable
        get() = (mutability == Mutability.VAR)
}

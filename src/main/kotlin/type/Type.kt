package type

class Type(
    val name: String,
    val nullable: Boolean,
    private val _parents: MutableSet<Type> = mutableSetOf(),
    private val _children: MutableSet<Type> = mutableSetOf(),
    val pythonName: String = name,
) {
    val parents get() = _parents.toSet()
    val children get() = _children.toSet()

    fun addParent(parent: Type) {
        this._parents += parent
        parent._children += this
        // todo: remove any/anyN?
    }

    fun addParents(vararg parents: Type) {
        parents.forEach(this::addParent)
    }

    fun addChild(child: Type) {
        this._children += child
        child._parents += this
        // todo: remove nothing/nothingN?
    }

    fun removeParent(parent: Type) {
        this._parents -= parent
        parent._children -= this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Type

        if (name != other.name) return false
        if (nullable != other.nullable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + nullable.hashCode()
        return result
    }

    init {
        parents.forEach { it._children += this }
        children.forEach { it._parents += this }
    }

    val fullName = name + if (nullable) "?" else ""

    override fun toString() = fullName
}

val Type.nullableVariant: Type
    get() {
        require(!nullable) { "Can't get nullable variant of nullable type" }

        return this.parents.find { it.name == name && it.nullable }
            ?: throw RuntimeException("Can't find nullable type variant of '$name' in its parents")
    }

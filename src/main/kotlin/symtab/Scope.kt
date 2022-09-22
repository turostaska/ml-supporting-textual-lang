package symtab

import symtab.Scope.Serial.serial
import type.nullableVariant

class Scope(
    val parent: Scope? = null,
    val children: MutableList<Scope> = mutableListOf(),
    val name: String = if (parent == null) "Global scope" else "Scope ${serial++}",
    private val symbols: MutableSet<Symbol> = mutableSetOf(),
) {
    init {
        parent?.children?.add(this)
    }

    fun resolveMethod(name: String): MethodSymbol? =
        symbols.find { it.name == name && it is MethodSymbol } as? MethodSymbol
            ?: this.parent?.resolveMethod(name)

    fun resolveVariable(name: String): VariableSymbol? =
        symbols.find { it.name == name && it is VariableSymbol } as? VariableSymbol
            ?: this.parent?.resolveVariable(name)

    fun resolveType(name: String): TypeSymbol? =
        symbols.find { it.name == name && it is TypeSymbol } as? TypeSymbol
            ?: this.parent?.resolveType(name)

    fun add(symbol: Symbol) {
        symbols += when (symbol) {
            is MethodSymbol -> {
                val matchingSymbol = symbols.find { it.name == symbol.name && it is MethodSymbol }
                if (matchingSymbol != null && matchingSymbol == symbol)
                    throw RuntimeException("Redefinition of function $symbol in scope ${this.name}")
                symbol
            }

            is VariableSymbol -> {
                if (symbols.find { it.name == symbol.name && it is VariableSymbol } != null)
                    throw RuntimeException("Redefinition of variable ${symbol.name} in scope ${this.name}")
                symbol
            }

            is TypeSymbol -> {
                if (symbols.find { it.name == symbol.name && it is TypeSymbol && it.type == symbol.type } != null)
                    throw RuntimeException("Redefinition of type ${symbol.name} in scope ${this.name}")
                symbol
            }
        }
    }

    /**
     * Adds the non-nullable and nullable version of the type symbol to the scope
     * @param symbol Symbol of non-nullable type
     */
    fun addType(symbol: TypeSymbol) {
        require(symbol.name.none { it == '?' }) { "Scope.addType should only be used with non-nullable types" }

        addAll(
            symbol,
            TypeSymbol("${symbol.name}?", symbol.referencedType.nullableVariant)
        )
    }

    fun addBuiltInType(symbol: BuiltInTypeSymbol) {
        require(symbol.name.none { it == '?' }) { "Scope.addBuiltInType should only be used with non-nullable types" }

        addAll(
            symbol,
            BuiltInTypeSymbol("${symbol.name}?", symbol.referencedType.nullableVariant)
        )
    }

    operator fun plusAssign(symbol: Symbol) = this.add(symbol)

    val isGlobal
        get() = (parent == null)

    val isLeaf
        get() = children.isEmpty()

    override fun toString(): String = StringBuilder().let { sb ->
        sb.appendLine("---------------")
        sb.appendLine(name)
        sb.appendLine("---------------")
        symbols.forEach { sb.appendLine((it.toString())) }
        sb.appendLine("---------------")
        sb.appendLine()

        sb.toString()
    }

    fun addAll(vararg symbols: Symbol) {
        symbols.forEach(this::add)
    }

    fun addAllBuiltInTypes(vararg symbols: BuiltInTypeSymbol) {
        symbols.forEach(this::addBuiltInType)
    }

    object Serial { var serial = 0 }
}

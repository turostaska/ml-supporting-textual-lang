package symtab

import symtab.Scope.Serial.serial

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

    fun add(symbol: Symbol) {
        symbols += when (symbol) {
            is MethodSymbol -> {
                if (symbols.find { it.name == symbol.name && it is MethodSymbol } != null)
                    throw RuntimeException("Redefinition of function ${symbol.name} in scope ${this.name}")
                symbol
            }

            is VariableSymbol -> {
                if (symbols.find { it.name == symbol.name && it is VariableSymbol } != null)
                    throw RuntimeException("Redefinition of variable ${symbol.name} in scope ${this.name}")
                symbol
            }
        }
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

    object Serial { var serial = 0 }
}

package symtab

import com.kobra.kobraParser.ForStatementContext
import com.kobra.kobraParser.UsingStatementContext
import symtab.Scope.Serial.serial
import type.nullableVariant
import util.splitOnFirst
import util.splitOnLast
import util.throwError

open class Scope(
    val parent: Scope? = null,
    val children: MutableList<Scope> = mutableListOf(),
    val name: String = if (parent == null) "Global scope" else "Scope ${serial++}",
    protected val symbols: MutableSet<Symbol> = mutableSetOf(),
) {
    init {
        parent?.children?.add(this)
    }

    private val globalScope: Scope = if (this.parent == null) this else this.parent.globalScope

    fun getForStatementScope(
        ctx: ForStatementContext,
    ): Scope? {
        return this.children.lastOrNull { (it as? ForStatementScope)?.ctx == ctx }
    }

    fun getUsingStatementScope(
        ctx: UsingStatementContext,
    ): Scope? {
        return this.children.lastOrNull { (it as? UsingStatementScope)?.ctx == ctx }
    }

    fun getParentTypeSymbol(): TypeSymbol {
        if (this is ClassDeclarationScope) return this.typeSymbol
        return this.parent?.getParentTypeSymbol()
            ?: throwError { "No class declaration scope found." }
    }

    fun resolveMethod(name: String): MethodSymbol? {
        return resolveMethodLocally(name) ?: this.parent?.resolveMethod(name)
    }

    fun resolveVariable(name: String): VariableSymbol? {
        return if ("." !in name)
            symbols.findLast { it.name == name && it is VariableSymbol } as? VariableSymbol
                ?: this.parent?.resolveVariable(name)
        else {
            val (module, name) = name.splitOnFirst(".")
            // todo: ha a module egy variable
            val typeScope = resolveVariable(module)?.typeSymbol?.scope
                ?: throwError { "Receiver for variable '$module' cannot be resolved" }
            typeScope.resolveVariable(name)
        }
    }

    fun resolveType(name: String): TypeSymbol? {
        return if ("." !in name)
            symbols.findLast { it.name == name && it is TypeSymbol } as? TypeSymbol
                ?: children.filterIsInstance<ModuleScope>().asSequence().map {
                    it.resolveTypeLocally(name)
                }.firstOrNull { it != null }
                ?: this.parent?.resolveType(name)
        else {
            val (module, name) = name.splitOnLast(".")
            val moduleSymbol = findModuleOrClassScope(module)
                ?: globalScope.findModuleOrClassScope(module)
                // ?: throwError { "Can't resolve $name: module or class $module not found" }
                ?: return null
            moduleSymbol.resolveType(name)
        }
    }

    fun resolveBuiltInType(name: String): BuiltInTypeSymbol? =
        symbols.findLast { it.name == name && it is BuiltInTypeSymbol } as? BuiltInTypeSymbol
            ?: this.parent?.resolveBuiltInType(name)

    fun resolveMethodLocally(name: String): MethodSymbol? =
        symbols.findLast { it.name == name && it is MethodSymbol } as? MethodSymbol

    fun resolveVariableLocally(name: String): VariableSymbol? =
        symbols.findLast { it.name == name && it is VariableSymbol } as? VariableSymbol

    fun resolveTypeLocally(name: String): TypeSymbol? =
        symbols.findLast { it.name == name && it is TypeSymbol } as? TypeSymbol

    fun resolve(name: String): Symbol? {
        return if ("." !in name)
            symbols.findLast { it.name == name }
                ?: parent?.resolve(name)
        else {
            val (module, name) = name.splitOnFirst(".")
            val moduleSymbol = findModuleOrClassScope(module)
                ?: throwError { "Can't resolve $name: module or class $module not found" }
            moduleSymbol.resolve(name)
        }
    }

    fun findModuleOrClassScope(name: String): Scope? =
        children.findLast {
            it is ModuleScope && it.importAlias == name || it is ModuleScope && it.moduleName == name ||
            it is ClassDeclarationScope && it.className == name
        } ?: parent?.findModuleOrClassScope(name)

    fun findClassScope(name: String): ClassDeclarationScope? =
        children.findLast { it is ClassDeclarationScope && it.className == name } as? ClassDeclarationScope
            ?: children.filterIsInstance<ModuleScope>().asSequence().map {
                it.findClassScope(name)
            }.firstOrNull { it != null }

    fun add(symbol: Symbol) {
        symbols += when (symbol) {
            is MethodSymbol -> {
                symbol
            }

            is VariableSymbol -> {
                symbol
            }

            is TypeSymbol -> {
                if (symbols.find { it.name == symbol.name && it is TypeSymbol && it.type == symbol.type } != null)
                    throw RuntimeException("Redefinition of type ${symbol.name} in scope ${this.name}")
                symbol
            }

            is ModuleSymbol -> {
                if (symbols.find { it is ModuleSymbol && it.moduleScope == symbol.moduleScope } != null)
                    throw RuntimeException("Redefinition of module ${symbol.name}")
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

    private fun getUniqueName(name: String): String {
        var name = name
        while ( symbols.find { it.name == name } != null )
            name = "_$name"
        return name
    }

    object Serial { var serial = 0 }
}

class FunctionScope(
    parent: Scope?,
    val methodSymbol: MethodSymbol,
    name: String = "Function scope of ${methodSymbol.name}",
): Scope(parent, name = name)

class ClassDeclarationScope(
    parent: Scope?,
    val typeSymbol: TypeSymbol,
    name: String = "Class declaration of ${typeSymbol.name}",
): Scope(parent, name = name) {
    init {
        typeSymbol.scope = this
    }

    val className = typeSymbol.name
    val classMethods: Set<MethodSymbol> get() = symbols.filterIsInstance<MethodSymbol>().toSet()
    val properties: Set<VariableSymbol> get() = symbols.filterIsInstance<VariableSymbol>().toSet()
}

class PrimaryConstructorScope(
    parent: Scope?,
    val typeSymbol: TypeSymbol,
    name: String = "Primary constructor of ${typeSymbol.name}",
): Scope(parent, name = name)

class ModuleScope(
    parent: Scope?,
    val moduleName: String,
    val importAlias: String? = null,
    name: String = "Module scope of $moduleName",
): Scope(parent, name = name)

class ForStatementScope(
    parent: Scope?,
    val ctx: ForStatementContext,
    name: String = "For scope of ${ctx.text}",
): Scope(parent, name = name)

class UsingStatementScope(
    parent: Scope?,
    val ctx: UsingStatementContext,
    name: String = "Using scope of ${ctx.text}",
): Scope(parent, name = name)

package symtab.import

import com.kobra.Python3Parser.*
import com.kobra.Python3ParserBaseVisitor
import python.*
import symtab.*
import type.TypeHierarchy
import type.TypeNames
import util.secondOrNull

// végtelen ciklusra odafigyelni: listát fenntartani
// működjön nem rekurzívan, utána lehet szórakozni

/*
 * from a import B, C   : add B and C to the global scope
 * from a import *      : add everything from 'a' to global scope
 * import a             : add new scope 'a' to current scope
 * import a as b        : add new scope 'a' to current scope with alias 'b'
 */

/**
 * Visits the definition of a Python library and adds all the symbols to the global scope given in the constructor
 * @param symbolsToImportAs Names of symbols that should be added. If null, all symbols will be imported.
 */
class ImportedLibVisitor(
    private val symtabBuilder: SymtabBuilderVisitor,
    private val typeHierarchy: TypeHierarchy,
    private val currentModuleName: String,
    private val symbolsToImportAs: Map<String, String>? = null,
): Python3ParserBaseVisitor<Unit>() {
    private var currentScope = symtabBuilder.currentScope
        set(value) {
            symtabBuilder.currentScope = value
            field = value
        }

    init {
        println("Visited module '$currentModuleName'")
    }

    private val namesToImport = mutableSetOf<String>()

    override fun visitImport_name(ctx: Import_nameContext): Unit = ctx.run {
        return super.visitImport_name(this) // todo

        dotted_as_names().dotted_as_name().filter {
            it.dotted_name()?.text in namesToImport
        }.forEach { ctx ->
            val name = ctx.dotted_name().text!!

            val libReader = ImportedLibraryReader(symtabBuilder, typeHierarchy)
            libReader.readAndAddAllSymbols("$currentModuleName.$name")
        }
        super.visitImport_name(this)
    }

    // todo: conditional imports?

    override fun visitImport_from(ctx: Import_fromContext): Unit = ctx.run {
        if (symbolsToImportAs?.keys?.contains(this.dotted_name()?.text) == false) return

        // This was needed because 'this.dotted_name().text' did not contain the starting '.' char for some reason
        val packageName = this.text.substringAfter("from").substringBefore("import").let {
            if (it.first() == '.') currentModuleName + it
            else it
        }

        val symbolsToImportAs = this.import_as_names()?.import_as_name()?.associate { ctx ->
            val name = ctx.NAME().first().text!!
            val importAlias = ctx.NAME().secondOrNull()?.text ?: name
            "$currentModuleName.$name" to importAlias
        }?.filter { it.value in namesToImport }

        val libReader = ImportedLibraryReader(symtabBuilder, typeHierarchy)
        if (symbolsToImportAs != null) {
            // add specified symbols
            libReader.readAndAddSpecifiedSymbols(packageName, symbolsToImportAs)
        } else {
            // add all symbols
            libReader.readAndAddAllSymbols(packageName)
        }

        super.visitImport_from(this)
    }

    override fun visitExpr_stmt(ctx: Expr_stmtContext): Unit = ctx.run {
        /* The __all__ list enumerates symbols which should be imported
           'import module_name' imports all symbols present and those listed in __all__ to a new scope
           'from module_name import *' imports only those listed in __all__ to the global scope
         */
        if (testlist_star_expr()?.firstOrNull()?.text != "__all__")
            return

        val names = this.testListComp()?.test()?.map { it.text } ?: emptyList()
        namesToImport.addAll(names)
    }

    override fun visitClassdef(ctx: ClassdefContext): Unit = ctx.run {
        if (symbolsToImportAs?.keys?.contains(classNamePy) == false) return

        val className = TypeNames.pythonTypeNamesToKobraMap[classNamePy] ?: classNamePy

        // Metaclasses are not supported yet
        val superClassesWithoutMetaClasses = this.arglist()?.argument()?.filter { it.ASSIGN() == null } ?: emptyList()

        // Some types are not supported yet, we filter these out
        val superClasses = superClassesWithoutMetaClasses.map { it.text }.filter {
            currentScope.resolveType(it) != null
        }

        val declaredType = typeHierarchy.addType(className, classNamePy, superClasses.toSet())
        val typeSymbol = TypeSymbol(className, declaredType)

        try {
            currentScope.addType(typeSymbol)
        } catch (_: RuntimeException) {}

        currentScope = ClassDeclarationScope(currentScope, typeSymbol)
        super.visitClassdef(ctx)
        currentScope = currentScope.parent!!
    }

    // todo: import *
    override fun visitFuncdef(ctx: FuncdefContext): Unit = ctx.run {
        println("Function definition of $functionName")

        if (symbolsToImportAs?.keys?.contains(functionName) == false) return

        // Some function headers may be identical since some types are not mapped yet and are substituted by 'Any?'.
        // If the type symbol can't be added, we should continue.
        try {
            currentScope += (currentScope as? ClassDeclarationScope)?.let {
                ClassMethodSymbol(functionName, returnTypeName, params, it.typeSymbol)
            } ?: MethodSymbol(functionName, returnTypeName, params)
        } catch (_: RuntimeException) {}

        super.visitFuncdef(ctx)
    }


    private val FuncdefContext.params get() = parameterNamesToTypeNameMap.mapValues {
        val typeNames = it.value
        it.value.map { typeName ->
            currentScope.resolveType(typeName)
                ?: throw RuntimeException("Can't find symbol for type '$typeNames' in global scope")
        }
    }
}

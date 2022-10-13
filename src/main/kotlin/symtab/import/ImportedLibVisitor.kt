package symtab.import

import com.kobra.Python3Parser.*
import com.kobra.Python3ParserBaseVisitor
import python.classNamePy
import python.functionName
import python.parameterNamesToTypeNameMap
import python.returnTypeName
import symtab.*
import symtab.extensions.resolveTypeOrThrow
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
    private val currentScope: Scope,
    private val symbolsToImportAs: Map<String, String>? = null,
): Python3ParserBaseVisitor<Unit>() {
    private var currentTypeSymbol: TypeSymbol? = null

    override fun visitImport_name(ctx: Import_nameContext): Unit = ctx.run {
        dotted_as_names().dotted_as_name().forEach { ctx ->
            // todo: submodules
            val name = ctx.dotted_name().text!!
            val importAlias = ctx.NAME()?.text ?: name

            val moduleScope = ModuleScope(currentScope, name, importAlias)
            currentScope.add(ModuleSymbol(moduleScope))
            ImportedLibraryReader(moduleScope).readAndAddAllSymbols(name)
        }
        super.visitImport_name(this)
    }

    override fun visitImport_from(ctx: Import_fromContext): Unit = ctx.run {
        val packageName = this.dotted_name().text
        val symbolsToImportAs = this.import_as_names()?.import_as_name()?.associate { ctx ->
            val name = ctx.NAME().first().text!!
            val importAlias = ctx.NAME().secondOrNull()?.text ?: name
            name to importAlias
        }

        if (symbolsToImportAs != null) {
            // add specified symbols
            ImportedLibraryReader(currentScope).readAndAddSpecifiedSymbols(packageName, symbolsToImportAs)
        } else {
            // add all symbols
            ImportedLibraryReader(currentScope).readAndAddAllSymbols(packageName)
        }

        super.visitImport_from(this)
    }

    override fun visitClassdef(ctx: ClassdefContext): Unit = ctx.run {
        val className = TypeNames.pythonTypeNamesToKobraMap[classNamePy]
            ?: throw RuntimeException("Can't resolve Python type '$classNamePy' to Kobra type")

        currentTypeSymbol = currentScope.resolveTypeOrThrow(className)
        println("Class definition of ${currentTypeSymbol!!.name}")
        super.visitClassdef(ctx)
        currentTypeSymbol = null
    }

    override fun visitFuncdef(ctx: FuncdefContext): Unit = ctx.run {
        println("Function definition of $functionName")

        currentScope += currentTypeSymbol?.let { currentTypeSymbol ->
            ClassMethodSymbol(functionName, returnTypeName, params, currentTypeSymbol)
        } ?: MethodSymbol(functionName, returnTypeName, params)

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

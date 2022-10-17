package symtab.import

import com.kobra.Python3Parser.*
import com.kobra.Python3ParserBaseVisitor
import python.classNamePy
import python.functionName
import python.parameterNamesToTypeNameMap
import python.returnTypeName
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

private val ignoredModules = listOf("nt", "org.python.core")

/**
 * Visits the definition of a Python library and adds all the symbols to the global scope given in the constructor
 * @param symbolsToImportAs Names of symbols that should be added. If null, all symbols will be imported.
 */
class ImportedLibVisitor(
    private val symtabBuilder: SymtabBuilderVisitor,
    private val typeHierarchy: TypeHierarchy,
    private val symbolsToImportAs: Map<String, String>? = null,
): Python3ParserBaseVisitor<Unit>() {
    private var currentScope = symtabBuilder.currentScope
        set(value) {
            symtabBuilder.currentScope = value
            field = value
        }

    override fun visitImport_name(ctx: Import_nameContext): Unit = ctx.run {
        return // todo

        dotted_as_names().dotted_as_name().filter {
            it?.dotted_name()?.text !in ignoredModules
        }.filter {
            it.dotted_name()?.text?.startsWith("_") == false
        }.forEach { ctx ->
            // todo: submodules
            val name = ctx.dotted_name().text!!
            val importAlias = ctx.NAME()?.text ?: name

            val moduleScope = ModuleScope(currentScope, name, importAlias)
            currentScope.add(ModuleSymbol(moduleScope))
            ImportedLibraryReader(symtabBuilder, typeHierarchy).readAndAddAllSymbols(name)
        }
        super.visitImport_name(this)
    }

    override fun visitImport_from(ctx: Import_fromContext): Unit = ctx.run {
        return // todo

        val packageName = this.dotted_name()?.text ?: return  // from . import {...}

        if (packageName in ignoredModules) return

        // TODO: made to bypass a conditional import
        if (packageName.startsWith("_")) return

        val symbolsToImportAs = this.import_as_names()?.import_as_name()?.associate { ctx ->
            val name = ctx.NAME().first().text!!
            val importAlias = ctx.NAME().secondOrNull()?.text ?: name
            name to importAlias
        }

        if (symbolsToImportAs != null) {
            // add specified symbols
            ImportedLibraryReader(symtabBuilder, typeHierarchy).readAndAddSpecifiedSymbols(packageName, symbolsToImportAs)
        } else {
            // add all symbols
            ImportedLibraryReader(symtabBuilder, typeHierarchy).readAndAddAllSymbols(packageName)
        }

        super.visitImport_from(this)
    }

    override fun visitClassdef(ctx: ClassdefContext): Unit = ctx.run {
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

    override fun visitFuncdef(ctx: FuncdefContext): Unit = ctx.run {
        println("Function definition of $functionName")

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

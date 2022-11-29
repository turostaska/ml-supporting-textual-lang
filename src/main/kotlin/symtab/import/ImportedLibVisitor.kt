package symtab.import

import com.kobra.Python3Parser.*
import com.kobra.Python3ParserBaseVisitor
import python.classNamePy
import python.functionName
import python.parameterNamesToTypeNameMap
import python.returnTypeName
import symtab.*
import symtab.extensions.resolveMethodOrThrow
import type.ANY
import type.TypeHierarchy
import type.TypeNames
import util.containsQualifier
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
    symbolsToImportAs: Map<String, String>? = null,
): Python3ParserBaseVisitor<Unit>() {
    private val symbolsToImportAs = symbolsToImportAs?.toMutableMap() ?: mutableMapOf()

    private var currentScope = symtabBuilder.currentScope
        set(value) {
            symtabBuilder.currentScope = value
            field = value
        }

    init {
        // println("Visited module '$currentModuleName'")

        findNamesForModule(currentModuleName).associateWith { it }.let { namesToAliases ->
            this.symbolsToImportAs?.plusAssign(namesToAliases)
        }
    }

    override fun visitImport_from(ctx: Import_fromContext): Unit = ctx.run {
        // This was needed because 'this.dotted_name().text' did not contain the starting '.' char for some reason
        val packageName = this.text.substringAfter("from").substringBefore("import").let {
            if (it.first() == '.') currentModuleName + it
            else it
        }

        if (this.STAR() != null) {
            // get all names in the scope entered, import them
            val namesToImport = symbolsToImportAs?.filter { it.key.startsWith("$packageName.") }
                ?.mapValues { it.value.substringAfterLast('.') }


            val libReader = ImportedLibraryReader(symtabBuilder, typeHierarchy)
            if (namesToImport != null) {
                libReader.readAndAddSpecifiedSymbols(packageName, namesToImport)
                return super.visitImport_from(this)
            }
        }

        val symbolsToImportAs = this.import_as_names()?.import_as_name()?.associate { ctx ->
            val name = ctx.NAME().first().text!!
            val importAlias = ctx.NAME().secondOrNull()?.text ?: name
            "$currentModuleName.$name" to importAlias
        }?.filter { symbolsToImportAs?.keys?.contains(it.key) == true }

        if (symbolsToImportAs?.isEmpty() == true)
            return

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

    override fun visitClassdef(ctx: ClassdefContext): Unit = ctx.run {
        val className = TypeNames.pythonTypeNamesToKobraMap[classNamePy] ?: classNamePy
        val parentModules = currentModuleName.split(".")

        if (symbolsToImportAs?.keys?.toList()?.containsQualifier(parentModules, className) == false)
            return

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

            // ha van őse, és nincs explicit konstruktora, le kell másolni az ős konstruktorát
            if (!this.hasExplicitConstructor && this.hasSuperclass) {
                val firstSuperclass = this.arglist().argument().first().text
                val superclassConstructor = currentScope.resolveMethodOrThrow(firstSuperclass)
                currentScope.add(
                    MethodSymbol(className, typeSymbol, superclassConstructor.params)
                )
            }
        } catch (_: RuntimeException) {}

        currentScope = ClassDeclarationScope(currentScope, typeSymbol)
        super.visitClassdef(ctx)
        currentScope = currentScope.parent!!
    }

    override fun visitFuncdef(ctx: FuncdefContext): Unit = ctx.run {
        // println("Function definition of $currentModuleName.$functionName")
        if (functionName == "__init__") {
            (currentScope as? ClassDeclarationScope)?.typeSymbol?.runCatching {
                currentScope.parent!!.add(MethodSymbol(this.name, this, params))
            }
            return
        }

        if (symbolsToImportAs?.keys?.contains("$currentModuleName.$functionName") == false
            && currentScope !is ClassDeclarationScope)
            return

        // todo: If return value is of unknown type, let's just replace it with Any for now
        val returnType = currentScope.resolveType(returnTypeName) ?: currentScope.ANY

        // Some function headers may be identical since some types are not mapped yet and are substituted by 'Any?'.
        // If the type symbol can't be added, we should continue.
        try {
            currentScope += (currentScope as? ClassDeclarationScope)?.let {
                ClassMethodSymbol(functionName, returnType, params, it.typeSymbol)
            } ?: MethodSymbol(functionName, returnType, params)
        } catch (e: RuntimeException) {
            // todo: new exception for redefinition of function (or any symbol) and catch that
            println(e.message)
        }

        super.visitFuncdef(ctx)
    }


    private val FuncdefContext.params get() = parameterNamesToTypeNameMap.mapValues {
        it.value.map { typeName ->
            currentScope.resolveType(typeName) ?: symtabBuilder.globalScope.resolveType("Any?")!!
        }
    }

    private val ClassdefContext.functions
        get() = suite()?.stmt()?.filter { it.compound_stmt()?.funcdef() != null }
            ?.map { it.compound_stmt().funcdef().NAME().text } ?: emptyList()

    private val ClassdefContext.hasExplicitConstructor
        get() = this.functions.any { it == "__init__" }

    private val ClassdefContext.hasSuperclass
        get() = this.arglist().argument().map { it.text }.any { it != "object" }
}

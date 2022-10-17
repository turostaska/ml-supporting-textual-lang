package python

import com.kobra.Python3Parser
import com.kobra.Python3ParserBaseVisitor
import symtab.*
import type.TypeHierarchy
import type.TypeNames.ANY_N
import type.TypeNames.pythonTypeNamesToKobraMap

// todo: fáj a szemem, ezt nagyon ki kell takarítani
class PythonLibVisitor(
    private val globalScope: Scope,
    private val typeHierarchy: TypeHierarchy,
) : Python3ParserBaseVisitor<Unit>() {
    private val mappedTypesPy = listOf("int", "float", "str", "bool", "list", "range")
    private var currentlyVisitedClass: String? = null  // todo: only for debugging purposes
    private var currentTypeSymbol: TypeSymbol? = null
    private var currentScope = globalScope

    private val mappedTypes
        get() = mappedTypesPy.map {
            pythonTypeNamesToKobraMap[it] ?: throw RuntimeException("Python type $it is not mapped to Kobra type")
        }

    override fun visitClassdef(ctx: Python3Parser.ClassdefContext): Unit = ctx.run {
        val className = pythonTypeNamesToKobraMap[classNamePy] ?: classNamePy
        currentTypeSymbol = if (classNamePy in mappedTypesPy) {
            currentScope.resolveType(className)
                ?: throw RuntimeException("Can't find type symbol for type '$className' in global scope")
        } else null

        if (classNamePy !in mappedTypesPy) {
            // TODO
            //   val superClasses = this.arglist().argument().map { it.text }

            val declaredType = typeHierarchy.addType(className, classNamePy, emptySet())
            val typeSymbol = TypeSymbol(className, declaredType)
            currentScope.addType(typeSymbol)

            currentScope = ClassDeclarationScope(currentScope, typeSymbol)
            super.visitClassdef(ctx)
            currentScope = currentScope.parent!!
            return
        }

        currentlyVisitedClass = currentlyVisitedClass?.plus(".$classNamePy") ?: classNamePy
        println("Class definition of $currentlyVisitedClass")
        super.visitClassdef(ctx)
        currentlyVisitedClass = null
        currentTypeSymbol = null
    }

    override fun visitFuncdef(ctx: Python3Parser.FuncdefContext): Unit = ctx.run {
        val params = parameterNamesToTypeNameMap.mapValues {
            it.value.map { typeName ->
                currentScope.resolveType(typeName) ?: globalScope.resolveBuiltInType(ANY_N)!!
            }
        }

        val returnType = returnTypeName?.also {
            if (it !in mappedTypes) return
        }

        println("Function definition of ${currentlyVisitedClass?.plus(".$functionName") ?: functionName}")

        // Some function headers may be identical since some types are not mapped yet and are substituted by 'Any?'.
        // If the type symbol can't be added, we should continue.
        try {
            currentScope += currentTypeSymbol?.let { currentTypeSymbol ->
                ClassMethodSymbol(functionName, returnType, params, currentTypeSymbol)
            } ?: MethodSymbol(functionName, returnType, params)
        } catch (_: RuntimeException) {}

        super.visitFuncdef(ctx)
    }
}

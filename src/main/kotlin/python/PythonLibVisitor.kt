package python

import com.kobra.Python3Parser
import com.kobra.Python3ParserBaseVisitor
import symtab.ClassMethodSymbol
import symtab.MethodSymbol
import symtab.Scope
import symtab.TypeSymbol
import type.TypeNames.pythonTypeNamesToKobraMap

// todo: fáj a szemem, ezt nagyon ki kell takarítani
class PythonLibVisitor(
    private val globalScope: Scope,
) : Python3ParserBaseVisitor<Unit>() {
    private val classesToVisitPy = listOf("int", "float", "str", "bool", "list", "range")
    private var currentlyVisitedClass: String? = null
    private var currentTypeSymbol: TypeSymbol? = null

    private val classesToVisit
        get() = classesToVisitPy.map {
            pythonTypeNamesToKobraMap[it] ?: throw RuntimeException("Python type $it is not mapped to Kobra type")
        }

    override fun visitClassdef(ctx: Python3Parser.ClassdefContext) {
        val classNamePy = ctx.NAME().text
        if (classNamePy !in classesToVisitPy) return

        val className = pythonTypeNamesToKobraMap[classNamePy]
            ?: throw RuntimeException("Can't resolve Python type '$classNamePy' to Kobra type")
        currentTypeSymbol = globalScope.resolveType(className)
            ?: throw RuntimeException("Can't find type symbol for type '$className' in global scope")

        currentlyVisitedClass = currentlyVisitedClass?.plus(".$classNamePy") ?: classNamePy
        println("Class definition of $currentlyVisitedClass")
        super.visitClassdef(ctx)
        currentlyVisitedClass = null
        currentTypeSymbol = null
    }

    override fun visitFuncdef(ctx: Python3Parser.FuncdefContext) {
        val functionName = ctx.NAME().text
        val params =
            try {
                ctx.parameterNamesToTypeNameMap.mapValues {
                    val typeName = it.value
                    globalScope.resolveType(typeName)
                        ?: throw RuntimeException("Can't find symbol for type '$typeName' in global scope")
                }
            } catch (e: Exception) {
                println("Caught exception: ${e.message}, continuing...")
                return
            }

        val returnType = ctx.returnTypeName?.also {
            if (it !in classesToVisit) return
        }

        println("Function definition of ${currentlyVisitedClass?.plus(".$functionName") ?: functionName}")

        globalScope += currentTypeSymbol?.let {
            ClassMethodSymbol(functionName, returnType, params, currentTypeSymbol!!)
        } ?: MethodSymbol(functionName, returnType, params)

        super.visitFuncdef(ctx)
    }
}

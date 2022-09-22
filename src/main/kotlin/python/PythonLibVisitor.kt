package python

import com.kobra.Python3Parser
import com.kobra.Python3ParserBaseVisitor
import symtab.Scope
import symtab.TypeSymbol
import type.TypeNames

class PythonLibVisitor(
    private val globalScope: Scope,
) : Python3ParserBaseVisitor<Unit>() {
    private val classesToVisit = listOf("int", "float", "str", "bool", "list", "range")
    private var currentlyVisitedClass: String? = null
    private var currentTypeSymbol: TypeSymbol? = null

    override fun visitClassdef(ctx: Python3Parser.ClassdefContext) {
        val classNamePy = ctx.NAME().text
        if (classNamePy !in classesToVisit) return

        val className = TypeNames.typeNamesToPythonMap[classNamePy]
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
        val paramTypes = ctx.parameters().typedargslist().tfpdef().mapNotNull { it.test()?.text }
        println("Function definition of ${currentlyVisitedClass?.plus(".$functionName") ?: functionName}")

//        globalScope += currentTypeSymbol?.let {
//            ClassMethodSymbol(functionName)
//        } ?: MethodSymbol()

        super.visitFuncdef(ctx)
    }
}

package python

import com.kobra.Python3Parser
import com.kobra.Python3ParserBaseVisitor
import symtab.ClassMethodSymbol
import symtab.MethodSymbol
import symtab.Scope
import symtab.TypeSymbol
import type.TypeNames.typeNamesToPythonMap
import util.filterNonNull

// todo: fáj a szemem, ezt nagyon ki kell takarítani
class PythonLibVisitor(
    private val globalScope: Scope,
) : Python3ParserBaseVisitor<Unit>() {
    private val classesToVisit = listOf("int", "float", "str", "bool", "list", "range")
    private var currentlyVisitedClass: String? = null
    private var currentTypeSymbol: TypeSymbol? = null

    override fun visitClassdef(ctx: Python3Parser.ClassdefContext) {
        val classNamePy = ctx.NAME().text
        if (classNamePy !in classesToVisit) return

        val className = typeNamesToPythonMap[classNamePy]
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
                ctx.parameters()?.typedargslist()?.tfpdef()?.filter { it.NAME()?.text != "self" }
                    ?.mapNotNull { it.NAME()?.text to it.test()?.text }
                    ?.toMap()?.mapValues {
                        it.value?.let { typeNamePy ->
                            val typeName = typeNamesToPythonMap[typeNamePy]!!
                            globalScope.resolveType(typeName)
                                ?: throw RuntimeException("Can't find symbol for type '$typeName' in global scope")
                        }
                    }?.filterNonNull() ?: emptyMap()
            } catch (e: Exception) {
                return
            }

        val returnType = ctx.test().or_test(0).and_test(0).not_test(0).comparison().expr(0)
            .xor_expr(0).and_expr(0).shift_expr(0).arith_expr(0).term(0).factor(0).power().atom_expr()
            .atom().NAME()?.text?.let {
                if (it !in classesToVisit) return
                typeNamesToPythonMap[it]
            }

        println("Function definition of ${currentlyVisitedClass?.plus(".$functionName") ?: functionName}")

        globalScope += currentTypeSymbol?.let {
            ClassMethodSymbol(functionName, returnType, params, currentTypeSymbol!!)
        } ?: MethodSymbol(functionName, returnType, params)

        super.visitFuncdef(ctx)
    }
}

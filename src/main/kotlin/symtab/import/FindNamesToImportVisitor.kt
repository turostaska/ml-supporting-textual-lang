package symtab.import

import com.kobra.Python3Lexer
import com.kobra.Python3Parser
import com.kobra.Python3Parser.File_inputContext
import com.kobra.Python3Parser.For_stmtContext
import com.kobra.Python3ParserBaseVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import python.atomExpr
import python.testListComp
import util.splitOnWhitespaces

private fun For_stmtContext.isDirCall() = (testlist()?.atomExpr()?.atom()?.NAME()?.text == "dir")
private fun For_stmtContext.isNotDirCall() = !isDirCall()

private fun findNamesFromRule(
    moduleName: String,
    rootRule: File_inputContext,
): List<String> {
    val visitor = FindNamesToImportVisitor(moduleName).apply {
        visit(rootRule)
    }
    return visitor.names.toList()
}

fun findNamesForModule(qualifier: String): List<String> {
    val sourceCode = getSourceOfHierarchicalPackage(qualifier)
    val lexer = Python3Lexer(CharStreams.fromString(sourceCode))
    val tokens = CommonTokenStream(lexer)
    val rootRule = Python3Parser(tokens).file_input()

    return findNamesFromRule(qualifier, rootRule)
        // todo: .map { it.removeSuffix("$qualifier.") }
}

class FindNamesToImportVisitor(
    val moduleName: String,
): Python3ParserBaseVisitor<Unit>() {
    val names = mutableSetOf<String>()

    override fun visitFor_stmt(
        ctx: For_stmtContext,
    ): Unit = ctx.run {
        // The dir function returns all names in the scope
        if (this.isNotDirCall())
            return

        // Make sure that the statements contain one that appends items to __all__
        val statements = this.suite(0).text.splitOnWhitespaces()
        if (statements.none { "__all__.append" in it })
            return

        val submoduleName = this.testlist()?.atomExpr()!!.trailer(0).text.trim('(', ')')
        val moduleToVisit = "$moduleName.$submoduleName"
        val namesToImport = findNamesForModule(moduleToVisit)

        names += namesToImport

        super.visitFor_stmt(this)
    }

    // todo: __all__.extend(['e', 'pi', 'nan', 'inf'])

    override fun visitExpr_stmt(ctx: Python3Parser.Expr_stmtContext): Unit = ctx.run {
        /* The __all__ list enumerates symbols which should be imported
           'import module_name' imports all symbols present and those listed in __all__ to a new scope
           'from module_name import *' imports only those listed in __all__ to the global scope
         */
        if (testlist_star_expr()?.firstOrNull()?.text != "__all__")
            return

        val namesToImport = this.testListComp()?.test()?.map { it.text }
            ?.map { it.removeSurrounding("'") }
            ?.filter { !it.startsWith("_") }
            ?.map { "$moduleName.$it" }
                ?: emptyList()
        names += namesToImport
    }
}

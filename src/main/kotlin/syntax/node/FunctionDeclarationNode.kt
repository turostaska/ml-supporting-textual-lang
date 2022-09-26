package syntax.node

import com.kobra.kobraParser.FunctionDeclarationContext
import com.kobra.kobraParser.StatementContext
import symtab.extensions.functionName
import symtab.extensions.params
import symtab.extensions.returnTypeName
import symtab.extensions.statements
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode
import type.TypeNames
import util.getKey
import util.joinToCodeWithTabToAllLinesButFirst

class FunctionDeclarationNode(
    functionCtx: FunctionDeclarationContext,
    parent: SyntaxTreeNode
): SyntaxTreeNode(parent) {
    private val functionName = functionCtx.functionName
    private val statements = functionCtx.statements
    private val returnTypeName = functionCtx.returnTypeName
    private val returnTypeNamePy = TypeNames.pythonTypeNamesToKobraMap.getKey(returnTypeName)
    private val params = functionCtx.params

    override fun toCode(): String {
        // todo: params
        // todo: statements as nodes
        return """
            |def $functionName() -> $returnTypeNamePy:
            |    ${statements.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() } }
        """.trimMargin()
    }

    private val statementsToCode
        get() = if (statements.any()) statements.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() } else "pass"

    private fun StatementContext.toCode() = this.expression()?.toPythonCode() ?: ""
}
package syntax.node

import com.kobra.kobraParser.FunctionDeclarationContext
import com.kobra.kobraParser.StatementContext
import symtab.MethodSymbol
import symtab.extensions.statements
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode
import type.TypeNames
import util.getKey
import util.joinToCodeWithTabToAllLinesButFirst

class FunctionDeclarationNode(
    functionCtx: FunctionDeclarationContext,
    parent: SyntaxTreeNode,
    methodSymbol: MethodSymbol,
): SyntaxTreeNode(parent) {
    private val functionName = methodSymbol.name
    private val statements = functionCtx.statements // FIXME
    private val returnTypeName = methodSymbol.returnTypeName
    // todo: MethodSymbol's return type should be a TypeSymbol
    private val returnTypeNamePy = TypeNames.pythonTypeNamesToKobraMap.getKey(returnTypeName)
    private val params = methodSymbol.params

    override fun toCode(): String {
        // todo: statements as nodes
        return """
            |def $functionName($paramsToCode) -> $returnTypeNamePy:
            |    ${statements.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() } }
        """.trimMargin()
    }

    private val paramsToCode
        get() = params.map { (k, v) -> "$k: ${v.referencedType.pythonName}" }.joinToString()

    private val statementsToCode
        get() = if (statements.any()) statements.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() } else "pass"

    private fun StatementContext.toCode() = this.expression()?.toPythonCode() ?: ""
}
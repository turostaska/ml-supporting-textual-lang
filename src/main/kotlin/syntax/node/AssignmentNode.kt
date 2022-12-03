package syntax.node

import com.kobra.kobraParser
import symtab.VariableSymbol
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode

class AssignmentNode(
    private val symbol: VariableSymbol,
    private val rhsExpression: kobraParser.ExpressionContext,
    parent: SyntaxTreeNode,
) : SyntaxTreeNode(_parent = parent) {
    override fun toCode() = "${symbol.pythonSymbolName} = $value"

    private val value = rhsExpression.toPythonCode()
}

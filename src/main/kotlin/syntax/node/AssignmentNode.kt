package syntax.node

import com.kobra.kobraParser
import symtab.VariableSymbol
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode

class AssignmentNode(
    private val symbol: VariableSymbol,
    private val receivers: List<String>,
    private val rhsExpression: kobraParser.ExpressionContext,
    parent: SyntaxTreeNode,
) : SyntaxTreeNode(_parent = parent) {
    override fun toCode() = "$receiverCode${symbol.pythonSymbolName} = $value"

    private val receiverCode = if (receivers.isEmpty()) ""
        else receivers.joinToString(".", postfix = ".")

    private val value = rhsExpression.toPythonCode()
}

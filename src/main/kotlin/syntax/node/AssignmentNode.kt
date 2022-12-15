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
    private val rhsIsOnSelf: Boolean = false,
) : SyntaxTreeNode(_parent = parent) {
    override fun toCode() = "$receiverCode${symbol.pythonSymbolName} = ${rhsReceiver}$value"

    private val rhsReceiver = if (rhsIsOnSelf) "self." else ""

    private val receiverCode: String = when {
        symbol.isMember -> "self."
        receivers.isEmpty() -> ""
        else -> receivers.joinToString(".", postfix = ".")
    }

    private val value = rhsExpression.toPythonCode()
}

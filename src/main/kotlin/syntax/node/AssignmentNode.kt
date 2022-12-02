package syntax.node

import symtab.VariableSymbol
import syntax.SyntaxTreeNode

class AssignmentNode(
    private val symbol: VariableSymbol,
    private val value: String,
    parent: SyntaxTreeNode,
) : SyntaxTreeNode(_parent = parent) {
    override fun toCode() = "${symbol.pythonSymbolName} = $value"
}

package syntax.node

import syntax.SyntaxTreeNode

sealed class StatementNode(parent: SyntaxTreeNode): SyntaxTreeNode(parent)

class ReturnStatementNode(
    parent: SyntaxTreeNode,
    val returnStatement: String,
): StatementNode(parent) {
    override fun toCode() = "return $returnStatement"
}

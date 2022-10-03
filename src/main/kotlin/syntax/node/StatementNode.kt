package syntax.node

import symtab.TypeSymbol
import syntax.SyntaxTreeNode

sealed class StatementNode(parent: SyntaxTreeNode): SyntaxTreeNode(parent)

class ReturnStatementNode(
    parent: SyntaxTreeNode,
    val returnStatement: String,
): StatementNode(parent) {
    override fun toCode() = "return $returnStatement"
}

class AssignmentStatementNode(
    parent: SyntaxTreeNode,
    val name: String,
    val type: TypeSymbol,
    val value: String,
): StatementNode(parent) {
    override fun toCode() = "$name: ${type.referencedType.pythonName} = $value"
}

class ExpressionStatementNode(
    parent: SyntaxTreeNode,
    val statement: String,
): StatementNode(parent) {
    override fun toCode() = statement
}

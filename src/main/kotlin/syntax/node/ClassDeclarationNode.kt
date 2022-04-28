package syntax.node

import syntax.SyntaxTreeNode

class ClassDeclarationNode(
    parent: SyntaxTreeNode,
): SyntaxTreeNode(_parent = parent) {
    override fun toCode(): String {
        TODO("Not yet implemented")
    }
}

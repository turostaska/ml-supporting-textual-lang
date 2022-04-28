package syntax.node

import syntax.SyntaxTreeNode

class RootNode: SyntaxTreeNode(
    _parent = null,
) {
    override fun toCode() = ""
}

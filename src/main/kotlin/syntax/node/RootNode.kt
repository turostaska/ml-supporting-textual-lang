package syntax.node

import syntax.SyntaxTreeNode

class RootNode: SyntaxTreeNode(
    _parent = null,
) {
    override fun toCode() = ""

    override fun appendCodeTo(sb: StringBuilder, indent: Int) {
        this._children.forEach {
            it.appendCodeTo(sb, 0)
        }
    }
}

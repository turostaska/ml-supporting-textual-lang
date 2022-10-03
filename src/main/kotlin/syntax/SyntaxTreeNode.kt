package syntax

import util.prependTab

abstract class SyntaxTreeNode(
    private var _parent: SyntaxTreeNode? = null,
) {
    protected val _children: MutableList<SyntaxTreeNode> = mutableListOf()

    init {
        _parent?._children?.add(this)
    }

    val parent get() = _parent
    val children get() = _children.toList()

    fun addChild(vararg children: SyntaxTreeNode) {
        this._children.addAll(children)
        children.forEach { it._parent = this }
    }

    open fun appendCodeTo(sb: StringBuilder, indent: Int = 0) {
        sb.append(this.toCode().prependTab(indent))
        sb.appendLine(System.lineSeparator())

        this._children.forEach {
            it.appendCodeTo(sb, indent + 1)
        }
    }

    abstract fun toCode(): String
}

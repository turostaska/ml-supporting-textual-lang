package syntax

import syntax.node.RootNode

class SyntaxTree {
    val root = RootNode()

    fun generateCode(): String = StringBuilder().also {
        root.appendCodeTo(it)
    }.toString()
}

package syntax.node

import syntax.SyntaxTreeNode

class ImportHeaderNode(
    val moduleName: String,
    val importAlias: String? = null,
    parent: SyntaxTreeNode,
): SyntaxTreeNode(_parent = parent) {
    override fun toCode() = "import $moduleName" + importAlias?.let { " as $it" }.orEmpty()
}
// todo: code generation a member függvényre
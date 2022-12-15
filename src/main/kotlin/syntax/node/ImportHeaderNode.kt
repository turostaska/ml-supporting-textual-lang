package syntax.node

import syntax.SyntaxTreeNode

class ImportHeaderNode(
    val moduleName: String,
    val importAlias: String? = null,
    parent: SyntaxTreeNode,
): SyntaxTreeNode(_parent = parent) {
    override fun toCode() = (if (this.importsClass)
            "from $moduleNameBeforeLastSegment import $moduleNameLastSegment"
        else "import $moduleName") + importAlias?.let { " as $it" }.orEmpty()

    private val moduleNameLastSegment = moduleName.split('.').last()
    private val moduleNameBeforeLastSegment = moduleName.substringBeforeLast('.')
    private val importsClass = moduleNameLastSegment.first().isUpperCase()
}

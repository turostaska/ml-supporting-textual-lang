package syntax.node

import symtab.Symbol
import syntax.SyntaxTreeNode

open class PropertyDeclarationNode(
    private val symbol: Symbol,
    protected val value: String?, // todo
    parent: SyntaxTreeNode,
): SyntaxTreeNode(_parent = parent) {
    override fun toCode() = if (isMutable) """
        |$name: ${type.pythonName} = $value
    """.trimMargin() else """
        |__$name: ${type.pythonName} = $value
        |def _$name():
        |   return __$name
    """.trimMargin()

    protected val isMutable = this.symbol.isMutable
    protected val type = this.symbol.type
    protected val name = this.symbol.name
}

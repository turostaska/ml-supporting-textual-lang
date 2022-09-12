package syntax.node

import symtab.VariableSymbol
import syntax.SyntaxTreeNode
import type.Type

open class PropertyDeclarationNode(
    private val symbol: VariableSymbol,
    protected val value: String?, // todo
    private val type: Type,
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
    val name = this.symbol.name
}

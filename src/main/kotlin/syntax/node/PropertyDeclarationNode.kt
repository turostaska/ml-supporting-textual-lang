package syntax.node

import symtab.VariableSymbol
import symtab.pythonName
import syntax.SyntaxTreeNode

open class PropertyDeclarationNode(
    private val symbol: VariableSymbol,
    protected val value: String,
    parent: SyntaxTreeNode,
): SyntaxTreeNode(_parent = parent) {
    override fun toCode() = "$name: $pythonTypeName = $value"

    protected val pythonTypeName = this.symbol.typeSymbol.pythonName()

    protected val isMutable = this.symbol.isMutable
    val name = this.symbol.name
}

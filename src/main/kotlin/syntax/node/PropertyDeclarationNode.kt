package syntax.node

import com.kobra.kobraParser.ExpressionContext
import symtab.VariableSymbol
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode
import type.Type

open class PropertyDeclarationNode(
    private val symbol: VariableSymbol,
    protected val value: ExpressionContext?,
    private val type: Type,
    parent: SyntaxTreeNode,
): SyntaxTreeNode(_parent = parent) {
    override fun toCode() = """
        |$name: ${type.pythonName} = ${value?.toPythonCode() ?: throw RuntimeException("Property has no value")}
    """.trimMargin()

    protected val isMutable = this.symbol.isMutable
    val name = this.symbol.name
}

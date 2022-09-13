package syntax.node

import com.kobra.kobraParser
import symtab.VariableSymbol
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode

class AssignmentNode(
    private val symbol: VariableSymbol,
    private val value: kobraParser.ExpressionContext,
    parent: SyntaxTreeNode,
) : SyntaxTreeNode(_parent = parent) {
    override fun toCode() = """
        |${symbol.name} = ${value.toPythonCode()}
    """.trimMargin()
}
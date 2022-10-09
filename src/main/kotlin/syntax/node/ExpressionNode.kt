package syntax.node

import com.kobra.kobraParser.ExpressionContext
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode

class ExpressionNode(
    parent: SyntaxTreeNode,
    val ctx: ExpressionContext,
): SyntaxTreeNode(parent) {
    override fun toCode() = ctx.toPythonCode()
}

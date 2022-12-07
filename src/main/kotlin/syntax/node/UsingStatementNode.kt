package syntax.node

import com.kobra.kobraParser.UsingStatementContext
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode
import util.joinToCodeWithTabToAllLinesButFirst
import util.prependTab

class UsingStatementNode(
    parent: SyntaxTreeNode,
    private val ctx: UsingStatementContext,
): SyntaxTreeNode(parent) {
    override fun toCode(): String {
        return """
            |with (${ctx.expression().toPythonCode()}):
            |    $statementsCode
        """.trimMargin()
    }

    private val statementsCode get() = if (this.children.any())
        this.children.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() }
    else "pass"

    override fun appendCodeTo(sb: StringBuilder, indent: Int) {
        sb.append(this.toCode().prependTab(indent))
        sb.appendLine(System.lineSeparator())
    }
}

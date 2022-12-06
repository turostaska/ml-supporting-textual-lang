package syntax.node

import com.kobra.kobraParser.ForStatementContext
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode
import util.joinToCodeWithTabToAllLinesButFirst
import util.prependTab

class ForStatementNode(
    parent: SyntaxTreeNode,
    private val ctx: ForStatementContext,
): SyntaxTreeNode(parent) {
    private val loopVariable = ctx.variableDeclaration().simpleIdentifier().text

    private val range = ctx.expression().toPythonCode()

    private val statementsCode get() = if (this.children.any())
        this.children.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() }
    else "pass"

    override fun toCode(): String = """
        |for $loopVariable in ($range):
        |    $statementsCode
    """.trimMargin()

    override fun appendCodeTo(sb: StringBuilder, indent: Int) {
        sb.append(this.toCode().prependTab(indent))
        sb.appendLine(System.lineSeparator())
    }
}
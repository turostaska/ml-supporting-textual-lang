package syntax.node

import symtab.MethodSymbol
import syntax.SyntaxTreeNode
import util.joinToCodeWithTabToAllLinesButFirst
import util.prependTab

class FunctionDeclarationNode(
    parent: SyntaxTreeNode,
    methodSymbol: MethodSymbol,
): SyntaxTreeNode(parent) {
    private val functionName = methodSymbol.pythonSymbolName
    private val returnTypeName = methodSymbol.returnTypeName
    private val returnTypeNamePy = methodSymbol.returnType?.referencedType?.pythonName ?: returnTypeName
    private val params = methodSymbol.params
    private val isClassMethod = parent is ClassDeclarationNode

    override fun toCode() = """
        |def $functionName($selfParam $paramsToCode) -> $returnTypeNamePy:
        |    $statementsToCode
    """.trimMargin()

    override fun appendCodeTo(sb: StringBuilder, indent: Int) {
        sb.append(this.toCode().prependTab(indent))
        sb.appendLine(System.lineSeparator())
    }

    private val selfParam = if (isClassMethod) "self," else ""

    private val paramsToCode
        get() = params.map { (k, v) -> "$k: ${v.first().referencedType.pythonName}" }.joinToString()

    private val statementsToCode
        get() = if (children.any()) children.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() } else "pass"

}
package syntax.node

import symtab.MethodSymbol
import syntax.SyntaxTreeNode
import type.TypeNames
import util.getKey
import util.joinToCodeWithTabToAllLinesButFirst
import util.prependTab

class FunctionDeclarationNode(
    parent: SyntaxTreeNode,
    methodSymbol: MethodSymbol,
): SyntaxTreeNode(parent) {
    private val functionName = methodSymbol.name
    private val returnTypeName = methodSymbol.returnTypeName
    // todo: MethodSymbol's return type should be a TypeSymbol
    private val returnTypeNamePy = TypeNames.pythonTypeNamesToKobraMap.getKey(returnTypeName)
    private val params = methodSymbol.params

    override fun toCode() = """
        |def $functionName($paramsToCode) -> $returnTypeNamePy:
        |    $statementsToCode
    """.trimMargin()

    override fun appendCodeTo(sb: StringBuilder, indent: Int) {
        sb.append(this.toCode().prependTab(indent))
        sb.appendLine(System.lineSeparator())
    }

    private val paramsToCode
        get() = params.map { (k, v) -> "$k: ${v.first().referencedType.pythonName}" }.joinToString()

    private val statementsToCode
        get() = if (children.any()) children.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() } else "pass"

}
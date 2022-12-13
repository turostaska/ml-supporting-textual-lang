package syntax.node

import symtab.MethodSymbol
import symtab.TypeSymbol
import symtab.pythonName
import syntax.SyntaxTreeNode
import type.TypeNames
import util.getKey
import util.joinToCodeWithTabToAllLinesButFirst
import util.prependTab

class FunctionDeclarationNode(
    parent: SyntaxTreeNode,
    methodSymbol: MethodSymbol,
    private val isOneLiner: Boolean,
): SyntaxTreeNode(parent) {
    private val functionName = methodSymbol.pythonSymbolName
    private val returnTypeName = methodSymbol.returnType?.name ?: "Unit"
    private val returnTypeNamePy = TypeNames.pythonTypeNamesToKobraMap.getKey(returnTypeName)
        ?: methodSymbol.returnType?.pythonSymbolName
        ?: returnTypeName

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
        get() = params.map { (k, v) -> "$k: ${v.first().pythonName()}" }.joinToString()

    private val statementsToCode
        get() = if (children.any()) {
            if (isOneLiner) "return ${children.first().toCode()}"
            else children.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() }
        } else "pass"

}

private fun TypeSymbol.pythonName(): String {
    return TypeNames.pythonTypeNamesToKobraMap.getKey(this.name)
        ?: this.pythonSymbolName
        ?: this.name
}

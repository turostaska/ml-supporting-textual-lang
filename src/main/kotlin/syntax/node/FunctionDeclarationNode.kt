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
    private val receiver: TypeSymbol? = null,
): SyntaxTreeNode(parent) {
    private val functionName = methodSymbol.pythonSymbolName
    private val returnTypeName = methodSymbol.returnType?.name ?: "Unit"
    private val returnTypeNamePy = TypeNames.pythonTypeNamesToKobraMap.getKey(returnTypeName)
        ?: methodSymbol.returnType?.pythonSymbolName
        ?: returnTypeName

    private val params = methodSymbol.params
    private val isClassMethod = parent is ClassDeclarationNode

    private val receiverParam = receiver?.let { "this: ${it.pythonName()}," }

    override fun toCode() = """
        |def $functionName($selfParam $paramsToCode) -> $returnTypeNamePy:
        |    $statementsToCode
        |$monkeyPatch
    """.trimMargin()

    override fun appendCodeTo(sb: StringBuilder, indent: Int) {
        sb.append(this.toCode().prependTab(indent))
        sb.appendLine(System.lineSeparator())
    }

    private val monkeyPatch: String = receiver?.let {
        "${receiver.pythonName()}.$functionName = $functionName"
    }.orEmpty()

    private val selfParam = if (isClassMethod) "self," else ""

    private val paramsToCode: String
        get() {
            return params
                .map { (k, v) -> "$k: ${v.first().pythonName()}" }
                .toMutableList().apply { if (receiverParam != null) add(0, receiverParam) }
                .joinToString()
        }

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

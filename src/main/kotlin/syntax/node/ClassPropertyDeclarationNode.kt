package syntax.node

import symtab.VariableSymbol
import syntax.SyntaxTreeNode
import type.Type
import util.appendIf

class ClassPropertyDeclarationNode(
    symbol: VariableSymbol,
    value: String? = null,
    type: Type,
    parent: SyntaxTreeNode,
    val isConstructorParameter: Boolean = true,
) : PropertyDeclarationNode(symbol, value, type, parent) {
    override fun toCode(): String {
        throw RuntimeException("toCode() shouldn't be used on an instance of ClassPropertyDeclarationNode")
    }

    fun toMemberDeclaration() = """
        |self._$name = ${if (isConstructorParameter) name else value}
    """.trimMargin()

    fun toPropertyCode() = """
        |@property
        |def $name(self):
        |    return self._$name
        |    
    """.appendIf(isMutable) { """
        |@$name.setter
        |def $name(self, value):
        |    self._$name = value
        |    
    """ }.trimMargin()
}

package syntax.node

import symtab.Symbol
import syntax.SyntaxTreeNode
import util.appendIf

class ClassPropertyDeclarationNode(
    symbol: Symbol,
    value: String? = null,
    parent: SyntaxTreeNode,
    val isConstructorParameter: Boolean = true,
) : PropertyDeclarationNode(symbol, value, parent) {
    override fun toCode(): String {
        throw RuntimeException("toCode() shouldn't be used on an instance of ClassPropertyDeclarationNode")
    }

    fun toMemberDeclaration() = """
        |self.$name = ${if (isConstructorParameter) name else value}
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

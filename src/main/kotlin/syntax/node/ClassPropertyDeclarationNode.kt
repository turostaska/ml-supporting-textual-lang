package syntax.node

import com.kobra.kobraParser.ExpressionContext
import symtab.VariableSymbol
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode
import type.Type
import util.appendIf

class ClassPropertyDeclarationNode(
    symbol: VariableSymbol,
    value: ExpressionContext? = null,
    type: Type,
    parent: SyntaxTreeNode,
    val isConstructorParameter: Boolean = true,
) : PropertyDeclarationNode(symbol, value, type, parent) {
    override fun toCode(): String {
        throw RuntimeException("toCode() shouldn't be used on an instance of ClassPropertyDeclarationNode")
    }

    fun toMemberDeclaration() = """
        |self._$name = ${
        if (isConstructorParameter)
            name
        else value?.toPythonCode() ?: throw RuntimeException("Property has no value")
    }
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

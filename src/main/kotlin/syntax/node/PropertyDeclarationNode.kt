package syntax.node

import symtab.Symbol
import syntax.SyntaxTreeNode
import type.util.appendIf

// todo: külön osztály class propertyknek és simáknak
class PropertyDeclarationNode(
    private val symbol: Symbol,
    private val value: String, // todo
    parent: SyntaxTreeNode,
): SyntaxTreeNode(_parent = parent) {
    override fun toCode(): String {
        require(!isClassProperty) { "Property '$name' seems to be a class property, use toMemberDeclarationCode() instead." }
        return """
            |__$name: ${type.pythonName} = $value
            |def _$name():
            |   return __$name
        """.trimMargin()
    }


    fun toMemberDeclaration(): String {
        require(isClassProperty) { "Property '$name' seems to be a class property, use toMemberDeclarationCode() instead." }
        return """
            |
        """.trimMargin()
    }

    fun toPropertyCode(): String {
        require(isClassProperty) { "Property '$name' does not seem to be a class property, use toCode() instead." }
        return """
            |@property
            |def $name(self):
            |   return self._$name
            |   
        """.appendIf(isMutable) { """
            |@$name.setter
            |def $name(self, value):
            |    self._$name = value
            |    
        """ }.trimMargin()
    }

    private val isClassProperty get() = (this.parent is ClassDeclarationNode)
    private val isMutable = this.symbol.isMutable
    private val type = this.symbol.type
    private val name = this.symbol.name
}

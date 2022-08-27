package syntax.node

import com.kobra.kobraParser
import symtab.extensions.className
import symtab.extensions.superClasses
import syntax.SyntaxTreeNode
import util.joinToCodeWithTabToAllLinesButFirst
import util.prependTab
import util.prependTabToAllLinesButFirst
import util.takeIf

class ClassDeclarationNode(
    ctx: kobraParser.ClassDeclarationContext,
    parent: SyntaxTreeNode,
): SyntaxTreeNode(_parent = parent) {
    private val name = ctx.className
    private val superClasses = ctx.superClasses
    private val members get() = children.filterIsInstance<ClassPropertyDeclarationNode>()
    private val constructorParameterMembers get() = members.filter { it.isConstructorParameter }

    // todo: check member functions
    private val isEmpty get() = members.isEmpty()

    // todo: static fields
    override fun toCode(): String {
        return """
            |class $name $superClassesCode:
            |    $emptyClassPassStatement
            |    ${constructorCode.prependTabToAllLinesButFirst(1)}
            |    ${propertyDeclarationCode.prependTabToAllLinesButFirst(1)}
        """.trimMargin()
    }

    override fun appendCodeTo(sb: StringBuilder, indent: Int) {
        sb.append(this.toCode().prependTab(indent))
        sb.appendLine(System.lineSeparator())
    }

    private val emptyClassPassStatement get() = takeIf(isEmpty) { """
        |pass
    """.trimMargin()
    }

    private val superClassesCode get() = takeIf(superClasses.any()) {
        superClasses.joinToString(prefix = "(", postfix = ")")
    }

    // todo: constructor parameters
    private val constructorCode get() = takeIf(members.any()) { """
            |def __init__(self, $constructorParameters):
            |    ${members.joinToCodeWithTabToAllLinesButFirst(1) { it.toMemberDeclaration() }}
            |    
        """.trimMargin()
    }

    private val constructorParameters get() = """
        |${constructorParameterMembers.joinToString(separator = ", ") { it.name }}
    """.trimMargin()

    private val propertyDeclarationCode get() = """
        |${members.joinToString(separator = System.lineSeparator()) { it.toPropertyCode() }}
        |
    """.trimMargin()
}

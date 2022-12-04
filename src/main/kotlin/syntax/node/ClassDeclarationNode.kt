package syntax.node

import com.kobra.kobraParser
import symtab.extensions.className
import symtab.extensions.superClasses
import syntax.SyntaxTreeNode
import syntax.expression.toPythonCode
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
    private val initStatements get() = children.filterIsInstance<ExpressionNode>()
    private val constructorParameterMembers get() = members.filter { it.isConstructorParameter }
    private val constructorParameters = ctx.primaryConstructor()?.classParameters()?.classParameter()?.map {
        it.simpleIdentifier().text
    } ?: emptyList()
    private val classMemberDeclarations: List<Pair<String, String?>> =
        ctx.classBody()?.classMemberDeclarations()?.classMemberDeclaration()?.filter {
            it.declaration()?.propertyDeclaration() != null
        }?.map {
            val name = it.declaration().propertyDeclaration().simpleIdentifier().text
            val rhs = it.declaration().propertyDeclaration()?.expression()?.toPythonCode()

            name to rhs
        } ?: emptyList()

    // todo: check member functions
    private val isEmpty get() = members.isEmpty() && superClasses.isEmpty()

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
        sb.appendLine()
    }

    private val emptyClassPassStatement get() = takeIf(isEmpty) { """
        |pass
    """.trimMargin()
    }

    private val classMemberDeclarationsCode get() = classMemberDeclarations.joinToCodeWithTabToAllLinesButFirst(1) {
        (lhs, rhs) -> rhs?.let {
            "_$lhs = $rhs"
        } ?: "_$lhs"
    }

    private val superClassesCode get() = takeIf(superClasses.any()) {
        superClasses.joinToString(prefix = "(", postfix = ")")
    }

    // todo: constructor parameters
    private val constructorCode get() = takeIf(!isEmpty) { """
            |def __init__(self, $constructorParametersCode):
            |    $superCall
            |    ${constructorParameterMembers.joinToCodeWithTabToAllLinesButFirst(1) { it.toMemberDeclaration() }}
            |    $classMemberDeclarationsCode
            |    $initCalls
            |    
        """.trimMargin()
    }

    private val superCall get() = if (superClasses.any()) { """
        |super().__init__($superCallParams)
    """.trimMargin() } else "pass"

    private val superCallParams = ctx.delegationSpecifiers()?.delegationSpecifier()?.firstOrNull()
        ?.constructorInvocation()?.valueArguments()?.valueArgument()?.joinToString { it.expression().toPythonCode() }
        ?: ""

    private val initCalls get() = if (initStatements.any()) """
        |${initStatements.joinToCodeWithTabToAllLinesButFirst(1) { it.toCode() } }
    """.trimMargin() else "pass"

    private val constructorParametersCode get() = """
        |${constructorParameters.joinToString(separator = ", ") { it }}
    """.trimMargin()

    private val propertyDeclarationCode get() = """
        |${members.joinToString(separator = System.lineSeparator()) { it.toPropertyCode() }}
        |
    """.trimMargin()
}

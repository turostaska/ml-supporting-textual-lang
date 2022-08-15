package syntax

import com.kobra.kobraBaseVisitor
import com.kobra.kobraParser
import symtab.Scope
import symtab.extensions.className
import symtab.extensions.isMember
import syntax.node.ClassDeclarationNode
import syntax.node.ClassPropertyDeclarationNode
import syntax.node.PropertyDeclarationNode

// feltételezzük, hogy az ast helyes
class SyntaxTreeBuilderVisitor(
    private val globalScope: Scope,
): kobraBaseVisitor<Any>() {
    val syntaxTree = SyntaxTree()
    private lateinit var currentNode: SyntaxTreeNode
    private lateinit var currentScope: Scope // todo: kipróbálni nélküle

    override fun visitProgram(ctx: kobraParser.ProgramContext): Any? {
        currentNode = syntaxTree.root
        currentScope = globalScope
        return super.visitProgram(ctx)
    }

    override fun visitPropertyDeclaration(ctx: kobraParser.PropertyDeclarationContext): Any? {
        val name = ctx.simpleIdentifier().text
        val symbol = currentScope[name] ?: throw RuntimeException("Symbol '$name' not found.")
        val value = ctx.expression().text

        if (currentScope.name.contains("Class declaration"))
            ClassPropertyDeclarationNode(symbol, value, currentNode, false).let { currentNode.addChild(it) }
        else
            PropertyDeclarationNode(symbol, value, currentNode).let { currentNode.addChild(it) }

        return super.visitPropertyDeclaration(ctx)
    }

    override fun visitClassDeclaration(ctx: kobraParser.ClassDeclarationContext): Any? {
        ClassDeclarationNode(ctx, currentNode).let {
            currentNode.addChild(it)
            currentNode = it
        }
        currentScope = currentScope.children.first { it.name == "Class declaration of ${ctx.className}" }
        super.visitClassDeclaration(ctx).also {
            currentScope = currentScope.parent!!
            currentNode = currentNode.parent!!
            return it
        }
    }

    override fun visitPrimaryConstructor(ctx: kobraParser.PrimaryConstructorContext): Any? {
        currentScope = currentScope.children.first { it.name == "Primary constructor" }
        super.visitPrimaryConstructor(ctx).also {
            currentScope = currentScope.parent!!
            return it
        }
    }

    override fun visitClassParameter(ctx: kobraParser.ClassParameterContext): Any? = ctx.run {
        val name = simpleIdentifier().text
        val value = this.expression()?.text
        val symbol = currentScope[name] ?: throw RuntimeException("Symbol '$name' not found.")

        if (isMember) {
            currentNode.addChild(ClassPropertyDeclarationNode(symbol, value, currentNode))
        } else {
            // todo: constructor parameter
        }
        return super.visitClassParameter(this)
    }
}

fun SyntaxTreeBuilderVisitor.generateCode() = StringBuilder().also {
    this.syntaxTree.root.appendCodeTo(it)
}.toString()

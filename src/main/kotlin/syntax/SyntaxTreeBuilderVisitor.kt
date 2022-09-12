package syntax

import com.kobra.kobraBaseVisitor
import com.kobra.kobraParser
import symtab.Scope
import symtab.extensions.className
import symtab.extensions.isMember
import syntax.node.ClassDeclarationNode
import syntax.node.ClassPropertyDeclarationNode
import syntax.node.PropertyDeclarationNode
import type.TypeHierarchy
import type.util.find

// feltételezzük, hogy az ast helyes
class SyntaxTreeBuilderVisitor(
    private val globalScope: Scope,
    private val typeHierarchy: TypeHierarchy,
): kobraBaseVisitor<Unit>() {
    val syntaxTree = SyntaxTree()
    private lateinit var currentNode: SyntaxTreeNode
    private lateinit var currentScope: Scope // todo: kipróbálni nélküle

    override fun visitProgram(ctx: kobraParser.ProgramContext) {
        currentNode = syntaxTree.root
        currentScope = globalScope
        super.visitProgram(ctx)
    }

    override fun visitPropertyDeclaration(ctx: kobraParser.PropertyDeclarationContext) {
        val name = ctx.simpleIdentifier().text
        val symbol = currentScope.resolveVariable(name) ?: throw RuntimeException("Symbol '$name' not found.")
        val value = ctx.expression().text
        val type = typeHierarchy.find(symbol.type)

        if (currentScope.name.contains("Class declaration"))
            ClassPropertyDeclarationNode(symbol, value, type, currentNode, false).let {
                currentNode.addChild(it)
            }
        else
            PropertyDeclarationNode(symbol, value, type, currentNode).let { currentNode.addChild(it) }

        super.visitPropertyDeclaration(ctx)
    }

    override fun visitClassDeclaration(ctx: kobraParser.ClassDeclarationContext) {
        ClassDeclarationNode(ctx, currentNode).let {
            currentNode.addChild(it)
            currentNode = it
        }
        currentScope = currentScope.children.first { it.name == "Class declaration of ${ctx.className}" }
        super.visitClassDeclaration(ctx).also {
            currentScope = currentScope.parent!!
            currentNode = currentNode.parent!!
        }
    }

    override fun visitPrimaryConstructor(ctx: kobraParser.PrimaryConstructorContext) {
        currentScope = currentScope.children.first { it.name == "Primary constructor" }
        super.visitPrimaryConstructor(ctx).also {
            currentScope = currentScope.parent!!
        }
    }

    override fun visitClassParameter(ctx: kobraParser.ClassParameterContext): Unit = ctx.run {
        val name = simpleIdentifier().text
        val value = this.expression()?.text
        val symbol = currentScope.resolveVariable(name) ?: throw RuntimeException("Symbol '$name' not found.")
        val type = typeHierarchy.find(symbol.type)

        if (isMember) {
            currentNode.addChild(ClassPropertyDeclarationNode(symbol, value, type, currentNode))
        } else {
            // todo: constructor parameter
        }
        super.visitClassParameter(this)
    }
}

fun SyntaxTreeBuilderVisitor.generateCode() = StringBuilder().also {
    this.syntaxTree.root.appendCodeTo(it)
}.toString()

package syntax

import kobraBaseVisitor
import kobraParser.ProgramContext
import kobraParser.PropertyDeclarationContext
import symtab.Scope
import syntax.node.PropertyDeclarationNode

// feltételezzük, hogy az ast helyes
class SyntaxTreeBuilderVisitor(
    private val globalScope: Scope,
): kobraBaseVisitor<Any>() {
    val syntaxTree = SyntaxTree()
    private lateinit var currentNode: SyntaxTreeNode
    private lateinit var currentScope: Scope // todo: kipróbálni nélküle

    override fun visitProgram(ctx: ProgramContext): Any? {
        currentNode = syntaxTree.root
        currentScope = globalScope
        return super.visitProgram(ctx)
    }

    override fun visitPropertyDeclaration(ctx: PropertyDeclarationContext): Any? {
        val name = ctx.simpleIdentifier().text
        val symbol = currentScope[name] ?: throw RuntimeException("Symbol '$name' not found.")
        val value = ctx.expression().text

        currentNode.addChild(PropertyDeclarationNode(symbol, value, currentNode))
        return super.visitPropertyDeclaration(ctx)
    }
}

fun SyntaxTreeBuilderVisitor.generateCode() = StringBuilder().also {
    this.syntaxTree.root.appendCodeTo(it)
}.toString()

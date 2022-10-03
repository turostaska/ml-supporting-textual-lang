package syntax

import com.kobra.kobraBaseVisitor
import com.kobra.kobraParser
import symtab.ClassDeclarationScope
import symtab.FunctionScope
import symtab.PrimaryConstructorScope
import symtab.Scope
import symtab.extensions.className
import symtab.extensions.functionName
import symtab.extensions.isMember
import syntax.expression.toPythonCode
import syntax.node.*
import type.TypeHierarchy

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
        val value = ctx.expression().toPythonCode()

        when (currentScope) {
            is ClassDeclarationScope -> ClassPropertyDeclarationNode(symbol, value, currentNode, false)
            else -> PropertyDeclarationNode(symbol, value, currentNode)
        }

        super.visitPropertyDeclaration(ctx)
    }

    override fun visitAssignment(ctx: kobraParser.AssignmentContext): Unit = ctx.run {
        val symbol = currentScope.resolveVariable(ctx.identifier().text)!!
        val value = expression().toPythonCode()

        AssignmentNode(symbol, value, currentNode)

        super.visitAssignment(this)
    }

    override fun visitClassDeclaration(ctx: kobraParser.ClassDeclarationContext) {
        ClassDeclarationNode(ctx, currentNode).let {
            currentNode = it
        }
        currentScope = currentScope.children.first { it is ClassDeclarationScope && it.typeSymbol.name == ctx.className }
        super.visitClassDeclaration(ctx).also {
            currentScope = currentScope.parent!!
            currentNode = currentNode.parent!!
        }
    }

    override fun visitPrimaryConstructor(ctx: kobraParser.PrimaryConstructorContext) {
        currentScope = currentScope.children.first { it is PrimaryConstructorScope }
        super.visitPrimaryConstructor(ctx).also {
            currentScope = currentScope.parent!!
        }
    }

    override fun visitClassParameter(ctx: kobraParser.ClassParameterContext): Unit = ctx.run {
        val name = simpleIdentifier().text
        val value = this.expression()?.toPythonCode() ?: ""
        val symbol = currentScope.resolveVariable(name) ?: throw RuntimeException("Symbol '$name' not found.")

        if (isMember) {
            ClassPropertyDeclarationNode(symbol, value, currentNode)
        } else {
            // todo: constructor parameter
        }
        super.visitClassParameter(this)
    }

    override fun visitFunctionDeclaration(ctx: kobraParser.FunctionDeclarationContext): Unit = ctx.run {
        // todo: class methods
        val methodSymbol = currentScope.resolveMethod(functionName)!!

        FunctionDeclarationNode(currentNode, methodSymbol).let {
            currentNode = it
        }
        currentScope = currentScope.children.first {
            it is FunctionScope && it.name == "Function scope of ${methodSymbol.name}"
        }

        super.visitFunctionDeclaration(ctx)

        currentNode = currentNode.parent!!
        currentScope = currentScope.parent!!
    }

    override fun visitJumpExpression(ctx: kobraParser.JumpExpressionContext): Unit = ctx.run {
        // todo: ehhez nem kell a symtab?
        ReturnStatementNode(currentNode, this.expression().toPythonCode())

        super.visitJumpExpression(ctx)
    }
}

fun SyntaxTreeBuilderVisitor.generateCode() = StringBuilder().also {
    this.syntaxTree.root.appendCodeTo(it)
}.toString()

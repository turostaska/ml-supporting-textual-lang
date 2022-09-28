package syntax

import com.kobra.kobraBaseVisitor
import com.kobra.kobraParser
import symtab.Scope
import symtab.extensions.className
import symtab.extensions.functionName
import symtab.extensions.isMember
import syntax.node.*
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
        val value = ctx.expression()
        val type = typeHierarchy.find(symbol.type)

        if (currentScope.name.contains("Class declaration"))
            ClassPropertyDeclarationNode(symbol, value, type, currentNode, false).let {
                currentNode.addChild(it)
            }
        else PropertyDeclarationNode(symbol, value, type, currentNode).let { currentNode.addChild(it) }

        super.visitPropertyDeclaration(ctx)
    }

    override fun visitAssignment(ctx: kobraParser.AssignmentContext): Unit = ctx.run {
        val symbol = currentScope.resolveVariable(ctx.identifier().text)!!
        val value = expression()

        AssignmentNode(symbol, value, currentNode).let { currentNode.addChild(it) }

        super.visitAssignment(this)
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
        val value = this.expression()
        val symbol = currentScope.resolveVariable(name) ?: throw RuntimeException("Symbol '$name' not found.")
        val type = typeHierarchy.find(symbol.type)

        if (isMember) {
            currentNode.addChild(ClassPropertyDeclarationNode(symbol, value, type, currentNode))
        } else {
            // todo: constructor parameter
        }
        super.visitClassParameter(this)
    }

    override fun visitFunctionDeclaration(ctx: kobraParser.FunctionDeclarationContext): Unit = ctx.run {
        // todo: class methods
        val methodSymbol = currentScope.resolveMethod(functionName)!!

        currentNode.addChild(
            FunctionDeclarationNode(this, currentNode, methodSymbol)
        )

        super.visitFunctionDeclaration(ctx)
    }
}

fun SyntaxTreeBuilderVisitor.generateCode() = StringBuilder().also {
    this.syntaxTree.root.appendCodeTo(it)
}.toString()

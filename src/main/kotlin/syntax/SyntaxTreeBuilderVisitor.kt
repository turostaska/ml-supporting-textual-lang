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
import symtab.extensions.resolveTypeOrThrow
import syntax.expression.toPythonCode
import syntax.node.*

// feltételezzük, hogy az ast helyes
class SyntaxTreeBuilderVisitor(
    private val globalScope: Scope,
): kobraBaseVisitor<Unit>() {
    val syntaxTree = SyntaxTree()
    private lateinit var currentNode: SyntaxTreeNode
    private lateinit var currentScope: Scope

    override fun visitProgram(ctx: kobraParser.ProgramContext) {
        currentNode = syntaxTree.root
        currentScope = globalScope
        super.visitProgram(ctx)
    }

    override fun visitImportHeader(ctx: kobraParser.ImportHeaderContext): Unit = ctx.run {
        val moduleName = identifier().text
        val importAlias = importAlias()?.simpleIdentifier()?.text

        ImportHeaderNode(moduleName, importAlias, currentNode)

        super.visitImportHeader(ctx)
    }

    override fun visitPropertyDeclaration(ctx: kobraParser.PropertyDeclarationContext) {
        val name = ctx.simpleIdentifier().text
        val symbol = currentScope.resolveVariable(name) ?: throw RuntimeException("Symbol '$name' not found.")
        val value = ctx.expression().toPythonCode()

        when (currentScope) {
            is ClassDeclarationScope -> ClassPropertyDeclarationNode(symbol, value, currentNode, false)
            else -> PropertyDeclarationNode(symbol, value, currentNode)
        }
    }

    override fun visitAssignment(ctx: kobraParser.AssignmentContext): Unit = ctx.run {
        val symbol = currentScope.resolveVariable(ctx.identifier().text)!!
        val receivers = ctx.identifier().text.split(".", "::").dropLast(1)

        // ToDo: ez így nem túl jó
        val rhsIsOnSelf = expression()?.text?.let {
            it.substringBefore(".", it.substringBefore("("))
        }?.let {
            currentScope.resolveVariable(it)
        }?.isMember == true

        AssignmentNode(symbol, receivers, expression(), currentNode, rhsIsOnSelf)
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
    }

    override fun visitFunctionDeclaration(ctx: kobraParser.FunctionDeclarationContext): Unit = ctx.run {
        // todo: class methods
        val methodSymbol = currentScope.resolveMethod(functionName)!!
        val isOneLiner = this.functionBody()?.ASSIGNMENT() != null
        val receiverName = this.receiverType()?.simpleIdentifier()?.text
        val receiver = receiverName?.let { currentScope.resolveTypeOrThrow(receiverName) }

        FunctionDeclarationNode(currentNode, methodSymbol, isOneLiner, receiver).let {
            currentNode = it
        }
        currentScope = currentScope.children.first {
            it is FunctionScope && it.name == "Function scope of ${methodSymbol.name}"
        }

        super.visitFunctionDeclaration(ctx)

        currentNode = currentNode.parent!!
        currentScope = currentScope.parent!!
    }

    override fun visitExpression(ctx: kobraParser.ExpressionContext): Unit = ctx.run {
        ExpressionNode(currentNode, this)
    }

    override fun visitInitBlock(ctx: kobraParser.InitBlockContext): Unit = ctx.run {
        currentScope = currentScope.children.first { it is PrimaryConstructorScope }
        super.visitInitBlock(this)
        currentScope = currentScope.parent!!
    }

    override fun visitForStatement(ctx: kobraParser.ForStatementContext): Unit = ctx.run {
        currentNode = ForStatementNode(currentNode, this)
        currentScope = currentScope.getForStatementScope(ctx)!!
        super.visitControlStructureBody(this.controlStructureBody())
        currentScope = currentScope.parent!!
        currentNode = currentNode.parent!!
    }

    override fun visitUsingStatement(ctx: kobraParser.UsingStatementContext): Unit = ctx.run {
        currentNode = UsingStatementNode(currentNode, this)
        currentScope = currentScope.getUsingStatementScope(ctx)!!
        super.visitControlStructureBody(this.controlStructureBody())
        currentScope = currentScope.parent!!
        currentNode = currentNode.parent!!
    }

    override fun visitDelegationSpecifiers(ctx: kobraParser.DelegationSpecifiersContext) {}
}

fun SyntaxTreeBuilderVisitor.generateCode() = StringBuilder().also {
    this.syntaxTree.root.appendCodeTo(it)
}.toString()

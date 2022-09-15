package symtab

import com.kobra.kobraBaseVisitor
import com.kobra.kobraParser.*
import symtab.extensions.*
import type.TypeHierarchy
import type.util.asType
import type.util.contains
import type.util.find
import type.util.findInSubtree

// symtab a hibakeresőben vagy a kódgeneráló visitorban?
class SymtabBuilderVisitor: kobraBaseVisitor<Unit>() {
    lateinit var currentScope: Scope
    val globalScope = Scope(parent = null)
    val typeHierarchy = TypeHierarchy()
    private val typeInference = TypeInference(this)

    override fun visitProgram(ctx: ProgramContext) {
        currentScope = globalScope
        super.visitProgram(ctx)
    }

    override fun visitClassDeclaration(ctx: ClassDeclarationContext): Unit = ctx.run {
        if (className in typeHierarchy)
            throw RuntimeException("Redefinition of class '$className'")

        typeHierarchy.addType(className, baseClassNames = superClasses.toSet())

        currentScope = Scope(parent = currentScope, name = "Class declaration of $className")
        super.visitClassDeclaration(ctx).also {
            currentScope = currentScope.parent!!
        }
    }

    override fun visitPrimaryConstructor(ctx: PrimaryConstructorContext) {
        currentScope = Scope(parent = currentScope, name = "Primary constructor")
        super.visitPrimaryConstructor(ctx).also {
            currentScope = currentScope.parent!!
        }
    }

    override fun visitClassParameter(ctx: ClassParameterContext): Unit = ctx.run {
        val name = simpleIdentifier().text
        val type = this.type().text

        if (expression()?.inferredType?.isNotSubtypeOf(type) == true)
            throw RuntimeException("Invalid value: $type should be given but ${expression().inferredType} found")

        if (isNotMember) {
            // We are in constructor scope, so we add the symbol to the symtab
            // todo: assert type of scope
            currentScope += VariableSymbol(name, type, Mutability.VAL)
        } else {
            // A class member, we add it to the class scope
            currentScope.parent!! += VariableSymbol(name, type, this.mutability.getAsMutability())
        }
        super.visitClassParameter(this)
    }

    override fun visitPropertyDeclaration(ctx: PropertyDeclarationContext): Unit = ctx.run {
        val name = simpleIdentifier().text
        val type = type()?.text ?: expression().inferredType

        if (currentScope.resolveVariable(name) != null)
            throw RuntimeException("Redeclaration of variable $name")

        if (expression().inferredType.isNotSubtypeOf(type))
            throw RuntimeException("Invalid value: $type should be given but ${expression().inferredType} found")

        currentScope += VariableSymbol(name, type, this.mutability.getAsMutability())
        super.visitPropertyDeclaration(this)
    }

    override fun visitAssignment(ctx: AssignmentContext): Unit = ctx.run {
        val varName = this.identifier().text
        val symbol = currentScope.resolveVariable(varName)
            ?: throw RuntimeException("Attempting to reassign value of undeclared variable $varName")

        if (symbol.isMutable) {
            if (expression().inferredType.isNotSubtypeOf(symbol.type)) {
                throw RuntimeException(
                    "Can't assign a value of type ${expression().inferredType} to a variable of type ${symbol.type}"
                )
            }
        } else throw RuntimeException("Attempting to reassign read-only variable $varName")

        super.visitAssignment(this)
    }

    private fun String.subtypeOf(other: String) =
        typeHierarchy.find(other).findInSubtree(this.asType()) != null

    private fun String.isNotSubtypeOf(other: String) = !this.subtypeOf(other)

    private val ExpressionContext.inferredType get() = typeInference.inferType(this)
}

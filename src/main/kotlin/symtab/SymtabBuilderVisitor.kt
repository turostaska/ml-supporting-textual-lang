package symtab

import kobraBaseVisitor
import kobraParser.*
import symtab.extensions.*
import type.TypeHierarchy
import type.util.contains

// symtab a hibakeresőben vagy a kódgeneráló visitorban?
class SymtabBuilderVisitor: kobraBaseVisitor<Any>() {
    private lateinit var currentScope: Scope
    val globalScope = Scope(parent = null)
    val typeHierarchy = TypeHierarchy()

    override fun visitProgram(ctx: ProgramContext): Any? {
        currentScope = globalScope
        return super.visitProgram(ctx)
    }

    override fun visitClassDeclaration(ctx: ClassDeclarationContext): Any? = ctx.run {
        if (className in typeHierarchy)
            throw RuntimeException("Redefinition of class '$className'")

        typeHierarchy.addType(className, baseClassNames = superClasses.toSet())

        currentScope = Scope(parent = currentScope, name = className)
        super.visitClassDeclaration(ctx).also {
            currentScope = currentScope.parent!!
            return it
        }
    }

    override fun visitPrimaryConstructor(ctx: PrimaryConstructorContext): Any? {
        currentScope = Scope(parent = currentScope, name = "Primary constructor")
        super.visitPrimaryConstructor(ctx).also {
            currentScope = currentScope.parent!!
            return it
        }
    }

    override fun visitClassParameter(ctx: ClassParameterContext): Any? = ctx.run {
        val name = simpleIdentifier().text
        val type = this.type().text

        if (isNotMember) {
            // We are in constructor scope, so we add the symbol to the symtab
            // todo: assert type of scope
            currentScope[name] = Symbol(name, type, mutable = false, typeHierarchy)
        } else {
            // A class member, we add it to the class scope
            currentScope.parent!![name] = Symbol(name, type, this.mutability, typeHierarchy)
        }
        return super.visitClassParameter(this)
    }

    override fun visitPropertyDeclaration(ctx: PropertyDeclarationContext): Any? = ctx.run {
        val name = simpleIdentifier().text
        val type = type()?.text ?: expression().inferredType

        currentScope[name] = Symbol(name, type, this.mutability, typeHierarchy)
        return super.visitPropertyDeclaration(this)
    }
}

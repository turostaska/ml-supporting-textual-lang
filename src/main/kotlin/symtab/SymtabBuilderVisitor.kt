package symtab

import kobraBaseVisitor
import kobraParser.*

// stack vagy linked-list alapú symtab?
// symtab a hibakeresőben vagy a kódgeneráló visitorban?
class SymtabBuilderVisitor: kobraBaseVisitor<Any>() {
    private lateinit var currentScope: Scope
    val globalScope = Scope(parent = null)

    override fun visitProgram(ctx: ProgramContext): Any? {
        currentScope = globalScope
        return super.visitProgram(ctx)
    }

    override fun visitClassDeclaration(ctx: ClassDeclarationContext): Any? {
        val name = ctx.simpleIdentifier().text

        currentScope = Scope(parent = currentScope, name = name)
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

        if (VAL() == null && VAR() == null) {
            // Not a class member, only a parameter
            // We are in constructor scope, so we add the symbol to the symtab
            currentScope[name] = Symbol(name, type, mutable = false)
        } else {
            // A class member, we add it to the class scope
            val mutability = VAL()?.text.orEmpty() + VAR()?.text.orEmpty()
            currentScope.parent!![name] = Symbol(name, type, mutability)
        }
        return super.visitClassParameter(this)
    }

    override fun visitPropertyDeclaration(ctx: PropertyDeclarationContext): Any? {
        val name = ctx.simpleIdentifier().text
        val type = ctx.type()?.text ?: "unknown" // todo: type inference
        val mutability = ctx.VAL()?.text.orEmpty() + ctx.VAR()?.text.orEmpty()

        currentScope[name] = Symbol(name, type, mutability)
        return super.visitPropertyDeclaration(ctx)
    }
}

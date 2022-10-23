package symtab

import com.kobra.kobraBaseVisitor
import com.kobra.kobraParser.*
import symtab.extensions.*
import symtab.import.ImportedLibraryReader
import type.*
import type.util.contains
import type.util.find
import type.util.findInSubtree

// symtab a hibakeresőben vagy a kódgeneráló visitorban?
class SymtabBuilderVisitor: kobraBaseVisitor<Unit>() {
    val globalScope = Scope(parent = null)
    var currentScope: Scope = globalScope
    val typeHierarchy = TypeHierarchy(this)

    private val typeInference = TypeInference(this)

    init {
        globalScope.addAllBuiltInTypes(
            *BuiltInTypes.all.filter { !it.nullable }.map { BuiltInTypeSymbol(it.name, it) }.toTypedArray()
        )
    }

    override fun visitProgram(ctx: ProgramContext) {
        currentScope = globalScope
        super.visitProgram(ctx)
    }

    override fun visitClassDeclaration(ctx: ClassDeclarationContext): Unit = ctx.run {
        if (className in typeHierarchy)
            throw RuntimeException("Redefinition of class '$className'")

        val declaredType = typeHierarchy.addType(className, baseClassNames = superClasses.toSet())
        val typeSymbol = TypeSymbol(className, declaredType)
        currentScope.addType(typeSymbol)

        currentScope = ClassDeclarationScope(currentScope, typeSymbol)
        super.visitClassDeclaration(ctx).also {
            currentScope = currentScope.parent!!
        }
    }

    override fun visitPrimaryConstructor(ctx: PrimaryConstructorContext) {
        val typeSymbol = (currentScope as? ClassDeclarationScope)?.typeSymbol
            ?: throw RuntimeException("Primary constructor found, but not in a class declaration")

        currentScope = PrimaryConstructorScope(currentScope, typeSymbol)
        super.visitPrimaryConstructor(ctx).also {
            currentScope = currentScope.parent!!
        }
    }

    override fun visitClassParameter(ctx: ClassParameterContext): Unit = ctx.run {
        val name = simpleIdentifier().text
        val type = this.type().text.trim()
        val typeSymbol = currentScope.resolveTypeOrThrow(type)

        if (expression()?.inferredType?.isNotSubtypeOf(typeSymbol) == true)
            throw RuntimeException("Invalid value: $type should be given but ${expression().inferredType} found")

        if (isNotMember) {
            // We are in constructor scope, so we add the symbol to the symtab
            // todo: assert type of scope
            currentScope += VariableSymbol(name, typeSymbol, Mutability.VAL)
        } else {
            // A class member, we add it to the class scope
            currentScope.parent!! += VariableSymbol(name, typeSymbol, this.mutability.getAsMutability(),)
        }
        super.visitClassParameter(this)
    }

    override fun visitPropertyDeclaration(ctx: PropertyDeclarationContext): Unit = ctx.run {
        val name = simpleIdentifier().text
        val typeSymbol = type()?.text?.trim()?.let { currentScope.resolveType(it) } ?: expression().inferredType

        // todo: rekurzívan feloldani a modulok szkópját

        if (currentScope.resolveVariableLocally(name) != null)
            throw RuntimeException("Redeclaration of variable '$name' in scope '${currentScope.name}'")

        if (expression().inferredType.isNotSubtypeOf(typeSymbol))
            throw RuntimeException("Invalid value: $typeSymbol should be given but ${expression().inferredType} found")

        currentScope += VariableSymbol(name, typeSymbol, this.mutability.getAsMutability(),)
        super.visitPropertyDeclaration(this)
    }

    override fun visitAssignment(ctx: AssignmentContext): Unit = ctx.run {
        val varName = this.identifier().text
        val symbol = currentScope.resolveVariable(varName)
            ?: throw RuntimeException("Attempting to reassign value of undeclared variable $varName")

        if (symbol.isMutable) {
            if (expression().inferredType.isNotSubtypeOf(symbol.typeSymbol)) {
                throw RuntimeException(
                    "Can't assign a value of type ${expression().inferredType} to a variable of type ${symbol.type}"
                )
            }
        } else throw RuntimeException("Attempting to reassign read-only variable $varName")

        super.visitAssignment(this)
    }

    override fun visitFunctionDeclaration(ctx: FunctionDeclarationContext): Unit = ctx.run {
        require(currentScope.resolveMethodLocally(functionName) == null) {
            "Redeclaration of method '$functionName' in scope '${currentScope.name}'"
        }

        val returnType = currentScope.resolveTypeOrThrow(returnTypeName)
        require(statements.lastOrNull()?.isReturnStatement == true || returnType == globalScope.UNIT) {
            "Last statement must be return OR return type must be Unit"
        }

        val params = this.params.mapValues { currentScope.resolveTypeOrThrow(it.value).let(::listOf) }
        val methodSymbol = MethodSymbol(functionName, returnType, params)
        currentScope += methodSymbol

        currentScope = FunctionScope(currentScope, methodSymbol).apply {
            params.forEach { (k, v) -> VariableSymbol(k, v.first(), Mutability.VAL).let(::add) }
        }
        super.visitFunctionDeclaration(ctx)
        currentScope = currentScope.parent!!
    }

    override fun visitJumpExpression(ctx: JumpExpressionContext): Unit = ctx.run {
        check(currentScope is FunctionScope)
        check((currentScope as FunctionScope).methodSymbol.returnType == this.expression().inferredType)

        super.visitJumpExpression(this)
    }

    override fun visitImportHeader(ctx: ImportHeaderContext): Unit = ctx.run {
        val moduleName = identifier().simpleIdentifier().first().text
        val importAlias = importAlias()?.simpleIdentifier()?.Identifier()?.text ?: moduleName

        val moduleScope = ModuleScope(currentScope, moduleName, importAlias)
        currentScope.add(ModuleSymbol(moduleScope))

        currentScope = moduleScope
        ImportedLibraryReader(this@SymtabBuilderVisitor, typeHierarchy).readAndAddAllSymbols(moduleName)
        currentScope = moduleScope.parent!!

        super.visitImportHeader(this)
    }


    private fun Type.subtypeOf(other: Type) =
        typeHierarchy.find(other)!!.findInSubtree(this) != null

    private fun TypeSymbol.isNotSubtypeOf(other: TypeSymbol) = !this.referencedType.subtypeOf(other.referencedType)

    private val ExpressionContext.inferredType get() = typeInference.inferType(this)

    private val FunctionDeclarationContext.returnTypeNameOfLastStatement
        get() = statements.lastOrNull()?.expression()?.let { typeInference.inferType(it) } ?: TypeNames.UNIT
}

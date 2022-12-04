package symtab.extensions

import com.kobra.kobraParser
import symtab.*
import syntax.expression.condition
import syntax.expression.elseBranch
import syntax.expression.ifBranch
import type.TypeNames
import util.second
import util.secondOrNull
import util.throwError

class TypeInference(
    private val symtabBuilder: SymtabBuilderVisitor,
) {
    private val currentScope get() = symtabBuilder.currentScope
    private val globalScope = symtabBuilder.globalScope

    private val BOOLEAN get() = globalScope.resolveTypeOrThrow(TypeNames.BOOLEAN)
    private val ANY get() = globalScope.resolveTypeOrThrow(TypeNames.ANY)
    private val UNIT get() = globalScope.resolveTypeOrThrow(TypeNames.UNIT)
    private val RANGE get() = globalScope.resolveTypeOrThrow(TypeNames.RANGE)
    private val INT get() = globalScope.resolveTypeOrThrow(TypeNames.INT)
    private val FLOAT get() = globalScope.resolveTypeOrThrow(TypeNames.FLOAT)
    private val STRING get() = globalScope.resolveTypeOrThrow(TypeNames.STRING)
    private val NOTHING_N get() = globalScope.resolveTypeOrThrow(TypeNames.NOTHING_N)
    private val NOTHING get() = globalScope.resolveTypeOrThrow(TypeNames.NOTHING)
    private val LIST get() = globalScope.resolveTypeOrThrow(TypeNames.LIST)

    fun inferType(expressionContext: kobraParser.ExpressionContext) = expressionContext.inferredType

    private val kobraParser.ExpressionContext.inferredType: TypeSymbol get() = this.disjunction().inferredType

    private val kobraParser.DisjunctionContext.inferredType: TypeSymbol
        get() = if (this.DISJ().any()) {
            require(this.conjunction().first().inferredType == BOOLEAN && this.conjunction().second().inferredType == BOOLEAN)
            BOOLEAN
        } else this.conjunction().first().inferredType

    private val kobraParser.ConjunctionContext.inferredType: TypeSymbol
        get() = if (this.CONJ().any()) {
            require(this.equality().first().inferredType == BOOLEAN && this.equality().second().inferredType == BOOLEAN)
            BOOLEAN
        } else this.equality().first().inferredType

    private val kobraParser.EqualityContext.inferredType: TypeSymbol
        get() = if (this.equalityOperator().any()) {
            require(this.comparison().first().inferredType == BOOLEAN && this.comparison().second().inferredType == BOOLEAN)
            BOOLEAN
        } else this.comparison().first().inferredType

    private val kobraParser.ComparisonContext.inferredType: TypeSymbol
        get() = if (this.comparisonOperator().any()) {
            require(this.genericCallLikeComparison().first().inferredType == this.genericCallLikeComparison().second().inferredType)
            BOOLEAN
        } else this.genericCallLikeComparison().first().inferredType

    private val kobraParser.GenericCallLikeComparisonContext.inferredType: TypeSymbol
        get() = this.infixOperation().inferredType

    private val kobraParser.InfixOperationContext.inferredType: TypeSymbol
        get() = when {
            inOperator().any() -> BOOLEAN
            isOperator().any() -> BOOLEAN
            else -> this.elvisExpression().first().inferredType
        }

    private val kobraParser.ElvisExpressionContext.inferredType: TypeSymbol
        get() = if (elvis().any()) {
            val firstOperand = infixFunctionCall().first()
            val secondOperand = infixFunctionCall().second()

            val firstOperandType = firstOperand.text.nonNullable().let {
                currentScope.resolveTypeOrThrow(it)
            }

            if (firstOperandType == secondOperand.inferredType)
                secondOperand.inferredType
            else ANY
        } else infixFunctionCall().first().inferredType

    private val kobraParser.InfixFunctionCallContext.inferredType: TypeSymbol
        get() = when (val id = this.simpleIdentifier().firstOrNull()?.Identifier()?.text) {
            null -> {
                rangeExpression().first().inferredType
            }

            else -> {
                val infixMethod = currentScope.resolveMethod(id)?.takeIf { it.isInfix }
                    ?: throw RuntimeException("Infix method $id is undefined")

                infixMethod.returnType ?: UNIT
            }
        }

    private val kobraParser.RangeExpressionContext.inferredType: TypeSymbol
        get() = if (this.RANGE().any())
            RANGE
        else this.additiveExpression().first().inferredType

    private val NUMERIC_TYPES get() = listOf(INT, FLOAT)

    // TODO: get return type of method with overloaded operator '+'
    private val kobraParser.AdditiveExpressionContext.inferredType: TypeSymbol
        get() {
            val op1 = this.multiplicativeExpression().first()
            val op2 = this.multiplicativeExpression().secondOrNull() ?:
                return op1.inferredType

            return when {
                op1.inferredType == op2.inferredType -> op1.inferredType
                op1.inferredType in NUMERIC_TYPES && op2.inferredType in NUMERIC_TYPES -> FLOAT
                else -> op1.inferredType.classMethods.find { it.name == "__add__" }?.returnType ?:
                    throwError { "Additive operator does not work on '${op1.text}' and '${op2.text}'" }
            }
        }

    private val kobraParser.MultiplicativeExpressionContext.inferredType: TypeSymbol
        get() = this.asExpression().first().inferredType

    private val kobraParser.AsExpressionContext.inferredType: TypeSymbol
        get() = if (this.asOperator().any()) {
            val id = this.type().first().simpleIdentifier().Identifier().text
            currentScope.resolveTypeOrThrow(id)
        } else this.prefixUnaryExpression().inferredType

    private val kobraParser.PrefixUnaryExpressionContext.inferredType: TypeSymbol
        get() = this.postfixUnaryExpression().inferredType

    // first.second.third.fourth().fifth
    private val kobraParser.PostfixUnaryExpressionContext.inferredType: TypeSymbol get() {
        if (postfixUnarySuffix()?.firstOrNull() == null)
            return this.primaryExpression().inferredType

        val receiverId = primaryExpression().simpleIdentifier().text

        var currentScope = currentScope
        var receiver = currentScope.resolveOrThrow(receiverId)
        for (suffix in postfixUnarySuffix()) {
            val suffixId = suffix.navigationSuffix()?.simpleIdentifier()?.text
            when {
                suffix.isStaticNavigationSuffix() -> {
                    require(receiver is ClassMethodSymbol || receiver is ModuleSymbol)
                    currentScope = currentScope.findModuleOrClassScope(receiverId)!!
                    receiver = currentScope.resolveOrThrow(suffixId!!)
                }

                suffix.isMemberNavigationSuffix() -> {
                    require(receiver !is ClassMethodSymbol && receiver !is ModuleSymbol)

                    when(receiver) {
                        is VariableSymbol, is ClassMethodSymbol -> {
                            currentScope = currentScope.findClassScope(receiver.type)!!
                            receiver = currentScope.resolveOrThrow(suffixId!!)
                        }
                        else -> throwError { "Unknown suffix: '${suffix.text}'" }
                    }
                }

                suffix.isCallSuffix() -> {
                    // todo: nem veszi figyelembe a paramétereket
                    // todo: csak akkor jó, ha az utolsó suffix, ha nem, akkor a visszatérési típus scope-ja kell
                    return currentScope.resolveMethodOrThrow(receiver.name).returnType ?: UNIT
//                    return currentScope.resolveMethod(receiver.name)?.returnType
//                        ?: currentScope.resolveVariable(receiver.name)?.typeSymbol?.let {
//                            currentScope.findModuleOrClassScope(it.name)?.resolveMethod("forward")?.returnType
//                        }
//                        ?: UNIT
                }
                else -> TODO()
            }
        }

        return (receiver as? VariableSymbol)?.typeSymbol
            ?: receiver.name.let { currentScope.resolveTypeOrThrow(it) }
    }

    private val kobraParser.PrimaryExpressionContext.inferredType: TypeSymbol
        get() = when {
            isBoolean -> BOOLEAN
            isInt -> INT
            isString -> STRING
            isFloat -> FLOAT
            isNullLiteral -> NOTHING_N
            isSimpleIdentifier -> {
                currentScope.resolveVariable(simpleIdentifier().text)?.typeSymbol
                    ?: throw RuntimeException("Simple identifier '${this.text}' has no type specified")
            }
            isParenthesized -> this.parenthesizedExpression().expression().inferredType
            isCollection -> LIST
            isReturnStatement -> NOTHING
            isIfExpression -> this.ifExpression()!!.inferredType
            else -> throw RuntimeException("Can't infer type for expression '${this.text}'")
        }

    private val kobraParser.IfExpressionContext.inferredType: TypeSymbol get() {
        val ifReturnType = ifBranch.block()?.statements()?.statement()?.last()?.expression()?.inferredType
            ?: ifBranch.statement().expression().inferredType
        val elseReturnType = elseBranch.block()?.statements()?.statement()?.last()?.expression()?.inferredType
            ?: elseBranch.statement().expression().inferredType

        require(condition.inferredType == BOOLEAN) { "The condition in an if expression must be Boolean" }
        require(ifReturnType == elseReturnType) { "Both branches must return with the same type" }

        return ifReturnType
    }
}

private fun String.nonNullable() = this.removeSuffix("?")
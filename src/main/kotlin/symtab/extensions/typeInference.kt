package symtab.extensions

import com.kobra.kobraParser
import symtab.SymtabBuilderVisitor
import type.TypeNames
import util.second

class TypeInference(
    private val symtabBuilder: SymtabBuilderVisitor,
) {
    private val currentScope get() = symtabBuilder.currentScope

    fun inferType(expressionContext: kobraParser.ExpressionContext) = expressionContext.inferredType

    private val kobraParser.ExpressionContext.inferredType: String get() = this.disjunction().inferredType

    private val kobraParser.DisjunctionContext.inferredType: String
        get() = if (this.DISJ().any())
            TypeNames.BOOLEAN
        else this.conjunction().first().inferredType

    private val kobraParser.ConjunctionContext.inferredType: String
        get() = if (this.CONJ().any())
            TypeNames.BOOLEAN
        else this.equality().first().inferredType

    private val kobraParser.EqualityContext.inferredType: String
        get() = if (this.equalityOperator().any())
            TypeNames.BOOLEAN
        else this.comparison().first().inferredType

    private val kobraParser.ComparisonContext.inferredType: String
        get() = if (this.comparisonOperator().any())
            TypeNames.BOOLEAN
        else this.genericCallLikeComparison().first().inferredType

    private val kobraParser.GenericCallLikeComparisonContext.inferredType: String
        get() = this.infixOperation().inferredType

    private val kobraParser.InfixOperationContext.inferredType: String
        get() = when {
            inOperator().any() -> TypeNames.BOOLEAN
            isOperator().any() -> TypeNames.BOOLEAN
            else -> this.elvisExpression().first().inferredType
        }

    private val kobraParser.ElvisExpressionContext.inferredType: String
        get() = if (elvis().any()) {
            val firstOperand = infixFunctionCall().first()
            val secondOperand = infixFunctionCall().second()

            if (firstOperand.inferredType.nonNullable() == secondOperand.inferredType)
                secondOperand.inferredType
            else TypeNames.ANY
        } else infixFunctionCall().first().inferredType

    private val kobraParser.InfixFunctionCallContext.inferredType: String
        get() = when (val id = this.simpleIdentifier().firstOrNull()?.Identifier()?.text) {
            null -> {
                rangeExpression().first().inferredType
            }

            else -> {
                val infixMethod = currentScope.resolveMethod(id)?.takeIf { it.isInfix }
                    ?: throw RuntimeException("Infix method $id is undefined")

                infixMethod.returnType ?: TypeNames.UNIT
            }
        }

    private val kobraParser.RangeExpressionContext.inferredType: String
        get() = if (this.RANGE().any())
            TypeNames.RANGE
        else this.additiveExpression().first().inferredType

    // TODO: get return type of method with overloaded operator '+'
    private val kobraParser.AdditiveExpressionContext.inferredType: String
        get() = this.multiplicativeExpression().first().inferredType

    private val kobraParser.MultiplicativeExpressionContext.inferredType: String
        get() = this.asExpression().first().inferredType

    private val kobraParser.AsExpressionContext.inferredType: String
        get() = if (this.asOperator().any())
            "${this.type().first().simpleIdentifier().Identifier().text}?"
        else this.prefixUnaryExpression().inferredType

    private val kobraParser.PrefixUnaryExpressionContext.inferredType: String
        get() = this.postfixUnaryExpression().inferredType

    private val kobraParser.PostfixUnaryExpressionContext.inferredType: String
        get() = this.primaryExpression().inferredType

    private val kobraParser.PrimaryExpressionContext.inferredType
        get() = when {
            isBoolean -> TypeNames.BOOLEAN
            isInt -> TypeNames.INT
            isString -> TypeNames.STRING
            isNullLiteral -> TypeNames.NOTHING_N
            isSimpleIdentifier -> {
                currentScope.resolveVariable(simpleIdentifier().text)?.type
                    ?: throw RuntimeException("Simple identifier '${this.text}' has no type specified")
            }
            isParenthesized -> this.parenthesizedExpression().expression().inferredType
            isCollection -> TypeNames.LIST
            else -> throw RuntimeException("Can't infer type for expression '${this.text}'")
        }
}

private fun String.nonNullable() = this.removeSuffix("?")
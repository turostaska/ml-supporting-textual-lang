package symtab.extensions

import com.kobra.kobraParser.*
import symtab.SymtabBuilderVisitor
import type.TypeNames.ANY
import type.TypeNames.BOOLEAN
import type.TypeNames.INT
import type.TypeNames.NOTHING_N
import type.TypeNames.RANGE
import type.TypeNames.STRING
import type.TypeNames.UNIT
import util.second

val PrimaryExpressionContext.isBoolean get() = (this.literalConstant()?.BooleanLiteral() != null)

val PrimaryExpressionContext.isString get() = (this.stringLiteral() != null)

val PrimaryExpressionContext.isInt get() = (this.literalConstant()?.IntegerLiteral() != null)

val PrimaryExpressionContext.isNullLiteral get() = (this.literalConstant()?.NullLiteral() != null)

val PrimaryExpressionContext.isSimpleIdentifier get() = (this.simpleIdentifier() != null)

class TypeInference(
    private val symtabBuilder: SymtabBuilderVisitor,
) {
    private val currentScope get() = symtabBuilder.currentScope

    fun inferType(expressionContext: ExpressionContext) = expressionContext.inferredType

    private val ExpressionContext.inferredType: String get() = this.disjunction().inferredType

    private val DisjunctionContext.inferredType: String
        get() = if (this.DISJ().any())
            BOOLEAN
        else this.conjunction().first().inferredType

    private val ConjunctionContext.inferredType: String
        get() = if (this.CONJ().any())
            BOOLEAN
        else this.equality().first().inferredType

    private val EqualityContext.inferredType: String
        get() = if (this.equalityOperator().any())
            BOOLEAN
        else this.comparison().first().inferredType

    private val ComparisonContext.inferredType: String
        get() = if (this.comparisonOperator().any())
            BOOLEAN
        else this.genericCallLikeComparison().first().inferredType

    private val GenericCallLikeComparisonContext.inferredType: String
        get() = this.infixOperation().inferredType

    private val InfixOperationContext.inferredType: String
        get() = when {
            inOperator().any() -> BOOLEAN
            isOperator().any() -> BOOLEAN
            else -> this.elvisExpression().first().inferredType
        }

    private val ElvisExpressionContext.inferredType: String
        get() = if (elvis().any()) {
            val firstOperand = infixFunctionCall().first()
            val secondOperand = infixFunctionCall().second()

            if (firstOperand.inferredType.nonNullable() == secondOperand.inferredType)
                secondOperand.inferredType
            else ANY
        } else infixFunctionCall().first().inferredType

    private val InfixFunctionCallContext.inferredType: String
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

    private val RangeExpressionContext.inferredType: String
        get() = if (this.RANGE().any())
            RANGE
        else this.additiveExpression().first().inferredType

    // TODO: get return type of method with overloaded operator '+'
    private val AdditiveExpressionContext.inferredType: String
        get() = this.multiplicativeExpression().first().inferredType

    private val MultiplicativeExpressionContext.inferredType: String
        get() = this.asExpression().first().inferredType

    private val AsExpressionContext.inferredType: String
        get() = if (this.asOperator().any())
            "${this.type().first().simpleIdentifier().Identifier().text}?"
        else this.prefixUnaryExpression().inferredType

    private val PrefixUnaryExpressionContext.inferredType: String
        get() = this.postfixUnaryExpression().inferredType

    private val PostfixUnaryExpressionContext.inferredType: String
        get() = this.primaryExpression().inferredType

    private val PrimaryExpressionContext.inferredType
        get() = when {
            isBoolean -> BOOLEAN
            isInt -> INT
            isString -> STRING
            isNullLiteral -> NOTHING_N
            isSimpleIdentifier -> {
                currentScope.resolveVariable(simpleIdentifier().text)?.type
                    ?: throw RuntimeException("Simple identifier '${this.text}' has no type specified")
            }

            else -> throw RuntimeException("Can't infer type for expression '${this.text}'")
        }
}

private fun String.nonNullable() = this.removeSuffix("?")

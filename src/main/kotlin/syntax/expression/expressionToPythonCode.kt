package syntax.expression

import com.kobra.kobraParser.*
import symtab.extensions.*
import util.second

fun ExpressionContext.toPythonCode(): String = this.disjunction().toPythonCode()

private fun DisjunctionContext.toPythonCode(): String = if (this.DISJ().any())
    "${conjunction().first().toPythonCode()} or ${conjunction().second().toPythonCode()}"
else this.conjunction().first().toPythonCode()

private fun ConjunctionContext.toPythonCode(): String = if (this.CONJ().any())
    "${equality().first().toPythonCode()} and ${equality().second().toPythonCode()}"
else this.equality().first().toPythonCode()

private fun EqualityContext.toPythonCode(): String = if (this.equalityOperator().any())
    "${comparison().first().toPythonCode()} and ${comparison().second().toPythonCode()}"
else this.comparison().first().toPythonCode()

private fun ComparisonContext.toPythonCode(): String {
    return if (this.comparisonOperator().any()) {
        val op = this.comparisonOperator().first().text
        "${genericCallLikeComparison().first().toPythonCode()} $op ${
            genericCallLikeComparison().second().toPythonCode()
        }"
    } else this.genericCallLikeComparison().first().toPythonCode()
}

private fun GenericCallLikeComparisonContext.toPythonCode(): String = this.infixOperation().toPythonCode()

private fun InfixOperationContext.toPythonCode(): String {
    return when {
        inOperator().any() -> {
            val negate = if (inOperator().first().IN() != null) "" else "not "

            "${elvisExpression().first().toPythonCode()} ${negate}in ${elvisExpression().second().toPythonCode()}"
        }

        isOperator().any() -> {
            val negate = if (isOperator().first().IS() != null) "" else "not "

            "${negate}isinstance(${elvisExpression().first().toPythonCode()}, ${
                elvisExpression().second().toPythonCode()
            })"
        }

        else -> this.elvisExpression().first().toPythonCode()
    }
}

private fun ElvisExpressionContext.toPythonCode(): String {
    return if (elvis().any()) {
        val nullable = infixFunctionCall().first().toPythonCode()
        val nonNullable = infixFunctionCall().second().toPythonCode()

        "($nullable) if ($nullable) is not None else ($nonNullable)"
    } else infixFunctionCall().first().toPythonCode()
}

private fun InfixFunctionCallContext.toPythonCode(): String {
    return when (val methodName = this.simpleIdentifier().firstOrNull()?.Identifier()?.text) {
        null -> {
            rangeExpression().first().toPythonCode()
        }

        else -> {
            val op0 = rangeExpression().first().toPythonCode()
            val op1 = rangeExpression().second().toPythonCode()
            "$methodName($op0, $op1)" // todo: naming convention for infix methods?
        }
    }
}

private fun RangeExpressionContext.toPythonCode(): String {
    return if (this.RANGE().any()) {
        val start = additiveExpression().first().toPythonCode()
        val end = additiveExpression().second().toPythonCode()

        "range($start, $end + 1)"
    } else this.additiveExpression().first().toPythonCode()
}

private fun AdditiveExpressionContext.toPythonCode(): String {
    return if (this.additiveOperator().any()) {
        val op0 = multiplicativeExpression().first().toPythonCode()
        val op1 = multiplicativeExpression().second().toPythonCode()
        val operator = additiveOperator().first().let { it.ADD()?.text ?: it.SUB()!!.text }

        "$op0 $operator $op1"
    } else this.multiplicativeExpression().first().toPythonCode()
}

private fun MultiplicativeExpressionContext.toPythonCode(): String {
    return if (this.multiplicativeOperator().any()) {
        val op0 = asExpression().first().toPythonCode()
        val op1 = asExpression().second().toPythonCode()
        val operator = multiplicativeOperator().first()
            .let { it.DIV()?.text ?: it.MOD()?.text ?: it.MULT()!!.text }

        "$op0 $operator $op1"
    } else this.asExpression().first().toPythonCode()
}

private fun AsExpressionContext.toPythonCode(): String {
    return if (this.asOperator().any()) {
        TODO("lehet ez egyáltalán expression?")
    } else this.prefixUnaryExpression().toPythonCode()
}

private fun PrefixUnaryExpressionContext.toPythonCode(): String {
    return if (this.unaryPrefix().any()) {
        val prefix = this.unaryPrefix().first().prefixUnaryOperator().let {
            if (it.excl() != null) "not " else it.ADD()?.text ?: it.SUB()!!.text
        }
        "$prefix ${postfixUnaryExpression().toPythonCode()}"
    } else this.postfixUnaryExpression().toPythonCode()
}

private fun PostfixUnaryExpressionContext.toPythonCode(): String {
    return if (this.postfixUnarySuffix().any()) {
        TODO("lehet ez egyáltalán expression?")
    } else this.primaryExpression().toPythonCode()
}

private fun PrimaryExpressionContext.toPythonCode(): String {
    return when {
        isBoolean -> if (literalConstant().BooleanLiteral().text == "true") "True" else "False"
        isNullLiteral -> "None"
        isInt -> this.literalConstant().IntegerLiteral().text
        isString -> this.stringLiteral().text
        isSimpleIdentifier -> this.simpleIdentifier().text
        isParenthesized -> "(${this.parenthesizedExpression().expression().toPythonCode()})"
        isCollection -> "[ ${this.collectionLiteral().expression().joinToString { it.toPythonCode() }} ]"
        else -> throw RuntimeException("Can't generate code from primary expression '${this.text}'")
    }
}

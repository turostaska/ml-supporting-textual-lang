package symtab.extensions

import com.kobra.kobraParser.ExpressionContext
import type.TypeNames.BOOLEAN
import type.TypeNames.INT
import type.TypeNames.NOTHING_N
import type.TypeNames.STRING

val ExpressionContext.isBoolean get() = (this.BooleanLiteral() != null)

val ExpressionContext.isString get() = (this.StringLiteral() != null)

val ExpressionContext.isInt get() = (this.IntegerLiteral() != null)

val ExpressionContext.isNullLiteral get() = (this.NullLiteral() != null)

val ExpressionContext.inferredType get() = when {
    isBoolean -> BOOLEAN
    isInt -> INT
    isString -> STRING
    isNullLiteral -> NOTHING_N
    else -> throw RuntimeException("Can't infer type for expression '${this.text}'")
}
package symtab.extensions

import kobraParser.ExpressionContext

val ExpressionContext.isBoolean get() = (this.BooleanLiteral() != null)

val ExpressionContext.isString get() = (this.StringLiteral() != null)

val ExpressionContext.isInt get() = (this.IntegerLiteral() != null)

val ExpressionContext.isNullLiteral get() = (this.NullLiteral() != null)

val ExpressionContext.inferredType get() = when {
    isBoolean -> "Boolean"
    isInt -> "Int"
    isString -> "String"
    isNullLiteral -> "Nothing?"
    else -> throw RuntimeException("Can't infer type for expression '${this.text}'")
}
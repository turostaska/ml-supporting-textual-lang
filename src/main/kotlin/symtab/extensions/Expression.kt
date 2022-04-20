package symtab.extensions

import kobraParser.ExpressionContext

val ExpressionContext.isBoolean get() = (this.BooleanLiteral() != null)

val ExpressionContext.isString get() = (this.StringLiteral() != null)

val ExpressionContext.isInt get() = (this.IntegerLiteral() != null)

// todo: null values should have explicit type declaration
val ExpressionContext.isNullLiteral get() = (this.NullLiteral() != null)

val ExpressionContext.inferredType get() = when {
    isBoolean -> "Boolean"
    isInt -> "Int"
    isString -> "String"
    isNullLiteral -> "Nothing?"
    else -> "Unknown"
}
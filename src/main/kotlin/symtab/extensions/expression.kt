package symtab.extensions

import com.kobra.kobraParser.PrimaryExpressionContext

val PrimaryExpressionContext.isBoolean get() = (this.literalConstant()?.BooleanLiteral() != null)

val PrimaryExpressionContext.isString get() = (this.stringLiteral() != null)

val PrimaryExpressionContext.isInt get() = (this.literalConstant()?.IntegerLiteral() != null)

val PrimaryExpressionContext.isNullLiteral get() = (this.literalConstant()?.NullLiteral() != null)

val PrimaryExpressionContext.isSimpleIdentifier get() = (this.simpleIdentifier() != null)

val PrimaryExpressionContext.isParenthesized get() = (this.parenthesizedExpression() != null)

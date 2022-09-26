package symtab.extensions

import com.kobra.kobraParser.StatementContext
import syntax.expression.toPythonCode

val StatementContext.primaryExpression
    get() = this.expression()?.disjunction()?.conjunction()?.firstOrNull()?.equality()?.firstOrNull()
        ?.comparison()?.firstOrNull()?.genericCallLikeComparison()?.firstOrNull()?.infixOperation()
        ?.elvisExpression()?.firstOrNull()?.infixFunctionCall()?.firstOrNull()?.rangeExpression()?.firstOrNull()
        ?.additiveExpression()?.firstOrNull()?.multiplicativeExpression()?.firstOrNull()?.asExpression()
        ?.firstOrNull()?.prefixUnaryExpression()?.postfixUnaryExpression()?.primaryExpression()

val StatementContext.jumpExpression
    get() = this.primaryExpression?.jumpExpression()

val StatementContext.isReturnStatement
    get() = this.primaryExpression?.isReturnStatement ?: false

val StatementContext.isAssignment
    get() = this.assignment() != null

val StatementContext.isPropertyDeclaration
    get() = this.declaration()?.propertyDeclaration() != null

fun StatementContext.toCode(): String {
    return when {
        isReturnStatement -> "return ${jumpExpression!!.expression().toPythonCode()}"
        isAssignment || isPropertyDeclaration ->
            "${assignment()!!.identifier()} = ${assignment().expression().toPythonCode()}"
        else -> throw RuntimeException("Unknown statement: '${this.text}'")
    }
}

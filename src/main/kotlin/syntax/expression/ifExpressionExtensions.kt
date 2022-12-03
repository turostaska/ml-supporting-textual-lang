package syntax.expression

import com.kobra.kobraParser.*
import util.second

val IfExpressionContext.condition: ExpressionContext
    get() = expression()

val IfExpressionContext.ifBranch: ControlStructureBodyContext
    get() = controlStructureBody().first()

val IfExpressionContext.elseBranch: ControlStructureBodyContext
    get() = controlStructureBody().second()

fun IfExpressionContext.isOneLiner(): Boolean {
    return ifBranch.block() == null && elseBranch.block() == null
}

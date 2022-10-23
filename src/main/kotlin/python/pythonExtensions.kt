package python

import com.kobra.Python3Parser.*
import type.TypeNames
import type.TypeNames.ANY_N
import util.filterNonNull
import util.secondOrNull

val FuncdefContext.returnTypeNamePy: String?
    get() = this.test()?.or_test(0)?.and_test(0)?.not_test(0)?.comparison()?.expr(0)
        ?.xor_expr(0)?.and_expr(0)?.shift_expr(0)?.arith_expr(0)?.term(0)?.factor(0)?.power()?.atom_expr()
        ?.atom()?.NAME()?.text

val FuncdefContext.returnTypeName: String
    get() = this.returnTypeNamePy?.let { TypeNames.pythonTypeNamesToKobraMap[it] } ?: returnTypeNamePy ?: TypeNames.UNIT

val FuncdefContext.functionName: String get() = NAME().text

fun TfpdefContext.isNotSelf() = this.NAME()?.text != "self"

val TestContext.atomExpr get() = this.or_test(0)?.and_test(0)?.not_test(0)?.comparison()?.expr(0)
    ?.xor_expr(0)?.and_expr(0)?.shift_expr(0)?.arith_expr(0)?.term(0)?.factor(0)?.power()?.atom_expr()

fun Atom_exprContext.isOptionalType() = this.atom()?.text == "Optional"

fun TestContext.isUnionType() = this.atomExpr?.atom()?.text == "Union"

fun TestContext.isOptionalType() = this.atomExpr?.isOptionalType() ?: false

fun TestContext.compatibleTypes() = if (this.isUnionType()) {
    this.atomExpr?.trailer(0)?.subscriptlist()?.subscript_()?.mapNotNull { it?.test(0)?.atomExpr } ?: emptyList()
} else {
    listOf(this.atomExpr)
}

fun Atom_exprContext.toKobraTypeNameOrAny() = if (this.isOptionalType()) {
    val typeNamePy = trailer(0).subscriptlist().subscript_(0).test(0).text
    TypeNames.pythonTypeNamesToKobraMap[typeNamePy]?.plus("?") ?: ANY_N
} else {
    val typeNamePy = atom().NAME()?.text ?: atom().text
    TypeNames.pythonTypeNamesToKobraMap[typeNamePy] ?: ANY_N
}

val FuncdefContext.parameterNamesToTypeNameMap: Map<String, List<String>>
    get() = this.parameters()?.typedargslist()?.tfpdef()?.filter { it.isNotSelf() }
        ?.mapNotNull { it.NAME()?.text to it.test()?.compatibleTypes() }
        ?.toMap()
        ?.mapValues {
            it.value.let { typeNamesPy ->
                typeNamesPy?.mapNotNull { typeNamePy -> typeNamePy?.toKobraTypeNameOrAny() }
            }
        }?.filterNonNull() ?: emptyMap()

val ClassdefContext.classNamePy: String get() = NAME().text

fun Expr_stmtContext.testListComp() = this.testlist_star_expr()?.secondOrNull()?.test()?.firstOrNull()?.or_test()?.firstOrNull()
    ?.and_test()?.firstOrNull()?.not_test()?.firstOrNull()?.comparison()?.expr()?.firstOrNull()
    ?.xor_expr()?.firstOrNull()?.and_expr()?.firstOrNull()?.shift_expr()?.firstOrNull()
    ?.arith_expr()?.firstOrNull()?.term()?.firstOrNull()?.factor()?.firstOrNull()?.power()?.atom_expr()
    ?.atom()?.testlist_comp()

fun TestlistContext.atomExpr() = this.test()?.firstOrNull()?.or_test()?.firstOrNull()?.and_test()?.firstOrNull()
    ?.not_test()?.firstOrNull()?.comparison()?.expr()?.firstOrNull()?.xor_expr()?.firstOrNull()?.and_expr()
    ?.firstOrNull()?.shift_expr()?.firstOrNull()?.arith_expr()?.firstOrNull()?.term()?.firstOrNull()?.factor()
    ?.firstOrNull()?.power()?.atom_expr()

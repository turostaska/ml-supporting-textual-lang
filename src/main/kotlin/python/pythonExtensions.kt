package python

import com.kobra.Python3Parser.*
import type.TypeNames
import util.filterNonNull

val FuncdefContext.returnTypeNamePy: String?
    get() = this.test()?.or_test(0)?.and_test(0)?.not_test(0)?.comparison()?.expr(0)
        ?.xor_expr(0)?.and_expr(0)?.shift_expr(0)?.arith_expr(0)?.term(0)?.factor(0)?.power()?.atom_expr()
        ?.atom()?.NAME()?.text

val FuncdefContext.returnTypeName: String?
    get() = this.returnTypeNamePy?.let { TypeNames.pythonTypeNamesToKobraMap[it] }

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

fun Atom_exprContext.toKobraTypeName() = if (this.isOptionalType()) {
    val typeNamePy = trailer(0).subscriptlist().subscript_(0).test(0).text
    TypeNames.pythonTypeNamesToKobraMap[typeNamePy]?.plus("?")
        ?: throw RuntimeException("No mapping for Python type '${this.text}'")
} else {
    val typeNamePy = atom().NAME()?.text ?: atom().text
    TypeNames.pythonTypeNamesToKobraMap[typeNamePy]
        ?: throw RuntimeException("No mapping for Python type '${this.text}'")
}

val FuncdefContext.parameterNamesToTypeNameMap: Map<String, List<String>>
    get() = this.parameters()?.typedargslist()?.tfpdef()?.filter { it.isNotSelf() }
        ?.mapNotNull { it.NAME()?.text to it.test()?.compatibleTypes() }
        ?.toMap()
        ?.mapValues {
            it.value.let { typeNamesPy ->
                typeNamesPy?.mapNotNull { typeNamePy -> typeNamePy?.toKobraTypeName() }
            }
        }?.filterNonNull() ?: emptyMap()

val ClassdefContext.classNamePy: String get() = NAME().text

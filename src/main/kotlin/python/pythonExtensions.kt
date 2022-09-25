package python

import com.kobra.Python3Parser.FuncdefContext
import com.kobra.Python3Parser.TfpdefContext
import type.TypeNames
import util.filterNonNull

val FuncdefContext.returnTypeNamePy: String?
    get() = this.test()?.or_test(0)?.and_test(0)?.not_test(0)?.comparison()?.expr(0)
        ?.xor_expr(0)?.and_expr(0)?.shift_expr(0)?.arith_expr(0)?.term(0)?.factor(0)?.power()?.atom_expr()
        ?.atom()?.NAME()?.text

val FuncdefContext.returnTypeName: String?
    get() = this.returnTypeNamePy?.let { TypeNames.pythonTypeNamesToKobraMap[it] }

fun TfpdefContext.isNotSelf() = this.NAME()?.text != "self"

val FuncdefContext.parameterNamesToTypeNameMap
    get() = this.parameters()?.typedargslist()?.tfpdef()?.filter { it.isNotSelf() }
        ?.mapNotNull { it.NAME()?.text to it.test()?.text }
        ?.toMap()?.mapValues {
            it.value?.let { typeNamePy ->
                TypeNames.pythonTypeNamesToKobraMap[typeNamePy]
                    ?: throw RuntimeException("No mapping for Python type '$typeNamePy'")
            }
        }?.filterNonNull() ?: emptyMap()

package modelvisualizer

import com.kobra.Python3Parser
import com.kobra.Python3Parser.*
import com.kobra.Python3ParserBaseVisitor
import util.second
import util.secondOrNull

private fun ClassdefContext.argList() = this.arglist()?.argument()?.toList()?.map { it.text } ?: emptyList()

private fun FuncdefContext.parameterList() =
    this.parameters()?.typedargslist()?.tfpdef()?.map { it.NAME().text } ?: emptyList()

private fun Python3Parser.StmtContext.isAssignation() =
    simple_stmt()?.small_stmt()?.firstOrNull()?.expr_stmt()?.ASSIGN()?.firstOrNull() != null

fun Expr_stmtContext.assignationLeftAtomExpr() = testlist_star_expr()?.firstOrNull()
    ?.test()?.firstOrNull()?.or_test()?.firstOrNull()
    ?.and_test()?.firstOrNull()?.not_test()?.firstOrNull()?.comparison()?.expr()?.firstOrNull()
    ?.xor_expr()?.firstOrNull()?.and_expr()?.firstOrNull()?.shift_expr()?.firstOrNull()
    ?.arith_expr()?.firstOrNull()?.term()?.firstOrNull()?.factor()?.firstOrNull()?.power()?.atom_expr()

fun Expr_stmtContext.assignationRightAtomExpr() = testlist_star_expr()?.secondOrNull()
    ?.test()?.firstOrNull()?.or_test()?.firstOrNull()
    ?.and_test()?.firstOrNull()?.not_test()?.firstOrNull()?.comparison()?.expr()?.firstOrNull()
    ?.xor_expr()?.firstOrNull()?.and_expr()?.firstOrNull()?.shift_expr()?.firstOrNull()
    ?.arith_expr()?.firstOrNull()?.term()?.firstOrNull()?.factor()?.firstOrNull()?.power()?.atom_expr()

fun Expr_stmtContext.parameters() =
    this.assignationRightAtomExpr()?.trailer()?.lastOrNull()?.arglist()?.argument() ?: emptyList()

fun ArgumentContext.atomExpr() = this.test()?.firstOrNull()?.or_test()?.firstOrNull()
    ?.and_test()?.firstOrNull()?.not_test()?.firstOrNull()?.comparison()?.expr()?.firstOrNull()
    ?.xor_expr()?.firstOrNull()?.and_expr()?.firstOrNull()?.shift_expr()?.firstOrNull()
    ?.arith_expr()?.firstOrNull()?.term()?.firstOrNull()?.factor()?.firstOrNull()?.power()?.atom_expr()

fun Atom_exprContext.forwardCalls(
    inputTensor: String,
    cumulativeCalls: List<String> = emptyList(),
): List<String> {
    val call = this.trailer().first().NAME().text
    val firstParam = this.trailer().second().arglist().argument().first()

    if (firstParam.text == inputTensor)
        return listOf(call)

    val previousCalls = firstParam.atomExpr()!!.forwardCalls(inputTensor).toTypedArray()
    return listOf(*previousCalls, call)
}

fun ArglistContext.forwardCalls(
    inputTensor: String,
    cumulativeCalls: List<String> = emptyList(),
): List<String> {
    if (this.argument()?.firstOrNull()?.atomExpr()?.trailer().isNullOrEmpty())
        return listOf(this.argument().first().text)

    val currentCall = this.argument(0).atomExpr()?.trailer()?.firstOrNull()?.NAME()?.text
        ?: this.argument(0).atomExpr()?.atom()?.NAME()?.text!!
    return listOf(*cumulativeCalls.toTypedArray(), currentCall)
}

class ModelVisualizerVisitor: Python3ParserBaseVisitor<Unit>() {
    private var inModelDefinition = false
    private val layerSymbols: MutableMap<String, Layer> = mutableMapOf()
    private val model = Model()

    override fun visitClassdef(ctx: ClassdefContext): Unit = ctx.run {
        if (argList().none { "Module" in it })
            return

        inModelDefinition = true
        super.visitClassdef(this)
    }

    override fun visitFuncdef(ctx: FuncdefContext): Unit = ctx.run {
        if (parameterList().firstOrNull() != "self")
            return

        val name = this.NAME().text

        return when {
            name == "__init__" && parameterList().count() == 1 -> {
                visitConstructor(this)
                super.visitFuncdef(this)
            }

            name == "forward" && parameterList().count() == 2 -> {
                visitForwardFunction(this)
                super.visitFuncdef(this)
            }

            else -> {}
        }
    }

    fun visitConstructor(ctx: FuncdefContext): Unit = ctx.run {
        val memberDeclarations = this.suite()?.stmt()
            ?.filter { it.text.startsWith("self.") && it.isAssignation() }
            ?.map { it.simple_stmt().small_stmt(0).expr_stmt()!! }
            ?: emptyList()

        val layerDeclarations = memberDeclarations.filter {
            val type = it.assignationRightAtomExpr()?.trailer()?.firstOrNull()?.NAME()?.text
            type in LayerType.values().map(LayerType::name)
        }

        for ((i, decl) in layerDeclarations.withIndex()) {
            val name = decl.assignationLeftAtomExpr()?.trailer()?.lastOrNull()?.NAME()?.text!!
            val type = decl.assignationRightAtomExpr()?.trailer()
                ?.findLast { it.arglist()?.argument().isNullOrEmpty() }?.NAME()?.text
                ?.let {  typeName -> LayerType.values().find { it.name == typeName } }!!

            // todo: MaxPool
            val inChannels = decl.parameters().let { params ->
                if (type.isMaxPool()) -1
                else if (params.first().ASSIGN() == null)
                    params.first().text.toInt()
                else params.find { it.test()?.firstOrNull()?.text == "in_channels" }?.test(1)?.text?.toInt()!!
            }
            val outChannels = decl.parameters().let { params ->
                when {
                    type.isMaxPool() -> -1
                    params.count() < 2 -> params.first().text.toInt()
                    params.second().ASSIGN() == null -> params.second().text.toInt()
                    else -> params.find { it.test()?.firstOrNull()?.text == "out_channels" }?.test(1)?.text?.toInt()!!
                }
            }

            layerSymbols[name] = Layer(type, inChannels, outChannels)
        }
    }

    fun visitForwardFunction(ctx: FuncdefContext): Unit = ctx.run {
        val inputTensor = parameterList().second()

        val forwardStatements = this.suite()?.stmt()
            ?.filter { it.text.startsWith(inputTensor) && it.isAssignation() }
            ?.filter { "$inputTensor.view(" !in it.text } // todo: flatten
            ?.map { it.simple_stmt().small_stmt(0).expr_stmt()!! }
            ?: emptyList()

        val forwardCalls = forwardStatements.flatMap {
            it.assignationRightAtomExpr()?.forwardCalls(inputTensor) ?: emptyList()
        }
//            .map { functionName ->
//            if (functionName == "relu")
//                LayerType.ReLU
//            else LayerType.values().find { it.name == functionName }
//                ?: throw RuntimeException("Can't find symbol $functionName")
//        }

        forwardCalls.forEachIndexed { index, functionName ->
            model += if (functionName == "relu") {
                val numChannels = if (index == 0) 1 else forwardCalls[index - 1].let { previousCall ->
                    layerSymbols[previousCall]!!.outChannels
                }
                Layer(LayerType.ReLU, numChannels, numChannels)
            } else if(layerSymbols[functionName]!!.type.isMaxPool()) {
                val numChannels = model[index - 1].outChannels
                Layer(layerSymbols[functionName]!!.type, numChannels, numChannels)
            } else {
                layerSymbols[functionName]!!
            }
        }
    }

}

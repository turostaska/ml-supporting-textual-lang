package modelvisualizer

import com.kobra.Python3Parser.*
import com.kobra.Python3ParserBaseVisitor
import util.second
import util.secondOrNull

private fun ClassdefContext.argList() = this.arglist()?.argument()?.toList()?.map { it.text } ?: emptyList()

private fun FuncdefContext.parameterList() =
    this.parameters()?.typedargslist()?.tfpdef()?.map { it.NAME().text } ?: emptyList()

private fun StmtContext.isAssignation() =
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
    this.assignationRightAtomExpr()?.parameters() ?: emptyList()

fun Atom_exprContext.parameters() =
    this.trailer()?.lastOrNull()?.arglist()?.argument() ?: emptyList()

fun ArgumentContext.atomExpr() = this.test()?.firstOrNull()?.or_test()?.firstOrNull()
    ?.and_test()?.firstOrNull()?.not_test()?.firstOrNull()?.comparison()?.expr()?.firstOrNull()
    ?.xor_expr()?.firstOrNull()?.and_expr()?.firstOrNull()?.shift_expr()?.firstOrNull()
    ?.arith_expr()?.firstOrNull()?.term()?.firstOrNull()?.factor()?.firstOrNull()?.power()?.atom_expr()

fun Atom_exprContext.forwardCalls(
    inputTensor: String,
): List<String> {
    val call = this.trailer().first().NAME().text
    val firstParam = this.trailer().second().arglist().argument().first()

    if (firstParam.text == inputTensor)
        return listOf(call)

    val previousCalls = firstParam.atomExpr()!!.forwardCalls(inputTensor).toTypedArray()
    return listOf(*previousCalls, call)
}

class ModelVisualizerVisitor: Python3ParserBaseVisitor<Unit>() {
    private var inModelDefinition = false
    private val layerSymbols: MutableMap<String, ILayer> = mutableMapOf()
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

    private fun visitConstructor(ctx: FuncdefContext): Unit = ctx.run {
        val memberDeclarations = this.suite()?.stmt()
            ?.filter { it.text.startsWith("self.") && it.isAssignation() }
            ?.map { it.simple_stmt().small_stmt(0).expr_stmt()!! }
            ?: emptyList()

        val layerDeclarations = memberDeclarations.filter {
            val type = it.assignationRightAtomExpr()?.trailer()?.firstOrNull()?.NAME()?.text
            type in LayerType.values().map(LayerType::name)
        }

        for (decl in layerDeclarations) {
            val name = decl.assignationLeftAtomExpr()?.trailer()?.lastOrNull()?.NAME()?.text!!
            val type = decl.assignationRightAtomExpr()?.trailer()
                ?.findLast { it.arglist()?.argument().isNullOrEmpty() && it.text != "()"  }?.NAME()?.text
                ?.let {  typeName -> LayerType.values().find { it.name == typeName } }!!

            if (type != LayerType.Sequential) {
                addLayerSymbol(name, type, decl.parameters())
            } else {
                val sequentialLayers = decl.assignationRightAtomExpr()!!.trailer().last().arglist().argument().mapIndexed { i, arg ->
                        val layerName = "${name}_$i"
                        val layerType = arg.atomExpr()?.trailer()?.findLast {
                                it.arglist()?.argument().isNullOrEmpty() && it.text != "()"
                            }?.NAME()?.text
                            ?.let {  typeName -> LayerType.values().find { it.name == typeName } }!!

                        createLayer(layerType, arg.atomExpr()!!.parameters())
                    }

                layerSymbols[name] = SequentialLayer(sequentialLayers)
            }
        }
    }

    private fun addLayerSymbol(
        name: String,
        type: LayerType,
        parameters: List<ArgumentContext>,
    ) {
        layerSymbols[name] = createLayer(type, parameters)
    }

    private fun createLayer(
        type: LayerType,
        parameters: List<ArgumentContext>,
    ): Layer {
        val inChannels = parameters.let { params ->
            when {
                type.isMaxPool() || type in listOf(LayerType.ReLU, LayerType.Flatten) -> -1
                params.first().ASSIGN() == null -> params.first().text.toInt()
                else -> params.find { it.test()?.firstOrNull()?.text == "in_channels" }?.test(1)?.text?.toInt()!!
            }
        }
        val outChannels = parameters.let { params ->
            when {
                type.isMaxPool() || type in listOf(LayerType.ReLU, LayerType.Flatten) -> -1
                params.count() < 2 -> params.first().text.toInt()
                params.second().ASSIGN() == null -> params.second().text.toInt()
                else -> params.find { it.test()?.firstOrNull()?.text == "out_channels" }?.test(1)?.text?.toInt()!!
            }
        }

        return Layer(type, inChannels, outChannels)
    }

    private fun visitForwardFunction(ctx: FuncdefContext): Unit = ctx.run {
        val inputTensor = parameterList().second()

        val forwardStatements = this.suite()?.stmt()
            ?.filter { it.text.startsWith(inputTensor) && it.isAssignation() }
            ?.filter { "$inputTensor.view(" !in it.text } // todo: flatten
            ?.filter { call -> layerSymbols.filter { it.value.type == LayerType.Flatten }.none { "${it.key}(" in call.text } }
            ?.map { it.simple_stmt().small_stmt(0).expr_stmt()!! }
            ?: emptyList()

        val forwardCalls = forwardStatements.flatMap {
            it.assignationRightAtomExpr()?.forwardCalls(inputTensor) ?: emptyList()
        }

        for (functionName in forwardCalls) {
            model += if (functionName == "relu") {
                ReluLayer()
            } else if(layerSymbols[functionName]!!.type.isMaxPool()) {
                MaxPoolLayer(layerSymbols[functionName]!!.type)
            } else {
                layerSymbols[functionName]!!
            }
        }
    }

    fun toGraphVizCode() = this.model.toGraphVizCode()

}

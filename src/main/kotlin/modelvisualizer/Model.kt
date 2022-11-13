package modelvisualizer

class Model(
    private val layers: MutableList<ILayer> = mutableListOf(),
): MutableList<ILayer> by layers {
    override fun add(element: ILayer): Boolean {
        return when {
            element.type == LayerType.ReLU -> {
                val numChannels = layers.last().outChannels
                layers.add( Layer(LayerType.ReLU, numChannels, numChannels) )
            }
            element.type.isMaxPool() -> {
                val numChannels = layers.lastOrNull()?.outChannels ?: 1
                layers.add( Layer(element.type, numChannels, numChannels) )
            }
            element is SequentialLayer -> {
                layers.addAll(element.layers)
            }
            else -> layers.add(element)
        }
    }

    fun toGraphVizCode() = """
       |digraph G {
       |${this@Model.firstOrNull()?.let { "start -> ${it.qualifier};" }}
       |${this@Model.zipWithNext().joinToString(System.lineSeparator()) { (first, second) ->
            "${first.qualifier} -> ${second.qualifier};"
        } }    
       |${this@Model.lastOrNull()?.let { "${it.qualifier} -> end;" }}
       |}
    """.trimMargin()

    private fun ILayer.getOrdinal(): Int = this@Model.filter { it.type == this.type }.indexOf(this)

    private val ILayer.qualifier get() = this.type.name + this.getOrdinal()
}

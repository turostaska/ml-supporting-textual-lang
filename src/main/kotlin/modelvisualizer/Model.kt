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
       |rankdir=LR;
       |node [shape=record];
       |${clusterCode()}
       |${clusters.firstOrNull()?.let { "start -> ${it.first().qualifier} [label=${it.first().inChannels}];" }}
       |${clusters.zipWithNext().joinToString(System.lineSeparator()) { (first, second) ->
            "${first.last().qualifier} -> ${second.first().qualifier} [label=${first.last().outChannels}];"
        } }    
       |${clusters.lastOrNull()?.let { "${it.last().qualifier} -> end [label=${it.last().outChannels}];" }}
       |}
    """.trimMargin()

    fun toConnectedGraphVizCode() = """
       |digraph G {
       |node [shape=circle];
       |edge [style=invis];
       |splines=false;
       |rankdir=LR;
       |${connectedClusterCode()}
       |
       |edge[style=solid, tailport=e, headport=w];
       |${clusters.firstOrNull()?.firstOrNull()?.outChannels?.let { numChannels ->
            "start -> {${ (0 until numChannels).joinToString("; ") { "Node_0_$it" } } };"
        }}
       |
       |${clusters.map { it.first().outChannels }.zipWithNext().mapIndexed { i, (first, second) ->
            "{ ${ (0 until first).joinToString("; ") { "Node_${i}_$it" } }  } -> " +
            "{ ${ (0 until second).joinToString("; ") { "Node_${i+1}_$it" } }  };"
        }.joinToString(System.lineSeparator()) }
       |
       |${clusters.lastOrNull()?.firstOrNull()?.outChannels?.let { numChannels ->
            "{${ (0 until numChannels).joinToString("; ") { "Node_${clusters.lastIndex}_$it" } } } -> end;"
        } }
       |}
    """.trimMargin()

    private val clusters: List<List<ILayer>> get() {
        // https://stackoverflow.com/questions/65248942/how-to-split-a-list-into-sublists-using-a-predicate-with-kotlin
        return layers.flatMapIndexed { index, layer ->
                when {
                    index == 0 || index == layers.lastIndex -> listOf(index)
                    layer.isClusterRoot() -> listOf(index - 1, index)
                    else -> emptyList()
                }
            }
            .windowed(size = 2, step = 2) { (from, to) -> layers.slice(from..to) }
            .let {
                if (it.lastOrNull()?.lastOrNull()?.isClusterRoot() == true) {
                    it.dropLast(1) + listOf(it.last().dropLast(1), it.last().takeLast(1))
                } else it
            }
    }

    private fun ILayer.isClusterRoot() = this.type.isConv() || this.type == LayerType.Linear

    private fun clusterCode(): String {
        return clusters.mapIndexed { i, cluster -> """
            |subgraph cluster_${i + 1} {
            |    ${cluster.joinToString(" -> ") { it.qualifier }};
            |    label="Layer ${i + 1}";
            |}
        """.trimMargin() }.joinToString(System.lineSeparator())
    }

    private fun connectedClusterCode(): String {
        return clusters.mapIndexed { i, cluster -> """
            |{
            |    ${cluster.firstOrNull()?.outChannels?.let {  numChannels ->
                (0 until numChannels).joinToString("; ") { "Node_${i}_$it" } }};
            |    label="Layer ${i + 1}: ${cluster.first().type}";
            |}
            |{ rank = same; ${cluster.firstOrNull()?.outChannels?.let { numChannels ->
                (0 until numChannels).joinToString(" -> ") { "Node_${i}_$it" } }}; };
        """.trimMargin() }.joinToString(System.lineSeparator())
    }

    private fun ILayer.getOrdinal(): Int = this@Model.filter { it.type == this.type }.indexOf(this)

    private val ILayer.qualifier get() = this.type.name + this.getOrdinal()
}

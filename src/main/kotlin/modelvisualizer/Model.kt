package modelvisualizer

class Model(
    layers: MutableList<Layer> = mutableListOf(),
): MutableList<Layer> by layers {
    fun toGraphVizCode() = """
       |digraph G {
       |${this@Model.firstOrNull()?.let { "start -> ${it.qualifier};" }}
       |${this@Model.zipWithNext().map { (first, second) -> 
            "${first.qualifier} -> ${second.qualifier};"
        }.joinToString(System.lineSeparator())}    
       |${this@Model.lastOrNull()?.let { "${it.qualifier} -> end;" }}
       |}
    """.trimMargin()

    private fun Layer.getOrdinal(): Int = this@Model.filter { it.type == this.type }.indexOf(this)

    private val Layer.qualifier get() = this.type.name + this.getOrdinal()
}

package modelvisualizer

import com.kobra.Python3Lexer
import com.kobra.Python3Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import util.Resources

fun main() {
    val source = Resources.read("model.py")
    val lexer = Python3Lexer(CharStreams.fromString(source))
    val tokens = CommonTokenStream(lexer)
    val program = Python3Parser(tokens).file_input()

    val visitor = ModelVisualizerVisitor().also { it.visit(program) }

    println(visitor.toGraphVizCode())
//    println(visitor.toConnectedGraphVizCode())
}

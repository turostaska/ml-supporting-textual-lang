import com.kobra.kobraLexer
import com.kobra.kobraParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import python.PythonHeaderReader
import symtab.SymtabBuilderVisitor
import syntax.SyntaxTreeBuilderVisitor
import syntax.generateCode
import util.Resources
import util.runPythonScript

val symtabBuilder = SymtabBuilderVisitor()

fun main() {
    val code = Resources.read("hello_world.kb")
    val lexer = kobraLexer(CharStreams.fromString(code))
    val tokens = CommonTokenStream(lexer)
    val program = kobraParser(tokens).program()

    PythonHeaderReader(symtabBuilder.globalScope).readAndAddSymbols()

    symtabBuilder.visit(program)

    val syntaxTreeBuilder = SyntaxTreeBuilderVisitor(symtabBuilder.globalScope).also {
        it.visit(program)
    }

    val pythonCode = syntaxTreeBuilder.generateCode()
    pythonCode.runPythonScript()
}
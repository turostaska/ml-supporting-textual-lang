import com.kobra.kobraLexer
import com.kobra.kobraParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import python.PythonHeaderReader
import symtab.SymtabBuilderVisitor
import syntax.SyntaxTreeBuilderVisitor
import syntax.generateCode
import util.Resources

val symtabBuilder = SymtabBuilderVisitor()

private val readKModule = true

fun main() {
    val code = Resources.read("mnist.kb", readKModule)
    val lexer = kobraLexer(CharStreams.fromString(code))
    val tokens = CommonTokenStream(lexer)
    val program = kobraParser(tokens).program()

    PythonHeaderReader(symtabBuilder.globalScope, symtabBuilder.typeHierarchy).readAndAddSymbols()

    symtabBuilder.visit(program)

    val syntaxTreeBuilder = SyntaxTreeBuilderVisitor(symtabBuilder.globalScope).also {
        it.visit(program)
    }

    syntaxTreeBuilder.generateCode().also(::println)
}
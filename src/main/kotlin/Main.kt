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

fun main() {
    val code = Resources.read("basic_class_declaration")
    val lexer = kobraLexer(CharStreams.fromString(code))
    val tokens = CommonTokenStream(lexer)
    val program = kobraParser(tokens).program()

    PythonHeaderReader(symtabBuilder.globalScope).readAndAddSymbols()

    symtabBuilder.visit(program)

    val syntaxTreeBuilder = SyntaxTreeBuilderVisitor(symtabBuilder.globalScope, symtabBuilder.typeHierarchy).also {
        it.visit(program)
    }

    syntaxTreeBuilder.generateCode().let(::println)
}
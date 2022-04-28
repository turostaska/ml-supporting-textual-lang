import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import symtab.SymtabBuilderVisitor
import symtab.extensions.walk
import syntax.SyntaxTreeBuilderVisitor
import syntax.generateCode
import util.Resources

val symtabBuilder = SymtabBuilderVisitor()

fun main() {
    val code = Resources.read("basic_prop_declarations")
    val lexer = kobraLexer(CharStreams.fromString(code))
    val tokens = CommonTokenStream(lexer)
    val program = kobraParser(tokens).program()

    symtabBuilder.visit(program)

    val syntaxTreeBuilder = SyntaxTreeBuilderVisitor(symtabBuilder.globalScope).also {
        it.visit(program)
    }

    val generatedCode = syntaxTreeBuilder.generateCode()
    symtabBuilder.walk().let(::println)
}
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import symtab.SymtabBuilderVisitor
import syntax.SyntaxTreeBuilderVisitor
import syntax.generateCode
import util.Resources

val symtabBuilder = SymtabBuilderVisitor()

// todo: dipterv portálra máj. 20. tartalomjegyzék
fun main() {
    val code = Resources.read("basic_class_declaration")
    val lexer = kobraLexer(CharStreams.fromString(code))
    val tokens = CommonTokenStream(lexer)
    val program = kobraParser(tokens).program()

    symtabBuilder.visit(program)

    val syntaxTreeBuilder = SyntaxTreeBuilderVisitor(symtabBuilder.globalScope).also {
        it.visit(program)
    }

    syntaxTreeBuilder.generateCode().let(::println)
}
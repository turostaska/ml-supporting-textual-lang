import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import symtab.SymtabBuilderVisitor
import symtab.extensions.walk
import util.Resources

val symtabBuilder = SymtabBuilderVisitor()

fun main() {
    val code = Resources.read("basic_class_declaration")
    val lexer = kobraLexer(CharStreams.fromString(code))
    val tokens = CommonTokenStream(lexer)
    kobraParser(tokens).program().let {
        symtabBuilder.visit(it)
    }

    symtabBuilder.walk().let(::println)
}
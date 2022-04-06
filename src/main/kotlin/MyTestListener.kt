import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

class MyTestListener(
    private val code: String,
): testBaseVisitor<Any>() {
    private val builder = StringBuilder()

    constructor(file: File) : this(file.readText())

    override fun visitProgram(ctx: testParser.ProgramContext) {
        builder.append("fun main() {")
        super.visitProgram(ctx)
    }

    override fun visitPrintStatement(ctx: testParser.PrintStatementContext) {
        builder.append("""${System.lineSeparator()}
            |   println(${ctx.STRING().text})
        """.trimMargin())
        super.visitPrintStatement(ctx)
    }

    fun getKotlinCode(): String {
        val lexer = testLexer(CharStreams.fromString(code))
        val tokens = CommonTokenStream(lexer)
        testParser(tokens).program().let {
            this.visit(it)
        }

        builder.append("${System.lineSeparator()}}")
        return builder.toString()
    }

}
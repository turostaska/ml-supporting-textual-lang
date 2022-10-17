package python

import com.kobra.Python3Lexer
import com.kobra.Python3Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import symtab.Scope
import type.TypeHierarchy
import util.Resources

class PythonHeaderReader(
    private val globalScope: Scope,
    private val typeHierarchy: TypeHierarchy,
) {
    private val source = Resources.read("builtins.pyi")
    private val lexer = Python3Lexer(CharStreams.fromString(source))
    private val tokens = CommonTokenStream(lexer)
    private val program = Python3Parser(tokens).file_input()

    fun readAndAddSymbols() {
        PythonLibVisitor(globalScope, typeHierarchy).visit(program)
    }
}

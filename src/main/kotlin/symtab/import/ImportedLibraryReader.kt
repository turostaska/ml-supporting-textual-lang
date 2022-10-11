package symtab.import

import com.kobra.Python3Lexer
import com.kobra.Python3Parser
import com.kobra.Python3Parser.File_inputContext
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import symtab.Scope
import util.Resources

const val GLOBAL_PACKAGES_PATH = "/usr/lib/python3.10/site-packages/"
const val LOCAL_PACKAGES_PATH = "~/.local/lib/python3.10/site-packages/"

private fun getSourceOfPackage(packageName: String) =
    "$GLOBAL_PACKAGES_PATH/$packageName/__init__.py".let(Resources::readOrNull)
        ?: "$LOCAL_PACKAGES_PATH/$packageName/__init__.py".let(Resources::read)

private fun getRootRuleOfPythonPackage(packageName: String): File_inputContext {
    val source = getSourceOfPackage(packageName)
    val lexer = Python3Lexer(CharStreams.fromString(source))
    val tokens = CommonTokenStream(lexer)
    return Python3Parser(tokens).file_input()
}

class ImportedLibraryReader(
    private val globalScope: Scope,
) {
    fun readAndAddAllSymbols(packageName: String) {
        val rootRule = getRootRuleOfPythonPackage(packageName)
        ImportedLibVisitor(globalScope).visit(rootRule)
    }

    fun readAndAddSpecifiedSymbols(packageName: String, symbolsToImportAs: Map<String, String>) {
        val rootRule = getRootRuleOfPythonPackage(packageName)
        ImportedLibVisitor(globalScope, symbolsToImportAs).visit(rootRule)
    }
}

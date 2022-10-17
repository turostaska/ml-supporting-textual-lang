package symtab.import

import com.kobra.Python3Lexer
import com.kobra.Python3Parser
import com.kobra.Python3Parser.File_inputContext
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import symtab.SymtabBuilderVisitor
import type.TypeHierarchy
import util.Resources
import util.tryOrNull
import java.io.FileNotFoundException

const val GLOBAL_PACKAGES_PATH = "/usr/lib64/python3.10"
const val GLOBAL_SITE_PACKAGES_PATH = "/usr/lib/python3.10/site-packages"
const val LOCAL_SITE_PACKAGES_PATH = "/home/rado/.local/lib/python3.10/site-packages"
const val LOCAL_VS_CODE_PACKAGES_PATH = "/home/rado/.vscode/extensions/ms-python.vscode-pylance-2022.10.20/dist/typeshed-fallback/stdlib"

val SITE_PACKAGE_DIRS = listOf(GLOBAL_SITE_PACKAGES_PATH, LOCAL_SITE_PACKAGES_PATH)

private fun getSourceOfPackage(packageName: String): String {
    val packageName = packageName.replace('.', '/')

    return tryOrNull { "$GLOBAL_PACKAGES_PATH/$packageName.py".let(Resources::read) } ?:
        tryOrNull { "$GLOBAL_PACKAGES_PATH/$packageName/__init__.py".let(Resources::read) } ?:
        SITE_PACKAGE_DIRS.map { dir -> tryOrNull {
            "$dir/$packageName/__init__.py".let(Resources::read)
        } }.find { it != null } ?:
        tryOrNull { "$LOCAL_VS_CODE_PACKAGES_PATH/$packageName.pyi".let(Resources::read) } ?:
        tryOrNull { "$LOCAL_VS_CODE_PACKAGES_PATH/$packageName/__init__.pyi".let(Resources::read) } ?:
        throw FileNotFoundException("Package not found: $packageName")
}

private val visitedModules = mutableSetOf<String>()

private fun getRootRuleOfPythonPackage(packageName: String): File_inputContext {
    val source = getSourceOfPackage(packageName)
    val lexer = Python3Lexer(CharStreams.fromString(source))
    val tokens = CommonTokenStream(lexer)
    return Python3Parser(tokens).file_input()
}

class ImportedLibraryReader(
    private val symtabBuilder: SymtabBuilderVisitor,
    private val typeHierarchy: TypeHierarchy,
) {
    fun readAndAddAllSymbols(packageName: String) {
        val rootRule = getRootRuleOfPythonPackage(packageName)

        if (!visitedModules.add(packageName)) return

        ImportedLibVisitor(symtabBuilder, typeHierarchy).visit(rootRule)
    }

    fun readAndAddSpecifiedSymbols(packageName: String, symbolsToImportAs: Map<String, String>) {
        val rootRule = getRootRuleOfPythonPackage(packageName)

        if (!visitedModules.add(packageName)) return

        ImportedLibVisitor(symtabBuilder, typeHierarchy, symbolsToImportAs).visit(rootRule)
    }
}

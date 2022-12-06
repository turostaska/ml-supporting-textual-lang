package symtab.import

import com.kobra.Python3Lexer
import com.kobra.Python3Parser
import com.kobra.Python3Parser.File_inputContext
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import symtab.SymtabBuilderVisitor
import type.TypeHierarchy
import util.Resources
import util.pathIfExistsElseNull
import util.tryOrNull
import java.io.File
import java.io.FileNotFoundException

const val GLOBAL_PACKAGES_PATH = "/usr/lib64/python3.10"
const val GLOBAL_SITE_PACKAGES_PATH = "/usr/lib/python3.10/site-packages"
const val LOCAL_SITE_PACKAGES_PATH = "/home/rado/.local/lib/python3.10/site-packages"
const val LOCAL_VS_CODE_PACKAGES_PATH = "/home/rado/.vscode/extensions/ms-python.vscode-pylance-2022.10.30/dist/typeshed-fallback/stdlib"

val SITE_PACKAGE_DIRS = listOf(GLOBAL_SITE_PACKAGES_PATH, LOCAL_SITE_PACKAGES_PATH)

fun findSubmoduleInFolder(folder: String, submoduleName: String): String {
    return File("$folder/$submoduleName.py").pathIfExistsElseNull()
        ?: File("$folder/$submoduleName/__init__.py").pathIfExistsElseNull()
        ?: throw FileNotFoundException("Submodule not found: $submoduleName")
}

fun findFolderToMainModule(moduleName: String): String {
    return findPathToMainModule(moduleName).substringBeforeLast('/')
}

private fun findPathToMainModule(moduleName: String): String {
    return File("$GLOBAL_PACKAGES_PATH/$moduleName.py").pathIfExistsElseNull()
        ?: SITE_PACKAGE_DIRS.map { dir ->
            File("$dir/$moduleName/__init__.py").pathIfExistsElseNull()
                ?: File("$dir/$moduleName.py").pathIfExistsElseNull()
        }.find { it != null } ?: throw FileNotFoundException("Package not found: $moduleName")
}

/**
 * @param qualifier: Full qualifier of package name separated with dots, e.g.: 'first.second.third.module'
 */
fun getSourceOfHierarchicalPackage(qualifier: String): String {
    val moduleName = qualifier.split(".").last()
    var parentModules = qualifier.split(".").dropLast(1)

    while (parentModules.isNotEmpty()) {
        tryOrNull { getSourceOfPackage(moduleName, parentModules) }?.let { return it }
        parentModules = parentModules.dropLast(1)
    }

    return getSourceOfPackage(moduleName, emptyList())
}

// todo: eltárolni a jelenlegi mappáját a visitelt python fájlnak
private fun getSourceOfPackage(
    moduleName: String,
    parentModules: List<String>,
): String {
    val packageName = (parentModules.joinToString("/", postfix = "/") + moduleName).removePrefix("/")

    val source = getSourceOfPackageOrNull(moduleName, parentModules)
        ?: if (moduleName.lowercase() != moduleName)
            getSourceOfPackageOrNull(moduleName.lowercase(), parentModules)
        else null
//        ?: getSourceOfPackageOrNull(parentModules.last(), parentModules.dropLast(1))

    return source
        ?: throw FileNotFoundException("Package not found: $packageName")
}

private fun getSourceOfPackageOrNull(
    moduleName: String,
    parentModules: List<String>,
): String? {
    val packageName = (parentModules.joinToString("/", postfix = "/") + moduleName).removePrefix("/")

    return "$GLOBAL_PACKAGES_PATH/$packageName.py".let(Resources::readOrNull) ?:
    "$GLOBAL_PACKAGES_PATH/$packageName/__init__.py".let(Resources::readOrNull) ?:
    SITE_PACKAGE_DIRS.map { dir ->
        "$dir/$packageName/__init__.pyi".let(Resources::readOrNull) ?:
        "$dir/$packageName/__init__.py".let(Resources::readOrNull) ?:
        "$dir/$packageName.pyi".let(Resources::readOrNull) ?:
        "$dir/$packageName.py".let(Resources::readOrNull)
    }.find { it != null } ?:
    "$LOCAL_VS_CODE_PACKAGES_PATH/$packageName.pyi".let(Resources::readOrNull) ?:
    "$LOCAL_VS_CODE_PACKAGES_PATH/$packageName.py".let(Resources::readOrNull) ?:
    "$LOCAL_VS_CODE_PACKAGES_PATH/$packageName/__init__.pyi".let(Resources::readOrNull)
}

private val visitedModules = mutableSetOf<String>()

private fun getRootRuleOfPythonPackage(packageName: String): File_inputContext {
    val source = getSourceOfHierarchicalPackage(packageName)
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

        ImportedLibVisitor(symtabBuilder, typeHierarchy, packageName).visit(rootRule)
    }

    fun readAndAddSpecifiedSymbols(packageName: String, symbolsToImportAs: Map<String, String>) {
        val rootRule = getRootRuleOfPythonPackage(packageName)

        if (!visitedModules.add(packageName)) return

        ImportedLibVisitor(symtabBuilder, typeHierarchy, packageName, symbolsToImportAs).visit(rootRule)
    }
}

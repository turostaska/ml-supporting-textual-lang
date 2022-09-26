package symtab.extensions

import com.kobra.kobraParser.*
import type.TypeNames
import util.second

val FunctionDeclarationContext.functionName: String
    get() = simpleIdentifier().Identifier().text

val FunctionParameterContext.paramName: String
    get() = identifier().first().simpleIdentifier().first().Identifier().text

val FunctionParameterContext.paramTypeName: String
    get() = identifier().second().simpleIdentifier().first().Identifier().text

val FunctionDeclarationContext.params
    get() = functionParameters().functionParameter()
        .associate { it.paramName to it.paramTypeName }

val FunctionDeclarationContext.returnTypeNameOrNull
    get() = type()?.simpleIdentifier()?.Identifier()?.text

val FunctionDeclarationContext.returnTypeName
    get() = returnTypeNameOrNull ?: TypeNames.UNIT

val FunctionDeclarationContext.statements: List<StatementContext>
    get() = functionBody().block().statements().statement()

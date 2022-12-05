package symtab.extensions

import com.kobra.kobraParser.PostfixUnarySuffixContext
import syntax.expression.toPythonCode

fun PostfixUnarySuffixContext.isStaticNavigationSuffix() =
    this.navigationSuffix()?.memberAccessOperator()?.COLONCOLON() != null

fun PostfixUnarySuffixContext.isMemberNavigationSuffix() =
    this.navigationSuffix()?.memberAccessOperator()?.DOT() != null

fun PostfixUnarySuffixContext.isCallSuffix() = this.callSuffix() != null

fun PostfixUnarySuffixContext.params() = callSuffix().valueArguments().valueArgument().toList()

fun PostfixUnarySuffixContext.namedParams() = callSuffix().valueArguments().valueArgument().map {
    it.simpleIdentifier()?.text to it.expression().toPythonCode()
}.joinToString { (k,v) -> k?.let { "$k=$v" } ?: v }

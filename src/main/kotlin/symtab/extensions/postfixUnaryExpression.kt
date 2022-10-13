package symtab.extensions

import com.kobra.kobraParser.PostfixUnarySuffixContext

fun PostfixUnarySuffixContext.isStaticNavigationSuffix() =
    this.navigationSuffix()?.memberAccessOperator()?.COLONCOLON() != null

fun PostfixUnarySuffixContext.isMemberNavigationSuffix() =
    this.navigationSuffix()?.memberAccessOperator()?.DOT() != null

fun PostfixUnarySuffixContext.isCallSuffix() = this.callSuffix() != null

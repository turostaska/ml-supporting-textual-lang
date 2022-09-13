package symtab.extensions

import com.kobra.kobraParser

val kobraParser.ClassParameterContext.isMember get() = (VAL() != null || VAR() != null)

val kobraParser.ClassParameterContext.isNotMember get() = !isMember

val kobraParser.ClassParameterContext.mutability: String get() = VAL()?.text.orEmpty() + VAR()?.text.orEmpty()

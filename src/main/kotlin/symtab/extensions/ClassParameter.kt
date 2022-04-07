package symtab.extensions

import kobraParser.ClassParameterContext

val ClassParameterContext.isMember get() = (VAL() != null || VAR() != null)

val ClassParameterContext.isNotMember get() = !isMember

val ClassParameterContext.mutability: String get() = VAL()?.text.orEmpty() + VAR()?.text.orEmpty()

val ClassParameterContext.isMutable get() = (this.mutability.lowercase() == "var")

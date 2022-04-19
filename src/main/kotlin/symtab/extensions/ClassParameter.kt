package symtab.extensions

import kobraParser.ClassParameterContext
import symtab.Symbol.Mutability.VAR
import symtab.Symbol.Mutability.valueOf

val ClassParameterContext.isMember get() = (VAL() != null || VAR() != null)

val ClassParameterContext.isNotMember get() = !isMember

val ClassParameterContext.mutability: String get() = VAL()?.text.orEmpty() + VAR()?.text.orEmpty()

val ClassParameterContext.isMutable get() = (valueOf(mutability.uppercase()) == VAR)

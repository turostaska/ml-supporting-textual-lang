package symtab.extensions

import com.kobra.kobraParser
import symtab.Symbol.Mutability.VAR
import symtab.Symbol.Mutability.valueOf

val kobraParser.ClassParameterContext.isMember get() = (VAL() != null || VAR() != null)

val kobraParser.ClassParameterContext.isNotMember get() = !isMember

val kobraParser.ClassParameterContext.mutability: String get() = VAL()?.text.orEmpty() + VAR()?.text.orEmpty()

val kobraParser.ClassParameterContext.isMutable get() = (valueOf(mutability.uppercase()) == VAR)

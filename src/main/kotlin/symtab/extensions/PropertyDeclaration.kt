package symtab.extensions

import com.kobra.kobraParser

val kobraParser.PropertyDeclarationContext.mutability: String get() = VAL()?.text.orEmpty() + VAR()?.text.orEmpty()

val kobraParser.PropertyDeclarationContext.isMutable get() = (this.mutability.lowercase() == "var")

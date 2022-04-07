package symtab.extensions

import kobraParser.PropertyDeclarationContext

val PropertyDeclarationContext.mutability: String get() = VAL()?.text.orEmpty() + VAR()?.text.orEmpty()

val PropertyDeclarationContext.isMutable get() = (this.mutability.lowercase() == "var")

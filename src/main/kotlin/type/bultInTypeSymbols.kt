package type

import symtab.Scope
import symtab.TypeSymbol
import symtab.extensions.resolveTypeOrThrow

val Scope.UNIT: TypeSymbol get() {
    if (!this.isGlobal)
        return this.parent!!.UNIT

    return resolveTypeOrThrow(TypeNames.UNIT)
}

val Scope.ANY: TypeSymbol get() {
    if (!this.isGlobal)
        return this.parent!!.ANY

    return resolveTypeOrThrow(TypeNames.ANY)
}

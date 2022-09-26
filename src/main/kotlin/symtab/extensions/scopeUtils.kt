package symtab.extensions

import symtab.Scope

fun Scope.resolveTypeOrThrow(name: String) =
    resolveType(name) ?: throw RuntimeException("Type '$name' does not exist in scope ${this.name}")

fun Scope.resolveMethodOrThrow(name: String) =
    resolveMethod(name) ?: throw RuntimeException("Method symbol '$name' does not exist in scope ${this.name}")

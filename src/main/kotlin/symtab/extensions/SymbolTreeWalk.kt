package symtab.extensions

import symtab.Scope
import symtab.SymtabBuilderVisitor

fun Scope.walk(): String = StringBuilder().also { sb ->
    sb.append(this.toString())
    this.children.forEach {
        sb.append(it.walk())
    }
}.toString()

fun SymtabBuilderVisitor.walk() = this.globalScope.walk()

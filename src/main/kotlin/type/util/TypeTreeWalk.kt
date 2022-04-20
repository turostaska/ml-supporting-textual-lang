package type.util

import type.Type
import type.TypeHierarchy

fun TypeHierarchy.find(type: Type): Type? = root.findInSubtree(type)

fun String.asType() = Type(this.removeSuffix("?"), "?" in this)

fun TypeHierarchy.find(type: String) = root.findInSubtree(type.asType()) ?:
    throw RuntimeException("Can't find type '$type' in type hierarchy")

fun Type.findInSubtree(type: Type): Type? {
    if (this == type) return this
    this.children.forEach { child ->
        child.findInSubtree(type)?.let { return it }
    }
    return null
}

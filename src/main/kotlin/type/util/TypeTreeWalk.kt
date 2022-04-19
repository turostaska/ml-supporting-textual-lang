package type.util

import type.Type
import type.TypeHierarchy

fun TypeHierarchy.find(type: Type): Type? = root.findInSubtree(type)

fun String.asType() = Type(this.removeSuffix("?"), "?" in this)

fun TypeHierarchy.find(type: String) : Type? = root.findInSubtree(type.asType())

fun Type.findInSubtree(type: Type): Type? {
    if (this == type) return this
    this.children.forEach { child ->
        child.findInSubtree(type)?.let { return it }
    }
    return null
}

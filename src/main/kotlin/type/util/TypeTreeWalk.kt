package type.util

import type.Type
import type.TypeHierarchy

fun TypeHierarchy.find(type: Type): Type? = root.findInSubtree(type)

fun String.asType(
    pythonName: String = this.removeSuffix("?")
) = Type(this.removeSuffix("?"), "?" in this, pythonName = pythonName)

fun TypeHierarchy.find(type: String): Type =
    root.findInSubtree(type.trim().asType()) ?: throw RuntimeException("Can't find type '$type' in type hierarchy")

operator fun TypeHierarchy.contains(type: Type) = root.findInSubtree(type) != null

operator fun TypeHierarchy.contains(type: String) = root.findInSubtree(type.asType()) != null

fun Type.findInSubtree(type: Type): Type? {
    if (this == type) return this
    this.children.forEach { child ->
        child.findInSubtree(type)?.let { return it }
    }
    return null
}

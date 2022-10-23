package util

fun <T> List<T>.second(): T = if (this.count() < 2)
    throw NoSuchElementException("List has no second item.")
else this[1]

fun <T> List<T>.secondOrNull(): T? = if (this.count() < 2) null else this[1]

fun String.splitOnWhitespaces(): List<String> = this.split("\\s+".toRegex()).filter { it.isNotBlank() }

fun List<String>.containsQualifier(parents: List<String>, name: String): Boolean {
    return if (parents.isEmpty()) {
        name in this
    } else {
        val qualifier = "${ parents.joinToString(".") }.$name"
        if (qualifier in this)
            true
        else this.containsQualifier(parents.dropLast(1), name)
    }
}

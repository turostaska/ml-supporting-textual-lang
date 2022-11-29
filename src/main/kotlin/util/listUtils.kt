package util

fun <T> List<T>.second(): T = if (this.count() < 2)
    throw NoSuchElementException("List has no second item.")
else this[1]

fun <T> List<T>.secondOrNull(): T? = if (this.count() < 2) null else this[1]

fun String.splitOnWhitespaces(): List<String> = this.split("\\s+".toRegex()).filter { it.isNotBlank() }

fun List<String>.containsQualifier(parents: List<String>, name: String): Boolean {
    return if (parents.isEmpty()) {
        this.any { name in it }
    } else {
        val qualifier = "${ parents.joinToString(".") }.$name"
        if (this.any { qualifier in it })
            true
        // To import stuff for Dropout, stuff from the dropout package are needed
        else if (this.any { it.lowercase() in qualifier })
            true
        else this.containsQualifier(parents.dropLast(1), name)
    }
}

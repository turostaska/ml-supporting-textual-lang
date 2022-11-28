package util

const val TAB = "\t"

fun String.prependTab(num: Int) =
    this.lines().joinToString(System.lineSeparator()) { it.prependIndent(TAB.repeat(num)) }

fun String.prependTabToAllLinesButFirst(num: Int) = this.prependTab(num).removePrefix(TAB.repeat(num))

inline fun String.appendIf(condition: Boolean, what: String.() -> String) =
    this + if (condition) what() else ""

fun takeIf(condition: Boolean, value: () -> String) = if (condition) value() else ""

fun <T> Iterable<T>.joinToCodeWithTabToAllLinesButFirst(num: Int, transform: ((T) -> String)) =
    this.joinToString(System.lineSeparator()) {
        transform(it).prependTab(num)
    }.removePrefix(TAB.repeat(num))

fun String.splitOnFirst(delimiter: String): Pair<String, String> {
    require(delimiter in this)

    val before = this.substringBefore(delimiter)
    val after = this.substringAfter(delimiter)

    return before to after
}

package util

const val TAB = "\t"

fun String.prependTab(num: Int) =
    this.lines().joinToString(System.lineSeparator()) { it.prependIndent(TAB.repeat(num)) }

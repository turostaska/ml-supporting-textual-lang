package syntax.expression

private val TEMPLATE_REGEX = """\${"$"}\{[^\}]*\}""".toRegex()

fun toPythonFormatString(str: String): String {
    val str = str.removeSurrounding("\"")
    val parts = str.split(TEMPLATE_REGEX)
    val identifiers = str.split(*parts.filter { it.isNotBlank() }.toTypedArray())
        .map { it.removePrefix("\$") }

    val sb = StringBuilder("f\"")
    parts.forEachIndexed { i, part ->
        sb.append(part)
        sb.append(identifiers[i])
    }
    sb.append("\"")

    return sb.toString()
}

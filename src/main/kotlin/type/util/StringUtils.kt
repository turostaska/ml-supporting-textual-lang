package type.util

inline fun String.appendIf(condition: Boolean, what: String.() -> String) =
    this + if (condition) what() else ""

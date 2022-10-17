package util

inline fun throwError(message: () -> String) {
    throw RuntimeException(message())
}

inline fun <R> tryOrNull(block: () -> R): R? {
    return try {
        block()
    } catch (e: Throwable) {
        null
    }
}

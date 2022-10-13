package util

inline fun throwError(message: () -> String) {
    throw RuntimeException(message())
}

package util

fun <T> List<T>.second(): T = if (this.count() < 2)
    throw NoSuchElementException("List has no second item.")
else this[1]

fun <T> List<T>.secondOrNull(): T? = if (this.count() < 2) null else this[1]

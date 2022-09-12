package util

fun <T> List<T>.second(): T = if (this.count() < 2)
    throw NoSuchElementException("List has no second item.")
else this[1]

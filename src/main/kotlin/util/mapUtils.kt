package util

fun <K, V> Map<K, V>.getKey(value: V) =
    entries.firstOrNull { it.value == value }?.key

fun <K, V> Map<K, V>.getKeyOrThrow(value: V) =
    getKey(value) ?: throw RuntimeException("Map does not contain value '$value'")

@Suppress("UNCHECKED_CAST")
fun <K, V> Map<K?, V?>.filterNonNull(): Map<K, V> =
    filterKeys { it != null }.filterValues { it != null } as Map<K, V>

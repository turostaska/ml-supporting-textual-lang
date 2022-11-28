package util

// source: https://gist.github.com/kiwiandroiddev/fef957a69f91fa64a46790977d98862b
/**
 * E.g.
 * cartesianProduct(listOf(1, 2, 3), listOf(true, false)) returns
 *  [(1, true), (1, false), (2, true), (2, false), (3, true), (3, false)]
 */
fun <T, U> cartesianProduct(c1: Collection<T>, c2: Collection<U>): List<Pair<T, U>> {
    return c1.flatMap { lhsElem -> c2.map { rhsElem -> lhsElem to rhsElem } }
}

@JvmName("cartesianProductExt")
fun<T, U> Collection<T>.cartesianProduct(collection: Collection<U>): List<Pair<T, U>> {
    return cartesianProduct(this, collection)
}

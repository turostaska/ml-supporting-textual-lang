package type

class TypeHierarchy {
    val anyN = Type("Any", true)
    val any = Type("Any", false, _parents = mutableSetOf(anyN))
    val stringN = Type("String", true, _parents = mutableSetOf(anyN))
    val string = Type("String", false, _parents = mutableSetOf(any))
    val numberN = Type("Number", true, _parents = mutableSetOf(anyN))
    val number = Type("Number", false, _parents = mutableSetOf(any))
    val booleanN = Type("Boolean", true, _parents = mutableSetOf(anyN))
    val boolean = Type("Boolean", false, _parents = mutableSetOf(any))
    val nothingN = Type("Nothing", true, _parents = mutableSetOf(stringN, numberN, booleanN))
    val nothing = Type("Nothing", false, _parents = mutableSetOf(string, number, boolean))

    val root = anyN
}

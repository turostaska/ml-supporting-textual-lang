package type

// todo: add Unit and Range to type hierarchy
object TypeNames {
    const val UNIT = "Unit"

    const val ANY = "Any"
    const val STRING = "String"
    const val BOOLEAN = "Boolean"
    const val NOTHING = "Nothing"
    const val INT = "Int"
    const val RANGE = "Range"
    const val LIST = "List"
    const val FLOAT = "Float"

    const val ANY_N = "Any?"
    const val STRING_N = "String?"
    const val BOOLEAN_N = "Boolean?"
    const val NOTHING_N = "Nothing?"
    const val INT_N = "Int?"
    const val RANGE_N = "Range?"
    const val LIST_N = "List?"
    const val FLOAT_N = "Float?"

    const val ANY_PY = "any"
    const val STRING_PY = "str"
    const val BOOLEAN_PY = "bool"
    const val INT_PY = "int"
    const val RANGE_PY = "range"
    const val UNIT_PY = "None"
    const val LIST_PY = "list"
    const val FLOAT_PY = "float"

    val pythonTypeNamesToKobraMap = mapOf(
        ANY_PY to ANY,
        STRING_PY to STRING,
        BOOLEAN_PY to BOOLEAN,
        INT_PY to INT,
        RANGE_PY to RANGE,
        LIST_PY to LIST,
        FLOAT_PY to FLOAT,
        UNIT_PY to UNIT,
    )
}

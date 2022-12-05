package type

import kotlin.reflect.full.memberProperties

object BuiltInTypes {
    val anyN = Type(TypeNames.ANY, true, pythonName = TypeNames.ANY_PY)
    val any = Type(TypeNames.ANY, false, pythonName = TypeNames.ANY_PY).apply {
        addParent(anyN)
    }

    val nothingN = Type(TypeNames.NOTHING, true, pythonName = TypeNames.UNIT_PY).apply {
        addParent(anyN)
    }
    val nothing = Type(TypeNames.NOTHING, false).apply {
        addParents(any, nothingN)
    }

    val string = Type(TypeNames.STRING, false, pythonName = TypeNames.STRING_PY).also {
        addBuiltInType(it)
    }
    val stringN = string.nullableVariant

    val int = Type(TypeNames.INT, false, pythonName = TypeNames.INT_PY).also {
        addBuiltInType(it)
    }
    val intN = int.nullableVariant

    val float = Type(TypeNames.FLOAT, false, pythonName = TypeNames.FLOAT_PY).also {
        addBuiltInType(it)
    }
    val floatN = float.nullableVariant

    val range = Type(TypeNames.RANGE, false, pythonName = TypeNames.RANGE_PY).also {
        addBuiltInType(it)
    }
    val rangeN = range.nullableVariant

    val boolean = Type(TypeNames.BOOLEAN, false, pythonName = TypeNames.BOOLEAN_PY).also {
        addBuiltInType(it)
    }
    val booleanN = boolean.nullableVariant

    val list = Type(TypeNames.LIST, false, pythonName = TypeNames.LIST_PY).also {
        addBuiltInType(it)
    }
    val listN = list.nullableVariant

    val tuple = Type(TypeNames.TUPLE, false, pythonName = TypeNames.TUPLE_PY).also {
        addBuiltInType(it)
    }
    val tupleN = tuple.nullableVariant

    val unit = Type(TypeNames.UNIT, false, pythonName = TypeNames.UNIT_PY).also {
        addBuiltInType(it)
    }
    val unitN = unit.nullableVariant

    val all = javaClass.kotlin.memberProperties.mapNotNull { it.get(this) as? Type }

    private fun Type.insertUnder(type: Type) {
        type.addParent(this)

        val nothingChild = if (type.nullable) nothingN else nothing
        type.addChild(nothingChild)

        nothingChild.removeParent(this)
    }

    private fun addBuiltInType(nonNullableType: Type) {
        require(!nonNullableType.nullable) { "Parameter should be non-nullable" }

        val nullableType = Type(nonNullableType.name.removeSuffix("?"), true).apply {
            addChild(nonNullableType)
        }

        any.insertUnder(nonNullableType)
        anyN.insertUnder(nullableType)
    }
}
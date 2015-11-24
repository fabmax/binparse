package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 17.11.2015.
 */

class ArrayParser private constructor(fieldName: String, type: FieldParser, length: ArrayParser.Length):
        FieldParser(fieldName) {

    internal enum class LengthMode {
        FIXED,
        BY_FIELD,
        BY_VALUE
    }

    internal data class Length(
            val mode: LengthMode,
            val strLength: String,
            val intLength: Int,
            val termParser: FieldParser?)

    private val type = type
    private val length = length

    override fun toString(): String {
        return "$fieldName: ArrayParser: type: $type, length-mode: $length"
    }

    override fun matchesDef(field: Field<*>, parent: StructInstance): Boolean {
        if (field is ArrayField) {
            if (field.find { !type.matchesDef(it, parent) } != null) {
                return false
            } else {
                return true
            }
        } else {
            return false
        }
    }

    override fun parse(reader: BinReader, result: StructInstance): ArrayField {
        val field = ArrayField(fieldName)

        if (length.mode == LengthMode.BY_VALUE) {
            while (length.termParser != null) {
                reader.mark()
                if (length.termParser.parseField(reader, result).getIntValue() == 0L) {
                    break
                }
                reader.reset()

                if (parseItem(field, reader, result)) {
                    break
                }
            }

        } else {
            val length = when (length.mode) {
                LengthMode.FIXED -> length.intLength
                LengthMode.BY_FIELD -> result[length.strLength].getIntValue().toInt()
                else -> 0
            }
            for (i in 1..length) {
                if (parseItem(field, reader, result)) {
                    break
                }
            }
        }
        return field
    }

    private fun parseItem(field: ArrayField, reader: BinReader, resultSet: StructInstance): Boolean {
        val item = type.parseField(reader, resultSet)
        item.index = field.value.size
        field.value.add(item)
        return item.hasQualifier(Field.QUAL_BREAK)
    }

    internal open class Factory() : FieldParserFactory() {
        override fun createParser(definition: Item): FieldParser {
            return ArrayParser(definition.identifier, parseType(definition), parseLength(definition))
        }

        internal fun parseLength(definition: Item): Length {
            val lengthItem = getItem(definition.childrenMap, "length")
            var lenMode = LengthMode.BY_FIELD
            val strLen = lengthItem.value
            val decLen = parseDecimal(strLen)
            var intLen = 0
            var termParser: FieldParser? = null

            if (decLen != null) {
                lenMode = LengthMode.FIXED
                intLen = decLen.toInt()
                if (intLen <= 0) {
                    throw IllegalArgumentException("Invalid length specified: $intLen")
                }
            } else if (FieldParserFactory.isType(strLen)) {
                lenMode = LengthMode.BY_VALUE
                termParser = FieldParserFactory.createParser(lengthItem)
            }
            return Length(lenMode, strLen, intLen, termParser)
        }

        private fun parseType(definition: Item): FieldParser {
            return FieldParserFactory.createParser(getItem(definition.childrenMap, "type"))
        }
    }
}

class ArrayField(name: String, items: ArrayList<Field<*>> = ArrayList<Field<*>>()) :
        Field<ArrayList<Field<*>>>(name, items), Iterable<Field<*>> by items {
    val length: Int
        get() = value.size

    operator fun get(index: Int): Field<*> {
        return value[index]
    }

    fun getArray(index: Int): ArrayField {
        return value[index] as ArrayField
    }

    fun getInt(index: Int): Int {
        return getLong(index).toInt()
    }

    fun getLong(index: Int): Long {
        return value[index].getIntValue()
    }

    fun getString(index: Int): String {
        return value[index].getStringValue()
    }

    fun getStruct(index: Int): StructInstance {
        return value[index] as StructInstance
    }

    override fun hasQualifier(qualifier: String): Boolean {
        if (qualifier != QUAL_COLLECT && super.hasQualifier(QUAL_COLLECT)) {
            return value.find { it.hasQualifier(qualifier) } != null
        } else {
            return super.hasQualifier(qualifier)
        }
    }

    override fun toString(indent: Int, withFieldName: Boolean): String {
        val buf = StringBuffer()
        for (i in 1 .. indent) {
            buf.append(' ')
        }
        val ind = buf.toString()

        if (withFieldName) {
            buf.append(name).append(" = ")
        }

        if (value.isEmpty()) {
            buf.append("[]")
        } else {
            buf.append("[\n")
            value.forEach { buf.append(it.toString(indent + 2, false)).append(",\n") }
            buf.append(ind).append("]")
        }
        return buf.toString()
    }

    operator fun Array<Int>.unaryPlus() {
        value.clear()
        var i = 0
        for (int in this) {
            value.add(IntField("[{$i++}]", int.toLong()))
        }
    }

    operator fun Array<Long>.unaryPlus() {
        value.clear()
        var i = 0
        for (long in this) {
            value.add(IntField("[{$i++}]", long))
        }
    }

    operator fun Array<String>.unaryPlus() {
        value.clear()
        var i = 0
        for (str in this) {
            value.add(StringField("[{$i++}]", str))
        }
    }

    operator fun Array<StructInstance>.unaryPlus() {
        value.clear()
        for (struct in this) {
            value.add(struct)
        }
    }

    operator fun Array<Field<*>>.unaryPlus() {
        value.clear()
        for (field in this) {
            value.add(field)
        }
    }
}

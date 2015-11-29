package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 17.11.2015.
 */

class ArrayDef private constructor(fieldName: String, type: FieldDef<*>, length: ArrayDef.Length) :
        FieldDef<ArrayField>(fieldName) {

    companion object {
        internal val nullTermCtx: ContainerField<*> = NullTermCtx()
    }

    private class NullTermCtx : ContainerField<IntField>("null", IntField("null", 0)) {
        override fun get(name: String): Field<*> = value
        override fun get(index: Int): Field<*> = value
    }

    internal enum class LengthMode {
        FIXED,
        BY_FIELD,
        BY_VALUE
    }

    internal data class Length(
            val mode: LengthMode,
            val strLength: String,
            val intLength: Int,
            val termParser: IntDef?)

    private val type = type
    private val length = length

    protected override fun parse(reader: BinReader, context: ContainerField<*>): ArrayField {
        val array = ArrayField(fieldName)

        if (length.mode == LengthMode.BY_VALUE) {
            while (length.termParser != null) {
                reader.mark()
                if (length.termParser.parseField(reader, nullTermCtx).value == 0L) {
                    break
                }
                reader.reset()
                if (parseItem(array, reader, context)) {
                    break
                }
            }
        } else {
            val length = when (length.mode) {
                LengthMode.FIXED -> length.intLength
                LengthMode.BY_FIELD -> context.getInt(length.strLength).intValue
                else -> 0
            }
            for (i in 1..length) {
                if (parseItem(array, reader, context)) {
                    break
                }
            }
        }
        return array
    }

    override fun prepareWrite(context: ContainerField<*>) {
        super.prepareWrite(context)
        val array = context.getArray(fieldName);
        if (length.mode == LengthMode.BY_FIELD) {
            context.getInt(length.strLength).intValue = array.size
        }
        for (i in 0 .. array.size - 1) {
            type.fieldName = i.toString()
            type.prepareWrite(array)
        }
    }

    override fun write(writer: BinWriter, context: ContainerField<*>) {
        val array = context.getArray(fieldName);
        var isBreak = false;
        for (i in 0 .. array.size - 1) {
            type.fieldName = i.toString()
            type.write(writer, array)
            if (array[i].hasQualifier(Field.QUAL_BREAK)) {
                isBreak = true;
                break;
            }
        }
        if (length.mode == ArrayDef.LengthMode.BY_VALUE && length.termParser != null && !isBreak) {
            length.termParser.write(writer, nullTermCtx)
        }
    }

    override fun matchesDef(context: ContainerField<*>): Boolean {
        val array = context[fieldName]
        if (array is ArrayField) {
            for (i in 0 .. array.size - 1) {
                type.fieldName = i.toString()
                if (!type.matchesDef(array)) {
                    return false
                }
            }
            return true
        }
        return false
    }

    private fun parseItem(field: ArrayField, reader: BinReader, context: ContainerField<*>): Boolean {
        val item = type.parseField(reader, context)
        item.index = field.value.size
        item.name = item.index.toString()
        field.value.add(item)
        return item.hasQualifier(Field.QUAL_BREAK)
    }

    override fun toString(): String {
        return "$fieldName: ArrayParser: type: $type, length-mode: $length"
    }

    internal open class Factory() : FieldDefFactory() {
        companion object {
            internal fun parseLength(definition: Item): Length {
                val lengthItem = FieldDefFactory.getItem(definition.childrenMap, "length")
                var lenMode = LengthMode.BY_FIELD
                val strLen = lengthItem.value
                val decLen = FieldDefFactory.parseDecimal(strLen)
                var intLen = 0
                var termParser: IntDef? = null

                if (decLen != null) {
                    lenMode = LengthMode.FIXED
                    intLen = decLen.toInt()
                    if (intLen <= 0) {
                        throw IllegalArgumentException("Invalid length specified: $intLen")
                    }
                } else if (FieldDefFactory.isType(strLen)) {
                    lenMode = LengthMode.BY_VALUE
                    termParser = FieldDefFactory.createParser(lengthItem) as IntDef
                }
                return Length(lenMode, strLen, intLen, termParser)
            }
        }

        override fun createParser(definition: Item): ArrayDef {
            return ArrayDef(definition.identifier, parseType(definition), parseLength(definition))
        }

        private fun parseType(definition: Item): FieldDef<*> {
            return FieldDefFactory.createParser(getItem(definition.childrenMap, "type"))
        }
    }
}

class ArrayField(name: String, items: ArrayList<Field<*>> = ArrayList<Field<*>>()) :
        ContainerField<ArrayList<Field<*>>>(name, items), Iterable<Field<*>> by items {
    val size: Int
        get() = value.size

    override operator fun get(index: Int): Field<*> {
        return value[index]
    }

    override operator fun get(name: String): Field<*> {
        return get(name.toInt())
    }

    override operator fun contains(key: String): Boolean {
        val idx = key.toInt()
        return idx >= 0 && idx < value.size
    }

    override fun put(field: Field<*>) {
        value.add(field)
    }

    override fun hasChildQualifier(qualifier: String): Boolean {
        return value.find { it.hasQualifier(qualifier) } != null
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

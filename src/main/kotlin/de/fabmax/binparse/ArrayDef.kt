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

    protected override fun parse(reader: BinReader, parent: ContainerField<*>): ArrayField {
        val array = ArrayField(fieldName)

        if (length.mode == LengthMode.BY_VALUE) {
            while (length.termParser != null) {
                reader.mark()
                if (length.termParser.parseField(reader, nullTermCtx).value == 0L) {
                    break
                }
                reader.reset()
                if (parseItem(array, reader, parent)) {
                    break
                }
            }
        } else {
            val length = when (length.mode) {
                LengthMode.FIXED -> length.intLength
                LengthMode.BY_FIELD -> parent.getInt(length.strLength).intValue
                else -> 0
            }
            for (i in 1..length) {
                if (parseItem(array, reader, parent)) {
                    break
                }
            }
        }
        return array
    }

    override fun prepareWrite(parent: ContainerField<*>) {
        super.prepareWrite(parent)
        val array = parent.getArray(fieldName);
        if (length.mode == LengthMode.BY_FIELD) {
            parent.getInt(length.strLength).intValue = array.size
        }
        for (i in 0 .. array.size - 1) {
            type.fieldName = i.toString()
            type.prepareWrite(array)
        }
    }

    override fun write(writer: BinWriter, parent: ContainerField<*>) {
        val array = parent.getArray(fieldName);
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

    override fun matchesDef(parent: ContainerField<*>): Boolean {
        val array = parent[fieldName]
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

    internal class Factory() : FieldDefFactory() {
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


package de.fabmax.binparse

import java.nio.charset.Charset
import java.util.*

/**
 * Created by max on 17.11.2015.
 */

class StringDef private constructor(fieldName: String, encoding: Charset, length: ArrayDef.Length):
        FieldDef(fieldName) {

    private val encoding = encoding
    private val length = length

    override fun toString(): String {
        return "$fieldName: StringParser: encoding: $encoding, length: $length"
    }

    override fun parse(reader: BinReader, parent: StructInstance): StringField {
        val data = when (length.mode) {
            ArrayDef.LengthMode.FIXED -> reader.readBytes(length.intLength)
            ArrayDef.LengthMode.BY_FIELD -> reader.readBytes(parent[length.strLength].getIntValue().toInt())
            ArrayDef.LengthMode.BY_VALUE -> readNullTerminated(reader, parent)
        }
        return StringField(fieldName, String(data, encoding))
    }

    fun readNullTerminated(reader: BinReader, resultSet: StructInstance): ByteArray {
        // fixme: this doesn't really work for UTF-16
        val bytes = ArrayList<Byte>()
        while (length.termParser != null) {
            reader.mark()
            if (length.termParser.parseField(reader, resultSet).getIntValue() == 0L) {
                break
            }
            reader.reset()
            bytes.add(reader.readU08().toByte())
        }
        return bytes.toByteArray()
    }

    override fun write(writer: BinWriter, field: Field<*>, parent: StructInstance) {
        println("array.write not yet implemented")
    }

    override fun matchesDef(field: Field<*>, parent: StructInstance): Boolean {
        return field is StringField
    }

    internal class Factory(encoding: Charset?) : ArrayDef.Factory() {
        val encoding = encoding

        override fun createParser(definition: Item): FieldDef {
            val encoding = this.encoding ?: parseEncoding(definition)
            return StringDef(definition.identifier, encoding, parseLength(definition))
        }

        private fun parseEncoding(definition: Item): Charset {
            val encoding = getItem(definition.childrenMap, "encoding").value
            if (Charset.isSupported(encoding)) {
                return Charset.forName(encoding)
            } else {
                throw IllegalArgumentException("Unsupported encoding: $encoding")
            }
        }
    }
}

class StringField(name: String, value: String): Field<String>(name, value) {
    override fun getStringValue(): String {
        return value;
    }

    operator fun String.unaryPlus() {
        value = this
    }
}

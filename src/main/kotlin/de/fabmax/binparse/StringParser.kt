package de.fabmax.binparse

import java.nio.charset.Charset
import java.util.*

/**
 * Created by max on 17.11.2015.
 */

class StringParser private constructor(fieldName: String, encoding: Charset, length: ArrayParser.Length):
        FieldParser(fieldName) {

    private val encoding = encoding
    private val length = length

    override fun toString(): String {
        return "$fieldName: StringParser: encoding: $encoding, length: $length"
    }

    override fun parse(reader: BinReader, result: StructInstance): Field {
        val data = when (length.mode) {
            ArrayParser.LengthMode.FIXED -> reader.readBytes(length.intLength)
            ArrayParser.LengthMode.BY_FIELD -> reader.readBytes(result[length.strLength].getDecimalValue().toInt())
            ArrayParser.LengthMode.BY_VALUE -> readNullTerminated(reader, result)
        }
        return StringField(fieldName, String(data, encoding))
    }

    fun readNullTerminated(reader: BinReader, resultSet: StructInstance): ByteArray {
        // fixme: this doesn't really work for UTF-16
        val bytes = ArrayList<Byte>()
        while (length.termParser != null) {
            reader.mark()
            if (length.termParser.parseField(reader, resultSet).getDecimalValue() == 0L) {
                break
            }
            reader.reset()
            bytes.add(reader.readU08().toByte())
        }
        return bytes.toByteArray()
    }

    internal class Factory(encoding: Charset?) : ArrayParser.Factory() {
        val encoding = encoding

        override fun createParser(definition: Item): FieldParser {
            val encoding = this.encoding ?: parseEncoding(definition)
            return StringParser(definition.identifier, encoding, parseLength(definition))
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

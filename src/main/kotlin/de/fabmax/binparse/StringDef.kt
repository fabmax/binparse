package de.fabmax.binparse

import java.nio.charset.Charset
import java.util.*

/**
 * Created by max on 17.11.2015.
 */

class StringDef private constructor(fieldName: String, encoding: Charset, length: ArrayDef.Length) :
        FieldDef<StringField>(fieldName) {

    private val encoding = encoding
    private val length = length

    override fun toString(): String {
        return "$fieldName: StringParser: encoding: $encoding, length: $length"
    }

    override fun parse(reader: BinReader, context: ContainerField<*>): StringField {
        val data = when (length.mode) {
            ArrayDef.LengthMode.FIXED -> reader.readBytes(length.intLength)
            ArrayDef.LengthMode.BY_FIELD -> reader.readBytes(context.getInt(length.strLength).intValue)
            ArrayDef.LengthMode.BY_VALUE -> readNullTerminated(reader)
        }
        return StringField(fieldName, String(data, encoding))
    }

    fun readNullTerminated(reader: BinReader): ByteArray {
        // fixme: this doesn't really work for UTF-16
        val bytes = ArrayList<Byte>()
        while (length.termParser != null) {
            reader.mark()
            if (length.termParser.parseField(reader, ArrayDef.nullTermCtx).value == 0L) {
                break
            }
            reader.reset()
            bytes.add(reader.readU08().toByte())
        }
        return bytes.toByteArray()
    }

    override fun prepareWrite(context: ContainerField<*>) {
        super.prepareWrite(context)
        val string = context.getString(fieldName)
        if (length.mode == ArrayDef.LengthMode.BY_FIELD) {
            context.getInt(length.strLength).intValue = string.value.toByteArray(encoding).size
        }
    }

    override fun write(writer: BinWriter, context: ContainerField<*>) {
        val string = context.getString(fieldName)
        writer.writeBytes(string.value.toByteArray(encoding));
        if (length.mode == ArrayDef.LengthMode.BY_VALUE && length.termParser != null) {
            length.termParser.write(writer, ArrayDef.nullTermCtx)
        }
    }

    override fun matchesDef(context: ContainerField<*>): Boolean {
        return context[fieldName] is StringField
    }

    internal class Factory(encoding: Charset?) : FieldDefFactory() {
        val encoding = encoding

        override fun createParser(definition: Item): StringDef {
            val encoding = this.encoding ?: parseEncoding(definition)
            return StringDef(definition.identifier, encoding, ArrayDef.Factory.parseLength(definition))
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
    operator fun String.unaryPlus() {
        value = this
    }
}

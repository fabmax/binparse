package de.fabmax.binparse

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

    override fun parse(reader: ParserReader, resultSet: ParseResult): Field {
        val field = ArrayField(fieldName)

        if (length.mode == LengthMode.BY_VALUE) {
            while (length.termParser != null) {
                reader.mark()
                if (length.termParser.parseField(reader, resultSet).getDecimalValue() == 0L) {
                    break
                }
                reader.reset()

                if (parseItem(field, reader, resultSet)) {
                    break
                }
            }

        } else {
            val length = when (length.mode) {
                LengthMode.FIXED ->
                    length.intLength
                LengthMode.BY_FIELD ->
                    resultSet[length.strLength]!!.getDecimalValue().toInt()
                else -> 0
            }
            for (i in 1..length) {
                if (parseItem(field, reader, resultSet)) {
                    break
                }
            }
        }
        return field
    }

    private fun parseItem(field: ArrayField, reader: ParserReader, resultSet: ParseResult): Boolean {
        val item = type.parseField(reader, resultSet)
        item.index = field.values.size
        field.values.add(item)
        return item.hasQualifier(Field.QUAL_BREAK)
    }

    open internal class Factory() : FieldParserFactory() {

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

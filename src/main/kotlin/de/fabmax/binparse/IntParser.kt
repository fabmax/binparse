package de.fabmax.binparse

import java.io.IOException

/**
 * Created by max on 17.11.2015.
 */

class IntParser(fieldName: String, size: Int, signedness: IntParser.Signedness): FieldParser(fieldName) {

    enum class Signedness {
        SIGNED,
        UNSIGNED
    }

    val bits = size
    val signedness = signedness

    override fun toString(): String {
        return "$fieldName: IntParser: bits: $bits, signedness: $signedness"
    }

    override fun parse(reader: BinReader, result: StructInstance): Field {
        try {
            var value = reader.readBits(bits);
            if (signedness == Signedness.SIGNED && value >= (1 shl (bits - 1))) {
                value -= (1 shl bits)
            }
            return DecimalField(fieldName, value)
        } catch (e: IOException) {
            throw IOException("Failed parsing int: name: $fieldName, bits: $bits", e)
        }
    }

    class Factory(bits: Int, signedness: IntParser.Signedness?) : FieldParserFactory() {
        val bits = bits
        val signedness = signedness

        override fun createParser(definition: Item): FieldParser {
            val bits = if (this.bits != 0) {
                this.bits
            } else {
                parseBits(definition)
            }
            val signedness = this.signedness ?: parseSignedness(definition)
            return IntParser(definition.identifier, bits, signedness)
        }

        fun parseSignedness(definition: Item): Signedness {
            val signednessIt = definition.childrenMap["signedness"]
            if (signednessIt != null) {
                when (signednessIt.value) {
                    Signedness.SIGNED.toString() -> return Signedness.SIGNED
                    Signedness.UNSIGNED.toString() -> return Signedness.UNSIGNED
                    else -> throw IllegalArgumentException("Invalid signedness: " + signednessIt.value)
                }
            } else {
                return Signedness.UNSIGNED
            }
        }

        fun parseBits(definition: Item): Int {
            val bitsIt = getItem(definition.childrenMap, "bits");
            return parseDecimal(bitsIt.value)?.toInt() ?:
                    throw NumberFormatException("Invalid bit size: " + bitsIt.value)
        }
    }
}

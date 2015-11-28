package de.fabmax.binparse

/**
 * Created by max on 17.11.2015.
 */

class IntDef(fieldName: String, size: Int, signedness: IntDef.Signedness): FieldDef(fieldName) {

    enum class Signedness {
        SIGNED,
        UNSIGNED
    }

    val bits = size
    val signedness = signedness

    override fun parse(reader: BinReader, parent: StructInstance): IntField {
        var value = reader.readBits(bits)
        if (signedness == Signedness.SIGNED && value >= (1 shl (bits - 1))) {
            value -= (1 shl bits)
        }
        return IntField(fieldName, value)
    }

    override fun write(writer: BinWriter, field: Field<*>, parent: StructInstance) {
        writer.writeBits(bits, field.getIntValue())
    }

    override fun matchesDef(field: Field<*>, parent: StructInstance): Boolean {
        return field is IntField
    }

    override fun toString(): String {
        return "$fieldName: IntParser: bits: $bits, signedness: $signedness"
    }

    internal class Factory(bits: Int, signedness: IntDef.Signedness?) : FieldDefFactory() {
        val bits = bits
        val signedness = signedness

        override fun createParser(definition: Item): FieldDef {
            val bits = if (this.bits != 0) {
                this.bits
            } else {
                parseBits(definition)
            }
            val signedness = this.signedness ?: parseSignedness(definition)
            return IntDef(definition.identifier, bits, signedness)
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

/**
 * A [Field] for storing ints with sizes of 1 to 64 bits. All ints are saved in a Long.
 */
class IntField(name: String, value: Long) : Field<Long>(name, value) {
    override fun getIntValue(): Long {
        return value
    }

//
//    Unfortunately this doesn't seem to work, because Int and Long use their native unaryPlus instead of the
//    overloaded one...
//
//    operator fun Int.unaryPlus() {
//        println("set int")
//        value = toLong()
//    }
//
//    operator fun Long.unaryPlus() {
//        value = this
//    }
}

package de.fabmax.binparse

/**
 * Created by max on 17.11.2015.
 */

class IntDef(fieldName: String, size: Int, signedness: IntDef.Signedness) : FieldDef<IntField>(fieldName) {

    enum class Signedness {
        SIGNED,
        UNSIGNED
    }

    val bits = size
    val signedness = signedness

    override fun parse(reader: BinReader, context: ContainerField<*>): IntField {
        var value = reader.readBits(bits)
        if (signedness == Signedness.SIGNED && value >= (1 shl (bits - 1))) {
            value -= (1 shl bits)
        }
        return IntField(fieldName, value)
    }

    override fun prepareWrite(context: ContainerField<*>) {
        if (hasQualifier(Field.QUAL_SIZE) && fieldName !in context) {
            context.put(IntField(fieldName, 0))
        }
        super.prepareWrite(context)
    }

    override fun write(writer: BinWriter, context: ContainerField<*>) {
        writer.writeBits(bits, context.getInt(fieldName).value)
    }

    override fun matchesDef(context: ContainerField<*>): Boolean {
        // IntFields with SIZE qualifier are added automatically before write
        return hasQualifier(Field.QUAL_SIZE) || context[fieldName] is IntField
    }

    override fun toString(): String {
        return "$fieldName: IntParser: bits: $bits, signedness: $signedness"
    }

    internal class Factory(bits: Int, signedness: IntDef.Signedness?) : FieldDefFactory() {
        val bits = bits
        val signedness = signedness

        override fun createParser(definition: Item): IntDef {
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
 * A [Field] for storing ints with sizes of 1 to 64 bits.
 */
class IntField(name: String, value: Long) : Field<Long>(name, value) {
    var intValue: Int
        get() = value.toInt()
        set(value) {
            this.value = value.toLong()
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

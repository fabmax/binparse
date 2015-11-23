package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 18.11.2015.
 */

abstract class Field(name: String) {

    companion object {
        val QUAL_BREAK = "BREAK";
        val QUAL_COLLECT = "COLLECT";
        val QUAL_SIZE = "SIZE";

        val QUALIFIERS = HashSet<String>()

        init {
            QUALIFIERS.add(QUAL_BREAK)
            QUALIFIERS.add(QUAL_COLLECT)
            QUALIFIERS.add(QUAL_SIZE)
        }
    }

    var name = name
    //val evaluators = ArrayList<FieldEvaluator>()

    var index = 0
    var qualifiers: Set<String>? = null
    var offset = 0

    /*fun getEvaluator(clazz: KClass<FieldEvaluator>): FieldEvaluator? {
        return null
    }*/

    open fun hasQualifier(qualifier: String): Boolean {
        return qualifiers?.contains(qualifier) ?: false
    }

    open fun getDecimalValue(): Long {
        val value = getValue()
        if (value is Long) {
            return value
        } else if (value is Int) {
            return value.toLong()
        } else {
            throw UnsupportedOperationException()
        }
    }

    open fun getFloatValue(): Double {
        val value = getValue()
        if (value is Double) {
            return value
        } else if (value is Float) {
            return value.toDouble()
        } else {
            throw UnsupportedOperationException()
        }
    }

    open fun getStringValue(): String {
        return getValue().toString()
    }

    abstract fun getValue(): Any;

    override fun toString(): String {
        return getStringValue()
    }

    open fun toString(indent: Int, withFieldName: Boolean = true): String {
        val buf = StringBuffer()
        for (i in 1 .. indent) {
            buf.append(' ')
        }
        if (withFieldName) {
            buf.append(name).append(" = ")
        }
        buf.append(getStringValue())
        return buf.toString()
    }
}

class ArrayField(name: String, values: ArrayList<Field> = ArrayList<Field>()): Field(name), Iterable<Field> by values {
    val values = values
    val length: Int
        get() = values.size

    override fun getValue(): Any {
        return values
    }

    operator fun get(index: Int): Field {
        return values[index]
    }

    fun getArray(index: Int): ArrayField {
        return values[index] as ArrayField
    }

    fun getStruct(index: Int): StructInstance {
        return values[index] as StructInstance
    }

    override fun hasQualifier(qualifier: String): Boolean {
        if (qualifier != QUAL_COLLECT && super.hasQualifier(QUAL_COLLECT)) {
            return values.find { field -> field.hasQualifier(qualifier) } != null
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

        if (values.isEmpty()) {
            buf.append("[]")
        } else {
            buf.append("[\n")
            values.forEach { field -> buf.append(field.toString(indent + 2, false)).append(",\n") }
            buf.append(ind).append("]")
        }
        return buf.toString()
    }
}

class DecimalField(name: String, value: Long): Field(name) {
    val value = value

    override fun getValue(): Any {
        return value
    }

    override fun getDecimalValue(): Long {
        return value
    }
}

class StringField(name: String, value: String): Field(name) {
    val value = value

    override fun getValue(): Any {
        return value
    }
}

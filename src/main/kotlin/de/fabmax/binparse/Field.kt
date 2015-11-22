package de.fabmax.binparse

import java.util.*
import kotlin.reflect.KClass

/**
 * Created by max on 18.11.2015.
 */

abstract class Field(name: String) {

    companion object {
        val QUAL_BREAK = "BREAK";
        val QUAL_COLLECT = "COLLECT";

        val QUALIFIERS = HashSet<String>()

        init {
            QUALIFIERS.add(QUAL_BREAK)
            QUALIFIERS.add(QUAL_COLLECT)
        }
    }

    var name = name
    val evaluators = ArrayList<FieldEvaluator>()

    var index = 0
    var qualifiers: Set<String>? = null;

    fun getEvaluator(clazz: KClass<FieldEvaluator>): FieldEvaluator? {
        return null
    }

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

class ArrayField(name: String): Field(name) {
    var values = ArrayList<Field>()

    override fun getValue(): Any {
        return values
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

class StructField(name: String, parseResult: ParseResult): Field(name) {
    val parseResult = parseResult

    override fun getValue(): Any {
        return parseResult
    }

    override fun hasQualifier(qualifier: String): Boolean {
        if (qualifier != QUAL_COLLECT && super.hasQualifier(QUAL_COLLECT)) {
            return parseResult.fields.values.find { field -> field.hasQualifier(qualifier) } != null
        } else {
            return super.hasQualifier(qualifier)
        }
    }

    override fun toString(indent: Int, withFieldName: Boolean): String {
        val buf = StringBuffer()
        for (i in 1 .. indent) {
            buf.append(' ')
        }
        if (withFieldName) {
            buf.append(name).append(" = ")
        }
        buf.append(parseResult.toString(indent))
        return buf.toString()
    }
}

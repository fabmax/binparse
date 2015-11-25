package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 18.11.2015.
 */

abstract class Field<T>(name: String, initVal: T) {

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
    var index = 0
    var offset = 0

    var qualifiers: Set<String>? = null

    var value = initVal

    open fun hasQualifier(qualifier: String): Boolean {
        return qualifiers?.contains(qualifier) ?: false
    }

    /**
     * Return the int value of a field. Because in binparse ints can have arbitrary sizes between 1 and 64 bits this
     * method returns a Long. For non-int fields this method throws an UnsupportedOperationException.
     */
    open fun getIntValue(): Long {
        val v = value
        if (v is Long) {
            return v
        } else if (v is Int) {
            return v.toLong()
        } else {
            throw UnsupportedOperationException()
        }
    }

    open fun getFloatValue(): Double {
        val v = value
        if (v is Double) {
            return v
        } else if (v is Float) {
            return v.toDouble()
        } else {
            throw UnsupportedOperationException()
        }
    }

    open fun getStringValue(): String {
        return value.toString()
    }

    open fun set(value: T) {
        this.value = value
    }

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

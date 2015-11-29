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

    var qualifiers: HashSet<String>? = null

    var value = initVal

    open fun hasQualifier(qualifier: String): Boolean {
        return qualifiers?.contains(qualifier) ?: false
    }

    fun addQualifier(qualifier: String) {
        if (qualifiers == null) {
            qualifiers = HashSet<String>()
        }
        qualifiers!!.add(qualifier)
    }

    open fun set(value: T) {
        this.value = value
    }

    override fun toString(): String {
        return value.toString()
    }

    open fun toString(indent: Int, withFieldName: Boolean = true): String {
        val buf = StringBuffer()
        for (i in 1 .. indent) {
            buf.append(' ')
        }
        if (withFieldName) {
            buf.append(name).append(" = ")
        }
        buf.append(value.toString())
        return buf.toString()
    }
}

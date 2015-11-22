package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 18.11.2015.
 */

class ParseResult {

    val fields: HashMap<String, Field> = HashMap()

    fun put(field: Field) {
        if (fields.containsKey(field.name)) {
            throw IllegalArgumentException("ParseResult already contains a field with name " + field.name)
        }
        fields.put(field.name, field)
    }

    operator fun get(name: String): Field? {
        return fields[name]
    }

    override fun toString(): String {
        val buf = StringBuilder()
        buf.append("{ ")
        fields.forEach { name, field -> buf.append(name).append(" = ").append(field).append("; ") }
        buf.append("}")
        return buf.toString()
    }

    fun toString(indent: Int): String {
        val buf = StringBuffer()
        for (i in 1 .. indent) {
            buf.append(" ")
        }
        var ind = buf.toString()
        buf.setLength(0)

        buf.append("{\n")
        fields.values.sortedBy { field -> field.index }
                .forEach { field -> buf.append(field.toString(indent + 2)).append('\n') }
        buf.append(ind).append('}')

        return buf.toString()
    }
}
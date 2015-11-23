package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 18.11.2015.
 */

class StructInstance(structName: String, fields: HashMap<String, Field> = HashMap<String, Field>()) :
        Field(structName), Iterable<Field> by fields.values {

    val fields = fields

    fun put(field: Field) {
        if (fields.containsKey(field.name)) {
            throw IllegalArgumentException("ParseResult already contains a field with name " + field.name)
        }
        fields.put(field.name, field)
    }

    @Throws(NoSuchFieldException::class)
    fun getArray(name: String): ArrayField {
        return get(name) as ArrayField
    }

    @Throws(NoSuchFieldException::class)
    fun getStruct(name: String): StructInstance {
        return get(name) as StructInstance
    }

    @Throws(NoSuchFieldException::class)
    fun getLong(name: String): Long {
        return get(name).getDecimalValue()
    }

    @Throws(NoSuchFieldException::class)
    fun getInt(name: String): Int {
        return getLong(name).toInt()
    }

    @Throws(NoSuchFieldException::class)
    fun getString(name: String): String {
        return get(name).getStringValue()
    }

    fun flat(): Iterator<Field> {
        return Flaterator(iterator())
    }

    @Throws(NoSuchFieldException::class)
    operator fun get(name: String): Field {
        val path = name.splitToSequence('.').iterator()
        var fName = path.next()
        var field = fields[fName] ?: throw NoSuchFieldException("No such field: $fName")
        for (cName in path) {
            if (field is StructInstance) {
                field = field[cName]
                fName = cName
            } else {
                throw NoSuchFieldException("No such field: $fName.$cName: $fName has no children")
            }
        }
        return field
    }

    override fun getValue(): Any {
        return fields
    }

    override fun hasQualifier(qualifier: String): Boolean {
        if (qualifier != QUAL_COLLECT && super.hasQualifier(QUAL_COLLECT)) {
            return fields.values.find { field -> field.hasQualifier(qualifier) } != null
        } else {
            return super.hasQualifier(qualifier)
        }
    }

    override fun toString(): String {
        val buf = StringBuilder()
        buf.append("{ ")
        fields.values.sortedBy { field -> field.index }
                .forEach { field -> buf.append(field.name).append(" = ").append(field).append("; ") }
        buf.append("}")
        return buf.toString()
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

        buf.append("{\n")
        fields.values.sortedBy { field -> field.index }
                .forEach { field -> buf.append(field.toString(indent + 2)).append('\n') }
        buf.append(ind).append('}')

        return buf.toString()
    }

    private class Flaterator(structIt: Iterator<Field>) : Iterator<Field> {
        val stack = Stack<Iterator<Field>>()

        init {
            stack.push(structIt);
        }

        override fun hasNext(): Boolean {
            while (!stack.isEmpty() && !stack.peek().hasNext()) {
                stack.pop()
            }
            return !stack.isEmpty()
        }

        override fun next(): Field {
            if (!hasNext()) {
                throw NoSuchElementException("No more fields")
            }

            val field = stack.peek().next()

            if (field is StructInstance) {
                stack.push(field.iterator())
            } else if (field is ArrayField) {
                stack.push(field.iterator())
            }
            return field
        }
    }
}
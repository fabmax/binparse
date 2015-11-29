package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 18.11.2015.
 */

class StructInstance(structName: String, fields: HashMap<String, Field<*>> = HashMap<String, Field<*>>()) :
        ContainerField<HashMap<String, Field<*>>>(structName, fields), Iterable<Field<*>> by fields.values {

    override operator fun contains(key: String): Boolean {
        return value.containsKey(key)
    }

    override fun get(index: Int): Field<*> {
        return value.values.find { it.index == index } ?: throw NoSuchFieldException("Index not found: $index")
    }

    override fun get(name: String): Field<*> {
        val path = name.splitToSequence('.').iterator()
        var fName = path.next()
        var field = value[fName] ?: throw NoSuchFieldException("No such field: $fName")
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

    fun flat(): Iterator<Field<*>> {
        return Flaterator(iterator())
    }

    override fun hasChildQualifier(qualifier: String): Boolean {
        return value.values.find { it.hasQualifier(qualifier) } != null
    }

    override fun toString(): String {
        val buf = StringBuilder()
        buf.append("{ ")
        value.values.sortedBy { it.index }.forEach { buf.append(it.name).append(" = ").append(it).append("; ") }
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
        value.values.sortedBy { it.index }.forEach { buf.append(it.toString(indent + 2)).append('\n') }
        buf.append(ind).append('}')

        return buf.toString()
    }

    private class Flaterator(structIt: Iterator<Field<*>>) : Iterator<Field<*>> {
        val stack = Stack<Iterator<Field<*>>>()

        init {
            stack.push(structIt);
        }

        override fun hasNext(): Boolean {
            while (!stack.isEmpty() && !stack.peek().hasNext()) {
                stack.pop()
            }
            return !stack.isEmpty()
        }

        override fun next(): Field<*> {
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

    override fun put(field: Field<*>) {
        if (value.containsKey(field.name)) {
            throw IllegalArgumentException("ParseResult already contains a field with name " + field.name)
        }
        field.index = value.size
        value.put(field.name, field)
    }

    operator fun Field<*>.unaryPlus() {
        put(this)
    }

    protected fun <F : Field<*>> initField(field: F, init: F.() -> Unit): F {
        field.init()
        put(field)
        return field
    }

    fun array(name: String, init: ArrayField.() -> Unit) = initField(ArrayField(name), init)
    fun int(name: String, init: IntField.() -> Unit) = initField(IntField(name, 0), init)
    fun string(name: String, init: StringField.() -> Unit) = initField(StringField(name, ""), init)
    fun struct(name: String, init: StructInstance.() -> Unit) = initField(StructInstance(name), init)
}

fun struct(name: String = "anonymous_struct", init: StructInstance.() -> Unit): StructInstance {
    val struct = StructInstance(name)
    struct.init()
    return struct
}
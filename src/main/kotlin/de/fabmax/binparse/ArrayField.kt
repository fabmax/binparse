package de.fabmax.binparse

import java.util.*

class ArrayField(name: String, items: ArrayList<Field<*>> = ArrayList<Field<*>>()) :
        ContainerField<ArrayList<Field<*>>>(name, items), Iterable<Field<*>> by items {
    val size: Int
        get() = value.size

    override operator fun get(index: Int): Field<*> {
        return value[index]
    }

    override operator fun get(name: String): Field<*> {
        return get(name.toInt())
    }

    override operator fun contains(key: String): Boolean {
        val idx = key.toInt()
        return idx >= 0 && idx < value.size
    }

    override fun put(field: Field<*>) {
        value.add(field)
    }

    override fun hasChildQualifier(qualifier: String): Boolean {
        return value.find { it.hasQualifier(qualifier) } != null
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

        if (value.isEmpty()) {
            buf.append("[]")
        } else {
            buf.append("[\n")
            value.forEach { buf.append(it.toString(indent + 2, false)).append(",\n") }
            buf.append(ind).append("]")
        }
        return buf.toString()
    }

    operator fun Array<Int>.unaryPlus() {
        value.clear()
        var i = 0
        for (int in this) {
            value.add(IntField("[{$i++}]", int.toLong()))
        }
    }

    operator fun Array<Long>.unaryPlus() {
        value.clear()
        var i = 0
        for (long in this) {
            value.add(IntField("[{$i++}]", long))
        }
    }

    operator fun Array<String>.unaryPlus() {
        value.clear()
        var i = 0
        for (str in this) {
            value.add(StringField("[{$i++}]", str))
        }
    }

    operator fun Array<StructInstance>.unaryPlus() {
        value.clear()
        for (struct in this) {
            value.add(struct)
        }
    }

    operator fun Array<Field<*>>.unaryPlus() {
        value.clear()
        for (field in this) {
            value.add(field)
        }
    }

    operator fun Field<*>.unaryPlus() {
        put(this)
    }
}
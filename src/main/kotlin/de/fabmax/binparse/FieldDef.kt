package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 17.11.2015.
 */

abstract class FieldDef<T : Field<*>>(fieldName: String) {

    var fieldName = fieldName
    val qualifiers = HashSet<String>()

    fun parseField(reader: BinReader, parent: ContainerField<*>): T {
        val offset = reader.pos
        val field = parse(reader, parent)
        field.offset = offset
        if (!qualifiers.isEmpty()) {
            field.qualifiers = HashSet<String>(qualifiers)
        }
        return field
    }

    fun hasQualifier(qualifier: String): Boolean {
        return qualifiers.contains(qualifier)
    }

    protected abstract fun parse(reader: BinReader, parent: ContainerField<*>): T

    abstract fun write(writer: BinWriter, parent: ContainerField<*>)

    protected open fun prepareWrite(parent: ContainerField<*>) {
        if (parent[fieldName].qualifiers != null) {
            parent[fieldName].qualifiers!!.addAll(qualifiers)
        } else {
            parent[fieldName].qualifiers = HashSet<String>(qualifiers)
        }
    }

    abstract fun matchesDef(parent: ContainerField<*>): Boolean
}
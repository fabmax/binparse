package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 17.11.2015.
 */

abstract class FieldDef<T : Field<*>>(fieldName: String) {

    var fieldName = fieldName
    val qualifiers = HashSet<String>()

    fun parseField(reader: BinReader, context: ContainerField<*>): T {
        val offset = reader.pos
        val field = parse(reader, context)
        field.offset = offset
        if (!qualifiers.isEmpty()) {
            field.qualifiers = HashSet<String>(qualifiers)
        }
        return field
    }

    fun hasQualifier(qualifier: String): Boolean {
        return qualifiers.contains(qualifier)
    }

    protected abstract fun parse(reader: BinReader, context: ContainerField<*>): T

    abstract fun write(writer: BinWriter, context: ContainerField<*>)

    protected open fun prepareWrite(context: ContainerField<*>) {
        if (context[fieldName].qualifiers != null) {
            context[fieldName].qualifiers!!.addAll(qualifiers)
        } else {
            context[fieldName].qualifiers = HashSet<String>(qualifiers)
        }
    }

    abstract fun matchesDef(context: ContainerField<*>): Boolean
}
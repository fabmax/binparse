package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 17.11.2015.
 */

abstract class FieldDef(fieldName: String) {

    val fieldName = fieldName
    val qualifiers = HashSet<String>()

    fun parseField(reader: BinReader, result: StructInstance): Field<*> {
        val offset = reader.pos
        val field = parse(reader, result)
        field.offset = offset
        if (!qualifiers.isEmpty()) {
            field.qualifiers = HashSet<String>(qualifiers);
        }
        return field
    }

    fun hasQualifier(qualifier: String): Boolean {
        return qualifiers.contains(qualifier)
    }

    protected abstract fun parse(reader: BinReader, parent: StructInstance): Field<*>

    abstract fun write(writer: BinWriter, field: Field<*>, parent: StructInstance)

    protected open fun prepareWrite(field: Field<*>, parent: StructInstance) {
        // default impl does nothing
    }

    abstract fun matchesDef(field: Field<*>, parent: StructInstance): Boolean
}
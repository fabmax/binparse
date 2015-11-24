package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 17.11.2015.
 */

abstract class FieldParser(fieldName: String) {

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

    abstract fun matchesDef(field: Field<*>, parent: StructInstance): Boolean;

    abstract fun parse(reader: BinReader, result: StructInstance): Field<*>
}
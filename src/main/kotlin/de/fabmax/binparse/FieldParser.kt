package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 17.11.2015.
 */

abstract class FieldParser(fieldName: String) {

    val fieldName = fieldName
    val qualifiers = HashSet<String>()

    fun parseField(reader: ParserReader, resultSet: ParseResult): Field {
        val field = parse(reader, resultSet)
        if (!qualifiers.isEmpty()) {
            field.qualifiers = HashSet<String>(qualifiers);
        }
        return field
    }

    abstract fun parse(reader: ParserReader, resultSet: ParseResult): Field

}
package de.fabmax.binparse

import java.io.InputStream
import java.util.*

/**
 * Created by max on 18.11.2015.
 */

class Struct(definition: Item) {

    private val name = definition.identifier;
    private val parserChain = ArrayList<FieldParser>()

    init {
        if (definition.value != "struct") {
            throw IllegalArgumentException("Invalid defintion value: " + definition.value)
        }
        definition.forEach { f -> parserChain.add(FieldParserFactory.createParser(f)) }

        // register this struct as a type
        FieldParserFactory.addParserFactory(name, ParserFactory())
    }

    fun parse(input: InputStream): ParseResult {
        return parse(ParserReader(input));
    }

    fun parse(reader: ParserReader): ParseResult {
        val result = ParseResult(name);

        //println("[struct $name]")
        for (parser in parserChain) {
            val field = parser.parseField(reader, result);
            //println(field.name + " = " + field)
            field.index = result.fields.size;
            result.put(field)
        }

        return result
    }

    private inner class ParserFactory(): FieldParserFactory() {
        override fun createParser(definition: Item): FieldParser {
            return StructFieldParser(definition.identifier)
        }
    }

    private inner class StructFieldParser(fieldName: String): FieldParser(fieldName) {
        override fun parse(reader: ParserReader, resultSet: ParseResult): Field {
            return parse(reader)
        }
    }
}
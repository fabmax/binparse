package de.fabmax.binparse

import java.io.InputStream
import java.util.*

/**
 * Created by max on 18.11.2015.
 */

class StructDef(definition: Item) {

    val name = definition.identifier;

    private val parserChain = ArrayList<FieldParser>()

    init {
        if (definition.value != "struct") {
            throw IllegalArgumentException("Invalid definition value: " + definition.value)
        }
        definition.forEach { f -> parserChain.add(FieldParserFactory.createParser(f)) }

        // register this struct as a type
        FieldParserFactory.addParserFactory(name, ParserFactory())
    }

    fun parse(input: InputStream): StructInstance {
        return parse(BinReader(input));
    }

    fun parse(reader: BinReader): StructInstance {
        val result = StructInstance(name);
        for (parser in parserChain) {
            val field = parser.parseField(reader, result);
            field.index = result.value.size;
            result.put(field)
        }
        return result
    }

    private inner class ParserFactory(): FieldParserFactory() {
        override fun createParser(definition: Item): FieldParser {
            if (definition.value != name) {
                throw IllegalArgumentException("Invalid def type: " + definition.value + " != $name")
            }
            return StructFieldParser(definition.identifier)
        }
    }

    private inner class StructFieldParser(fieldName: String): FieldParser(fieldName) {
        override fun parse(reader: BinReader, result: StructInstance): StructInstance {
            return parse(reader)
        }
    }
}
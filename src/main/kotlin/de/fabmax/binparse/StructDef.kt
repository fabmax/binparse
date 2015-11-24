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
        FieldParserFactory.addParserFactory(name, ParserFactory(this))
    }

    fun parse(input: InputStream): StructInstance {
        return parse(BinReader(input));
    }

    fun parse(reader: BinReader): StructInstance {
        val result = StructInstance(name);
        for (parser in parserChain) {
            result.put(parser.parseField(reader, result))
        }
        return result
    }

    fun matchesDef(struct: StructInstance): Boolean {
        for (parser in parserChain) {
            if (parser.fieldName !in struct) {
                return false
            } else {
                return parser.matchesDef(struct[parser.fieldName], struct)
            }
        }
        return parserChain.find { it.fieldName !in struct } == null
    }

    private inner class ParserFactory(val def: StructDef): FieldParserFactory() {
        override fun createParser(definition: Item): FieldParser {
            if (definition.value != name) {
                throw IllegalArgumentException("Invalid def type: " + definition.value + " != $name")
            }
            return StructFieldParser(definition.identifier, def)
        }
    }

    private inner class StructFieldParser(fieldName: String, val def: StructDef): FieldParser(fieldName) {
        override fun matchesDef(field: Field<*>, parent: StructInstance): Boolean {
            if (field is StructInstance) {
                return def.matchesDef(field)
            } else {
                return false
            }
        }

        override fun parse(reader: BinReader, result: StructInstance): StructInstance {
            return parse(reader)
        }
    }
}
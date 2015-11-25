package de.fabmax.binparse

import java.io.InputStream
import java.util.*

/**
 * Created by max on 18.11.2015.
 */

class StructDef(definition: Item) : FieldDef(definition.identifier) {

    val name = definition.identifier;

    private val parserChain = ArrayList<FieldDef>()

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

    override fun parse(reader: BinReader, parent: StructInstance): StructInstance {
        return parse(reader)
    }

    override fun write(writer: BinWriter, field: Field<*>, parent: StructInstance) {
        if (field is StructInstance) {
            parserChain.forEach {
                it.write(writer, field[it.fieldName], field);
            }
        } else {
            throw IllegalArgumentException("field has to be a StructInstance (is " + field.javaClass.name + ")")
        }
    }

    override fun matchesDef(field: Field<*>, parent: StructInstance): Boolean {
        if (field is StructInstance) {
            return matchesDef(field)
        } else {
            return false
        }
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

    private inner class ParserFactory(val def: StructDef) : FieldParserFactory() {
        override fun createParser(definition: Item): FieldDef {
            if (definition.value != name) {
                throw IllegalArgumentException("Invalid def type: " + definition.value + " != $name")
            }
            return def
        }
    }
}
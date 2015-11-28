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
        definition.forEach { f -> parserChain.add(FieldDefFactory.createParser(f)) }

        // register this struct as a type
        FieldDefFactory.addParserFactory(name, DefFactory(this))
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
        val instance = asStructInstance(field)
        prepareWrite(instance)
        if (matchesDef(instance, false)) {
            parserChain.forEach {
                it.write(writer, instance[it.fieldName], instance);
            }
        } else {
            throw IllegalArgumentException("Instance doesn't match this StructDef")
        }
    }

    override fun matchesDef(field: Field<*>, parent: StructInstance): Boolean {
        return matchesDef(asStructInstance(field))
    }

    fun matchesDef(struct: StructInstance): Boolean {
        return matchesDef(struct, true)
    }

    private fun matchesDef(struct: StructInstance, recursively: Boolean): Boolean {
        for (parser in parserChain) {
            if (parser.fieldName !in struct) {
                return false
            } else if (recursively || parser !is StructDef){
                return parser.matchesDef(struct[parser.fieldName], struct)
            }
        }
        return parserChain.find { it.fieldName !in struct } == null
    }

    override protected fun prepareWrite(field: Field<*>, parent: StructInstance) {
        prepareWrite(asStructInstance(field))
    }

    private fun prepareWrite(instance: StructInstance) {
        for (parser in parserChain) {
            // StructDefs prepare themselves within their write() method, so don't call them recursively
            // Fields with SIZE qualifier are added automatically and might not exist yet (and don't need any
            // preparation)
            if (parser !is StructDef && !parser.hasQualifier(Field.QUAL_SIZE)) {
                parser.prepareWrite(instance[parser.fieldName], instance)
            }
        }
    }

    private fun asStructInstance(field: Field<*>): StructInstance {
        if (field is StructInstance) {
            return field
        } else {
            throw IllegalArgumentException("field has to be a StructInstance (is " + field.javaClass.name + ")")
        }
    }

    private inner class DefFactory(val def: StructDef) : FieldDefFactory() {
        override fun createParser(definition: Item): FieldDef {
            if (definition.value != name) {
                throw IllegalArgumentException("Invalid def type: " + definition.value + " != $name")
            }
            return def
        }
    }
}
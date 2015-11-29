package de.fabmax.binparse

import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Created by max on 18.11.2015.
 */

class StructDef(definition: Item) : FieldDef<StructInstance>(definition.identifier) {

    val name = definition.identifier;

    private val parserChain = ArrayList<FieldDef<*>>()

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

    override fun parse(reader: BinReader, context: ContainerField<*>): StructInstance {
        return parse(reader)
    }

    fun write(output: OutputStream, instance: StructInstance) {
        writeInstance(BinWriter(output), instance)
    }

    override fun write(writer: BinWriter, context: ContainerField<*>) {
        writeInstance(writer, context.getStruct(fieldName))
    }

    private fun writeInstance(writer: BinWriter, instance: StructInstance) {
        prepareChildren(instance)
        if (matchesInstance(instance, false)) {
            parserChain.forEach {
                it.write(writer, instance);
            }
        } else {
            throw IllegalArgumentException("Instance doesn't match this StructDef")
        }
    }

    override fun matchesDef(context: ContainerField<*>): Boolean {
        return matchesInstance(context.getStruct(fieldName), true)
    }

    fun matchesInstance(instance: StructInstance): Boolean {
        return matchesInstance(instance, true)
    }

    private fun matchesInstance(instance: StructInstance, recursively: Boolean): Boolean {
        var matches = true
        for (parser in parserChain) {
            if (parser.fieldName !in instance) {
                matches = false
            } else if (parser !is StructDef) {
                matches = matches && parser.matchesDef(instance)
            } else if (parser is StructDef && recursively) {
                matches = matches && parser.matchesDef(instance)
            }

            if (!matches) {
                break
            }
        }
        return matches
    }

    override protected fun prepareWrite(context: ContainerField<*>) {
        prepareChildren(context.getStruct(fieldName))
    }

    private fun prepareChildren(instance: StructInstance) {
        for (parser in parserChain) {
            // StructDefs prepare themselves within their write() method, so don't call them recursively
            if (parser !is StructDef) {
                parser.prepareWrite(instance)
            }
        }
    }

    private inner class DefFactory(val def: StructDef) : FieldDefFactory() {
        override fun createParser(definition: Item): StructDef {
            if (definition.value != name) {
                throw IllegalArgumentException("Invalid def type: " + definition.value + " != $name")
            }
            return def
        }
    }
}
package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 21.11.2015.
 */

class SelectDef private constructor(fieldName: String, selector: String, choices: Map<Long?, FieldDef>):
        FieldDef(fieldName) {

    private val selector = selector;
    private val choices = choices;

    override fun parse(reader: BinReader, parent: StructInstance): Field<*> {
        val sel = parent.getLong(selector)
        val type = choices[sel] ?: choices[null] ?:
                throw IllegalArgumentException("Unmapped selector value: $sel")
        val field = type.parseField(reader, parent)
        field.name = fieldName
        return field
    }

    override fun prepareWrite(field: Field<*>, parent: StructInstance) {
        if (selector !in parent) {
            // make an educated guess about the selector field
            for ((key, fieldDef) in choices) {
                if (fieldDef.matchesDef(field, parent)) {
                    if (selector !in parent) {
                        parent.int(selector) {}
                    }
                    if (key != null) {
                        (parent[selector] as IntField).set(key)
                    }
                }
            }
        }
        val sel = parent.getLong(selector)
        val type = choices[sel] ?: choices[null] ?:
                throw IllegalArgumentException("Unmapped selector value: $sel")
        try {
            type.prepareWrite(field, parent)
        } catch(e: NoSuchFieldException) {
            println("no such field, sel=$sel")
            println(parent.toString(0))
            throw e
        }
    }

    override fun write(writer: BinWriter, field: Field<*>, parent: StructInstance) {
        val sel = parent.getLong(selector)
        val type = choices[sel] ?: choices[null] ?:
                throw IllegalArgumentException("Unmapped selector value: $sel")
        type.write(writer, field, parent)
    }

    override fun matchesDef(field: Field<*>, parent: StructInstance): Boolean {
        val sel = parent[selector]
        val parser = choices[sel.getIntValue()] ?: choices[null] ?: return false
        return parser.matchesDef(field, parent)
    }

    internal class Factory() : FieldDefFactory() {
        override fun createParser(definition: Item): FieldDef {
            val selector = getItem(definition.childrenMap, "selector").value
            val choices = HashMap<Long?, FieldDef>()

            definition.childrenMap.filter { item -> item.key != "selector" }
                    .forEach { item -> addFieldParser(item.value, choices) }

            return SelectDef(definition.identifier, selector, choices)
        }

        private fun addFieldParser(definition: Item, choices: HashMap<Long?, FieldDef>) {
            val parserDef = getItem(definition.childrenMap, "use")
            val key = if (definition.value == "*") {
                null
            } else {
                parseDecimal(definition.value) ?: throw NumberFormatException("Invalid select mapping: " +
                        definition.identifier + ": " + definition.value)
            }
            choices.put(key, FieldDefFactory.createParser(parserDef))
        }
    }
}

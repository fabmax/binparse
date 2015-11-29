package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 21.11.2015.
 */

class SelectDef private constructor(fieldName: String, selector: String, choices: Map<Long?, FieldDef<*>>) :
        FieldDef<Field<*>>(fieldName) {

    private val selector = selector;
    private val choices = choices;

    override fun parse(reader: BinReader, context: ContainerField<*>): Field<*> {
        val sel = context.getInt(selector).value
        val type = choices[sel] ?: choices[null] ?:
                throw IllegalArgumentException("Unmapped selector value: $sel")
        val field = type.parseField(reader, context)
        field.name = fieldName
        return field
    }

    override fun prepareWrite(context: ContainerField<*>) {
        super.prepareWrite(context)
//        if (selector !in parent) {
//            // make an educated guess about the selector field
//            for ((key, fieldDef) in choices) {
//                if (fieldDef.matchesDef(field, parent)) {
//                    if (selector !in parent) {
//                        parent.int(selector) {}
//                    }
//                    if (key != null) {
//                        (parent[selector] as IntField).set(key)
//                    }
//                }
//            }
//        }
        val sel = context.getInt(selector).value
        val type = choices[sel] ?: choices[null] ?:
                throw IllegalArgumentException("Unmapped selector value: $sel")
        type.prepareWrite(context)
    }

    override fun write(writer: BinWriter, context: ContainerField<*>) {
        val sel = context.getInt(selector).value
        val type = choices[sel] ?: choices[null] ?:
                throw IllegalArgumentException("Unmapped selector value: $sel")
        type.write(writer, context)
    }

    override fun matchesDef(context: ContainerField<*>): Boolean {
        val sel = context.getInt(selector).value
        val parser = choices[sel] ?: choices[null] ?: return false
        return parser.matchesDef(context)
    }

    internal class Factory() : FieldDefFactory() {
        override fun createParser(definition: Item): SelectDef {
            val selector = getItem(definition.childrenMap, "selector").value
            val choices = HashMap<Long?, FieldDef<*>>()

            definition.childrenMap.filter { item -> item.key != "selector" }
                    .forEach { item -> addFieldParser(definition.identifier, item.value, choices) }

            return SelectDef(definition.identifier, selector, choices)
        }

        private fun addFieldParser(fieldName: String, definition: Item, choices: HashMap<Long?, FieldDef<*>>) {
            val parserDef = getItem(definition.childrenMap, "use")
            val key = if (definition.value == "*") {
                null
            } else {
                parseDecimal(definition.value) ?: throw NumberFormatException("Invalid select mapping: " +
                        definition.identifier + ": " + definition.value)
            }
            val fieldDef = FieldDefFactory.createParser(parserDef)
            fieldDef.fieldName = fieldName
            choices.put(key, fieldDef)
        }
    }
}

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
        val sel = parent[selector]
        val parser = choices[sel.getIntValue()] ?: choices[null] ?:
                throw IllegalArgumentException("Unmapped selector value: $sel")
        val field = parser.parseField(reader, parent)
        field.name = fieldName
        return field
    }

    override fun write(writer: BinWriter, field: Field<*>, parent: StructInstance) {
        throw UnsupportedOperationException()
    }

    override fun matchesDef(field: Field<*>, parent: StructInstance): Boolean {
        val sel = parent[selector]
        val parser = choices[sel.getIntValue()] ?: choices[null] ?: return false
        return parser.matchesDef(field, parent)
    }

    internal class Factory() : FieldParserFactory() {
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
            choices.put(key, FieldParserFactory.createParser(parserDef))
        }
    }
}

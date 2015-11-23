package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 21.11.2015.
 */

class SelectParser private constructor(fieldName: String, selector: String, choices: Map<Long?, FieldParser>):
        FieldParser(fieldName) {

    private val selector = selector;
    private val choices = choices;

    override fun parse(reader: BinReader, result: StructInstance): Field {
        val sel = result[selector]
        val parser = choices[sel.getDecimalValue()] ?: choices[null] ?:
                throw IllegalArgumentException("Unmapped selector value: $sel")
        val field = parser.parseField(reader, result)
        field.name = fieldName
        return field
    }

    internal class Factory() : FieldParserFactory() {
        override fun createParser(definition: Item): FieldParser {
            val selector = getItem(definition.childrenMap, "selector").value
            val choices = HashMap<Long?, FieldParser>()

            definition.childrenMap.filter { item -> item.key != "selector" }
                    .forEach { item -> addFieldParser(item.value, choices) }

            return SelectParser(definition.identifier, selector, choices)
        }

        private fun addFieldParser(definition: Item, choices: HashMap<Long?, FieldParser>) {
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

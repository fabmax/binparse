package de.fabmax.binparse

import java.util.*
import kotlin.text.Regex

/**
 * Created by max on 15.11.2015.
 */

abstract class FieldParserFactory {

    private val decimalRegex = Regex("([0-9]*)|(0x[0-9a-fA-F]*)|(0b[01]*)")

    companion object {
        private val parserFactories = HashMap<String, FieldParserFactory>()

        init {
            parserFactories.put("int", IntParser.Factory(0, null))

            parserFactories.put("bit", IntParser.Factory(1, IntParser.Signedness.UNSIGNED))
            parserFactories.put("u08", IntParser.Factory(8, IntParser.Signedness.UNSIGNED))
            parserFactories.put("u16", IntParser.Factory(16, IntParser.Signedness.UNSIGNED))
            parserFactories.put("u32", IntParser.Factory(32, IntParser.Signedness.UNSIGNED))
            parserFactories.put("u64", IntParser.Factory(64, IntParser.Signedness.UNSIGNED))

            parserFactories.put("i08", IntParser.Factory(8, IntParser.Signedness.SIGNED))
            parserFactories.put("i16", IntParser.Factory(16, IntParser.Signedness.SIGNED))
            parserFactories.put("i32", IntParser.Factory(32, IntParser.Signedness.SIGNED))
            parserFactories.put("i64", IntParser.Factory(64, IntParser.Signedness.SIGNED))

            parserFactories.put("string", StringParser.Factory(null))
            parserFactories.put("utf-8", StringParser.Factory(Charsets.UTF_8))

            parserFactories.put("array", ArrayParser.Factory())
            parserFactories.put("select", SelectParser.Factory())
        }

        fun createParser(definition: Item): FieldParser {
            try {
                val fac = parserFactories[definition.value] ?:
                        throw IllegalArgumentException("Unknown type: " + definition.value)
                val parser = fac.createParser(definition)
                addQualifiers(definition, parser)
                return parser
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to create parser for " + definition.identifier + ": "
                        + definition.value, e)
            }
        }

        fun addParserFactory(typeName: String, factory: FieldParserFactory) {
            if (parserFactories.containsKey(typeName)) {
                throw IllegalArgumentException("Type name is already registered")
            }
            parserFactories.put(typeName, factory)
        }

        fun isType(name: String): Boolean {
            return parserFactories.containsKey(name)
        }

        private fun addQualifiers(definition: Item, parser: FieldParser) {
            val qualifiers = definition.childrenMap["_qualifiers"] ?: return

            qualifiers.value.splitToSequence('|').forEach {
                val q = it.trim()
                if (Field.QUALIFIERS.contains(q)) {
                    parser.qualifiers.add(q)
                } else {
                    throw IllegalArgumentException("Invalid / unknown qualifier: $q")
                }
            }
        }
    }

    abstract fun createParser(definition: Item): FieldParser;

    protected fun parseDecimal(string: String): Long? {
        if (string.matches(decimalRegex)) {
            if (string.startsWith("0x")) {
                return java.lang.Long.parseLong(string.substring(2), 16);
            } else if (string.startsWith("0b")) {
                return java.lang.Long.parseLong(string.substring(2), 1);
            } else {
                return java.lang.Long.parseLong(string);
            }
        } else {
            return null
        }
    }

    protected fun getItem(items: Map<String, Item>, name: String): Item {
        val item = items[name] ?: throw IllegalArgumentException("Missing attribute \"$name\"");
        return item;
    }
}

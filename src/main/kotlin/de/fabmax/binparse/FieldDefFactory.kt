package de.fabmax.binparse

import java.util.*
import kotlin.text.Regex

/**
 * Created by max on 15.11.2015.
 */

abstract class FieldDefFactory {

    private val decimalRegex = Regex("([0-9]*)|(0x[0-9a-fA-F]*)|(0b[01]*)")

    companion object {
        private val parserFactories = HashMap<String, FieldDefFactory>()

        init {
            parserFactories.put("int", IntDef.Factory(0, null))

            parserFactories.put("bit", IntDef.Factory(1, IntDef.Signedness.UNSIGNED))
            parserFactories.put("u08", IntDef.Factory(8, IntDef.Signedness.UNSIGNED))
            parserFactories.put("u16", IntDef.Factory(16, IntDef.Signedness.UNSIGNED))
            parserFactories.put("u32", IntDef.Factory(32, IntDef.Signedness.UNSIGNED))
            parserFactories.put("u64", IntDef.Factory(64, IntDef.Signedness.UNSIGNED))

            parserFactories.put("i08", IntDef.Factory(8, IntDef.Signedness.SIGNED))
            parserFactories.put("i16", IntDef.Factory(16, IntDef.Signedness.SIGNED))
            parserFactories.put("i32", IntDef.Factory(32, IntDef.Signedness.SIGNED))
            parserFactories.put("i64", IntDef.Factory(64, IntDef.Signedness.SIGNED))

            parserFactories.put("string", StringDef.Factory(null))
            parserFactories.put("utf-8", StringDef.Factory(Charsets.UTF_8))

            parserFactories.put("array", ArrayDef.Factory())
            parserFactories.put("select", SelectDef.Factory())
        }

        fun createParser(definition: Item): FieldDef {
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

        fun addParserFactory(typeName: String, factory: FieldDefFactory) {
            if (parserFactories.containsKey(typeName)) {
                throw IllegalArgumentException("Type name is already registered")
            }
            parserFactories.put(typeName, factory)
        }

        fun isType(name: String): Boolean {
            return parserFactories.containsKey(name)
        }

        private fun addQualifiers(definition: Item, fieldDef: FieldDef) {
            val qualifiers = definition.childrenMap["_qualifiers"] ?: return

            qualifiers.value.splitToSequence('|').forEach {
                val q = it.trim()
                if (Field.QUALIFIERS.contains(q)) {
                    fieldDef.qualifiers.add(q)
                } else {
                    throw IllegalArgumentException("Invalid / unknown qualifier: $q")
                }
            }
        }
    }

    abstract fun createParser(definition: Item): FieldDef;

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

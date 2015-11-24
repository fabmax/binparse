package de.fabmax.binparse

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.text.Regex

/**
 * Created by max on 15.11.2015.
 */

fun main(args: Array<String>) {
    //Parser("main: struct {some_val: int { bits: 4; } some_ival: int{ bits: 6; signedness: SIGNED; } bla: i32;" +
    //        "body:select{selector:some_val;val_a:0 {use:nop;}val_b:1{use:u16;}val_c:2{use:i32;}blub:*{use:u64;}}}")
//    val parser = Parser("text: struct {" +
//            "type: int { bits: 2;}" +
//            "len: int { bits: 6;} text: string {encoding: utf-8; length:len;}" +
//            "}" +
//            "flags: struct { QR: bit;OPCODE:int{bits:4;}AA:bit;TC:bit;RD:bit;RA:bit;Z:bit;AD:bit;CD:bit;RCODE:int{bits:4;}}" +
//            "main: struct { id: u16; flags: flags; num_questions: u16; num_answers: u16; " +
//            "num_authorities: u16; num_additionals: u16;" +
//            "domain: array { type: text; length: u08;}}");

    val parser = Parser.fromFile("dns-sd.sbp")

    val input = ByteArrayInputStream(byteArrayOf(
            0.toByte(), 0.toByte(), 132.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(),
            0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 13.toByte(), 49.toByte(), 57.toByte(), 50.toByte(),
            45.toByte(), 49.toByte(), 54.toByte(), 56.toByte(), 45.toByte(), 49.toByte(), 48.toByte(), 45.toByte(),
            55.toByte(), 50.toByte(), 5.toByte(), 108.toByte(), 111.toByte(), 99.toByte(), 97.toByte(), 108.toByte(),
            0.toByte(), 0.toByte(), 1.toByte(), 128.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 14.toByte(),
            16.toByte(), 0.toByte(), 4.toByte(), 192.toByte(), 168.toByte(), 10.toByte(), 72.toByte()))

    val struct = parser.structs["main"]!!
    val result = struct.parse(input)
    println(result.toString(0))
    //println(result["flags"])
}

/**
 * Parses stuff.
 *
 * @sample main
 *
 */
class Parser(source: String) {

    val items: List<Item>
    val structs = HashMap<String, StructDef>()

    init {
        items = parseBlock(source)
        for (item in items) {
            if (item.value == "struct") {
                structs.put(item.identifier, StructDef(item))
            }
        }
        //items.forEach { println(it.prettyPrint()) }
    }

    companion object {
        fun fromFile(fileName: String): Parser {
            val source = StringBuilder()
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(InputStreamReader(FileInputStream(fileName)))

                var line = reader.readLine()
                while (line != null) {
                    if (!line.trim().startsWith('#')) {
                        source.append(line)
                    }
                    line = reader.readLine()
                }

            } finally {
                reader?.close()
            }

            return Parser(source.toString())
        }
    }

    private fun parseBlock(source: String): List<Item> {
        val items = ArrayList<Item>();

        var rest = source.trim()
        while (!rest.isEmpty()) {
            // discard surrounding braces
            while (rest.startsWith('{') && rest.endsWith('}')) {
                rest = rest.substring(1, rest.length - 1).trim();
            }

            var idx = getBlockEndIdx(rest)
            val block = rest.substring(0, idx);

            if (hasValidIdentifier(block)) {
                val name = block.substring(0, block.indexOf(':')).trim()
                val value = block.substring(block.indexOf(':') + 1, block.indexOfAny(";{".toCharArray())).trim()
                val body = block.substring(block.indexOfAny(";{".toCharArray())).trim();

                val item = Item(name, value);
                if (body != ";") {
                    item.addChildren(parseBlock(body));
                }
                items.add(item)
            } else {
                println("invalid identifier: " + block)
            }

            rest = rest.substring(idx).trim();
        }

        return items;
    }

    private fun getBlockEndIdx(text: String): Int {
        var idx = text.indexOfAny(";{".toCharArray());
        if (idx > 0 && text[idx] == '{') {
            // look for matchin '{'
            var braceCnt = 1;
            while (++idx < text.length) {
                if (text[idx] == '{') {
                    braceCnt++;
                } else if (text[idx] == '}' && --braceCnt == 0) {
                    break;
                }
            }
            if (braceCnt > 0) {
                throw IllegalStateException("Unexpected end of String, missing '}'")
            }
        }
        if (idx < 0) {
            idx = text.length;
        } else {
            idx++;
        }
        return idx;
    }

    private fun hasValidIdentifier(block: String): Boolean {
        return block.matches(Regex("[A-Za-z][A-Za-z0-9_]*\\s*:.*[;{].*"));
    }
}

class Item(identifier: String, value: String, children : ArrayList<Item> = ArrayList<Item>())
        : Iterable<Item> by children {

    val identifier = identifier
    val value = value
    val children = children

    val childrenMap = HashMap<String, Item>()

    init {
        for (item in children) {
            if (childrenMap.containsKey(item.identifier)) {
                throw IllegalArgumentException("Duplicate identifier: " + item.identifier)
            }
            childrenMap.put(item.identifier, item)
        }
    }

    fun addChildren(items: List<Item>) {
        for (it in items) {
            addChild(it)
        }
    }

    fun addChild(item: Item) {
        if (childrenMap.containsKey(item.identifier)) {
            throw IllegalArgumentException("Duplicate identifier: " + item.identifier)
        }
        childrenMap.put(item.identifier, item)
        children.add(item)
    }

    fun prettyPrint(indent: Int = 0): String {
        val builder = StringBuilder();
        builder.append("$identifier: $value");
        if (children.isEmpty()) {
            builder.append(";\n")
        } else {
            builder.append(" {\n")
            for (c in children) {
                for (i in 1 .. indent + 2) {
                    builder.append(' ')
                }
                builder.append(c.prettyPrint(indent + 2))
            }
            for (i in 1 .. indent) {
                builder.append(' ')
            }
            builder.append("}\n")
        }
        return builder.toString()
    }
}


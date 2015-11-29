package de.fabmax.binparse.examples

import de.fabmax.binparse.Field
import de.fabmax.binparse.IntField
import de.fabmax.binparse.Parser
import de.fabmax.binparse.struct
import java.io.ByteArrayOutputStream

/**
 * Created by max on 29.11.2015.
 */

fun main(args: Array<String>) {
    val parser = Parser.fromFile("src/test/binparse/array.bp")
    val arrayDef = parser.structs["main"]!!

    val instance = struct {
        array("array") { +arrayOf<Field<*>>(IntField("first", 1)) }
    }

    println(instance.toString(0))

    val out = ByteArrayOutputStream()
    arrayDef.write(out, instance)
    for (b in out.toByteArray()) {
        System.out.printf("%03d ", b.toInt() and 0xff);
    }
}
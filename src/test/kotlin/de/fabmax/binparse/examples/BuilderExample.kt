package de.fabmax.binparse.examples

import de.fabmax.binparse.BinWriter
import de.fabmax.binparse.Parser
import de.fabmax.binparse.struct
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Created by max on 24.11.2015.
 */

fun main(args: Array<String>) {
    val parser = Parser.fromFile("src/test/binparse/dns.bp")
    val msgDef = parser.structs["main"]!!

    // builders rock!
    val instance = struct {
        int("id") { set(12345) }
        struct("flags") {
            int("QR") { set(1) }
            int("OPCODE") { }
            int("AA") { set(23) }
            int("TC") { }
            int("RD") { }
            int("RA") { }
            int("Z") { }
            int("AD") { }
            int("CD") { }
            int("RCODE"){ set(1) }
        }
//        int("num_questions") { }
//        int("num_answers") { }
//        int("num_authorities") { }
//        int("num_additionals") { }
        array("questions") { }
        array("answers") { }
        array("authorities") { }
        array("additionals") { }
    }

    println("matches instance: " + msgDef.matchesInstance(instance))

    val outStream = ByteArrayOutputStream()
    msgDef.write(outStream, instance)

    println()
    for (b in outStream.toByteArray()) {
        System.out.printf("%03d ", b.toInt() and 0xff);
    }
    println("\n")

    val inStream = ByteArrayInputStream(outStream.toByteArray())
    val parsed = msgDef.parse(inStream)

    println(parsed.toString(0))
}

package de.fabmax.binparse.examples

import de.fabmax.binparse.Parser
import de.fabmax.binparse.struct

/**
 * Created by max on 24.11.2015.
 */

fun main(args: Array<String>) {
    val parser = Parser.fromFile("src/test/binparse/dns.bp")
    val msgDef = parser.structs.get("main")!!

    val instance = struct {
        int("id") {+0}
        struct("flags") {
            int("QR") {+1}
            int("OPCODE") { }
            int("AA") { }
            int("TC") { }
            int("RD") { }
            int("RA") { }
            int("Z") { }
            int("AD") { }
            int("CD") { }
            int("RCODE"){ }
        }
        int("num_questions") {+0}
        int("num_answers") {+0}
        int("num_authorities") {+0}
        int("num_additionals") {+0}
        array("questions") { }
        array("answers") { }
        array("authorities") { }
        array("additionals") { }
    }

    println(msgDef.matchesDef(instance))
}

package de.fabmax.binparse.examples

import de.fabmax.binparse.ArrayField
import de.fabmax.binparse.ParseResult
import de.fabmax.binparse.StringField
import java.net.InetAddress
import java.util.*

/**
 * Created by max on 22.11.2015.
 */

class DnsMessage(parsed: ParseResult, from: InetAddress) {

    companion object {
        val TYPE_A = 1
        val TYPE_AAAA = 28
        val TYPE_PTR = 12
        val TYPE_TXT = 16
        val TYPE_SRV = 33
    }

    private val from = from
    private val parsed = parsed

    private val stringMap = HashMap<Int, String>()

    private val questions = HashMap<String, Question>()
    private val answers = HashMap<Int, ResourceRec>()

    init {
        collectStrings()
        buildAnswers()
        buildQuestions()

        if (answers.containsKey(TYPE_A) && answers.containsKey(TYPE_SRV)) {
            val srv = answers[TYPE_SRV]!!
            val addr = answers[TYPE_A]!!
            println("Service advertised from $from: " + srv.name + " - " + addr.addr + ":" + srv.port +
                    ", ttl: " + srv.ttl + ", answers: " + answers.keys)
        }
        questions.values.forEach { println("Question from $from: $it") }

        if (parsed.getInt("num_questions") == 0 && parsed.getInt("num_answers") == 0 &&
                parsed.getInt("num_authorities") == 0 && parsed.getInt("num_additionals") == 0) {
            println("Empty message from $from, flags: " + parsed.getStruct("flags"))
        }
    }

    private fun collectStrings() {
        val stringParts = HashMap<Int, String>()
        parsed.flat().asSequence()
                .filter { it is StringField }
                .forEach { stringParts.put(it.offset - 1, it.getStringValue()) }

        stringParts.keys.forEach { key ->
            val builder = StringBuilder()
            var s = stringParts[key]!!
            builder.append(s).append('.')

            var nextKey = key + s.length + 1
            while (stringParts.containsKey(nextKey)) {
                s = stringParts[nextKey]!!
                builder.append(s).append('.')
                nextKey += s.length + 1
            }
            stringMap.put(key, builder.toString())
        }
    }

    private fun buildQuestions() {
        parsed.getArray("questions")
                .filter { it is ParseResult }
                .map { it as ParseResult }
                .forEach {
                    val question = Question(it)
                    questions.put(question.name, question)
                }
    }

    private fun buildAnswers() {
        parsed.getArray("answers")
                .filter { it is ParseResult }
                .map { it as ParseResult }
                .forEach {
                    val answer = ResourceRec(it)
                    answers.put(answer.type, answer)
                }
    }

    private fun getName(labels: ArrayField): String {
        val name = StringBuilder()
        for (i in 0 .. labels.length - 1) {
            val item = labels.getStruct(i)
            val type = item.getInt("type")
            when (type) {
                0 -> name.append(item.getString("value.text")).append('.')
                3 -> name.append(stringMap[item.getInt("value")])
            }
        }
        return name.toString()
    }

    inner class Question(parsed: ParseResult) {
        val name: String
        val type: Int
        val clazz: Int

        init {
            name = getName(parsed.getArray("name"))
            type = parsed.getInt("type")
            clazz = parsed.getInt("class")
        }

        override fun toString(): String {
            return "$name [$type/$clazz]"
        }
    }

    inner class ResourceRec(parsed: ParseResult) {

        val name: String
        val type: Int
        val ttl: Int

        var port: Int = 0
        var target: String = ""
        var addr: String = ""

        init {
            name = getName(parsed.getArray("name"))
            type = parsed.getInt("type")
            ttl = parsed.getInt("ttl")

            if (type == TYPE_SRV) {
                port = parsed.getInt("data.port")
                target = getName(parsed.getArray("data.target"))
            } else if (type == TYPE_A) {
                addr = "" + parsed.getInt("data.a0") + "." + parsed.getInt("data.a1") +
                        "." + parsed.getInt("data.a2") + "." + parsed.getInt("data.a3")
            }
        }
    }
}

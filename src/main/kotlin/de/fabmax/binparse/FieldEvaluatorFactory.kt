package de.fabmax.binparse

import java.util.*

/**
 * Created by max on 15.11.2015.
 */

abstract class FieldEvaluatorFactory {

    companion object {
        val evaluatorFactories = HashMap<String, FieldEvaluatorFactory>()

        init {
            evaluatorFactories.put("mask", MaskEvaluator.Factory())
        }

        fun createEvaluator(definition: Item): FieldEvaluator {
            val fac = evaluatorFactories[definition.value];
            if (fac != null) {
                return fac.createEvaluator(definition)
            }
            throw IllegalArgumentException("Unknown evaluator type: " + definition.value)
        }
    }

    abstract fun createEvaluator(definition: Item) : FieldEvaluator;
}

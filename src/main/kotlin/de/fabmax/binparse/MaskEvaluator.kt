package de.fabmax.binparse

/**
 * Created by max on 18.11.2015.
 */

class MaskEvaluator: FieldEvaluator() {

    class Factory: FieldEvaluatorFactory() {
        override fun createEvaluator(definition: Item): FieldEvaluator {
            return MaskEvaluator()
        }
    }
}

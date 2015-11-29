package de.fabmax.binparse

/**
 * Created by max on 29.11.2015.
 */

abstract class ContainerField<T>(name: String, value: T) : Field<T>(name, value) {

    abstract operator fun get(name: String) : Field<*>

    abstract operator fun get(index: Int) : Field<*>

    open fun getArray(index: Int) : ArrayField {
        return get(index) as ArrayField
    }

    open fun getArray(name: String) : ArrayField {
        return get(name) as ArrayField
    }

    open fun getInt(index: Int) : IntField {
        return get(index) as IntField
    }

    open fun getInt(name: String) : IntField {
        return get(name) as IntField
    }

    open fun getString(index: Int) : StringField {
        return get(index) as StringField
    }

    open fun getString(name: String) : StringField {
        return get(name) as StringField
    }

    open fun getStruct(index: Int) : StructInstance {
        return get(index) as StructInstance
    }

    open fun getStruct(name: String) : StructInstance {
        return get(name) as StructInstance
    }

    open operator fun contains(key: String): Boolean {
        return false
    }

    open fun put(field: Field<*>) {
        throw UnsupportedOperationException("put is not allowed for this container")
    }

    override fun hasQualifier(qualifier: String): Boolean {
        if (qualifier != QUAL_COLLECT && super.hasQualifier(QUAL_COLLECT)) {
            return hasChildQualifier(qualifier)
        } else {
            return super.hasQualifier(qualifier)
        }
    }

    open fun hasChildQualifier(qualifier: String) : Boolean {
        return false
    }
}
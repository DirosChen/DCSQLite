package com.dirosc.sqlite

import android.content.ContentValues
import android.util.Log

infix fun Pair<String, Any?>.updateTo(table: String): Param.Update {
    return mapOf(this).updateTo(table)
}

infix fun Map<String, Any?>.updateTo(table: String): Param.Update {
    require(this != null && !table.isNullOrEmpty())
    var contentValue = ContentValues()
    this.forEach {
        if(it.value != null) {
            when(it.value!!::class.simpleName) {
                "int" -> contentValue.put(it.key, it.value as Int)
                "long" -> contentValue.put(it.key, it.value as Long)
                "double" -> contentValue.put(it.key, it.value as Double)
                "String" -> contentValue.put(it.key, it.value as String)
                "float" -> contentValue.put(it.key, it.value as Float)
                "byte" -> contentValue.put(it.key, it.value as Byte)
                "byte[]" -> contentValue.put(it.key, it.value as ByteArray)
                else -> contentValue.put(it.key, it.value.toString())
            }
        } else {
            contentValue.putNull(it.key)
        }
    }
    return Param.Update(table, contentValue)
}

infix fun Param.Update.whereAnd(value: Pair<String, Any?>): Param.Update {
    return this.whereAnd(mapOf(value))
}

infix fun Param.Update.and(value: Pair<String, Any?>): Param.Update {
    return this.whereAnd(mapOf(value))
}

infix fun Param.Update.and(value: Map<String, Any?>): Param.Update {
    return this.whereAnd(value)
}

infix fun Param.Update.and(value: Set<Pair<String, Any?>>): Param.Update {
    return this.whereAnd(value)
}

infix fun Param.Update.whereAnd(value: Set<Pair<String, Any?>>): Param.Update {
    require(value != null)
    return if(!this.statement.isNullOrEmpty()) {
        this.also {
            it.statement = "${it.statement} and ${setTo(value, "and")}"
        }
    } else {
        this.also {
            it.statement = setTo(value, "and")
        }
    }
}

infix fun Param.Update.whereAnd(value: Map<String, Any?>): Param.Update {
    require(value != null)
    return if(!this.statement.isNullOrEmpty()) {
        this.also {
            it.statement = "${it.statement} and ${mapTo(value, "and")}"
        }
    } else {
        this.also {
            it.statement = mapTo(value, "and")
        }
    }
}

infix fun Param.Update.or(value: Pair<String, Any?>): Param.Update {
    return this.whereOr(mapOf(value))
}

infix fun Param.Update.or(value: Map<String, Any?>): Param.Update {
    return this.whereOr(value)
}

infix fun Param.Update.or(value: Set<Pair<String, Any?>>): Param.Update {
    return this.whereOr(value)
}

infix fun Param.Update.whereOr(value: Pair<String, Any?>): Param.Update {
    return this.whereOr(mapOf(value))
}

infix fun Param.Update.whereOr(value: Map<String, Any?>): Param.Update {
    require(value != null)
    return if(!this.statement.isNullOrEmpty()) {
        this.also {
            it.statement = "${it.statement} or ${mapTo(value, "or")}"
        }
    } else {
        this.also {
            it.statement = mapTo(value, "or")
        }
    }
}

infix fun Param.Update.whereOr(value: Set<Pair<String, Any?>>): Param.Update {
    require(value != null)
    return if(!this.statement.isNullOrEmpty()) {
        this.also {
            it.statement = "${it.statement} or ${setTo(value, "or")}"
        }
    } else {
        this.also {
            it.statement = setTo(value, "or")
        }
    }
}
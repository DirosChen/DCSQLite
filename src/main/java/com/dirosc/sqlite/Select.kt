package com.dirosc.sqlite

import android.util.Log

infix fun Pair<String, Any?>.selectTo(query: String): Param.Select {
    return mapOf(this).selectTo(query)
}

infix fun Map<String, Any?>.selectTo(query: String): Param.Select {
    require(this != null && !query.isNullOrEmpty())
    var queryStr = query
    this.forEach {
        var before = "#{${it.key}}"
        var after = when(it.value) {
            is List<*> -> (it.value as List<*>).joinToString(",")
            else -> it.value.toString()
        }
        Log.i("TestDB", "before:$before   after=$after ")
        queryStr = queryStr.replace(before, after)
    }
    Log.i("TestDB", "query: $query   queryStr=$queryStr")
    return Param.Select(queryStr)
}

fun String.select(): Param.Select {
    require(this != null)
    return Param.Select("select * from $this")
}
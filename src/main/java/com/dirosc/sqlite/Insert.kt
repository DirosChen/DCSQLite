package com.dirosc.sqlite

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.dirosc.sqlite.ann.Column
import com.dirosc.sqlite.ann.Primary
import com.dirosc.sqlite.ann.Table
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

private var objectMapper: ObjectMapper =
    ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

infix fun List<Any>.insertTo(table: String): Param.Insert {
    require(!this.isNullOrEmpty() && !table.isNullOrEmpty())
    var list = mutableListOf<ContentValues>()
    this.forEach {
        var flag = false
        var contentValues = ContentValues()
        it::class.java.declaredFields.forEach {field ->
            field.isAccessible = true
            field.getAnnotation(Column::class.java)?.let { column ->
                if(!flag) {
                    var primary = field.getAnnotation(Primary::class.java)
                    if(primary != null) {
                        flag = true
                        if(!primary.auto) {
                            contentValues.put(column.value, if(column.json) if(field.get(it) != null) objectMapper.writeValueAsString(field.get(it)) else null else field.get(it).toString())
                        }
                    } else {
                        contentValues.put(column.value, if(column.json) if(field.get(it) != null) objectMapper.writeValueAsString(field.get(it)) else null else field.get(it).toString())
                    }
                } else {
                    contentValues.put(column.value, if(column.json) if(field.get(it) != null) objectMapper.writeValueAsString(field.get(it)) else null else field.get(it).toString())
                }
            }
        }
        list.add(contentValues)
    }
    return Param.Insert(table, list)
}

infix fun Any.insertTo(table: String): Param.Insert {
    require(this::class.java.isAnnotationPresent(Table::class.java) && !table.isNullOrEmpty())
    var flag = false
    var contentValues = ContentValues()
    this::class.java.declaredFields.forEach {
        it.isAccessible = true
        it.getAnnotation(Column::class.java)?.let {column ->
            if(!flag) {
                var primary = it.getAnnotation(Primary::class.java)
                if(primary != null) {
                    flag = true
                    if(!primary.auto) {
                        contentValues.put(column.value, if(column.json) if(it.get(this) != null) objectMapper.writeValueAsString(it.get(this)) else null else it.get(this).toString())
                    }
                } else {
                    contentValues.put(column.value, if(column.json) if(it.get(this) != null) objectMapper.writeValueAsString(it.get(this)) else null else it.get(this).toString())
                }
            } else {
                contentValues.put(column.value, if(column.json) if(it.get(this) != null) objectMapper.writeValueAsString(it.get(this)) else null else it.get(this).toString())
            }
        }
    }
    return Param.Insert(table, listOf(contentValues))
}

infix fun Map<String, Any?>.insertTo(table: String): Param.Insert {
    require(!this.isNullOrEmpty() && !table.isNullOrEmpty())
    var contentValues = ContentValues()
    this.forEach {
        if(it.value != null) {
            when(it.value!!::class.simpleName) {
                "int" -> contentValues.put(it.key, it.value as Int)
                "long" -> contentValues.put(it.key, it.value as Long)
                "double" -> contentValues.put(it.key, it.value as Double)
                "String" -> contentValues.put(it.key, it.value as String)
                "float" -> contentValues.put(it.key, it.value as Float)
                "byte" -> contentValues.put(it.key, it.value as Byte)
                "byte[]" -> contentValues.put(it.key, it.value as ByteArray)
                else -> contentValues.put(it.key, it.value.toString())
            }
        } else {
            contentValues.putNull(it.key)
        }
    }
    return Param.Insert(table, listOf(contentValues))
}


infix fun Param.Insert.conflict(value: Int): Param.Insert {
    require(value != null)
    return this.also {
        it.conflict = value
    }
}
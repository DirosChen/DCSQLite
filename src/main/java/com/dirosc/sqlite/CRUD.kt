package com.dirosc.sqlite

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.database.getBlobOrNull
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getFloatOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.dirosc.sqlite.ann.Column
import com.dirosc.sqlite.ann.Table
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.TimeUnit

sealed class Param {
    data class Select(var statement: String): Param()
    data class Update(var table: String, var contentValues: ContentValues, var statement: String? = null): Param()
    data class Insert(var table: String, var contentValues: List<ContentValues>, var conflict: Int = SQLiteDatabase.CONFLICT_REPLACE): Param()
    data class Delete(var table: String, var statement: String? = null): Param()
    data class Native(var sql: String, var wr: Int): Param()
    data class Alter(var table: String, var way: String, var type: String? = null, var column: String): Param()
    data class Transaction(var cruds: Array<out Param>): Param()
}

data class Not<T>(var t: T)

infix fun <A, B> A.toNot(that: B): Pair<A, Not<B>> {
    return Pair(this, Not(that))
}

internal fun setTo(set: Set<Pair<String, Any?>>, logic: String): String {
    return set.asSequence().map {
        if(it.second != null) {
            when (it.second) {
                is Not<*> -> {
                    var t = (it.second as Not<*>).t
                    if(t != null) {
                        if(t is List<*>) {
                            if(logic == "and") {
                                "${it.first} not in (${t.joinToString(","){m -> "'${m.toString()}'"}})"
                            } else {
                                (it.second as List<*>).joinToString(" or ") { m -> "${it.first}<>'${m.toString()}'" }
                            }
                        } else {
                            "${it.first}<>'${t}'"
                        }
                    } else {
                        "${it.first} is not null"
                    }
                }
                is List<*> -> {
                    if(logic == "and") {
                        "${it.first} in (${(it.second as List<*>).joinToString(","){m -> "'${m.toString()}'"}})"
                    } else {
                        (it.second as List<*>).joinToString(" or ") { m -> "${it.first}='${m.toString()}'" }
                    }
                }
                else -> "${it.first}='${it.second}'"
            }
        } else {
            "${it.first} is null"
        }
    }.joinToString(" $logic ")
}


internal fun mapTo(map: Map<String, Any?>, logic: String): String {
    return map.asSequence().map {
        if(it.value != null) {
            when (it.value) {
                is Not<*> -> {
                    var t = (it.value as Not<*>).t
                    if(t != null) {
                        if(t is List<*>) {
                            if(logic == "and") {
                                "${it.key} not in (${t.joinToString(","){m -> "'${m.toString()}'"}})"
                            } else {
                                (it.value as List<*>).joinToString(" or ") { m -> "${it.key}<>'${m.toString()}'" }
                            }
                        } else {
                            "${it.key}<>'${t}'"
                        }
                    } else {
                        "${it.key} is not null"
                    }
                }
                is List<*> -> {
                    if(logic == "and") {
                        "${it.key} in (${(it.value as List<*>).joinToString(","){m -> "'${m.toString()}'"}})"
                    } else {
                        (it.value as List<*>).joinToString(" or ") { m -> "${it.key}='${m.toString()}'" }
                    }
                }
                else -> "${it.key}='${it.value}'"
            }
        } else {
            "${it.key} is null"
        }
    }.joinToString(" $logic ")
}

class CRUD(val write: SQLiteDatabase, val read: SQLiteDatabase) {

    var objectMapper: ObjectMapper =
        ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
    var cache = CacheBuilder.newBuilder().maximumSize(50).expireAfterAccess(1, TimeUnit.HOURS).build<String, Any?>()
    val semaphore = Semaphore(1)

    @Suppress("UNCHECKED_CAST")
    inline fun<reified T> execSelect(param: Param.Select, isCache: Boolean): List<T> {
        return if(isCache) {
            cache.get(param.statement) {
                realSelect<T>(param)
            } as List<T>
        } else {
            realSelect(param)
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun<reified T> realSelect(param: Param.Select): List<T> {
       var cursor = read.rawQuery(param.statement, null)
       return when(T::class.simpleName) {
           "List" -> {
               var list = mutableListOf<List<Any?>>()
               while(cursor.moveToNext()) {
                   var valueList = mutableListOf<Any?>()
                   for(i in 0 until cursor.columnCount) {
                       when(cursor.getType(i)) {
                           Cursor.FIELD_TYPE_NULL -> valueList.add(null)
                           Cursor.FIELD_TYPE_INTEGER -> valueList.add(cursor.getLongOrNull(i))
                           Cursor.FIELD_TYPE_FLOAT -> valueList.add(cursor.getDoubleOrNull(i))
                           Cursor.FIELD_TYPE_STRING -> valueList.add(cursor.getStringOrNull(i))
                           Cursor.FIELD_TYPE_BLOB -> valueList.add(cursor.getBlobOrNull(i))
                       }
                   }
                   list.add(valueList)
               }
               cursor.close()
               list as List<T>
           }
           "Map" -> {
               var list = mutableListOf<Map<String, Any?>>()
               while(cursor.moveToNext()) {
                   var map = mutableMapOf<String, Any?>()
                   for(i in 0 until cursor.columnCount) {
                       var columnName = cursor.getColumnName(i)
                       when(cursor.getType(i)) {
                           Cursor.FIELD_TYPE_NULL -> map[columnName] = null
                           Cursor.FIELD_TYPE_INTEGER -> map[columnName] = cursor.getLongOrNull(i)
                           Cursor.FIELD_TYPE_FLOAT -> map[columnName] = cursor.getDoubleOrNull(i)
                           Cursor.FIELD_TYPE_STRING -> map[columnName] = cursor.getStringOrNull(i)
                           Cursor.FIELD_TYPE_BLOB -> map[columnName] = cursor.getBlobOrNull(i)
                       }
                   }
                   list.add(map)
               }
               cursor.close()
               list as List<T>
           }
           else -> {
                if(T::class.java.isAnnotationPresent(Table::class.java)) {
                    var list = mutableListOf<T>()
                    while(cursor.moveToNext()) {
                        //Log.i(TAG, "realSelect: ${T::class.java.declaredFields}")
                        var obj = try {
                            T::class.java.getConstructor().newInstance()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        T::class.java.declaredFields.forEach {
                            it.isAccessible = true
                            try {
                                it.getAnnotation(Column::class.java)?.run {
                                    var index = cursor.getColumnIndex(this.value)
                                    var json = this.json
                                    //Log.i(TAG, "type:${it.type.simpleName}")
                                    when(it.type.simpleName.lowercase()) {
                                        "int" -> it.set(obj, cursor.getIntOrNull(index))
                                        "long" -> it.set(obj, cursor.getLongOrNull(index))
                                        "String" -> it.set(obj, cursor.getStringOrNull(index))
                                        "byte[]" -> it.set(obj, cursor.getBlobOrNull(index))
                                        "byte" -> it.set(obj, cursor.getIntOrNull(index))
                                        "double" -> it.set(obj, cursor.getDoubleOrNull(index))
                                        "float" -> it.set(obj, cursor.getFloatOrNull(index))
                                        else -> {
                                            cursor.getStringOrNull(index)?.run {
                                                if(json) {
                                                    try {
                                                        it.set(obj, objectMapper.readValue(this, it.type))
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                } else {
                                                    it.set(obj, this)
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                throw e
                            }
                        }
                        list.add(obj as T)
                    }
                    cursor.close()
                    list
                } else {
                    throw Exception("unchecked type")
                }
           }
       }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun<reified T> execInsert(param: Param.Insert): List<T> {
        var ids = mutableListOf<Long>()
        param.contentValues.forEach {
            var id = write.insertWithOnConflict(param.table, null, it, param.conflict)
            ids.add(id)
        }
        cache.cleanUp()
        return ids as List<T>
    }

    @Suppress("UNCHECKED_CAST")
    inline fun<reified T> execDelete(param: Param.Delete): List<T> {
        cache.cleanUp()
        return listOf(write.delete(param.table, param.statement, arrayOf()).toLong()) as List<T>
    }

    @Suppress("UNCHECKED_CAST")
    inline fun<reified T> execUpdate(param: Param.Update): List<T>  {
        cache.cleanUp()
        return listOf(write.update(param.table, param.contentValues, param.statement, arrayOf()).toLong()) as List<T>
    }

    @Suppress("UNCHECKED_CAST")
    suspend inline fun<reified T> execTransaction(param: Param.Transaction): List<T>  {
        return lock {
            write.beginTransaction()
            try {
                param.cruds.forEach {
                    when(it) {
                        is Param.Insert -> execInsert<Long>(it)
                        is Param.Update -> execUpdate<Long>(it)
                        is Param.Delete -> execDelete<Long>(it)
                        is Param.Native -> execNative<Unit>(it)
                        else -> {/*do nothing*/}
                    }
                }
                write.setTransactionSuccessful()
                cache.cleanUp()
                return listOf(param.cruds.size.toLong()) as List<T>
            } catch (e: Exception) {
                throw e
            } finally {
                write.endTransaction()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun<reified T> execNative(param: Param.Native): List<T> {
        when(param.wr) {
            NATIVE_WRITE -> {
                write.execSQL(param.sql)
            }
            else -> read.execSQL(param.sql)
        }
        return listOf(Unit) as List<T>
    }

    suspend inline fun<T> lock(block : () -> List<T>): List<T> {
        semaphore.acquire()
        try {
            return block()
        } finally {
            semaphore.release()
        }
    }
}
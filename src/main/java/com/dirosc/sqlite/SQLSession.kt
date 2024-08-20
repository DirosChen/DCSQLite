package com.dirosc.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class Session(val context: Context, val path: String, val version: Long, val cache: Boolean = false,
              val tables: Array<String>?, val alters: Array<Param.Alter>?){
    private val TAG = "SQLSession"
    private lateinit var dbHelper: DBHelper
    private var isOpen = AtomicBoolean(false)
    var crud: CRUD? = null

    fun open() {
        if(!isOpen.get()) {
            try {
                var file = File(path)
                file.parentFile?.run {
                    if(!exists()) {
                        mkdirs()
                    }
                }
                dbHelper = DBHelper(context, path, null, version.toInt())
                crud = CRUD(dbHelper.writableDatabase, dbHelper.readableDatabase)
                isOpen.set(true)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun close() {
        if(isOpen.get()) {
            try {
                dbHelper.close()
                crud?.cache?.cleanUp()
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            isOpen.set(false)
        }
    }

    private inner class DBHelper(
        context: Context?,
        name: String?,
        factory: SQLiteDatabase.CursorFactory?,
        version: Int
    ) : SQLiteOpenHelper(context, name, factory, version) {

        override fun onCreate(p0: SQLiteDatabase?) {
            tables?.forEach {
                p0?.execSQL("create table IF NOT EXISTS $it")
            }
        }

        override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
            Log.i(TAG, "onUpgrade: $p1 $p2")
            if(p1 < p2) {
                tables?.forEach {
                    p0?.execSQL("create table IF NOT EXISTS $it")
                }
                alters?.forEach {
                    var alter = "alter table ${it.table} ${it.way} column ${it.column} ${it.type}"
                    Log.d(TAG, "alters:$alter")
                    var exist = false
                    try {
                        var cursor = p0?.rawQuery("PRAGMA table_info(${it.table})", null)
                        while (cursor != null && cursor.moveToNext()) {
                            var index = cursor.getColumnIndex("name")
                            if(index >= 0) {
                                var column_name = cursor.getString(index)
                                if(column_name == it.column) {
                                    exist = true
                                    break
                                }
                            }
                        }
                        cursor?.close()
                        if(!exist) {
                            p0?.execSQL(alter)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

    }
}

suspend inline fun<reified T> String.useDB(crossinline exec: () -> Param): List<T> {
    require(DCSQLite.sessions.containsKey(this))
    if(DCSQLite.sessions[this@useDB] != null) {
        return DCSQLite.sessions[this@useDB]!!.crud!!.let {
            when(val fn = exec()) {
                is Param.Transaction -> it.execTransaction(fn)
                is Param.Select -> it.execSelect(fn, DCSQLite.sessions[this@useDB]!!.cache)
                is Param.Insert -> it.lock { it.execInsert(fn) }
                is Param.Update -> it.lock { it.execUpdate(fn) }
                is Param.Native -> if(fn.wr == NATIVE_WRITE) it.lock { it.execNative(fn) } else it.execNative(fn)
                else -> it.lock { it.execDelete(fn as Param.Delete) }
            }
        }
    } else {
        throw Exception("no session instance")
    }
}

inline fun<reified T> String.useDBFlow(crossinline exec: () -> Param): Flow<List<T>> {
    require(DCSQLite.sessions.containsKey(this))
    return flow {
        DCSQLite.sessions[this@useDBFlow]?.crud?.let {
            when(val fn = exec()) {
                is Param.Transaction -> emit(it.execTransaction(fn))
                is Param.Select -> emit(it.execSelect(fn, DCSQLite.sessions[this@useDBFlow]!!.cache))
                is Param.Insert -> emit(it.lock { it.execInsert(fn) })
                is Param.Update -> emit(it.lock { it.execUpdate(fn) })
                is Param.Delete -> emit(it.lock { it.execDelete(fn) })
                is Param.Native -> emit(if(fn.wr == NATIVE_WRITE) it.lock { it.execNative(fn) } else it.execNative(fn))
                else -> {/*do nothing*/}
            }
        }
    }
}

package com.dirosc.sqlite

import android.content.Context

object DCSQLite {

    var sessions = mutableMapOf<String, Session>()

    fun close() {
        sessions.values.forEach {
            it.close()
        }
    }

    private fun createSession(build: Builder) {
        require(build.mContext != null && !build.mPath.isNullOrEmpty() && build.mVersion != null)
        if(!sessions.containsKey(build.mPath)) {
            sessions[build.mPath!!] = Session(build.mContext!!, build.mPath!!, build.mVersion!!, build.mCache, build.mTables, build.mAlter)
        }
    }

    class Builder {

        var mContext: Context? = null
        var mPath: String? = null
        var mVersion: Long? = null
        var mTables: Array<String>? = null
        var mAlter: Array<Param.Alter>? = null
        var mCache = false

        fun context(ctx: Context): Builder {
            this.mContext = ctx
            return this
        }

        fun path(path: String): Builder {
            this.mPath = path
            return this
        }

        fun version(version: Long): Builder {
            this.mVersion = version
            return this
        }

        fun cache(cache: Boolean): Builder {
            this.mCache = cache
            return this
        }

        fun table(vararg table: String): Builder {
            this.mTables = arrayOf(*table)
            return this
        }

        fun alter(vararg alert: Param.Alter): Builder {
            this.mAlter = arrayOf(*alert)
            return this
        }

        fun build() = DCSQLite.createSession(this)
    }

}

fun String.openSQL() {
    DCSQLite.sessions[this]?.open()
}

fun String.closeSQL() {
    DCSQLite.sessions[this]?.close()
}


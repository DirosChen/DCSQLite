package com.dirosc.sqlite


const val NATIVE_READ = 0
const val NATIVE_WRITE = 1

infix fun String.runNativeSQL(wr: Int): Param.Native {
    return Param.Native(this, wr)
}
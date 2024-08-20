package com.dirosc.sqlite

import android.util.Log

fun Any.transaction(vararg arr: Param): Param.Transaction {
    require(!arr.isNullOrEmpty())
    return Param.Transaction(arr)
}

inline fun<T> List<T>.forTransaction(block: (index: Int,T) -> Param): Param.Transaction {
    require(!this.isNullOrEmpty())
    var param = arrayOf<Param>()
    this.forEachIndexed { index, t ->
        param += (block(index, t))
    }
    param.forEach {
        Log.i("TAG", "param:$it")
    }
    return Param.Transaction(param)
}

package com.dirosc.sqlite.ann

@Retention(value = AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Column(val value: String, val json: Boolean = false)

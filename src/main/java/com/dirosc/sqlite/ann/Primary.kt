package com.dirosc.sqlite.ann

@Retention(value = AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Primary(val auto: Boolean = true)

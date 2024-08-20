package com.dirosc.sqlite

infix fun String.addColumn(column: String): Param.Alter {
    return Param.Alter(this, "add", column = column)
}

infix fun Param.Alter.type(type: String): Param.Alter {
    return this.apply { this.type = type }
}
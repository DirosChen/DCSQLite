package com.dirosc.sqlite

fun String.delete(): Param.Delete {
    require(!this.isNullOrEmpty())
    return Param.Delete(table = this)
}

infix fun String.deleteWhereAnd(value: Pair<String, Any?>): Param.Delete {
    return deleteWhereAnd(mapOf(value))
}

infix fun String.deleteWhereAnd(value: Map<String, Any?>): Param.Delete {
    require(!this.isNullOrEmpty() && !value.isNullOrEmpty())
    return Param.Delete(this, mapTo(value, "and"))
}

infix fun String.deleteWhereAnd(value: Set<Pair<String, Any?>>): Param.Delete {
    require(!this.isNullOrEmpty() && !value.isNullOrEmpty())
    return Param.Delete(this, setTo(value, "and"))
}

infix fun String.deleteWhereOr(value: Pair<String, Any?>): Param.Delete {
    return deleteWhereOr(mapOf(value))
}

infix fun String.deleteWhereOr(value: Set<Pair<String, Any?>>): Param.Delete {
    require(!this.isNullOrEmpty() && !value.isNullOrEmpty())
    return Param.Delete(this, setTo(value, "or"))
}

infix fun String.deleteWhereOr(value: Map<String, Any?>): Param.Delete {
    require(!this.isNullOrEmpty() && !value.isNullOrEmpty())
    return Param.Delete(this, mapTo(value, "or"))
}

infix fun Param.Delete.or(value: Pair<String, Any?>): Param.Delete {
    return or(mapOf(value))
}

infix fun Param.Delete.or(value: Map<String, Any?>): Param.Delete {
    require(this != null && !value.isNullOrEmpty())
    return this.also {
        if(!this.statement.isNullOrEmpty()) {
            this.also {
                it.statement = "${it.statement} or ${mapTo(value, "or")}"
            }
        } else {
            this.also {
                it.statement = mapTo(value, "or")
            }
        }
    }
}

infix fun Param.Delete.or(value: Set<Pair<String, Any?>>): Param.Delete {
    require(this != null && !value.isNullOrEmpty())
    return this.also {
        if(!this.statement.isNullOrEmpty()) {
            this.also {
                it.statement = "${it.statement} or ${setTo(value, "or")}"
            }
        } else {
            this.also {
                it.statement = setTo(value, "or")
            }
        }
    }
}

infix fun Param.Delete.and(value: Pair<String, Any?>): Param.Delete {
    return and(mapOf(value))
}

infix fun Param.Delete.and(value: Map<String, Any?>): Param.Delete {
    require(this != null && !value.isNullOrEmpty())
    return this.also {
        if(!this.statement.isNullOrEmpty()) {
            this.also {
                it.statement = "${it.statement} and ${mapTo(value, "and")}"
            }
        } else {
            this.also {
                it.statement = mapTo(value, "and")
            }
        }
    }
}

infix fun Param.Delete.and(value: Set<Pair<String, Any?>>): Param.Delete {
    require(this != null && !value.isNullOrEmpty())
    return this.also {
        if(!this.statement.isNullOrEmpty()) {
            this.also {
                it.statement = "${it.statement} and ${setTo(value, "and")}"
            }
        } else {
            this.also {
                it.statement = setTo(value, "and")
            }
        }
    }
}
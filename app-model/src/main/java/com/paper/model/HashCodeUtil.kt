package com.paper.model

object HashCodeUtil {

    @JvmStatic
    fun hashCode(value: Long): Int {
        return value.xor(value.shr(32)).toInt()
    }
}

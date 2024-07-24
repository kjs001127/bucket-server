package com.icemelon404.bucket.common

import java.nio.ByteBuffer

val ByteBuffer.string
    get()= String(ByteArray(int).also { get(it) })

fun ByteBuffer.putString(str: String) =
    str.toByteArray().let {
        putInt(it.size)
        put(it)
    }
fun sizeOfString(str: String) = str.toByteArray().size + Integer.BYTES

fun Boolean.toByte() =
    if (this) 1.toByte()
    else 0.toByte()

fun Byte.toBoolean() = this == 1.toByte()

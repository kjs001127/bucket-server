package com.icemelon404.bucket.network.util

import io.netty.buffer.ByteBuf
import java.nio.ByteBuffer

val ByteBuffer.string
    get()= String(ByteArray(int).also { get(it) })

fun ByteBuffer.putString(str: String) =
    str.toByteArray().let {
        putInt(it.size)
        put(it)
    }
fun bufferSize(str: String) = str.toByteArray().size + 4

fun Boolean.toByte() =
    if (this) 1.toByte()
    else 0.toByte()

fun Byte.toBoolean() = this == 1.toByte()
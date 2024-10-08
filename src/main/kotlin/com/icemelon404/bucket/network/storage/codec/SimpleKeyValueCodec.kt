package com.icemelon404.bucket.network.storage.codec

import com.icemelon404.bucket.common.sizeOfString
import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.string
import com.icemelon404.bucket.core.KeyValue
import java.nio.ByteBuffer

class SimpleKeyValueCodec  {

    fun serialize(keyValue: List<KeyValue>): ByteBuffer {
        val bufferSize = keyValue.sumOf { bufferSize(it) }
        return ByteBuffer.allocate(bufferSize).apply {
            keyValue.forEach { write(it) }
            flip()
        }
    }

    private fun ByteBuffer.write(keyValue: KeyValue) {
        putString(keyValue.key)
        keyValue.value?.let { value->
            putInt(value.size)
            put(value)
        }?: putInt(0)
    }

    private fun bufferSize(keyValue: KeyValue) =
        sizeOfString(keyValue.key) + Integer.SIZE + (keyValue.value?.size?:0)

    fun deserialize(buffer: ByteBuffer): List<KeyValue> {
        val ret = mutableListOf<KeyValue>()
        while (true) {
            buffer.mark()
            try {
                ret.add(buffer.read())
            } catch (e: Exception) {
                buffer.reset()
                return ret
            }
        }
    }

    private fun ByteBuffer.read(): KeyValue {
        val key = string
        val valueSize = int
        val value = ByteArray(valueSize).also { get(it) }
        return KeyValue(key, value)
    }
}

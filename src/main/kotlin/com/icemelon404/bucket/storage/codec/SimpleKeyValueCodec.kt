package com.icemelon404.bucket.storage.codec

import com.icemelon404.bucket.network.util.bufferSize
import com.icemelon404.bucket.network.util.putString
import com.icemelon404.bucket.network.util.string
import com.icemelon404.bucket.storage.KeyValue
import java.nio.ByteBuffer

class SimpleKeyValueCodec: KeyValueCodec {

    override fun serialize(keyValue: List<KeyValue>): ByteBuffer {
        val bufferSize = keyValue
            .sumOf { bufferSize(it.key) + 4 + (it.value?.size?:0) }
        return ByteBuffer.allocate(bufferSize).apply {
            keyValue.forEach {
                putString(it.key)
                it.value?.let { value->
                    putInt(value.size)
                    put(value)
                }?: putInt(0)
            }
            flip()
        }
    }

    override fun deserialize(buffer: ByteBuffer): List<KeyValue> {
        val ret = mutableListOf<KeyValue>()
        while (true) {
            buffer.mark()
            try {
                val key = buffer.string
                val value = ByteArray(buffer.int).also { buffer.get(it) }
                ret.add(KeyValue(key, value))
            } catch (e: Exception) {
                buffer.reset()
                return ret
            }
        }
    }
}
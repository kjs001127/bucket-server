package com.icemelon404.bucket.adapter.core.storage.aof

import com.icemelon404.bucket.common.sizeOfString
import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.string
import com.icemelon404.bucket.storage.KeyValue
import java.nio.ByteBuffer

data class TermKeyValue(val term: Long, val keyValue: KeyValue)

class TermKeyValueCodec {

    fun serialize(keyValue: List<TermKeyValue>): ByteBuffer {
        val bufferSize = keyValue.sumOf { bufferSize(it) }
        return ByteBuffer.allocate(bufferSize).apply {
            keyValue.forEach { write(it) }
            flip()
        }
    }

    private fun ByteBuffer.write(data: TermKeyValue) {
        putLong(data.term)
        putString(data.keyValue.key)
        data.keyValue.value?.let { value->
            putInt(value.size)
            put(value)
        }?: putInt(0)
    }

    private fun bufferSize(data: TermKeyValue) =
        Long.SIZE_BYTES + sizeOfString(data.keyValue.key) + Integer.SIZE + (data.keyValue.value?.size?:0)

    fun deserialize(buffer: ByteBuffer): List<TermKeyValue> {
        val ret = mutableListOf<TermKeyValue>()
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

    private fun ByteBuffer.read(): TermKeyValue {
        val term = long
        val key = string
        val valueSize = int
        val value = ByteArray(valueSize).also { get(it) }
        return TermKeyValue(term, KeyValue(key, value))
    }
}
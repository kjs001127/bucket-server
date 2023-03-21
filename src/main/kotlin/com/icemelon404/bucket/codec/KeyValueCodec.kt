package com.icemelon404.bucket.codec

import com.icemelon404.bucket.storage.KeyValue
import java.nio.ByteBuffer

interface KeyValueCodec {
    fun serialize(keyValue: List<KeyValue>): ByteBuffer
    fun deserialize(buffer: ByteBuffer): List<KeyValue>
}
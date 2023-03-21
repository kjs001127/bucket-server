package com.icemelon404.bucket.replication

import com.icemelon404.bucket.storage.KeyValue

interface OffsetAwareWritable {
    val offset: Long
    fun truncate(offset: Long)
    fun write(keyValues: List<KeyValue>)
}

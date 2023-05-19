package com.icemelon404.bucket.replication

interface OffsetAwareWritable: OffsetReadable {
    fun write(bytes: ByteArray)
    fun truncate(offset: Long)
}

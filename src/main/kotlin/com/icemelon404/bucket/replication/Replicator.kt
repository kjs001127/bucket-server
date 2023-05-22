package com.icemelon404.bucket.replication

import java.io.Closeable
import java.nio.ByteBuffer

interface ReplicatorFactory {
    fun newReplicator(offset: Long): Replicator
}

interface Replicator : Closeable {
    fun read(buf: ByteBuffer, timeoutMs: Long)
}

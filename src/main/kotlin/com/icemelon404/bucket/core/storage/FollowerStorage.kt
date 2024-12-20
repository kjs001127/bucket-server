package com.icemelon404.bucket.core.storage

import com.icemelon404.bucket.core.aof.AppendOnlyFile
import com.icemelon404.bucket.common.withTry
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.OffsetAwareWritable
import com.icemelon404.bucket.core.KeyValue
import com.icemelon404.bucket.core.KeyValueStorage
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

class FollowerStorage(
    private val file: AppendOnlyFile,
    val storage: KeyValueStorage,
) : OffsetAwareWritable, StorageStatus {

    override val offset: Long
        get() = file.offset
    private lateinit var leader: InstanceAddress
    private val lock = ReentrantLock()
    private val buffer = ByteBuffer.allocateDirect(10 * 1024 * 1024)

    override fun truncate(offset: Long) = lock.withLock {
        buffer.clear()
        storage.clear()
        file.truncate(offset)
        file.loadAndFix { storage.write(it.keyValue) }
    }

    override fun write(bytes: ByteArray): Unit = lock.withTry {
        buffer.put(bytes)
        buffer.flip()

        file.write(buffer).forEach {
            storage.write(it.keyValue)
        }

        buffer.compact()
    }


    override fun close() {
    }

    override fun write(keyValue: KeyValue) {
        throw RedirectException(leader)
    }

    override fun read(key: String): ByteArray? {
        return storage.read(key)
    }

    override fun clear() {
        storage.clear()
    }

    fun startWith(leader: InstanceAddress) {
        this.buffer.clear()
        this.leader = leader
    }
}

class RedirectException(val to: InstanceAddress) : RuntimeException()

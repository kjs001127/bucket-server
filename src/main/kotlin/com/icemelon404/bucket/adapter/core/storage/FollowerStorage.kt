package com.icemelon404.bucket.adapter.core.storage

import com.icemelon404.bucket.adapter.core.storage.aof.AppendOnlyFile
import com.icemelon404.bucket.adapter.core.storage.aof.withTry
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.OffsetAwareWritable
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FollowerStorage(
    private val file: AppendOnlyFile,
    val storage: KeyValueStorage,
) : OffsetAwareWritable,StorageStatus  {

    override val offset: Long
        get() = lock.withTry { file.offset }
    private lateinit var leader: InstanceAddress
    private val lock = ReentrantLock()
    private val buffer = ByteBuffer.allocateDirect(10 * 1024 * 1024)

    override fun truncate(offset: Long): Unit = lock.withLock {
        storage.clear()
        file.truncate(offset).forEach {
            storage.write(it.keyValue)
        }
        buffer.clear()
    }

    override fun write(bytes: ByteArray): Unit = lock.withTry {
        buffer.put(bytes)
        buffer.flip()

        file.write(buffer).forEach {
            storage.write(it.keyValue)
        }

        buffer.compact()
    }

    override fun start() {
        buffer.clear()
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

    fun setLeader(leader: InstanceAddress) {
        this.leader = leader
    }
}

class RedirectException(val to: InstanceAddress): RuntimeException()
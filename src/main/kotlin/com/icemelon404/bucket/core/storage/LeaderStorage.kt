package com.icemelon404.bucket.core.storage

import com.icemelon404.bucket.core.aof.AofIterator
import com.icemelon404.bucket.core.aof.AppendOnlyFile
import com.icemelon404.bucket.core.aof.TermKeyValue
import com.icemelon404.bucket.replication.OffsetReadable
import com.icemelon404.bucket.replication.Replicator
import com.icemelon404.bucket.replication.ReplicatorFactory
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LeaderStorage(
    private val aof: AppendOnlyFile,
    private val storage: KeyValueStorage,
) : ReplicatorFactory, StorageStatus, OffsetReadable {

    @Volatile
    private var term: Long = -1
    private val lock = ReentrantLock()
    private val replicators = mutableSetOf<ReplicatorImpl>()

    override val offset: Long
        get() = lock.withLock { aof.offset }

    fun startWith(term: Long) {
        this.term = term
    }

    override fun close() = lock.withLock {
        replicators.toMutableList().forEach {
            it.closeFile()
        }
    }

    override fun write(keyValue: KeyValue) = lock.withLock {
        aof.write(listOf(TermKeyValue(term, keyValue))).let { bytes ->
            replicators.forEach { rep -> rep.addWrite(bytes) }
        }
        storage.write(keyValue)
    }

    override fun read(key: String): ByteArray? {
        return storage.read(key)
    }

    override fun clear(): Unit = lock.withLock {
        storage.clear()
        aof.truncate(0)
    }

    override fun newReplicator(offset: Long) = lock.withLock {
        ReplicatorImpl(offset, aof.offset).also { replicators.add(it) }
    }


    inner class ReplicatorImpl(
        startOffset: Long,
        endOffset: Long
    ) : Replicator {

        private val queue = LinkedBlockingQueue<ByteBuffer>()
        private val aof: AofIterator = this@LeaderStorage.aof.iterator(startOffset, endOffset)


        override fun read(buf: ByteBuffer, timeoutMs: Long) {
            if (aof.hasNext()) {
                aof.read(buf)
            } else {
                queue.poll(timeoutMs, TimeUnit.MILLISECONDS)?.let {
                    buf.put(it)
                }
            }
        }

        fun closeFile() = aof.close()

        override fun close() =
            lock.withLock {
                replicators.remove(this)
                aof.close()
            }


        fun addWrite(buf: ByteBuffer) = queue.add(buf)
    }
}

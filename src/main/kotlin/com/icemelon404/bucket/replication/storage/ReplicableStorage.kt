package com.icemelon404.bucket.replication.storage

import com.icemelon404.bucket.replication.OffsetAwareWritable
import com.icemelon404.bucket.replication.Replicator
import com.icemelon404.bucket.replication.ReplicatorFactory
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ReplicableStorage(
    makeAof: () -> AppendOnlyFile,
) : ReplicatorFactory, KeyValueStorage, OffsetAwareWritable {

    private var map: ConcurrentMap<String, ByteArray> = ConcurrentHashMap()
    private val aof: AppendOnlyFile = makeAof()
    private val lock = ReentrantLock()
    private val replicators = mutableSetOf<ReplicatorImpl>()

    override val offset: Long
        get() = lock.withLock { aof.offset }

    init {
        map = aof.toMap()
    }

    override fun truncate(offset: Long) = lock.withLock {
        aof.truncate(offset)
        map.clear()
        map = aof.toMap()

    }

    override fun newReplicator(offset: Long) = lock.withLock {
        ReplicatorImpl(offset, this.offset).also { replicators.add(it) }
    }

    override fun write(keyValue: KeyValue) = withTryLock {
        writeInMemory(keyValue)
        aof.write(listOf(keyValue))
        replicators.forEach { it.addWrite(keyValue) }
    }

    override fun write(keyValues: List<KeyValue>) = withTryLock {
        keyValues.forEach { writeInMemory(it) }
        aof.write(keyValues)
        replicators.forEach { it.addWrite(keyValues) }
    }

    private inline fun <T> withTryLock(block: () -> T): T {
        if (!lock.tryLock(100, TimeUnit.MILLISECONDS))
            error("Storage is not available")
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

    private fun writeInMemory(keyValue: KeyValue) {
        if (keyValue.value == null)
            map.remove(keyValue.key)
        else
            map[keyValue.key] = keyValue.value
    }

    override fun read(key: String): ByteArray? = withTryLock { map[key] }


    inner class ReplicatorImpl(
        startOffset: Long,
        endOffset: Long
    ) : Replicator {

        private val queue = LinkedBlockingQueue<KeyValue>()
        private val aof: AofIterator = this@ReplicableStorage.aof.iterator(startOffset, endOffset)


        override fun next(): KeyValue? {
            return if (aof.hasNext())
                aof.next()
            else
                queue.poll(50, TimeUnit.MILLISECONDS)
        }

        override fun close() {
            lock.withLock {
                replicators.remove(this)
                aof.close()
            }
        }

        fun addWrite(keyValue: KeyValue) = queue.add(keyValue)
        fun addWrite(keyValues: List<KeyValue>) = queue.addAll(keyValues)
    }
}


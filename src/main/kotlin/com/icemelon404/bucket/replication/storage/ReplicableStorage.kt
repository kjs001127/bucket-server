package com.icemelon404.bucket.replication.storage

import com.icemelon404.bucket.replication.OffsetAwareWritable
import com.icemelon404.bucket.replication.Replicator
import com.icemelon404.bucket.replication.ReplicatorFactory
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ReplicableStorage(
    makeAof: () -> AppendOnlyFile,
) : ReplicatorFactory, KeyValueStorage, OffsetAwareWritable {

    private val map = ConcurrentHashMap<String, ByteArray>()
    private val aof: AppendOnlyFile = makeAof()
    private val lock = ReentrantLock()
    private val replicators = mutableSetOf<ReplicatorImpl>()

    override var offset: Long
        get() = lock.withLock { aof.offset }
        set(value) {
            lock.withLock {
                aof.truncate(value)
                map.clear()
                aof.initMapAndFix(map)
            }
        }

    init {
        aof.initMapAndFix(map)
    }

    override fun newReplicator(startOffset: Long) = lock.withLock {
        ReplicatorImpl(startOffset, offset).also { replicators.add(it) }
    }

    override fun write(keyValue: KeyValue) = withTryLock {
        doWrite(keyValue)
        aof.write(listOf(keyValue))
        replicators.forEach { it.addWrite(keyValue) }
    }


    override fun write(keyValues: List<KeyValue>) = withTryLock {
        keyValues.forEach { doWrite(it) }
        aof.write(keyValues)
        replicators.forEach { it.addWrite(keyValues) }
    }

    private fun withTryLock(block: () -> Unit) {
        if (!lock.tryLock(100, TimeUnit.MILLISECONDS))
            error("Storage is not available")
        block()
        lock.unlock()
    }

    private fun doWrite(keyValue: KeyValue) {
        if (keyValue.value == null)
            map.remove(keyValue.key)
        else
            map[keyValue.key] = keyValue.value
    }

    override fun read(key: String) = map[key]

    inner class ReplicatorImpl(
        private val startOffset: Long,
        private val endOffset: Long
    ) : Replicator {

        private val queue = LinkedBlockingQueue<KeyValue>()

        override fun withKeyValue(consumer: (KeyValue?) -> Boolean) {
            try {
                readFromSnapShot(consumer)
                readFromQueue(consumer)
            } catch (_: Exception) {
            }
        }

        private fun readFromQueue(consumer: (KeyValue?) -> Boolean) {
            while (true) {
                queue.poll(50, TimeUnit.MILLISECONDS).let {
                    if (!consumer(it))
                        error("Consumer returned false")
                }
            }
        }

        private fun readFromSnapShot(consumer: (KeyValue) -> Boolean) {
            aof.read(startOffset, endOffset) {
                it.forEach { keyVal ->
                    if (!consumer(keyVal))
                        error("Consumer returned false")
                }
                true
            }
        }

        override fun close() {
            lock.withLock {
                replicators.remove(this)
            }
        }

        fun addWrite(keyValue: KeyValue) = queue.add(keyValue)
        fun addWrite(keyValues: List<KeyValue>) = queue.addAll(keyValues)
    }
}


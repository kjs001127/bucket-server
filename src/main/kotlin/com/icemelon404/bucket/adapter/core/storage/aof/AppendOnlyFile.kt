package com.icemelon404.bucket.adapter.core.storage.aof

import com.icemelon404.bucket.cluster.ClusterLog
import com.icemelon404.bucket.cluster.TermAndOffset
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

class AppendOnlyFile(
    private val filePath: String,
) : ClusterLog {

    private val lock = ReentrantLock()
    private lateinit var file0: FileChannel
    private val codec = TermKeyValueCodec()
    private var term: Long = 0

    private val file: FileChannel
        get() {
            if (!this::file0.isInitialized || !file0.isOpen) {
                file0 = openFile(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
                file0.position(file0.size())
            }
            return file0
        }
    val offset: Long
        get() = lock.withTry { file.position() }
    override val termAndOffset: TermAndOffset
        get() = lock.withTry { TermAndOffset(term, offset) }


    fun truncate(offset: Long) = lock.withLock {
        file.truncate(offset)
        file.force(true)
        load()
    }

    fun load() = lock.withLock {
        val ret = mutableListOf<TermKeyValue>()
        val codec = TermKeyValueCodec()
        val it = iterator(0, file.size())
        val buf = ByteBuffer.allocate(1024 * 1024 * 3)
        while (it.hasNext()) {
            it.read(buf)
            buf.flip()
            codec.deserialize(buf).forEach {
                term = max(term, it.term)
                ret.add(it)
            }
            buf.compact()
        }
        buf.flip()
        file.truncate(file.size() - buf.remaining())
        ret
    }

    fun iterator(start: Long, end: Long): AofIterator {
        return AofIterator(openFile(StandardOpenOption.READ), start, end)
    }

    private fun openFile(vararg extra: StandardOpenOption) =
        FileChannel.open(Paths.get(filePath), *extra)

    fun write(buf: ByteBuffer): List<TermKeyValue> = lock.withTry {
        val keyValues = codec.deserialize(buf)
        term = keyValues.maxOf { it.term }
        val limit = buf.limit()

        buf.flip()
        while (buf.hasRemaining()) {
            file.write(buf)
        }

        buf.limit(limit)
        return keyValues
    }

    fun write(data: List<TermKeyValue>): ByteBuffer = lock.withTry {
        term = data.maxOf { it.term }

        val buf = codec.serialize(data)

        while (buf.hasRemaining()) {
            file.write(buf)
        }

        buf.flip()
        return buf
    }
}

class AofIterator(
    val channel: FileChannel,
    private val end: Long,
    start: Long,
) {

    init {
        channel.position(start)
    }

    fun read(buffer: ByteBuffer) {
        val remaining = (end - channel.position()).toInt()
        if (remaining <= 0) {
            return
        }

        buffer.limit(min(buffer.capacity(), remaining))
        channel.read(buffer)
    }

    fun hasNext(): Boolean {
        return channel.position() < end
    }


    fun close() {
        channel.close()
    }
}

inline fun <T> ReentrantLock.withTry(f: ()->T): T {
    this.tryLock()
    try {
        return f()
    } finally {
        this.unlock()
    }
}
package com.icemelon404.bucket.core.aof

import com.icemelon404.bucket.cluster.ClusterLog
import com.icemelon404.bucket.cluster.TermAndOffset
import io.netty.buffer.UnpooledDirectByteBuf
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.function.Consumer

class AppendOnlyFile(
    private val filePath: String,
) : ClusterLog {

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
        get() = file.position()
    override val termAndOffset: TermAndOffset
        get() = TermAndOffset(term, offset)


    fun truncate(offset: Long) {
        file.truncate(offset)
        file.force(true)
    }


    fun loadAndFix(consumer: Consumer<TermKeyValue>) {

        val it = iterator(0, file.size())
        val buf = ByteBuffer.allocate(1024 * 1024 * 3)

        while (it.hasNext()) {
            it.read(buf)
            buf.flip()
            codec.deserialize(buf).forEach {
                consumer.accept(it)
            }
            buf.compact()
        }

        truncate(file.position() - buf.position())
    }

    fun iterator(start: Long, end: Long): AofIterator {
        return AofIterator(openFile(StandardOpenOption.READ), start, end)
    }

    private fun openFile(vararg extra: StandardOpenOption) =
        FileChannel.open(Paths.get(filePath), *extra)

    fun write(buf: ByteBuffer): List<TermKeyValue> {
        val keyValues = codec.deserialize(buf)
        if (keyValues.isEmpty()) {
            return emptyList()
        }

        term = keyValues.maxOf { it.term }
        val limit = buf.limit()

        buf.flip()
        while (buf.hasRemaining()) {
            file.write(buf)
        }

        buf.limit(limit)
        return keyValues
    }

    fun write(data: List<TermKeyValue>): ByteBuffer {
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

        buffer.limit(min(buffer.limit(), buffer.position() + remaining))
        channel.read(buffer)
    }

    fun hasNext(): Boolean {
        return channel.position() < end
    }


    fun close() {
        channel.close()
    }
}


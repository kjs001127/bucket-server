package com.icemelon404.bucket.replication.storage

import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.codec.KeyValueCodec
import java.lang.IllegalStateException
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class AppendOnlyFile(
    private val filePath: String,
    private val codec: KeyValueCodec
) {

    private val file: FileChannel
    val offset: Long
        get() = file.position()

    init {
        file = openFile(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
        file.position(file.size())
    }

    fun truncate(offset: Long) {
        file.truncate(offset)
        file.force(true)
    }

    fun iterator(start: Long, end: Long) : AofIterator {
        return AofIterator(openFile(StandardOpenOption.READ), codec, start, end)
    }

    fun toMap() : ConcurrentMap<String, ByteArray> {
        val ret = ConcurrentHashMap<String, ByteArray>()
        val iterator = iterator(0, file.size())
        while (iterator.hasNext()) {
            val keyVal = iterator.next()
            if (keyVal.value != null)
                ret[keyVal.key] = keyVal.value
            else
                ret.remove(keyVal.key)
        }
        file.truncate(file.position())
        file.force(true)
        return ret
    }

    private fun openFile(vararg extra: StandardOpenOption) =
        FileChannel.open(Paths.get(filePath), *extra)

    fun write(dataList: List<KeyValue>) {
        file.write(codec.serialize(dataList))
    }
}

class AofIterator(
    val channel: FileChannel,
    val codec: KeyValueCodec,
    val end: Long,
    start: Long,
) {

    var buffer = ByteBuffer.allocateDirect(1024 * 1024 * 10)
    var queue : Queue<KeyValue> = LinkedList()

    init {
        channel.position(start)
    }

    fun next(): KeyValue {
        if (queue.isNotEmpty()) {
            return queue.poll();
        }
        queue.addAll(read())
        return queue.poll()?: throw IllegalStateException()
    }

    private fun read() : List<KeyValue> {
        try {
            val left = end - channel.position()
            buffer.limit(min(buffer.capacity(), (buffer.position() + left).toInt()))
            channel.read(buffer)
            buffer.flip()
            return codec.deserialize(buffer)
        } finally {
            buffer.compact()
        }
    }

    fun hasNext() = channel.position() < end

    fun close() {
        channel.close()
    }
}
package com.icemelon404.bucket.replication.storage

import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.codec.KeyValueCodec
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

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

    fun read(start: Long, end: Long, block: (List<KeyValue>) -> Boolean) {
        doRead(directBuffer(), openFile(StandardOpenOption.READ), start, end, block)
    }

    fun initMapAndFix(target: MutableMap<String, ByteArray>) {
        val buffer = directBuffer()
        doRead(buffer, file, 0, file.size()) {
            it.forEach { (key, value) ->
                if (value == null)
                    target.remove(key)
                else
                    target[key] = value
            }
            true
        }
        val remove = buffer.position()
        file.truncate(file.size() - remove)
        file.force(true)
    }

    private fun doRead(
        buffer: ByteBuffer,
        channel: FileChannel,
        start: Long,
        end: Long,
        block: (List<KeyValue>) -> Boolean
    ) {
        channel.position(start)
        while (channel.position() < end) {
            val left = end - channel.position()
            buffer.limit(min(buffer.capacity(), (buffer.position() + left).toInt()))
            channel.read(buffer)
            buffer.flip()
            if (!block(codec.deserialize(buffer)))
                return
            buffer.compact()
        }
    }

    private fun openFile(vararg extra: StandardOpenOption) =
        FileChannel.open(Paths.get(filePath), *extra)

    private fun directBuffer() = ByteBuffer.allocateDirect(1024 * 1024 * 10)

    fun write(dataList: List<KeyValue>) {
        file.write(codec.serialize(dataList))
    }
}
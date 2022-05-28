package com.icemelon404.bucket.replication.storage

import com.icemelon404.bucket.replication.LogRepository
import com.icemelon404.bucket.replication.listener.IdAndOffset
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class ReplicationLogImpl(
    private val path: String
) : LogRepository {

    private val currentMasterFile = openFile("current")
    private val lastMasterFile = openFile("last")

    override var currentMaster: String?
        get() = readCurrentMaster()
        set(value) = writeCurrentMaster(value)

    override var lastMaster: IdAndOffset?
        get() = readLastMaster()
        set(value) = writeLastMaster(value)

    private fun openFile(fileName: String): FileChannel {
        val dir = Paths.get(path)
        Files.createDirectories(dir)
        return FileChannel.open(Paths.get(path + File.separator + fileName), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
    }

    private fun readCurrentMaster(): String? {
        val buffer = ByteBuffer.allocate(currentMasterFile.size().toInt())
        currentMasterFile.position(0)
        currentMasterFile.read(buffer)
        return String(buffer.array()).takeIf { it.isNotEmpty() }
    }

    private fun readLastMaster(): IdAndOffset? {
        val buffer = ByteBuffer.allocate(lastMasterFile.size().toInt())
        lastMasterFile.position(0)
        lastMasterFile.read(buffer)
        buffer.flip()
        return try {
            val idSize = buffer.int
            val idStr = String(ByteArray(idSize).also { buffer.get(it) })
            val offset = buffer.long
            IdAndOffset(idStr, offset)
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCurrentMaster(str : String?) {
        currentMasterFile.truncate(0)
        if (str == null)
            return
        currentMasterFile.write(ByteBuffer.wrap(str.toByteArray()))
    }
    private fun writeLastMaster(idAndOffset: IdAndOffset?) {
        lastMasterFile.truncate(0)
        idAndOffset?.let {
            val strBytes = idAndOffset.id.toByteArray()
            val buffer = ByteBuffer.allocate(strBytes.size + 12)
            buffer.putInt(strBytes.size)
            buffer.put(strBytes)
            buffer.putLong(it.offset)
            lastMasterFile.write(buffer)
        }
    }


}
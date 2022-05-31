package com.icemelon404.bucket.log

import com.icemelon404.bucket.replication.listener.IdAndOffset
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class FileLogRepository(
    private val path: String
) : LogRepository {

    private val currentMasterFile = openFile("current")
    private val lastMasterFile = openFile("last")

    override var currentMaster: Long?
        get() = readCurrentMaster()
        set(value) = writeCurrentMaster(value)

    override var lastMaster: IdAndOffset?
        get() = readLastMaster()
        set(value) = writeLastMaster(value)

    private fun openFile(fileName: String): FileChannel {
        val dir = Paths.get(path)
        Files.createDirectories(dir)
        return FileChannel.open(
            Paths.get(path + File.separator + fileName),
            StandardOpenOption.READ,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE
        )
    }

    private fun readCurrentMaster(): Long? {
        return try{
            val buffer = ByteBuffer.allocate(8)
            currentMasterFile.position(0)
            currentMasterFile.read(buffer)
            buffer.flip()
            buffer.long
        } catch (e : Exception) {
            null
        }
    }

    private fun readLastMaster(): IdAndOffset? {
        val buffer = ByteBuffer.allocate(16)
        lastMasterFile.position(0)
        lastMasterFile.read(buffer)
        buffer.flip()
        return try {
            val id = buffer.long
            val offset = buffer.long
            IdAndOffset(id, offset)
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCurrentMaster(id: Long?) {
        currentMasterFile.truncate(0)
        if (id == null)
            return
        currentMasterFile.write(ByteBuffer.allocate(8).putLong(id).apply { flip() })
        currentMasterFile.force(true)
    }

    private fun writeLastMaster(idAndOffset: IdAndOffset?) {
        lastMasterFile.truncate(0)
        idAndOffset?.let {
            val buffer = ByteBuffer.allocate(16).apply {
                putLong(idAndOffset.id)
                putLong(idAndOffset.offset)
                flip()
            }
            lastMasterFile.write(buffer)
        }
        lastMasterFile.force(true)
    }


}
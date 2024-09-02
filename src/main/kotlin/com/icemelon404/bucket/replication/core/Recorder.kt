package com.icemelon404.bucket.replication.core

import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.sizeOfString
import com.icemelon404.bucket.common.string
import com.icemelon404.bucket.replication.VersionAndOffset
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class Recorder(
    val current: FileChannel,
) {
    var lastVersionAndOffset: VersionAndOffset? = null

    var currentVersion: String
        set(value) {
            current.truncate(0)
            current.position(0)

            val buf = ByteBuffer.allocate(sizeOfString(value) * 2)
            buf.putString(value)
            buf.putString(value)

            buf.flip()
            current.write(buf)

        }
        get() {
            return try {
                val buf = ByteBuffer.allocate(current.size().toInt())
                current.read(buf)
                buf.flip()

                val id0 = buf.string
                val id1 = buf.string

                if (buf.position() == 0 || id0 != id1) throw RuntimeException()

                id0

            } catch (e: RuntimeException) {
                val new = UUID.randomUUID().toString()
                currentVersion = new
                new
            }
        }

    fun rollWith(offset: Long) {
        rollWith(UUID.randomUUID().toString(), offset)
    }

    fun rollWith(newVersion: String, offset: Long) {
        lastVersionAndOffset = VersionAndOffset(currentVersion, offset)
        currentVersion = newVersion
    }
}

package com.icemelon404.bucket.replication.core

import com.icemelon404.bucket.replication.VersionAndOffset
import com.icemelon404.bucket.replication.OffsetAwareWritable
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class VersionOffsetManager {

    var lastVersionAndOffset: VersionAndOffset? = null
            private set

    var currentVersion: String = UUID.randomUUID().toString()

    fun rollWith(offset: Long) {
        rollWith(UUID.randomUUID().toString(), offset)
    }

    fun rollWith(newVersion: String, offset: Long) {
        lastVersionAndOffset = VersionAndOffset(currentVersion, offset)
        currentVersion = newVersion
    }
}
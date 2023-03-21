package com.icemelon404.bucket.replication

import com.icemelon404.bucket.replication.listener.IdAndOffset
import com.icemelon404.bucket.storage.KeyValue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class VersionOffsetWriter(
    private var version: Long,
    private val writable: OffsetAwareWritable,
) : VersionOffsetManager {

    private var lock = ReentrantReadWriteLock()
    override var versionAndOffset: IdAndOffset
        get() {
            lock.read { return IdAndOffset(version, writable.offset) }
        }
        set(value)  {
            lock.write {
                version = value.id
                writable.truncate(value.offset)
            }
        }

    fun write(keyValues: List<KeyValue>) {
        this.writable.write(keyValues)
    }
}
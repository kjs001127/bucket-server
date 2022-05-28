package com.icemelon404.bucket.replication

import com.icemelon404.bucket.replication.listener.IdAndOffset
import java.util.*

class ReplicationLogHandler(
    private val aof: OffsetAwareWritable,
    private val log: LogRepository
) {

    val currentMaster: IdAndOffset
        get() = IdAndOffset(currentMasterId, aof.offset)

    var lastMaster: IdAndOffset? = log.lastMaster
        set(value) {
            field = value
            log.lastMaster = value
        }

    private var currentMasterId: String = ""
        get() {
            if (field.isNotEmpty())
                return field
            return try {
                log.currentMaster?.takeIf { it.isNotEmpty() } ?: error("null")
            } catch (e: Exception) {
                UUID.randomUUID().toString().also {
                    field = it
                    log.currentMaster = it
                }
            }
        }
        set(value) {
            field = value
            log.currentMaster = value
        }

    fun newMasterId(id: String = UUID.randomUUID().toString()) {
        lastMaster = currentMaster
        currentMasterId = id
    }
}

interface LogRepository {
    var currentMaster: String?
    var lastMaster: IdAndOffset?
}

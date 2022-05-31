package com.icemelon404.bucket.log

import com.icemelon404.bucket.cluster.election.AppendLogIndex
import com.icemelon404.bucket.replication.OffsetAwareWritable
import com.icemelon404.bucket.replication.listener.IdAndOffset
import com.icemelon404.bucket.replication.MasterLog

class LogHandler(
    private val aof: OffsetAwareWritable,
    private val log: LogRepository
) : MasterLog, AppendLogIndex {

    override val currentMaster: IdAndOffset
        get() = IdAndOffset(currentMasterId, aof.offset)

    override var lastMaster: IdAndOffset? = log.lastMaster
        set(value) {
            field = value
            log.lastMaster = value
        }

    override val id: Long
        get() = currentMasterId
    override val offset: Long
        get() = aof.offset

    private var currentMasterId: Long = log.currentMaster?:0
        set(value) {
            field = value
            log.currentMaster = value
        }

    init {
        lastMaster?.let {
            if (it.id > currentMasterId)
                currentMasterId = it.id
        }
    }

    override fun newMasterId(id: Long) {
        lastMaster = currentMaster
        currentMasterId = id
    }
}

interface LogRepository {
    var currentMaster: Long?
    var lastMaster: IdAndOffset?
}


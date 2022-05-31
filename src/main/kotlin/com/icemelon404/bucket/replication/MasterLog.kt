package com.icemelon404.bucket.replication

import com.icemelon404.bucket.replication.listener.IdAndOffset

interface MasterLog {
    val currentMaster: IdAndOffset
    var lastMaster: IdAndOffset?

    fun newMasterId(id: Long)
}

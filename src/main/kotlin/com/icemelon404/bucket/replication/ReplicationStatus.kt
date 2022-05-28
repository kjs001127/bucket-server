package com.icemelon404.bucket.replication

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.listener.ReplicationListener


interface ReplicationStatus : ReplicationListener {
    fun onStart() {}
    fun close() {}
}

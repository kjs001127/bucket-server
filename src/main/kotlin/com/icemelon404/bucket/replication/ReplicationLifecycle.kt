package com.icemelon404.bucket.replication

import com.icemelon404.bucket.replication.api.ReplicationListener
import com.icemelon404.bucket.replication.api.StorageStatus


interface ReplicationLifecycle : ReplicationListener, StorageStatus {
    fun start() {}
    fun close() {}
}

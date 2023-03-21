package com.icemelon404.bucket.replication

import com.icemelon404.bucket.replication.listener.ReplicationListener


interface ReplicationLifecycle : ReplicationListener {
    fun start() {}
    fun close() {}
}

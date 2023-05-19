package com.icemelon404.bucket.replication


interface ReplicationStatus : ReplicationService {
    fun start() {}
    fun close() {}
}

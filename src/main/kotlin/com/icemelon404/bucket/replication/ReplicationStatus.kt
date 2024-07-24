package com.icemelon404.bucket.replication


interface ReplicationStatus : ReplicationStrategy {
    fun start() {}
    fun close() {}
}
